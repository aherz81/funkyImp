/*
 * File:   LinearArray.h
 * Author: aherz
 *
 * Created on January 23, 2012, 6:10 PM
 */

#ifndef LINEARARRAY_H
#define	LINEARARRAY_H

#include "types.h"

#include <tbb/spin_mutex.h>
#include <tbb/parallel_for.h>
#include <tbb/parallel_reduce.h>
#include <tbb/blocked_range.h>
#include <tbb/concurrent_hash_map.h>
#include <tbb/spin_rw_mutex.h>
#include <tbb/cache_aligned_allocator.h>
#include <tbb/scalable_allocator.h>

#include <stdarg.h>
#define SSE_ALIGNMENT 32

#define GC_ARRAY //undef this to disable gc on arrays

//currently (14.11.2012), intel's icpc is the only compiler that offers a working alignment hint
//g++-4.7 has a different hint that doesn't work in the deeply nested setting we're using it
//so we define the hint as a NOP for non intel compilers
#ifdef __INTEL_COMPILER
#define __assume_aligned(v,a) __assume_aligned(v,a);
#else
#define __assume_aligned(v,a) //v=__builtin_assume_aligned(v, a)
#endif

#ifdef ARRAY_DEBUG
#define PARALLEL_ARRAY 2^32
#else
#define PARALLEL_ARRAY 64
#endif

#include <vector>

namespace funky {

    /*
     * same interface as PersistentArray but this one is non persistant (unique arrays)
     */
    template <class T>
    struct RemoveConst {
        typedef T type;
    };

    template <class T>
    struct RemoveConst<const T> {
        typedef T type;
    };

    //make g++ shut up about taking address of temporary (used when applying operators on arrays)

    template<typename T> const T* rvalue_address(const T& in) {
        return &in;
    }

#define BLOCK 256

    template<typename T = int>
    class LinearArray : public boehmgc::gc {
        uint32 SIZE;
        bool ATOMIC;
    public:
        T *items;

        template<typename RET, typename F>
        class ReduceVersionBlock {
            LinearArray<T>* array;
            RET& retval;
            F f;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                RET red __attribute__((aligned(SSE_ALIGNMENT))) = retval;

                const uint32 blocks = (end - start) / BLOCK;

#pragma vector always
                for (uint32 block = 0; block < blocks; block++) {
#pragma vector aligned
#pragma simd vectorlength(BLOCK) //enables unrolling...but doesn't give more speed on nehalem
                    //#pragma unroll BLOCK//do not set to specific value!
                    for (uint32 i = 0; i < BLOCK; ++i) {
                        red = f(block * BLOCK + start + i, red);
                    }
                }

                //handle rest
#pragma vector always
                for (uint32 i = start + (blocks + 1) * BLOCK; i < end; i++)
                    red = f(i, red);

                retval = red;
            }

            ReduceVersionBlock(LinearArray<T>* array, RET& retval, F f) :
            array(array), retval(retval), f(f) {
            }
        };

        template<typename RET, typename F>
        class ReduceVersion {
            LinearArray<T>* array;
            RET& retval;
            F f;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                RET red __attribute__((aligned(SSE_ALIGNMENT))) = retval;

#pragma vector always
                for (uint32 i = start; i < end; ++i) {
                    red = f(i, red);
                }

                retval = red;
            }

