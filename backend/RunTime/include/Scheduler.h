/*
 * File:   Scheduler.h
 * Author: aherz
 *
 * Created on November 25, 2011, 4:22 PM
 */

#ifndef SCHEDULER_H
#define	SCHEDULER_H

/*
 * - Interface to the funky run time (at least the task based portion)
 * - depends on TBB4.1
 * - uses a pool of tbb::threads (almost like std::thread but made to register with boehm GC)
 * - exposes several debug interfaces
 * - uses custom futex based signal (that behaves predictably when signal arioves before wait unlike
 * pthread cond var)
 * - the threads in the pool augment the tbb worker threads (blocking ops are performed on the
 * pool so they do no block tbb worker threads and progress is guaranteed)
 * - the amount of pool threads is currently unlimited
 */

#include <tbb/compat/thread> //tbb threads (includes gc_cpp.h for boehm GC)
//#include <thread> //doesn't inlcude gc_cpp.h
#include <math.h>

#include "AddressFixedArray.h"

#include "fsignal.h" //custom futex based signal
#include "Task.h" //wrapper over tbb task

#include <tbb/enumerable_thread_specific.h> //TLS for reentrant custom threads

namespace funky
{


#define THREAD_PAGE_SIZE 4 //page size for thread pool, for each new page that many threads will be spawned

#define task_handle tbb::task
#define thread_handle Thread

//a thread-pool thread
class Thread {
protected:
    unsetsignal task_ready; //set when an nprocessed work item was assigned to a thread
	setsignal free; //set when a thread is not currently processing a task
	bool sig_free; //flag whether this instance uses free signal
    task_handle* task; //actual work item (this is derived from Task in Task.h, so we can assume a context)
	RefCountBase *context; //may have to delete context when we're done

	//ALEX: MUST!! use tbb_thread instead of std::thread so that threads are registered with GC
    tbb::tbb_thread thread; //actual thread, MUST be placed after signal so signals are init before threads starts

	//standard worker (re-useable thread)
    static void* run(void* vo);

    inline void Work();

	//special purpose thread (e.g. all ogl calls are processed on one thread (thread(xxx) in funky)
	static void* runPermanent(void* vo);

    inline void WorkPermanent();

	//called from worker to suspend itself (unless there is new work ready to be processed)
    inline void SuspendTask()
    {
#ifdef DEBUG_THREADS
        LOG("Thread::SuspendTask\n");
#endif

        task_ready.wait();

#ifdef DEBUG_THREADS
        LOG("Thread::SuspendTask FINSIHED\n");
#endif
	}

	//called from outside worker to continue a suspended worker
    inline void SignalTask()
    {
#ifdef DEBUG_THREADS
        LOG("Thread::SignalTask\n");
#endif
        task_ready.fire();
    }

	//called from worker to indicate that thread has finished processing task
	inline void SignalFree()
	{
		free.fire();
	}

	//called from outside worker to check that thread is free (for special purpose threads)
	inline void WaitFree()
	{
		free.wait();
	}

public:

	//uses TLS to get current thread
	static Thread* getCurrentThread();

	//default constructor does NOT spawn thread
    Thread():sig_free(false)
    {
        task=NULL;
		context=NULL;
    }

	//used by AddressFixedArray sets sigs and spawns thread
    Thread(thread_handle* o):task_ready(0),sig_free(false),task(NULL),context(NULL),thread(&run,o)
	{
		thread.detach();//must detach when using std::thread to avoid exception at shut down
	}

	//used by special purpose threads
    Thread(bool b):task_ready(0),sig_free(false),task(NULL),context(NULL),thread(&runPermanent,this) {
		thread.detach();
    }

    ~Thread() {
    }

	//get thread from pool
    static thread_handle* getOSThread();

	//register special purpose thread (via name)
	static thread_handle* registerThread(char* thread);

