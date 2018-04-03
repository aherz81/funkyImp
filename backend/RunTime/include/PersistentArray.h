/*
 * File:   PersistentArray.h
 * Author: aherz
 *
 * Created on January 23, 2012, 6:10 PM
 */

#ifndef PERSISTENTARRAY_H
#define	PERSISTENTARRAY_H

#include <tbb/recursive_mutex.h>
#include "types.h"

#include "Persistent.h"

namespace funky
{



//parameter to PersistentArray<T,BS,Resizeable or Fixed>
//with Resizeable the size of the array is dynamic, with Fixed it is not
class Resizeable
{
	//map to keep track of existing array sizes
    typedef tbb::concurrent_hash_map<uint32,tbb::atomic<uint32> > ArraySizeMap;
    ArraySizeMap size_map;

    struct Max
    {
        uint32 max;

        ArraySizeMap * map;
        void operator()(ArraySizeMap::range_type range)
        {
            ArraySizeMap::iterator size=range.begin();
            max=(*size).first;

            for (++size; size != range.end(); ++size)
            {
                max=std::max(max,(*size).first);
            }
        }

        Max(ArraySizeMap * map):max(0),map(map)
        {
        }

        Max(Max& x,tbb::split s):max(x.max),map(x.map)
        {}

        void join(const Max& y)
        {
            max=std::max(max,y.max);
        }
    };

public:

    Resizeable(uint32 size)
    {}

	//new size of array in use
    void AddSizeUser(uint32 size)
    {
        ArraySizeMap::accessor acc;

        if(size_map.insert(acc,size))
            acc->second=1;
        else
            acc->second++; //would need read access only

        acc.release();
    }

	//use of array with size ended
    uint32 RemoveSizeUser(uint32 size)
    {
        ArraySizeMap::accessor acc;

        size_map.find(acc,size);
        uint32 num=acc->second;
        if(num==1)
            size_map.erase(acc);
        else
            acc->second--;
        acc.release();
        return num;
    }

    uint32 GetMaxSize()
    {
		//FIXME: is this worth it?
        Max max(&size_map);
        tbb::parallel_reduce(size_map.range(), max, tbb::auto_partitioner() );
        return max.max;
    }
};

class Fixed
{
    tbb::atomic<uint32> users;
    const uint32 size;

public:

    Fixed(uint32 size):size(size)
    {
        users=0;
    }

    inline void AddSizeUser(uint32 size)
    {
		//FIXME: should fail loud
        //assert(size==this->size);
        users++;
    }

    inline uint32 RemoveSizeUser(uint32 size)
    {
		//FIXME: should fail loud??
        //assert(size==this->size);
        return --users;
    }

    inline uint32 GetMaxSize()
    {
        return size;
    }
};

//array subdevided into batches, each batch managed as BinaryVersionTree, so updating inside one batch doesn't require to copy rest of the array
//of course this may only pay back if only small portions of an aray are modified
template<typename T,int BS,typename R> //2^BS is the batch size
class PersistentArray:public iVersionDS
{
    tbb::spin_rw_mutex mutex;    //required for linear array??
    R size;
    //tmp:

//public:
    struct Batch:public BinaryVersionTree
    {
        T items[(1<<BS)] __attribute__ ((aligned (16))); //alligned data for SIMD
        Batch()
        {}
        Batch(VersionTag* vt):BinaryVersionTree(vt)
        {}
    };

    uint32 SIZE;

    tbb::spin_rw_mutex* GetRWMutex()
    {
        return &mutex;
    }

	//array subdevided into batches (deps on array size)
    Batch *data; //align!! //+1??
//tbb operators to apply parallel_for on the blocks of the array

//reduce (part of block)

    template<typename RET,typename F>
    class ReduceVersion
    {
        const uint32 offs;
        Batch* old_block;
        RET& retval;
        F f; //fun to be applied : requires c++Ox to pass lambdas
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            T* __restrict old_values=old_block->items;
            __assume_aligned(old_values, 16); //make sure icpc trys to apply vector ops (we know these are alligned, fails with gcc4.7)

            const int end=range.end();

