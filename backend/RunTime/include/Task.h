/*
 * File:   Task.h
 * Author: aherz
 *
 * Created on December 28, 2011, 5:58 PM
 */

#ifndef TASK_H
#define	TASK_H

#define COMMA ,

#define GC_THREADS //multi-threaded GC
#define GC_NAMESPACE
#include <gc_cpp.h> //for garbage collection
#undef GC_NAMESPACE
#undef GC_THREADS

#include "types.h"
#include <cstdlib>

#include <tbb/scalable_allocator.h> //needed?

#include <tbb/task_scheduler_init.h>
#include <tbb/task.h>
#include <tbb/atomic.h>
#include <tbb/mutex.h>
#include <tbb/queuing_mutex.h>
#include <tbb/concurrent_queue.h>
#include <tbb/tick_count.h> //profile
#include <queue>

#ifdef OPENCLBACKEND
#include "ocl.h" //OpenCL-Runtime support
#endif

#include "fsignal.h" //signal isn't really portable...

//#define LOCK_ELISION

#ifdef LOCK_ELISION
#include "elision_mutex.h" //upgradeable reader writer lock
#else
#include "sustained_rw_mutex.h" //upgradeable reader writer lock
#endif

#include "LinearArray.h" //unique domains

//#define REFCOUNT_DEBUG
using namespace std;

namespace funky
{

#define LOG(txt) {fprintf(stdout,txt);fflush(stdout);}


struct RefCountBase
{
    tbb::atomic<uint32> refcount; //track possible returns
	bool isrc;

public:

#ifdef REFCOUNT_DEBUG
	static tbb::atomic<uint32> liveContexts;
#endif

	RefCountBase(bool isrc):isrc(isrc)
	{
#ifdef REFCOUNT_DEBUG
		if(isrc)
			liveContexts++;
#endif

	}

#ifdef REFCOUNT_DEBUG
	~RefCountBase()
	{
		if(isrc)
			liveContexts--;
	}
#endif

	void addRef()
	{
		if(isrc)
			++refcount;
	}

	void release()
	{
		if(isrc)
			if(--refcount==0)
			{
#ifdef REFCOUNT_DEBUG
				//fprintf(stderr,"kill %016p\n",this);
#endif
				delete this;
			}
	}
};

template <typename T>
struct ContextBase: public RefCountBase//context is the equivalent of a stack frame of a method in a multi-threaded env
//actual countext is derived of this and contains parameters and 'local' vars
//:boehmgc::gc //garbage collect context: DOES NOT WORK!
{
    T retval; //retval of method
	uint32 kernel_level; //track recursion level
    tbb::atomic<uint32> exitcount; //track possible returns
    tbb::atomic<bool> hasreturned; //has method already returned

    ContextBase(uint32 kernel_level, bool hasreturned,bool isrc=true):RefCountBase(isrc),kernel_level(kernel_level)
    {
		//memset(&retval,0,sizeof(T));
		exitcount=0;
        this->hasreturned = hasreturned;//it's atomic
		refcount=1;
#ifdef REFCOUNT_DEBUG
		fprintf(stderr,"create %016p\n",this);
#endif
    }

	//we explicitly refcount (instead of boehm gc) because boehm failed to pic up context refs from TBB, so they were killed to early
	//refcounts are generated in constructor of Task (addref) and with END_TASK() makro


	//should use Return() before calling this
	//this is not forced in here to avoid calculating val if hasreturned is already set
    inline void SetReturn(T val)
    {
		retval=val;
    }

    inline T GetReturn()
    {
		return retval;
    }

    inline bool Return()//check/set ret?
    {
        //check that return was not yet performed:
        return !hasreturned.compare_and_swap(true,false);
    }

};

template <typename T>
class localContext //kills one ref when scope is left (for fun that allocs context)
{
	ContextBase<T>* ctx;
public:
	localContext(ContextBase<T>* ctx):ctx(ctx)
	{}

	~localContext()
	{
		ctx->release();
	}
};

template <typename C>
class Task : public tbb::task //wrapper around tbb::task to include a context
{
protected:
    C* ctx;
public:

    Task(C *ctx) : ctx(ctx)
    {
#ifdef REFCOUNT_DEBUG
		fprintf(stderr,"Task %016p\n",this);
#endif
		if(ctx!=NULL)
			ctx->addRef();
    }

    tbb::task * execute()
	{
		return NULL;
	}

    inline C* context() const //get context
    {
/*
		if(ctx->exitcount<0||ctx->exitcount>100)
		{
			fprintf(stderr,"ctx fail:%016p\n",ctx);
			fflush(stderr);
			__asm("int $3");
		}
*/
        return (ctx);
    }
};

template <typename C>
class ConstTask : public tbb::task //special version of context to pass ref to local object call contexts
{
protected:
    C& ctx;
public:

    ConstTask(C &ctx) : ctx(ctx)
    {
#ifdef REFCOUNT_DEBUG
		fprintf(stderr,"ConstTask %016p\n",this);
#endif
		//ctx.addRef();//only used with spawn
    }

    tbb::task * execute()
	{
		return NULL;
	}

    inline C* context() const
    {
        return (&ctx);
    }
};

template <uint32 I = 0> //make it a singleton (linker should join all instances)
class TaskRoot
{
public:

