/*
 * File:   Persistent.h
 * Author: aherz
 *
 * Created on January 4, 2012, 5:06 PM
 */

#ifndef PERSISTENT_H
#define	PERSISTENT_H

#include <tbb/spin_mutex.h>
#include <tbb/parallel_for.h>
#include <tbb/parallel_reduce.h>
#include <tbb/blocked_range.h>
#include <tbb/concurrent_hash_map.h>
#include <tbb/spin_rw_mutex.h>
#include <tbb/cache_aligned_allocator.h>
#include <tbb/scalable_allocator.h>

#include <stdarg.h>

namespace funky
{

typedef sint64 VersionOrder;

class iVersionDS
{
public:
    virtual tbb::spin_rw_mutex* GetRWMutex()=0;
};

//VersionTag is a double linked list (left/right) of Versions of a part of a data structure (UID) giving a complete order for the versions
//rather than a partial one
//The data structure maintains a BinaryTree of versions:
//if v2 is child of v1 then all parts of the ds version v2 contain the data of node v2
//if v2 is non-existing for some parts then the parent version at that part is used instead (v1 or v1's parents if v1 is absent as well)
//the version aware complete order on the versions allows to use binary search to find the right version for a part

class VersionTag : boehmgc::gc_cleanup
{
    tbb::spin_mutex *mutex;
    VersionOrder order;
    VersionTag* left;
    VersionTag* right;
    uint32 childs;
    tbb::atomic<bool> free;

    VersionTag(VersionOrder value)
    {
        mutex=NULL;
        order=value;
        left=NULL;
        right=NULL;
        childs=0;
        free=false;
    }

	//we devide the space left or right from the parent to get our new UID
    VersionTag(VersionTag* parent,iVersionDS* pVDS)
    {
        free=false;
        tbb::spin_mutex::scoped_lock lock(*parent->mutex); //lock version tree

        if(parent->childs++%2) //add to the left
        {
            mutex=parent->mutex;
            order=((parent->left->order>>1)+(parent->order>>1));

            left=parent->left;
            right=parent;

			//we've run out of space, redistribute the UIDs for all versions
            if(order==left->order||order==right->order)
            {
                lock.release(); //must lock version after tree

                tbb::spin_rw_mutex* treemutex=pVDS->GetRWMutex();
                tbb::spin_rw_mutex::scoped_lock treelock(*treemutex,true); //writer lock
                lock.acquire(*parent->mutex);
                left=parent->left;
                right=parent;

                //count versions (left and right)
                VersionTag* cur=this;
                uint64 c=1;
                while(cur->right)
                {
                    cur=cur->right;
                    c++;
                }
                cur=this;
                while(cur->left)
                {
                    cur=cur->left;
                    c++;
                }

                //calc best distribution
                uint64 delta=std::numeric_limits<VersionOrder>::max()/c;
                delta-=std::numeric_limits<VersionOrder>::min()/c;

                //check that there is enough space
                assert(delta); //2^64 versions??

                //write new versions
                cur=cur->right;
                c=std::numeric_limits<VersionOrder>::min()+delta;
                while(cur->right)
                {
                    cur->order=c;
                    //updateorder
                    c+=delta;
                }
            }

            parent->left->right=this;
            parent->left=this;

        }
        else //add to right hand side..otherwise same as above
        {
            mutex=parent->mutex;
            order=((parent->order>>1)+(parent->right->order>>1));

            left=parent;
            right=parent->right;

            if(order==left->order||order==right->order) //throw exception, too many versions!
            {
                lock.release(); //must lock version after tree
                tbb::spin_rw_mutex* treemutex=pVDS->GetRWMutex();
                tbb::spin_rw_mutex::scoped_lock treelock(*treemutex,true); //writer lock
                lock.acquire(*parent->mutex);

                left=parent;
                right=parent->right;

                //count versions
                VersionTag* cur=this;
                uint64 c=1;
                while(cur->right)
                {
                    cur=cur->right;
                    c++;
                }
                cur=this;
                while(cur->left)
                {
                    cur=cur->left;
                    c++;
                }

                //calc best distribution
                uint64 delta=std::numeric_limits<VersionOrder>::max()/c;
                delta-=std::numeric_limits<VersionOrder>::min()/c;

                //check that there is enough space
                assert(delta); //2^64 versions??

                //write new versions
                cur=cur->right;
                c=std::numeric_limits<VersionOrder>::min()+delta;
                while(cur->right)
                {
                    cur->order=c;
                    c+=delta;
                }
            }

            parent->right->left=this;
            parent->right=this;
        }
    }

	//remove left nodes
    void DestroyLeft()
    {
        if(left)
            left->DestroyLeft();
        delete this;
    }