            ReduceVersion(LinearArray<T>* array, RET& retval, F f) :
            array(array), retval(retval), f(f) {
            }
        };

        template<typename RET, typename F>
        class ReduceVersionVoid {
            LinearArray<T>* array;
            RET& retval;
            F f;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                RET red __attribute__((aligned(SSE_ALIGNMENT))) = retval;

#pragma vector always
                for (uint32 i = start; i < end; ++i) {
                    f(i, red);
                }

                retval = red;
            }

            ReduceVersionVoid(LinearArray<T>* array, RET& retval, F f) :
            array(array), retval(retval), f(f) {
            }
        };

        template<typename F, typename FJ>
        class ParReduceVersion {
            LinearArray<T>* array;
            F f;
            FJ fj;
        public:
            const T init;
            T value;

            void join(ParReduceVersion& prv) {
                value = fj(value, prv.value);
            }

            ParReduceVersion(ParReduceVersion& prv, tbb::split sp) : array(prv.array), f(prv.f), fj(prv.fj), value(0), init(0) {
            }

            void operator()(const tbb::blocked_range<uint32>& range) {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                T red __attribute__((aligned(SSE_ALIGNMENT))) = value;

#pragma vector always
                for (uint32 i = start; i < end; ++i) {
                    red = f(i, red);
                }

                value = red;
            }

            ParReduceVersion(LinearArray<T>* array, T init, F f, FJ fj) :
            array(array), value(init), f(f), fj(fj), init(init) {
            }
        };

        template<typename F>
        class Apply {
            LinearArray<T>* array;
            F f;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                T* values = array->items;

                __assume_aligned(values, SSE_ALIGNMENT);

#pragma vector always
#pragma ivdep
                for (uint32 s = start; s < end; s++) {
                    values[s] = f(s);
                }
            }

            Apply(LinearArray<T>* array, F f) :
            array(array), f(f) {
            }
        };

        template<typename F>
        class ApplyNew //no benefit from ApplyNewBlocked
        {
            LinearArray<T>* array;
            LinearArray<T>* newarray;
            F f;
            uint32 offset;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                T* __restrict__ newvalues = newarray->items;

                __assume_aligned(newvalues, SSE_ALIGNMENT);

#pragma vector always
                for (uint32 s = start; s < end; s++) {
                    newvalues[s + offset] = (typename RemoveConst<T>::type)f(s);
                }
            }

            ApplyNew(LinearArray<T> *array, LinearArray<T> *newarray, F f, uint32 offset) :
            array(array), newarray(newarray), f(f), offset(offset) {
            }
        };

        template<typename F>
        class ApplyFor {
            F f;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

#pragma vector always
                for (uint32 s = start; s < end; s++) {
                    f(s);
                }
            }

            ApplyFor(F f) : f(f) {
            }
        };

        template<typename F>
        class ApplyVoid {
            LinearArray<T>* array;
            F f;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                //printf("process range [%d,%d[\n",range.begin(),range.end());
                const uint32 start = range.begin();
                const uint32 end = range.end();

#pragma vector always
                for (uint32 s = start; s < end; s++) {
                    f(s);
                }
            }

            ApplyVoid(LinearArray<T>* array, F f) :
            array(array), f(f) {
            }
        };

        uint32 GetSize() const {
            return SIZE;
        }

    public:

        //1D access!

        class Version : public boehmgc::gc //_cleanup
        {
        public:
            LinearArray<T>* array;
            uint32 COUNT;
            std::vector<uint32> dims;

        public:

            void dump(char* format) {
                for (int i = 0; i < COUNT; i++)
                    printf(format, access(i), i);
            }

            inline uint32 getDim(uint32 dim) const {
                return dims[dim];
            }

            uint32 getSizeDim() const {
                uint32 scale = 1;
                for (int i = 0; i < dims.size(); i++)
                    scale *= dims[i];

                return scale;
            }

            inline uint32 getOffsetDim(uint32 i1) const {
                return i1;
            }

            inline uint32 getOffsetDim(uint32 i1, uint32 i2, ...) const {
                uint32 scale = dims[1];

                uint32 index = i1 + scale*i2;

                va_list vl;
                va_start(vl, i2); //take care, cannot use i1 here if i2 is present!!

                for (int d = 2; d < dims.size(); d++) {
                    scale *= dims[d];
                    index += va_arg(vl, uint32) * scale;
                }

                va_end(vl);

                return index;
            }

            inline T& __restrict__ accessDim(uint32 index) const {
                return access(index);
            }

            //INDICES IN REVERTED ORDER!!

            inline T& __restrict__ accessDim(uint32 i1, uint32 i2, ...) const {
                uint32 scale = dims[1];

                uint32 index = i1 + scale*i2;

                va_list vl;
                va_start(vl, i2); //take care, cannot use i1 here if i2 is present!!

                for (int d = 2; d < dims.size(); d++) {
                    scale *= dims[d];
                    index += va_arg(vl, uint32) * scale;
                }

                va_end(vl);

                return access(index);
            }

            //static access:

            inline T& __restrict__ access(uint32 index) const {
                T* __restrict__ items = array->items;
                __assume_aligned(items, SSE_ALIGNMENT);
                return items[index];
            }

            inline T& __restrict__ get(uint32 index) const {
                T* __restrict__ items = array->items;
                __assume_aligned(items, SSE_ALIGNMENT);
                return items[index];
            }

            inline Version(LinearArray<T>* iarray, uint32 dim = 0, ...) : array(iarray), COUNT(iarray->GetSize()) {
                if (dim > 0) {
                    dims.reserve(dim);
                    va_list vl;
                    va_start(vl, dim);
                    for (uint32 i = 0; i < dim; i++) {
                        dims.push_back(va_arg(vl, uint32));
                    }
                    va_end(vl);
                    assert(COUNT == getSizeDim());
                } else {
                    dims.push_back(COUNT);
                }
            }

            Version() {
            }

            ~Version() {
                //array->DelVersion(tag);

                //array->Resize(COUNT, 0);
                fprintf(stderr, "kill array %x", this);
                fflush(stderr);
                //__asm__("int $3");
            }

            inline uint32 GetCount() const {
                return COUNT;
            }

            //multi dim resize/count

                        /*
                                    void Split(Version* a,long pos,Version* b)
                                    {

                                    }
             */
            Version* SetCount(uint32 count, uint32 dim = 0, ...) {
                if (count == COUNT)
                    return this;

                //resize array
                array->Resize(COUNT, count, false);

                COUNT = count;

                if (dim > 0) {
                    va_list vl;
                    va_start(vl, dim);
                    for (uint32 i = 0; i < dim; i++) {
                        dims[i] = (va_arg(vl, uint32));
                    }
                    va_end(vl);
                    assert(COUNT == getSizeDim());
                } else
                    dims[0] = COUNT;

                return this;
            }

            inline LinearArray<T>* GetArray() const {
                return array;
            }

            inline T* toNative() const {
                return array->items;
            }

            template <typename F, typename G>
            inline G reduce_seq(F f, G init, uint32 from, uint32 to) const {
                return reduce(f, init, from, to);
            }

            template <typename F, typename G>
            inline G reduce_void(F f, G init, uint32 from, uint32 to) const {
                if (from == to)
                    return init;

                G &retval = init;

                ReduceVersionVoid<G, F > (array, retval, f)(tbb::blocked_range<uint32 > (from, to));

                return retval;
            }

            //sequential reduce

            template <typename F, typename G>
            inline G reduce(F f, G init, uint32 from, uint32 to) const {
                if (from == to)
                    return init;

                G &retval = init;

                if (to - from >= BLOCK)
                    ReduceVersionBlock<G, F > (array, retval, f)(tbb::blocked_range<uint32 > (from, to));
                else
                    ReduceVersion<G, F > (array, retval, f)(tbb::blocked_range<uint32 > (from, to));

                return retval;
            }
/*
            template <typename F, typename G>
            inline G reduce_seq(F f, G init, uint32 from, uint32 to) const {
                return reduce(f, init, from, to);
            }
*/ 
            //parallel reduce
/*
            template <typename F, typename G>
            inline G reduce(F f, G init, uint32 from, uint32 to) const {
                if (from == to)
                    return init;

                //if ((sizeof (T) <= sizeof (void*) || to - from < PARALLEL_ARRAY * 100))
                return reduce(f, init, from, to);

                //FIXME: ParReduceVersion assumes reduction over T, but reduction may be over higher dimensional space...
                
                                                ParReduceVersion<F, FJ> result(array, init, f, fj);

                                                tbb::parallel_reduce(tbb::blocked_range<uint32 > (from, to, PARALLEL_ARRAY / sizeof (T)), result, tbb::auto_partitioner());

                                                return result.value;
                
            }
*/
            inline Version* getNewVersion() const {
                LinearArray<T> * newarray = new LinearArray<T > (array->SIZE, array->ATOMIC);
                Version *new_version = new Version(newarray);
                return new_version;
            }

            template <typename F>
            inline Version* mapNewVersion_seq(F f, uint32 from, uint32 to, uint32 offset, Version *new_version) const {
                if (from == to)
                    return new_version;

                //return complete new array if there is no sharing?
                if (new_version == NULL)
                    new_version = getNewVersion();

                ApplyNew<F > (array, new_version->array, f, offset)(tbb::blocked_range<uint32 > (from, to));

                return new_version;
            }

            template <typename F>
            inline Version* mapNewVersion(F f, uint32 from, uint32 to, uint32 offset, Version *new_version) const {
                if (from == to)
                    return new_version;

                //return complete new array if there is no sharing?
                if (new_version == NULL)
                    new_version = getNewVersion();

                if (sizeof (T) <= sizeof (void*) && to - from < PARALLEL_ARRAY) //non primitive types will not be vectorized
                    ApplyNew<F > (array, new_version->array, f, offset)(tbb::blocked_range<uint32 > (from, to));
                else
                    tbb::parallel_for(tbb::blocked_range<uint32 > (from, to), ApplyNew<F > (array, new_version->array, f, offset), tbb::auto_partitioner());

                return new_version;
            }

            template <typename F>
            inline static void forall(F f, uint32 from, uint32 to) //parallelize outer loops
            {
                if (from == to)
                    return;

                if (sizeof (T) <= sizeof (void*) && to - from < PARALLEL_ARRAY)
                    (ApplyFor<F > (f))(tbb::blocked_range<uint32 > (from, to));
                else
                    tbb::parallel_for(tbb::blocked_range<uint32 > (from, to), ApplyFor<F > (f), tbb::auto_partitioner());
            }

            template <typename F>
            inline static void forceall(F f, uint32 from, uint32 to) //parallelize outer loops
            {
                if (from == to)
                    return;
                //(ApplyFor<F > (f))(tbb::blocked_range<uint32 > (from, to));
                tbb::parallel_for(tbb::blocked_range<uint32 > (from, to), ApplyFor<F > (f));
            }

            template <typename F>
            inline Version* map_seq(F f, uint32 from, uint32 to) {
                if (from == to)
                    return this;

                Apply<F > (array, f)(tbb::blocked_range<uint32 > (from, to));

                return this;
            }

            template <typename F>
            inline Version* map(F f, uint32 from, uint32 to) {
                if (from == to)
                    return this;

                if (sizeof (T) <= sizeof (void*) && to - from < PARALLEL_ARRAY)
                    Apply<F > (array, f)(tbb::blocked_range<uint32 > (from, to));
                else
                    tbb::parallel_for(tbb::blocked_range<uint32 > (from, to), Apply<F > (array, f), tbb::auto_partitioner());

                return this;
            }

            template <typename F>
            inline void mapVoid_seq(F f, uint32 from, uint32 to) {
                if (from == to)
                    return;

                ApplyVoid<F > (array, f)(tbb::blocked_range<uint32 > (from, to));
            }

            template <typename F>
            inline void mapVoid(F f, uint32 from, uint32 to) {
                if (from == to)
                    return;

                if (sizeof (T) <= sizeof (void*) && to - from < PARALLEL_ARRAY)
                    ApplyVoid<F > (array, f)(tbb::blocked_range<uint32 > (from, to));
                else
                    tbb::parallel_for(tbb::blocked_range<uint32 > (from, to), ApplyVoid<F > (array, f), tbb::auto_partitioner());
            }

        };

        void Resize(uint32 old_count, uint32 count, bool del_old = true) {
            uint32 NEW_SIZE = count;
            uint32 OLD_SIZE = old_count;


            if (OLD_SIZE > NEW_SIZE) {
                uint32 max = NEW_SIZE;

                if (max < SIZE) {
                    if (!max) {
#ifdef ARRAY_DEBUG
                        fprintf(stderr, "delete array\n");
#endif
                        //scalable_aligned_free(items);
                        items = NULL; //DELETE this DS??
                    } else if (max < OLD_SIZE / 2) // resize only if here is some room to be gained (not just for one item)
                    {

                        //assert(false);
#ifdef ARRAY_DEBUG
                        fprintf(stderr, "realloc array\n");
#endif
                        T* newItems;
#ifdef GC_ARRAY
                        if (!ATOMIC)
                            newItems = (T*) GC_malloc(sizeof (T) * max);
                        else
                            newItems = (T*) GC_malloc_atomic_ignore_off_page(sizeof (T) * max);
#else
                        newItems = (T*) scalable_aligned_malloc(sizeof (T) * max, SSE_ALIGNMENT);
#endif

                        memcpy(newItems, items, sizeof (T) * SIZE);

                        items = newItems;


                        //						items = (T*) scalable_aligned_realloc(items, sizeof (T) * max, SSE_ALIGNMENT);
                        SIZE = max;
                    }
                }

            } else if (NEW_SIZE > OLD_SIZE) {

#ifdef ARRAY_DEBUG
                fprintf(stderr, "realloc array\n");
#endif

                T* newItems;
#ifdef GC_ARRAY
                if (!ATOMIC)
                    newItems = (T*) GC_malloc(sizeof (T) * NEW_SIZE);
                else
                    newItems = (T*) GC_malloc_atomic_ignore_off_page(sizeof (T) * NEW_SIZE);
#else
                newItems = (T*) scalable_aligned_malloc(sizeof (T) * NEW_SIZE, SSE_ALIGNMENT);
#endif                                

                memcpy(newItems, items, sizeof (T) * SIZE);

                items = newItems;
                //				items = (T*) scalable_aligned_realloc(items, sizeof (T) * NEW_SIZE, SSE_ALIGNMENT);
                SIZE = NEW_SIZE;
            }
        }

        inline LinearArray(uint32 count = 4, bool atomic = false) {
            ATOMIC = atomic;
            SIZE = count;
#ifdef GC_ARRAY
            if (!atomic) {
                //printf("warning: NON_ATOMIC_ALLOC\n");
                items = (T*) GC_malloc(sizeof (T) * SIZE); //clear automatically
            } else {
                items = (T*) GC_malloc_atomic_ignore_off_page(sizeof (T) * SIZE); //doesn't clear!!
                memset(items, 0, count * sizeof (T)); //need only clear if a under spec dom is used
            }
#else
            printf("WARNING: ARRAY NOT GCed!\n");
            items = (T*) scalable_aligned_malloc(sizeof (T) * SIZE, SSE_ALIGNMENT);
#endif


            //items = (T*) scalable_aligned_malloc(sizeof (T) * SIZE, SSE_ALIGNMENT);
        }

        class Init {
            typename RemoveConst<T>::type* to;
            typename RemoveConst<T>::type* from;
        public:

            void operator()(const tbb::blocked_range<uint32>& range) const {
                const uint32 start = range.begin();
                const uint32 end = range.end();

                __assume_aligned(to, SSE_ALIGNMENT);

#pragma vector always
                for (uint32 s = start; s < end; s++) {
                    to[s] = from[s];
                }
            }

            Init(typename RemoveConst<T>::type* to, typename RemoveConst<T>::type* from) :
            to(to), from(from) {
            }
        };

        LinearArray(uint32 count, typename RemoveConst<T>::type* data, bool atomic = false) {
            SIZE = count;
            ATOMIC = atomic;

            if ((uint64) data % SSE_ALIGNMENT) {
                //alloc and init non-const data:

                //				typename RemoveConst<T>::type* preinit=static_cast<typename RemoveConst<T>::type*>(scalable_aligned_malloc(sizeof (T) * SIZE, SSE_ALIGNMENT));

                typename RemoveConst<T>::type* preinit;
#ifdef GC_ARRAY
                if (!atomic)
                    preinit = static_cast<typename RemoveConst<T>::type*> (GC_malloc(sizeof (T) * SIZE));
                else
                    preinit = static_cast<typename RemoveConst<T>::type*> (GC_malloc_atomic_ignore_off_page(sizeof (T) * SIZE));
#else
                preinit = static_cast<typename RemoveConst<T>::type*> (scalable_aligned_malloc(sizeof (T) * SIZE, SSE_ALIGNMENT));
#endif

                if (sizeof (T) <= sizeof (void*) && count < PARALLEL_ARRAY)
                    Init(data, preinit)(tbb::blocked_range<uint32 > (0, count));
                else
                    tbb::parallel_for(tbb::blocked_range<uint32 > (0, count), Init(data, preinit), tbb::auto_partitioner());

                //now make data const
                items = (T*) preinit;

            } else
                items = data;

        }

        static T* alloc(uint32 count, bool atomic = false) {
#ifdef GC_ARRAY
            if (!atomic)
                return (T*) GC_malloc(sizeof (T) * count);
            else
                return (T*) GC_malloc_atomic_ignore_off_page(sizeof (T) * count);
#else
            return (T*) scalable_aligned_malloc(sizeof (T) * count, SSE_ALIGNMENT);
#endif
            //return (T*) scalable_aligned_malloc(sizeof (T) * count, SSE_ALIGNMENT);
        }

        static uint32 getDimSize(uint32 dim, ...) {
            uint32 size = 1;
            va_list vl;
            va_start(vl, dim);
            for (uint32 i = 0; i < dim; i++) {
                size *= (va_arg(vl, uint32));
            }
            va_end(vl);
            return size;
        }
    };

    template <typename V>
    struct Projection {
        uint32 offset; //make this offset?
        typename LinearArray<V>::Version* object;

        //lot's of "interesting" constructors

        Projection(typename LinearArray<V>::Version* object, uint32 offset = 0) : object(object), offset(offset) {
        }

        Projection(Projection p, uint32 offset) : object(p.object), offset(p.offset + offset) {
        }

        Projection(Projection& p) : object(p.object), offset(p.offset) {
        }

        Projection(const Projection& p) : object(p.object), offset(p.offset) {
        }
/*        
        Projection(Projection&& p) : object(p.object), offset(p.offset) {
        }

        Projection(const Projection&& p) : object(p.object), offset(p.offset) {
        }
*/        
        
/*        
        Projection operator=(Projection& p)//actually move data
        {
            memcpy(object->toNative(),p.object->toNative(),sizeof(V)*p.object->GetCount());
            offset=p.offset;
        }

        Projection operator=(const Projection& p)//actually move data
        {
            memcpy(object->toNative(),p.object->toNative(),sizeof(V)*p.object->GetCount());
            offset=p.offset;
        }
*/
        void dump(char* format) {
            object->dump(format);
        }

        operator typename LinearArray<V>::Version* () const //allow cast to plain array??
        {
            return object;
        }

        template <typename ... Ts>
        inline V& __restrict__ accessDim(uint32 index, Ts ... ts) const {
            return object->access(offset + object->getOffsetDim(index, ts ...));
        }

        inline V& __restrict__ accessDim(uint32 index) const {
            return object->access(offset + object->getOffsetDim(index));
        }

        //perfect forwarding requiring c++0x

        template <typename ... Ts>
        inline uint32 getOffsetDim(uint32 index, Ts ... ts) const {
            return offset + object->getOffsetDim(index, ts ...);
        }

        inline uint32 getOffsetDim(uint32 index) const {
            return offset + index;
        }

        inline const V& __restrict__ access(uint32 index) const {
            return object->access(offset + index);
        }

        inline V& __restrict__ get(uint32 index) const {
            return object->get(offset + index);
        }

        inline uint32 getDim(uint32 dim) const {
            return object->getDim(dim);
        }

    };


}

#endif	/* LinearArray_H */

