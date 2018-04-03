/* 
 * File:   wRefCounter.hpp
 * Author: aherz
 *
 * Created on April 5, 2011, 5:59 PM
 */

#ifndef WREFCOUNTER_HPP
#define	WREFCOUNTER_HPP

#include <pthread.h>
#include <limits.h>
#include <algorithm>

#include "bitcount.hpp"

using namespace std;

#define MAX_SHIFT (UINT_MAX-2)
//#define MAX_WEIGHT (1L<<MAX_SHIFT)

class cWRCDataContainer
{
    //uint64 mWeight;
    DynArray<uint32> mShifts;//can become arbitrarily large...compress consecutive?
    pthread_mutex_t mutex;

    uint32 mReleased;

    void* mData;
public:
    cWRCDataContainer(void* data)
    {
        mData=data;
        mutex = PTHREAD_MUTEX_INITIALIZER;
        //mWeight = MAX_WEIGHT;
        //int byte_size_u64=sizeof(uint64);
        //mWeight=MAX_WEIGHT;//max
    }
   
    inline void* getData()
    {
        return mData;
    }
/*
    inline void release(uint64 weight)
    {
        if(__sync_sub_and_fetch(&mWeight,weight)==0)
        {
            //should delete data as well!!
            free(mData);
            delete this;
        }
    }
*/
    bool find_shift(uint32 shift,uint32 pmin,uint32 pmax,uint32* offs)
    {
        if(mShifts.size()==0)
        {
            *offs=0;
            return false;
        }
        
        while(pmin<pmax)
        {
            uint32 half=pmin+(pmax-pmin)>>1;
            if(mShifts.at(half)==shift)
            {
                *offs=half;
                return true;
            }
            else
            {
                if(shift>half)
                    pmin=half+1;
                else
                    pmax=half-1;
            }
        }
        *offs= min(pmin,pmax);
        return false;
    }

    bool insert_shift(uint32 shift)
    {
        
        uint32 offs;
        
        while(find_shift(shift,0,mShifts.size()-1,&offs))
        {
            if(shift+1==MAX_SHIFT)
                return true;

            if(mShifts.at(offs+1)!=shift+1)
            {
                mShifts.at(offs)=shift+1;
                break;
            }
            else
            {
                mShifts.remove(offs);
            }
            shift++;
            if(shift==MAX_SHIFT)
                return true;
        }
        
        mShifts.insert_at(offs,shift);

        if(mShifts.size()>10)
        {
            
        }

        return false;
    }

    inline void release(uint32 shift)
    {
        bool del=false;

        pthread_mutex_lock( &mutex );
        
        //insert shift into shift list, order! for binary search
        del=insert_shift(shift);

        pthread_mutex_unlock( &mutex );

        if(del)
        {
            //should delete data as well!!
            free(mData);
            delete this;
        }
    }

};

class wRefCount
{
    uint32 mShift;
    cWRCDataContainer* mData;
    
    wRefCount(cWRCDataContainer* data,uint32 shift)
    {
        mData=data;
        mShift=shift;
    }

    wRefCount(cWRCDataContainer* data)
    {
        mData=data;
    }
protected:

    void SetWeight(uint32 shift)
    {
        mShift=shift;
    }
    
public:
   
    static wRefCount* createObject(void* data)//need dynamic refcounter??
    {
        return new wRefCount(new cWRCDataContainer(data),MAX_SHIFT);
    }
    
    wRefCount* AddRef()//FIXME: add helper node
    {
        wRefCount* nrc=new wRefCount(mData);
        uint32 shift=__sync_sub_and_fetch(&mShift,1);
        
        if(shift==0)
        {
            //add node
            throw ">2^32 references not allowed";
        }
        
        nrc->SetWeight(shift);
        return nrc;
    }

    void Release()
    {
        mData->release(mShift);
        delete this;
    }

    void* GetData()
    {
        return mData->getData();
    }
};

#endif	/* WREFCOUNTER_HPP */

