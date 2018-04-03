/* 
 * File:   persistence.hpp
 * Author: aherz
 *
 * Created on April 1, 2011, 1:27 PM
 */

#include <memory.h>

#ifndef PERSISTENCE_HPP
#define	PERSISTENCE_HPP


typedef unsigned long uint64;
typedef unsigned int uint32;
typedef unsigned char uint8;

template<class T>
class DynArray
{
public:
    T* mData;
    uint32 mCapacity;
    uint32 mSize;
    bool mSingle;

    DynArray();

    void prealloc(uint32 cap)
    {
        if(mSingle)
        {
            mSingle=false;
            void* d0=mData;
            mData=(T*)malloc(sizeof(T)*4);

            if(mSize>0)
                mData[0]=*((T*)&d0);

            mCapacity=4;
        }
        
        if(mCapacity<cap)
        {
            mData=(T*)realloc(mData,sizeof(T)*cap);
            mCapacity=cap;
        }
    }
    
    inline void push_back(T data);
    inline void insert_at(uint32 pos,T data);
    inline T& at(uint32 i) const;
    inline uint32 size() const;
    inline T& atSingle() const;
    inline T& atMultiple(uint32) const;
    inline bool isSingle() const;
    inline void remove(uint32 pos);

};


template<class T>
DynArray<T>::DynArray()
{
    mSize=0;

    if(sizeof(T)==sizeof(void*))
    {
        mSingle=true;
        mCapacity=1;
    }
    else
    {
        mSingle=false;
        mCapacity=2;
        mData=(T*)malloc(sizeof(T)*mCapacity);
    }
}

template<class T>
void DynArray<T>::remove(uint32 pos)
{
    if(mSingle)
    {
        if(mSize==mCapacity)
            mSize=0;
        return;
    }
    
    if(mSize)
    {
        memmove(mData+sizeof(T)*(pos+1),mData+sizeof(T)*(pos),sizeof(T)*(mSize-pos-1));
    }
}

template<class T>
void DynArray<T>::insert_at(uint32 pos,T data)
{
    if(pos==mSize)
    {
        push_back(data);
        return;
    }

    if(mSingle)
    {
        if(mSize<mCapacity)
            mData=*((T**)&data);
        else
        {
            mSingle=false;
            void* d0=mData;
            mData=(T*)malloc(sizeof(T)*4);
            mData[0]=*((T*)&d0);
            mCapacity=4;          
        }
    }
    
    if(!(mSize<mCapacity))
    {
        T* oldData=mData;
        mCapacity*=2;
        mData=(T*)malloc(sizeof(T)*mCapacity);
        memcpy(oldData,mData,sizeof(T)*pos);
        memcpy(oldData+sizeof(T)*(pos),mData+sizeof(T)*(pos+1),sizeof(T)*(mSize-pos));
        free(oldData);
    }
    else
    {
        memmove(mData+sizeof(T)*(pos),mData+sizeof(T)*(pos+1),sizeof(T)*(mSize-pos));
    }
    mData[pos]=data;
    mSize++;
}

template<class T>
void DynArray<T>::push_back(T d)
{
    if(mSingle)
    {
        if(mSize<mCapacity)
        {
            mData=*((T**)&d);
            mSize++;
        }
        else
        {
            mSingle=false;
            void* d0=mData;
            mData=(T*)malloc(sizeof(T)*4);
            mData[0]=*((T*)&d0);
            mData[1]=d;
            mSize++;
            mCapacity=4;
        }
    }
    else
    {
        if(mSize<mCapacity)
        {
            mData[mSize]=d;
            mSize++;
        }
        else
        {
            mCapacity*=2;
            mData=(T*)realloc(mData,sizeof(T)*mCapacity);

            mData[mSize]=d;
            mSize++;
        }
    }
}

template<class T>
T& DynArray<T>::at(uint32 i) const
{
    if(mSingle)
    {
        return *((T*)&mData);
    }
    else
    {
        return mData[i];
    }
}

template<class T>
bool DynArray<T>::isSingle() const
{
    return mSingle;
}

template<class T>
T& DynArray<T>::atSingle() const
{
    return *((T*)&mData);
}

template<class T>
T& DynArray<T>::atMultiple(uint32 i) const
{
    return mData[i];
}

template<class T>
uint32 DynArray<T>::size() const
{
    return mSize;
}



#endif	/* PERSISTENCE_HPP */