	//root task the main fun waits for to finish, free tasks are added to this to avoid premature app exit
	//this means that the app will NOT exit unless all tasks finish to execute
    static tbb::task*& GetRoot()
    {
        static tbb::task* root;
        return root;
    }
};

//base class for event objects (contains upgradeable mutex for events and samples)
class Singular//: public boehmgc::gc_cleanup
{
protected:
    mutable sustained_rw_mutex mutex  __attribute__ ((aligned (16))); //it's not really part of the state of the object
    Singular():mutex()
    {

    }
};

/************MACROS TO DEFINE TASKS**************/

#define BEGIN_TASK(CLASS,NAME,CONTEXT) \
        tbb::task * CLASS::NAME::execute() {

#define END_TASK() \
            return NULL; \
        }

#define DECLARE_TASK(NAME,CONTEXT) \
    class NAME : public funky::Task<CONTEXT> \
    { \
        public: NAME(CONTEXT * context) : funky::Task<CONTEXT> (context){} \
        private: tbb::task * execute(); \
    };

//allow temporary objects for call contexts (requires c++0x)
#define DECLARE_CONST_TASK(NAME,CONTEXT) \
    class NAME : public funky::ConstTask<CONTEXT> \
    { \
        public: NAME(CONTEXT && context) : funky::ConstTask<CONTEXT> (context){} \
        public: NAME(CONTEXT & context) : funky::ConstTask<CONTEXT> (context){} \
        private: tbb::task * execute(); \
    };


//includes execute impl
#define GLOBAL_TASK(NAME,CONTEXT) \
    class NAME : public funky::Task<CONTEXT> \
    { \
        public: NAME(CONTEXT * context) : funky::Task<CONTEXT> (context){} \
        private: tbb::task * execute() {

#define END_GLOBAL_TASK() \
            return NULL; \
        } \
    };

//create a context for a method
#define CONTEXT(TYPE,NAME,P,F,T) struct NAME : public funky::ContextBase<TYPE>\
    { \
        struct NAME##PARAMS P;\
        struct NAME##LOCALS F;\
        struct NAME##TASKS T;\
        NAME##PARAMS params;\
        NAME##LOCALS frame;\
        NAME##TASKS tasks;\
        NAME(funky::uint32 kl,bool hasreturned,bool isrc=true) : funky::ContextBase<TYPE>(kl, hasreturned,isrc){}\
        NAME(): funky::ContextBase<TYPE>(0xffffffff, false,false){}\
    };
//argless constructor only for main()'s context which is not rc!

#define __RTOOB(INDEX,OBJECT) ([&]()->funky::uint32{funky::uint32 index=INDEX;if(index>=OBJECT){fprintf(stderr,"Array out of Bounds"); exit(-1);}return index;}())


//mvar for triggering (wait with write until empty, wait with read until full)

//NOTE: current trigger impl deviates from thesis continuations and may use an unbounded number of threads

template <typename C>
class TriggerState //it's an mvar
{
    setsignal empty;
    unsetsignal full;
    tbb::mutex mutex;
    std::queue<ConstTask<C>*> queue;

public:

    inline bool can_write()
    {
        return empty.isset();
    }
    
    void lock()
    {
        mutex.lock();
    }

    void unlock()
    {
        mutex.unlock();
    }
    
    void enqueue(ConstTask<C>* t)
    {
        queue.push(t);
    }
    
    ConstTask<C>* dequeue()
    {
        ConstTask<C>* res=queue.back();
        queue.pop();
        return res;
    }
    
    bool queueEmpty()
    {
        return queue.empty();
    }
    
    inline void start_write()
    {
        empty.wait();
    }

    inline void finish_write()
    {
        full.fire();
    }

    inline bool can_read(int id)
    {
        return full.isset();
    }

    inline int start_read(int id)
    {
        full.wait();
        return 0;
    }

    inline void finish_read()
    {
        empty.fire();
    }
};

/*
//note: this impl uses global var int TRIGGER_ID_XXX=0 the reset to 0 only happens at ap init, so that it screws with profiling
class TriggerState //it's an mvar
{
    setsignal empty;
	change uid;
	tbb::mutex mutex;

public:

	TriggerState():uid(0)
	{
	}

    inline void start_write()
    {
        empty.wait(); //no update before trigger was read at least once
		mutex.lock(); //no inconsistent reads
    }

    inline void finish_write()
    {
		mutex.unlock();
		uid.increment();//update uid
    }

    inline bool can_read(int id)
    {
        return id!=uid.state();
    }

    inline int start_read(int id)
    {
		uid.wait(id);//wait that uid != our id (so contents has changed)
		mutex.lock();//no update during read
		return uid.state();//return new id
    }

    inline void finish_read()
    {
		mutex.unlock(); //finished
        empty.tryfire();//may be multiple fires..doesn't wait for fire to be unset! but wakes up waiters
		//first time we come here empty is guaranteed to be unset
    }
};
*/

//create instance of trigger (to store data in)
#define TRIGGER(NAME,P,I,C) struct NAME:public funky::TriggerState<C> P;NAME I;

//get current tbb::task from TLS
#define SELF() tbb::task::self()

//get current Task (with context)
#define SELFTASK(TYPE) ((funky::Task<funky::ContextBase<TYPE> >&)SELF())

}

#include "Scheduler.h"
template<typename T=int>
char getChar(const char* ca,int offset)
{
    return ca[offset];
}

template<typename T=int>
int* array_sum(int* array,int size)
{
	int sum=0;
	for(int i=0;i<size;i++)
		sum+=array[i];

	printf("sum=%d\n",sum);

	return array;
}

#endif	/* TASK_H */