            for(int i=range.begin();i!=end;++i)
            {
                retval=f(retval,old_values[i]);
            }
        }
        ReduceVersion(uint32 offs,Batch* old_block,RET& retval,F f):
        offs(offs),old_block(old_block),retval(retval),f(f)
        {}
    };

//reduce whole block (no borders)
    template<typename RET,typename F>
    class ReduceVersionWholeBlock
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
        RET& retval;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            for(int b=range.begin();b!=range.end();++b)
            {
                Batch* block=(Batch*)array->data[b].GetVersion(cur);

                const uint32 offs=b<<BS; //was b*(1<<BS)
                //sequential SSE or parellel ????

                T* __restrict old_values =block->items;
                __assume_aligned(old_values, 16);

                //assert(((uint64)(&block->items)%16)==0); //OK!!
                //assert(((uint64)(&new_block->items)%16)==0);
//                    __asm("int $3");

        #pragma vector aligned always
                for(uint32 c=0;c<(1<<BS);c++) //hopefully compiler applies SIMD here
                {
                    retval=f(retval,old_values[c]);
                }

//                    __asm("int $3");
             }


        }
        ReduceVersionWholeBlock(PersistentArray<T,BS,R> *array,VersionTag * cur,RET& retval,F f):
        array(array),cur(cur),retval(retval),f(f)
        {}
    };

//bucket devides an index range into start, end range and complete blocks in between

    template<typename RET,typename F>
    class ReduceBucket
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
        RET& retval;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            uint32 start_block=range.begin()>>BS;
            const uint32 end_block=(range.end())>>BS;
            const uint32 start_offs=range.begin()&(((uint32)-1)>>(32-BS));
            const uint32 end_offs=(range.end())&(((uint32)-1)>>(32-BS));


            if(start_block==end_block)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);

                T* __restrict values=block->items;
                const int offs=start_block*(1<<BS);

                ReduceVersion<RET,F>(offs,block,retval,f)(tbb::blocked_range<uint32>(start_offs, end_offs+1));
                //tbb::parallel_for(tbb::blocked_range<uint32>(start_offs, end_offs+1), ApplyNewVersion<F>(offs,block,new_block), tbb::auto_partitioner());

                return;
            }

            const int dangling_items=(end_offs<((1<<BS)-1));

            if(start_offs)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);

                T* __restrict values=block->items;
                const int offs=start_block*(1<<BS);

                ReduceVersion<RET,F>(offs,block,retval,f)(tbb::blocked_range<uint32>(start_offs, (1<<BS)));
                //tbb::parallel_for(tbb::blocked_range<uint32>(start_offs, (1<<BS)), ApplyNewVersion<F>(offs,block,new_block), tbb::auto_partitioner());

                start_block++;
            }

            //shift ApplyNewVersion to blocks rather than items!
            //const int end=end_block-dangling_items;
            const int end=end_block-dangling_items;
            //FASTER WITHOUT threads:
//#ifndef PARALLEL_ARRAY
            ReduceVersionWholeBlock<RET,F>(array,cur,retval,f)(tbb::blocked_range<uint32>(start_block, end+1));
//#else
//            tbb::parallel_for(tbb::blocked_range<uint32>(start_block, end+1, GRAIN), ReduceVersionWholeBlock<F>(array,cur,vt,retval,f), tbb::simple_partitioner());
//#endif

            if(dangling_items)
            {
                Batch* block=(Batch*)array->data[end_block].GetVersion(cur);
                T* __restrict values=block->items;
                ReduceVersion<RET,F>(end_block*(1<<BS),block,retval,f)(tbb::blocked_range<uint32>(0, end_offs+1));
                //tbb::parallel_for(tbb::blocked_range<uint32>(0, end_offs+1), ApplyNewVersion<F>(end_block*(1<<BS),block,new_block), tbb::auto_partitioner());

            }
        }
        ReduceBucket(PersistentArray<T,BS,R> *array,VersionTag * cur,RET& retval,F f):
        array(array),cur(cur),retval(retval),f(f)
        {}
    };