	//wait for execution of task on thread pool thread
    template<class T>
    static T BlockTask(task_handle* task)
    {
#ifdef DEBUG_THREADS
		fprintf(stderr,"Block 0x%016p, Parent 0x%016p\n",task,task->parent());
#endif
		Thread* owner=getCurrentThread();

		if(owner==NULL)//spawned from tbb worker thread
		{
			task_handle& self=SELF();
			self.set_ref_count(2); //task must be child of self (should assert)

			thread_handle* o=getOSThread(); //get thread from pool

			//assign task
			o->task=task;
			//we don't use the free flag in this case
			o->sig_free=false;

			//continue worker thread
			o->SignalTask();

			self.wait_for_all();//wait using tbb api (so we don't block the worker thread)
/*
			while(self.ref_count()>1)
			{
				printf("self.wait_for_all(); FAILED\n");
				//fflush(stdout);
				__asm__("int $3");
				self.wait_for_all();
			}
*/
		}
		else//spawned from non-tbb thread (so from thread pool thread)
		{
			thread_handle* o=getOSThread();

			//make sure free is unset
			o->free.reset();

			//assign task
			o->task=task;
			//we want sig_free
			o->sig_free=true;

			//continue waiting thread
			o->SignalTask();

			//when spawned from non-tbb worker thread, self.wait_for_all(); fails to wait so we use a signal instead
			o->WaitFree();//wait for sig_free
		}

		//return result from context
        return static_cast<Task<ContextBase<T> >*>(task)->context()->GetReturn();
    }

	//same as prev but instead of thread from thread-pool the given special-purpose thread o is used to run task
    template<class T>
    static T BlockTask(task_handle* task, thread_handle* o)
    {
#ifdef DEBUG_THREADS
		fprintf(stderr,"Block 0x%016p, Parent 0x%016p\n",task,task->parent());
#endif
		Thread* owner = getCurrentThread();
		if(owner!=o)//if we are not already on o!
		{
			if(owner==NULL)
			{
				task_handle& self=SELF();
				self.set_ref_count(2);

				o->WaitFree();//make sure o is not in use

				o->task=task;

				o->SignalTask();

				self.wait_for_all();
			}
			else
			{
				o->WaitFree();//make sure o is not in use

				o->sig_free=true;

				o->task=task;

				o->SignalTask();

				o->WaitFree();//wait for result
			}
		}
		else //we're already on o, so we execute the task directly
		{
			task->execute();
			//task::destroy() does NOT notify parent so we do it by hand
			task_handle* parent=task->parent();
			if(parent)
			{
				parent->decrement_ref_count();
				task->set_parent(NULL); //set parent NULL so destroy doesn't mess up parent's ref_count
			}

			tbb::task::destroy(*task);
		}

		//return result
        return static_cast<Task<ContextBase<T> >*>(task)->context()->GetReturn();
    }

	//spawn task on thread-pool thread without waiting for result
	template<class T>
    static void SpawnTask(task_handle* task)
    {
#ifdef DEBUG_THREADS
		fprintf(stderr,"Spawn 0x%016p, Parent 0x%016p\n",task,task->parent());
#endif
        thread_handle* o=getOSThread();

        o->task=task;
		o->context=static_cast<Task<ContextBase<T> >*>(task)->context();

        o->SignalTask();
    }

	//as above, but spawn on given special purpose thread
	template<class T>
    static void SpawnTask(task_handle* task, thread_handle* o)
    {
#ifdef DEBUG_THREADS
		fprintf(stderr,"Spawn 0x%016p, Parent 0x%016p\n",task,task->parent());
#endif

		if(getCurrentThread()!=o)//already on o?
		{
			//wait for worker to become free
			o->WaitFree();

			o->task=task;
			o->context=static_cast<Task<ContextBase<T> >*>(task)->context();

			o->SignalTask();
		}
		else//we're already on the right thread!
		{
			task->execute();

			task_handle* parent=task->parent();
			if(parent)
			{
				parent->decrement_ref_count();
				task->set_parent(NULL);
			}

			static_cast<Task<ContextBase<T> >*>(task)->context()->release();

			tbb::task::destroy(*task);
		}
    }
};

//debug interface
class Debug
{
public:
	//used when -FORCEGCCLEANUP is given as argument
    static void StartTask(uint32 name); //called at task start to track currently running tasks
    static void FinishTask(uint32 name); //called at task end
    static void Abort(); //exception occured: dumps still running tasks
    static void Exit(); //normal exit, dumps some stats
	//outdated funcs to track context allocation, now done via GC
//    static void RTAlloc(void* addr,uint32 count,char* name);
//    static void RTFree(void* addr,uint32 task);
//    static void RTRequestFree(void* addr,uint32 task);
};

//used for profiling (#TIME(var,"name") inside funky or -PROFILE n compiler arg)
//uses atomic counters, must be careful with resolution (avoids lock)
struct ProfileEntry
{
    tbb::atomic<uint64> value_sum; //for average
    tbb::atomic<uint64> value_squared_sum; //for error
    tbb::atomic<uint64> count; //for error
    const char* name; //info
	const char* desc;
	const char* stmts;
	double estimate,correction; //for profiling
	bool custom; //used with -PROFILE n or #TIME