	//remove right nodes
    void DestroyRight()
    {
        if(right)
            right->DestroyRight();
        delete this;
    }
public:

	//remove all nodes
    void Destroy()
    {
        if(left)
            left->DestroyLeft();
        if(right)
            right->DestroyRight();
        delete mutex;
        delete this;
    }

    void Delete()
    {
		//mark as free to use
        free.compare_and_swap(true,false); //mark for reuse!
    }

	//equivalenze relation
    inline bool operator==(VersionTag* vp)
    {
        return mutex==vp->mutex;
    }

    ~VersionTag()//should be called by gc??
    {
        //refcount??
        Delete();
    }

	//root version
    VersionTag()
    {
        mutex=new tbb::spin_mutex();
        order=0;
        left=new VersionTag (std::numeric_limits<VersionOrder>::min());
        right=new VersionTag (std::numeric_limits<VersionOrder>::max());
        childs=0;
    }


    static VersionTag* Create(VersionTag* parent,iVersionDS* treemutex)
    {
		//try to get a free VersionTag from left or right (without lock)
        VersionTag* right=parent->right;
        if(right->free.compare_and_swap(false,true))
        {
            return right;
        }
        VersionTag* left=parent->left;
        if(left->free.compare_and_swap(false,true))
        {
            return left;
        }

        return NULL;
    }

    inline VersionOrder GetOrder()
    {
        return order;//get UID
    }

	//get UID with fallback
    inline VersionOrder GetOrder(VersionOrder ret)
    {
        if(!this)
            return ret;
        else
            return order;
    }
};

//actual bintree of versions, data structure part is derived of this
class BinaryVersionTree
{

    template<typename T,int BS,typename R> //2^BS is the batch size
    friend class PersistentArray;

    tbb::atomic<VersionTag*> version;
    tbb::atomic<BinaryVersionTree*> left,right;

public:

    BinaryVersionTree()
    {
        version=NULL;
        left=NULL;
        right=NULL;
    }

    BinaryVersionTree(VersionTag* vt)
    {
        version=vt;
        left=NULL;
        right=NULL;
    }

    inline VersionTag* GetVersion()
    {
        return version;
    }

private:

	//binary search for correct version of part
    inline BinaryVersionTree* GetVersion(VersionTag* vt)
    {
        if(vt==version) //quick check for common case..
            return this;

        VersionOrder cmp,vtv;
        BinaryVersionTree* cur=this;
        vtv=vt->GetOrder();

        while(true)
        {
            cmp=cur->version->GetOrder()-vtv;

            if(cur->left&&cmp>0)
                cur=cur->left;
            else if(cur->right&&cmp<0)
                cur=cur->right;
            else
                return cur;
        }
    }

    void DelChilds()
    {
        if(left)
            left->Del();
        if(right)
            right->Del();
    }

public:
	//kill node
    void Del()
    {
        if(left)
            left->Del();
        if(right)
            right->Del();

        delete this;
        //scalable_aligned_free(this);
    }

	//create a new version of part (using atomics)
    void AddVersion(BinaryVersionTree* nt)
    {
        VersionTag* tag=nt->version;

        VersionOrder vt=tag->GetOrder();
        VersionOrder cmp;
        tbb::atomic<BinaryVersionTree*>* prev=NULL;
        BinaryVersionTree* cur=this;

        while(true)
        {
            cmp=cur->version->GetOrder()-vt;
            if(cmp>0)
            {
                if(NULL==cur->left.compare_and_swap(nt,NULL))
                {
                    return;
                }
                else
                {
                    prev=&cur->left;
                    cur=cur->left;
                }
            }
            else if(cmp<0)
            {
                if(NULL==cur->right.compare_and_swap(nt,NULL))
                {
                    return;
                }
                else
                {
                    prev=&cur->right;
                    cur=cur->right;
                }
            }
            else
            {
//                if(cur->version)
//                    throw "double version entry";

                assert(false);
/*
                nt->left=cur->left;
                nt->right=cur->right;

                if(prev)(*prev)=nt;
 */

                return;
            }
        }
    }

    void SetVersion(VersionTag* tag)
    {
        version=tag;
    }

//private:
/*
    void DelVersion(VersionTag* tag)
    {
        VersionOrder vt=tag->GetOrder();
        VersionOrder cmp;
        BinaryVersionTree* cur=this;

        while(true)
        {
            if(tag==cur->version.compare_and_swap(NULL,tag))
            {
                return;
            }

            cmp=cur->version->GetOrder()-vt;

            if(cmp>0)
            {
                if(!cur->left)
                    return;
                cur=cur->left;
            }
            else if(cmp<0)
            {
                if(!cur->right)
                    return;
                cur=cur->right;
            }
        }
    }
 */
};

}

#endif	/* PERSISTENT_H */