// apply:
    template<typename F>
    class ApplyNewVersion
    {
        const uint32 offs;
        Batch* old_block;
        Batch* new_block;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            T* __restrict old_values=old_block->items;
            __assume_aligned(old_values, 16);

            T* __restrict new_values=new_block->items;
            __assume_aligned(new_values, 16);
            const int end=range.end();

            for(int i=range.begin();i!=end;++i)
            {
                new_values[i]=f(offs+i,old_values[i]);
            }
        }
        ApplyNewVersion(uint32 offs,Batch* old_block,Batch* new_block,F f):
        offs(offs),old_block(old_block),new_block(new_block),f(f)
        {}
    };

    template<typename F>
    class ApplyNewVersionWholeBlock
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
        VersionTag * vt;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            for(int b=range.begin();b!=range.end();++b)
            {
                Batch* block=(Batch*)array->data[b].GetVersion(cur);
                Batch* iblock=(Batch*)array->data[b].GetVersion(vt);//try to reuse old (free) block
                Batch* new_block;

                if(iblock->GetVersion()==vt)
                {
                    new_block=iblock;
                }
                else
                {
                    new_block=new Batch(vt);
                    iblock->AddVersion(new_block);
                }

                const uint32 offs=b<<BS; //was b*(1<<BS)
                //sequential SSE or parellel ????

                if(sizeof(T)<=sizeof(void*)) //try SSE for small items..also check BS??
                {
                    T* __restrict old_values =block->items;
                    T* __restrict new_values =new_block->items;
                    __assume_aligned(old_values, 16);
                    __assume_aligned(new_values, 16);

                    //assert(((uint64)(&block->items)%16)==0); //OK!!
                    //assert(((uint64)(&new_block->items)%16)==0);
//                    __asm("int $3");

            #pragma vector aligned always
                    for(uint32 c=0;c<(1<<BS);c++) //hopefully compiler applies SIMD here
                    {
                        new_values[c]=f(offs+c,old_values[c]);
                    }

//                    __asm("int $3");
                }
                else tbb::parallel_for(tbb::blocked_range<uint32>(0, 1<<BS), ApplyNewVersion<F>(offs,block,new_block,f), tbb::auto_partitioner());

            }

        }
        ApplyNewVersionWholeBlock(PersistentArray<T,BS,R> *array,VersionTag * cur,VersionTag* vt,F f):
        array(array),cur(cur),vt(vt),f(f)
        {}
    };

    template<typename F>
    class ApplyNewVersionBucket
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
        VersionTag * vt;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            uint32 start_block=range.begin()>>BS;
            const uint32 end_block=(range.end())>>BS;
            const uint32 start_offs=range.begin()&(((uint32)-1)>>(32-BS));
            const uint32 end_offs=(range.end())&(((uint32)-1)>>(32-BS));


            if(start_block==end_block)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);

                Batch* iblock=(Batch*)array->data[start_block].GetVersion(vt);
                Batch* new_block;

                if(iblock->GetVersion()==vt)
                {
                    new_block=iblock;
                }
                else
                {
                    new_block=new Batch(vt);
                    iblock->AddVersion(new_block);
                }

                T* __restrict values=block->items;
                T* __restrict new_values=new_block->items;
                const int offs=start_block*(1<<BS);

                memcpy(new_values,values,sizeof(T)*(1<<BS));

                ApplyNewVersion<F>(offs,block,new_block,f)(tbb::blocked_range<uint32>(start_offs, end_offs+1));
                //tbb::parallel_for(tbb::blocked_range<uint32>(start_offs, end_offs+1), ApplyNewVersion<F>(offs,block,new_block), tbb::auto_partitioner());

                return;
            }

            const int dangling_items=(end_offs<((1<<BS)-1));

            if(start_offs)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);
                Batch* iblock=(Batch*)array->data[start_block].GetVersion(vt);
                Batch* new_block;

                if(iblock->GetVersion()==vt)
                {
                    new_block=iblock;
                }
                else
                {
                    new_block=new Batch(vt);
                    iblock->AddVersion(new_block);
                }

                T* __restrict values=block->items;
                T* __restrict new_values=new_block->items;
                const int offs=start_block*(1<<BS);

                memcpy(new_values,values,sizeof(T)*start_offs);

                ApplyNewVersion<F>(offs,block,new_block,f)(tbb::blocked_range<uint32>(start_offs, (1<<BS)));
                //tbb::parallel_for(tbb::blocked_range<uint32>(start_offs, (1<<BS)), ApplyNewVersion<F>(offs,block,new_block), tbb::auto_partitioner());

                start_block++;
            }

            //shift ApplyNewVersion to blocks rather than items!
            //const int end=end_block-dangling_items;
            const int end=end_block-dangling_items;

            if(end-start_block<PARALLEL_ARRAY)
                ApplyNewVersionWholeBlock<F>(array,cur,vt,f)(tbb::blocked_range<uint32>(start_block, end+1));
            else
                tbb::parallel_for(tbb::blocked_range<uint32>(start_block, end+1), ApplyNewVersionWholeBlock<F>(array,cur,vt,f), tbb::simple_partitioner());

            if(dangling_items)
            {
                Batch* block=(Batch*)array->data[end_block].GetVersion(cur);
                Batch* iblock=(Batch*)array->data[end_block].GetVersion(vt);
                Batch* new_block;

                if(iblock->GetVersion()==vt)
                {
                    new_block=iblock;
                }
                else
                {
                    new_block=new Batch(vt);
                    iblock->AddVersion(new_block);
                }

                T* __restrict values=block->items;
                T* __restrict new_values=new_block->items;
                ApplyNewVersion<F>(end_block*(1<<BS),block,new_block,f)(tbb::blocked_range<uint32>(0, end_offs+1));
                //tbb::parallel_for(tbb::blocked_range<uint32>(0, end_offs+1), ApplyNewVersion<F>(end_block*(1<<BS),block,new_block), tbb::auto_partitioner());

                memcpy(new_values+end_offs+1,values+end_offs+1,sizeof(T)*((1<<BS)-end_offs-1));
            }
        }
        ApplyNewVersionBucket(PersistentArray<T,BS,R> *array,VersionTag * cur,VersionTag* vt,F f):
        array(array),cur(cur),vt(vt),f(f)
        {}
    };