    ProfileEntry(const char*name,const char* desc,const char* stmts,double estimate,double correction,bool custom)
    {
        this->name=name;
		this->desc=desc;
		this->stmts=stmts;
		this->estimate=estimate;
		this->correction=correction;
		this->custom=custom;
                count=0;
                value_sum=0;
                value_squared_sum=0;
    }

	//register new measurement
    void AddMeasurement(uint64 value)
    {
        if(value_squared_sum+value*value>value_squared_sum)
        {
            value_sum+=value;
            value_squared_sum+=value*value;
            count++;
        }
    }

	//calc average
    double GetAverage()
    {
		if(count<1)
			return 0;
        return value_sum/(double)count;
    }

    double GetStdDev()
    {
		if(count<=1)
			return 0;
        return sqrt(1.0/(count-1)*((double)value_squared_sum-value_sum*(value_sum/(double)count)));
    }

	//dump stats
    float Dump(FILE* f,float cpt,uint64 samples)
    {
		if(!custom)
		{
			fprintf(f,"%s_EST=%f\n",name,estimate);
			fprintf(f,"%s_COR=%f\n",name,correction);
			float avrg=GetAverage()*cpt;
			fprintf(f,"%s_MES=%f\n",name,avrg);
			float dev=GetStdDev()*cpt;
			fprintf(f,"%s_STDDEV=%f\n",name,dev);
			fprintf(f,"%s_SAMPLES=%lld\n",name,count.load());
			fprintf(f,"%s_DESC=%s\n",name,desc);
			fprintf(f,"%s_STMTS=%s\n\n",name,stmts);

			float error=sqrt((estimate-avrg)*(estimate-avrg));

			if(error<dev)
				return 0;
			return error*count.load()/(float)samples;//wheigh error by coverage
		}
		else
		{
			float avrg=GetAverage()*cpt;
			float dev=GetStdDev()*cpt;
			fprintf(f,"#TIME(%s::%s)@%s: %.1f +/- %.1f [us] (%lld samples)\n",desc,stmts,name,avrg,dev,count.load());
			return avrg;
		}
    }
};

//profiling interface
class Profile
{
public:
    static ProfileEntry* Register(char* name,char* desc,char* stmts,double estimate,double correction);//returns propid (-PROFILE)
    static ProfileEntry* RegisterCustom(char* name,char* desc,char* stmts);//returns propid ('TIME())
    static void DumpProfileStats(char* file,int runs,float cpt);//dump default profile info to file
	static void DumpCustomProfileStats(int runs,float cpt);//dump human readable info to stdout
};

//group interface (groups guarantee non-overlapping calls to functions in the same group using cmp&swap+tbb wait)
class Group
{
public:

    static int registerGroup(char* group);//create group
    static void aquireGroup(int id); //wait for group to be free
    static void releaseGroup(int id); //release group

};

//void Init();

}

funky::uint64 tick(funky::uint64 clocks);
int getWorkerThreadID();
int getCurrentMaxThreadID();
int rand(); // RAND_MAX assumed to be 32767
void srand(unsigned int seed);

#endif	/* SCHEDULER_H */

