/*
 * File:   AddressFixedArray.h
 * Author: aherz
 *
 * Created on November 24, 2011, 6:15 PM
 */

#ifndef ADDRESSFIXEDARRAY_H
#define	ADDRESSFIXEDARRAY_H

#include <new>
#include <memory>
#include <cstdlib>
#define _MULTI_THREADED

#include <tbb/mutex.h>
#include <stdint.h>
#include <assert.h>

#include "types.h"
#include "WorkingSet.h"

/*
 * - AddressFixedArray template allocates pages of objects
 * - alloc and release of an object does not change the addresses of other objects
 * - can be made thread safe (instantiate with TS=true)
 * - TSIZE gives page size
 * - uses WorkingSet as fast cache
*/
namespace funky
{

#ifdef _DEBUG
#define PCHECK(v) if(v!=0){printf("pthread_fail");fflush(stdout);exit(-1);}
#else
#define PCHECK(v) v
#endif

template <class T, uint32 TSIZE, bool DALLOC,bool TS>
class AddressFixedArray {
public:

    class Wrapper:public T {
        uint32 NEXT;
        uint32 WRAPPER_TBL;
        uint32 WRAPPER_ID;
        bool TAGGED_FREE;
        Wrapper(Wrapper*o):T(o){}
        friend class AddressFixedArray;
    };
private:

    struct Table {
        Wrapper* mem;
        uint32 next;
        uint32 next_table;
        uint32 free;
        tbb::atomic<uint32> tagged;
    };
    Table* Tables;
    uint32 count;
    uint32 next;

    //pthread_mutex_t mutex;
	tbb::mutex *mutex;


    WorkingSet<Wrapper*,WSIZE> cache;

public:
    void CollectFree()
    {
        if(TS)return;
        for(uint32 t=0;t<count;t++)
        {
            if(Tables[t].tagged)
            {
                uint32 found=0;
                for(uint32 i=0;i<TSIZE;i++)
                {
                    if(Tables[t].mem[i].TAGGED_FREE)
                    {
                        found++;
                        Tables[t].mem[i].TAGGED_FREE=false;
                        free(&Tables[t].mem[i]);
                    }
                }
				Tables[t].tagged-=found;
                //__sync_sub_and_fetch(&Tables[t].tagged,found);
            }
        }
    }

    void InitTable(Table* t, uint32 WRAPPER_TBL, uint32 next_table) {
        t->mem = (Wrapper*) malloc(sizeof (Wrapper) * TSIZE);
        t->next = 0;
        for (uint32 i = 0; i < TSIZE; i++) {
            t->mem[i].WRAPPER_TBL = WRAPPER_TBL;
            t->mem[i].WRAPPER_ID = i;
            t->mem[i].TAGGED_FREE = false;
            new (&t->mem[i]) Wrapper(&t->mem[i]); //run constructor
            t->mem[i].NEXT = i + 1;
        }
        t->next_table = next_table;
        t->free = TSIZE;
        t->tagged = 0;
    }

    AddressFixedArray() {
        //FIXME: use tbb mutex
        if(TS)mutex=new tbb::mutex();//PCHECK(pthread_mutex_init(&mutex,NULL));
        count = 1;
        next = 0;
        Tables = (Table*) malloc(count * sizeof (Table));
        InitTable(&Tables[0], 0, 1);
    }

    AddressFixedArray(const char* name)
    {
        new (this) AddressFixedArray();
    }

    ~AddressFixedArray() {
        CollectFree();
        if (DALLOC)
            for (uint32 t = 0; t < count; t++) {
                for (uint32 t = 0; t < count; t++) {
                    Tables[t].mem->~Wrapper();
                }
            }
        if(TS)delete mutex;//PCHECK(pthread_mutex_destroy(&mutex));
    }

    T* get(uintptr_t seed)
    {
        return get();
    }

    T* get() {
        Wrapper* item;
        if(cache.get(&item))
            return item;

		tbb::mutex::scoped_lock lock;
        if(TS)lock.acquire(*mutex);//PCHECK(pthread_mutex_lock(&mutex));

        uint32 res = next;
        uint32 index = Tables[res].next;
        Tables[res].next = Tables[res].mem[index].NEXT;

        if (Tables[next].next >= TSIZE)//table full?
        {
            next = Tables[next].next_table;
            if (next >= count)//all tables full?
            {
                count *= 2;
                Tables = (Table*) realloc(Tables, count * sizeof (Table));
                for (uint32 i = count / 2; i < count; i++) {
                    InitTable(&Tables[i], i, i + 1);
                }
            }
        }

        Tables[res].free--;
        Wrapper* pw=&Tables[res].mem[index];

        //if(TS)PCHECK(pthread_mutex_unlock(&mutex));
        return pw;
    }

    void tag_free(T* io)
    {
        Wrapper* o=(Wrapper*)io;
        o->TAGGED_FREE=true;
        //__sync_add_and_fetch(&Tables[o->WRAPPER_TBL].tagged,1);
		Tables[o->WRAPPER_TBL].tagged+=1;
    }

    void free(T* io) {
        //assert(TS);
        if(cache.put((Wrapper*)io))return;

        Wrapper* o=(Wrapper*)io;
		tbb::mutex::scoped_lock lock;
        if(TS)lock.acquire(*mutex);//PCHECK(pthread_mutex_lock(&mutex));

        uint32 WRAPPER_TBL = o->WRAPPER_TBL;
        uint32 WRAPPER_ID = o->WRAPPER_ID;
        Tables[WRAPPER_TBL].mem[WRAPPER_ID].NEXT = Tables[WRAPPER_TBL].next;
        Tables[WRAPPER_TBL].next = WRAPPER_ID;
        Tables[WRAPPER_TBL].free++;
        if (Tables[WRAPPER_TBL].free == 1)//table was full..tell the world we're back
        {
            Tables[WRAPPER_TBL].next_table = next;
            next = WRAPPER_TBL;
        }

//        if(TS)PCHECK(pthread_mutex_unlock(&mutex));
    }
};

template <class T,uint32 PS>
class LocalPool
{
	struct Wrapper:public T
	{
        LocalPool* __POOLHANDLE;
        Wrapper(Wrapper*o):T(o){}
	};

    AddressFixedArray<Wrapper,PS,false,false> pool;
public:
    LocalPool()
    {
    }


public:
    void CollectFree()
    {
        pool.CollectFree();
    }

	T* get()
	{
		Wrapper*w=pool.get();
		w->__POOLHANDLE=this;
		return w;
	}

	void free(T* iw)
	{
		Wrapper*w=(Wrapper*)iw;
		w->__POOLHANDLE->pool.tag_free(w);
	}
};

}

#endif	/* ADDRESSFIXEDARRAY_H */