//overwrites old data:

    template<typename F>
    class Apply
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            uint32 start_block=range.begin()>>BS;
            const uint32 end_block=(range.end())>>BS;
            const uint32 start_offs=range.begin()&(((uint32)-1)>>(32-BS));
            const uint32 end_offs=(range.end())&(((uint32)-1)>>(32-BS));
            const int dangling_items=(end_offs<(1<<BS)-1);

            if(start_block==end_block)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);
                T* values=block->items;

                __assume_aligned(values, 16);

                const int offs=start_block*(1<<BS);
                for(int s=start_offs;s<=end_offs;s++)
                {
                    values[s]=f(offs+s,values[s]);
                }
                return;
            }

            if(start_offs)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);
                T* values=block->items;

                __assume_aligned(values, 16);

                const int offs=start_block*(1<<BS);

                for(int s=start_offs;s<(1<<BS);s++)
                {
                    values[s]=f(offs+s,values[s]);
                }
                start_block++;
            }

            const int end=end_block-dangling_items;
            for(int b=start_block;b<=end;b++)
            {
                Batch* block=(Batch*)array->data[b].GetVersion(cur);
                T* values=block->items;

                __assume_aligned(values, 16);

                for(int s=0;s<(1<<BS);s++)
                {
                    values[s]=f(b*(1<<BS)+s,values[s]); //simd??
                }
            }

            if(dangling_items)
            {
                Batch* block=(Batch*)array->data[end_block].GetVersion(cur);
                T* values=block->items;
                __assume_aligned(values, 16);
                for(int s=0;s<=end_offs;s++)
                {
                    values[s]=f(end_block*(1<<BS)+s,values[s]);
                }
            }
        }
        Apply(PersistentArray<T,BS,R> *array,VersionTag * cur,F f):
        array(array),cur(cur),f(f)
        {}
    };


//produces no output array:

    template<typename F>
    class ApplyVoid
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
        F f;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            //printf("process range [%d,%d[\n",range.begin(),range.end());
            uint32 start_block=range.begin()>>BS;
            const uint32 end_block=(range.end())>>BS;
            const uint32 start_offs=range.begin()&(((uint32)-1)>>(32-BS));
            const uint32 end_offs=(range.end())&(((uint32)-1)>>(32-BS));
            const int dangling_items=(end_offs<(1<<BS)-1);

            if(start_block==end_block)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);
                T* values=block->items;
                __assume_aligned(values, 16);
                int offs=start_block*(1<<BS);
                for(int s=start_offs;s<=end_offs;s++)
                {
                    f(offs+s,values[s]);
                }
                return;
            }

            if(start_offs)
            {
                Batch* block=(Batch*)array->data[start_block].GetVersion(cur);
                T* values=block->items;
                __assume_aligned(values, 16);
                int offs=start_block*(1<<BS);
                for(int s=start_offs;s<(1<<BS);s++)
                {
                    f(offs+s,values[s]);
                }
                start_block++;
            }

            const int end=end_block-dangling_items;
            for(int b=start_block;b<=end;b++)
            {
                Batch* block=(Batch*)array->data[b].GetVersion(cur);
                T* values=block->items;
                __assume_aligned(values, 16);

                for(int s=0;s<(1<<BS);s++)
                {
                    f(b*(1<<BS)+s,values[s]); //simd??
                }
            }

            if(dangling_items)
            {
                Batch* block=(Batch*)array->data[end_block].GetVersion(cur);
                T* values=block->items;
                __assume_aligned(values, 16);

                for(int s=0;s<=end_offs;s++)
                {
                    f(end_block*(1<<BS)+s,values[s]);
                }
            }
        }
        ApplyVoid(PersistentArray<T,BS,R> *array,VersionTag * cur,F f):
        array(array),cur(cur),f(f)
        {}
    };

    class Setup
    {
        PersistentArray<T,BS,R>* array;
        VersionTag * cur;
    public:
        void operator()(const tbb::blocked_range<uint32>& range) const
        {
            for(int i=range.begin();i<range.end();i++)
            {
                new (&array->data[i]) Batch();
                array->data[i].SetVersion(cur);
            }
        }
        Setup(PersistentArray<T,BS,R> *array,VersionTag* vt):
        array(array),cur(vt)
        {}
    };

    void SetupRange(uint32 from,uint32 to,VersionTag* vt)
    {
        //Setup(this,vt)(tbb::blocked_range<uint32>(from, to));
		//create batches for array
        tbb::parallel_for(tbb::blocked_range<uint32>(from, to), Setup(this,vt), tbb::auto_partitioner());
    }

    uint32 GetSize()
    {
        return SIZE*(1<<BS);
    }

