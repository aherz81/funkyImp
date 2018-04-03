/*
 * File:   WorkingSet.h
 * Author: aherz
 *
 * Created on December 21, 2011, 2:42 PM
 */

#ifndef WORKINGSET_H
#define	WORKINGSET_H

#include <string.h>
#include <tbb/atomic.h>

namespace funky
{

#define WSIZE 32*50

/*
 * uses atomic ops to cache a working set (cheap retrieval of free items)
 */

template <class T, uint32 CSIZE>
class WorkingSet
{
    tbb::atomic<T> table[CSIZE];
    tbb::atomic<uint32> free;

public:

    WorkingSet()
    {
        free=CSIZE;
        memset(&table,0,sizeof(T)*CSIZE);
    }

    inline uint32 size()
    {
        return CSIZE-free;
    }

    inline bool empty()
    {
        return CSIZE==free;
    }

    inline bool put(T item)
    {
        if(free>0)
        {
            for(uint32 i=0;i<CSIZE;i++)
            {
//                if(__sync_bool_compare_and_swap(&table[i],NULL,item))
                if(NULL==table[i].compare_and_swap(item,NULL))
                {
                    //__sync_sub_and_fetch(&free,1);
                    free--;
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    inline bool get(T* pitem)
    {
        //DO NOT CHECK FOR EMPTY CACHE HERE, it's slow
        for(uint32 i=0;i<CSIZE;i++)//should try random entry?
        {
            T local=table[i];
//            if(local&&__sync_bool_compare_and_swap(&table[i],local,NULL))
            if(local&&local==table[i].compare_and_swap(NULL,local))
            {
                //__sync_add_and_fetch(&free,1);
                free++;
                *pitem=local;
                return true;
            }
        }
        return false;
    }
};

}

#endif	/* WORKINGSET_H */