public:

    //must allocate dynamically!!!!
	//a version allows to access a specific version of the array:
    class Version : VersionTag //is deriv from gc_cleanup
    {
        //VersionTag *tag;
        PersistentArray<T,BS,R>* array; //contains many versions
        uint32 COUNT;//array size

        //CONSTRUCTORS are never to be USED directly, use the create functions below!
        Version(Version* vt,PersistentArray<T,BS,R>* array,uint32 COUNT):VersionTag(*vt,array),array(array),COUNT(COUNT)
        {
            array->SetupVersion(COUNT);
        }

        Version(PersistentArray<T,BS,R>* array,uint32 COUNT):VersionTag(),array(array),COUNT(COUNT)
        {
            array->Init(this);
            array->SetupVersion(COUNT);
        }

        Version(){}

        ~Version()//should be called by GC when version becomes unreachable
        {
			//remove size user, kills array when none is left
            array->Resize(COUNT,0);
            if(array->GetSize())
                Delete();
            else
                Destroy();
        }

		//create new version of array
        Version* Create(Version* vt,PersistentArray<T,BS,R>* array,uint32 COUNT)
        {
            Version* nv=(Version)VersionTag::Create(*vt,array); //see if we can reuse a Version
            if(!nv)
                nv=new Version(vt,array,COUNT);//nope, must create new one
            else
            {
                array->SetupVersion(COUNT);//reuse nv
                nv->array=array;
                nv->COUNT=COUNT;
            }

            return nv;
        }

        //same as above, just without COUNT
        Version* Create(Version* vt,PersistentArray<T,BS,R>* array)
        {
            Version* nv=(Version)VersionTag::Create(*vt,array);
            if(!nv)
                nv=new Version(vt,array,array->GetSize());
            else
            {
                array->SetupVersion(array->GetSize());
                nv->array=array;
                nv->COUNT=array->GetSize();
            }

            return nv;
        }

    public://public interface

        operator VersionTag*()
        {
            return this;
        }

        Version* Create(PersistentArray<T,BS,R>* array,uint32 COUNT)
        {
            return new Version(array,COUNT);
        }

        Version* Create(PersistentArray<T,BS,R>* array)
        {
            return new Version(array,array->GetSize());
        }

        uint32 GetCount()
        {
            return COUNT;
        }

        //multi dim resize/count
        Version* SetCount(uint32 count)
        {
            if(count==COUNT)
                return this;

            //resize array
            array->Resize(COUNT,count,false);

            return Create(this,array,count);
        }

        template <typename F>
        Version* mapNewVersion(F f,uint32 from=0, uint32 to=0)
        {
            if(0==to)
                to=GetCount()-1;
            else
                assert(to<GetCount());

            //return complete new array if there is no sharing?

            tbb::spin_rw_mutex::scoped_lock lock(array->mutex,false);
            Version* new_version=Create(this,array);

            ApplyNewVersionBucket<F>(array, *this,(VersionTag*)*new_version,f)(tbb::blocked_range<uint32>(from, to));
            //need custom partitioner that does not partition across blocks!
            //tbb::parallel_for(tbb::blocked_range<uint32>(from, to), ApplyNewVersion<F>(array,f, tag,new_version), tbb::simple_partitioner());
            return new_version;
        }

        template <typename F>
        T reduce(F f,uint32 from=0, uint32 to=0)
        {
            if(0==to)
                to=GetCount()-1;
            else
                assert(to<GetCount());

			T init=access(from);
            T &retval=init;

            tbb::spin_rw_mutex::scoped_lock lock(array->mutex,false);


            ReduceBucket<T,F>(array,retval,f)(tbb::blocked_range<uint32>(from+1, to));
            //need custom partitioner that does not partition across blocks!
            //tbb::parallel_for(tbb::blocked_range<uint32>(from, to), ApplyNewVersion<F>(array,f, new_version), tbb::simple_partitioner());
            return retval;
        }

        template <typename F>
        void map(F f,uint32 from=0, uint32 to=0)
        {
            if(0==to)
                to=GetCount()-1;
            else
                assert(to<GetCount());

            tbb::spin_rw_mutex::scoped_lock lock(array->mutex,false);

            if(to-from<PARALLEL_ARRAY)
                Apply<F>(array,*this, f)(tbb::blocked_range<uint32>(from, to));
            else
                tbb::parallel_for(tbb::blocked_range<uint32>(from, to), Apply<F>(array, *this, f), tbb::auto_partitioner());
        }

        template <typename F>
        void mapVoid(F f,uint32 from=0, uint32 to=0)
        {
            if(0==to)
                to=GetCount()-1;
            else
                assert(to<GetCount());
            tbb::spin_rw_mutex::scoped_lock lock(array->mutex,false);
            ApplyVoid<F>(array, *this, f)(tbb::blocked_range<uint32>(from, to));
            //tbb::parallel_for(tbb::blocked_range<uint32>(from, to), ApplyVoid<F>(array, tag, f), tbb::auto_partitioner());
        }

        T access(uint32 index) const
        {
            tbb::spin_rw_mutex::scoped_lock lock(array->mutex,false);
            Batch* block=(Batch*)array->data[index>>BS].GetVersion(*this);
            return block->items[index&(((uint32)-1)>>BS)];
        }
    };

    void Resize(uint32 old_count,uint32 count,bool del_old=true)
    {
        uint32 NEW_SIZE=count/(1<<BS)+(bool)(count%(1<<BS));
        uint32 OLD_SIZE=old_count/(1<<BS)+(bool)(old_count%(1<<BS));

        if(count&&del_old)
            size.AddSizeUser(NEW_SIZE);

        uint32 num;
        if(del_old)
            num=size.RemoveSizeUser(OLD_SIZE);
        else
            num=2;

        if(OLD_SIZE>NEW_SIZE)
        {
            if(num==1)//last with this size??
            {
                //get max size
                tbb::spin_rw_mutex::scoped_lock lock(mutex,true);

                uint32 max=size.GetMaxSize();

                if(max<SIZE)
                {
                    for(uint32 i=max;i<SIZE;i++)
                    {
                        data[i].DelChilds();
                    }

                    if(!max)
                    {
                        scalable_aligned_free(data);
                        data=NULL; //DELETE this DS??
                        delete this;
                    }
                    else
                        data=(Batch*)scalable_aligned_realloc(data,sizeof(Batch)*max,16);
                    SIZE=max;
                }
            }
        }
        else if(NEW_SIZE>OLD_SIZE)
        {
            tbb::spin_rw_mutex::scoped_lock lock(mutex,true);
            data=(Batch*)scalable_aligned_realloc(data,sizeof(Batch)*NEW_SIZE,16);
            SetupRange(SIZE,NEW_SIZE,data[0].GetVersion());
            SIZE=NEW_SIZE;
        }

    }

    PersistentArray(uint32 count=4):size(count/(1<<BS)+(bool)(count%(1<<BS)))
    {

        SIZE=count/(1<<BS)+(bool)(count%(1<<BS));

        data=(Batch*)scalable_aligned_malloc(sizeof(Batch)*SIZE,16);

        //data=(Batch*)malloc(sizeof(T)*SIZE);
    }

private:

    void SetupVersion(uint32 count)
    {
        if(count)
        {
            uint32 VSIZE=count/(1<<BS)+(bool)(count%(1<<BS));
            size.AddSizeUser(VSIZE);
        }
    }

    void Init(VersionTag* vt)
    {
        SetupRange(0,SIZE,vt);
    }

};

}

#endif	/* PERSISTENTARRAY_H */

