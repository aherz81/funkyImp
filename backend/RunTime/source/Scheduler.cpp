#include <unistd.h>
#include <stdio.h>
#include <assert.h>

#include <stdlib.h>
#include <string.h>

#include "../include/Scheduler.h"

#include <set>
#include <map>
#include <vector>
#include <string>
#include <algorithm>
#include <iostream>

#include <tbb/mutex.h>

using std::cout;
using std::endl;

namespace funky
{

#ifdef REFCOUNT_DEBUG
	tbb::atomic<uint32> RefCountBase::liveContexts;
#endif
/*
void Init()
{
	char* FP=getenv("FUNKY_LIB_PATH");

	if(FP!=NULL)
	{
		char*  LDP=getenv("LD_LIBRARY_PATH");
		char colon[]=":";
		if(LDP!=NULL)
			setenv("LD_LIBRARY_PATH",strcat(FP,strcat(colon,LDP)),1);
		else
			setenv("LD_LIBRARY_PATH",FP,1);
	}
	else
		printf("environment variable FUNKY_LIB_PATH not set, continue on your own risk ...\n");
}
*/
tbb::enumerable_thread_specific<Thread*,tbb::cache_aligned_allocator<Thread*>,tbb::ets_key_usage_type::ets_key_per_instance> current_thread;


#ifdef _DEBUG
	AddressFixedArray<Thread,THREAD_PAGE_SIZE,true,true> gThreadPool("Blocking Threads");
#else
	AddressFixedArray<Thread,THREAD_PAGE_SIZE,false,true> gThreadPool("Blocking Threads");
#endif


void Thread::Work()
{
    task->execute();

	//CAREFUL: tbb::task::destroy decrements refcount of parent but DOES NOT SPAWN parent
	task_handle* parent=task->parent();
	if(parent)
	{
		if(!sig_free)
			parent->decrement_ref_count(); //spawning ref counting ourselfes
		task->set_parent(NULL); //remove parent so that destroy doesn't mess up our refcount
	}

	tbb::task::destroy(*task); //frees task

	if(context!=NULL)
		context->release();

	context=NULL;
	task=NULL;

	if(sig_free)
	{
		sig_free=false;
		SignalFree();
	}

	gThreadPool.free(this); //default thread-pool thread, released after usage

	SuspendTask();
}

void* Thread::run(void* vo)
{

#ifdef DEBUG_THREADS
	LOG("IO worker started\n");
#endif
	thread_handle* o=(thread_handle*)vo;
	current_thread.local()=o;

	o->SuspendTask();

#ifdef DEBUG_THREADS
        LOG("IO worker thread running\n");
#endif

	while(true){o->Work();}

	return NULL;
}

void Thread::WorkPermanent()
{
    task->execute();

	task_handle* parent=task->parent();
	if(parent)
	{
		if(!sig_free)
			parent->decrement_ref_count();
		task->set_parent(NULL);
	}

	tbb::task::destroy(*task);

	if(context!=NULL)
		context->release();

	context=NULL;
	task=NULL;

	SignalFree();

	if(sig_free)
	{
		sig_free=false;
		SignalFree();
	}

	SuspendTask();
}

Thread* Thread::getCurrentThread()
{
	if(current_thread.empty())
		return NULL;

	return current_thread.local();
}

void* Thread::runPermanent(void* vo)
{
#ifdef DEBUG_THREADS
	LOG("IO worker started\n");
#endif
	thread_handle* o=(thread_handle*)vo;
	current_thread.local()=o; //store current special-purpose thread in TLS for getCurrentThread

	o->SuspendTask();

#ifdef DEBUG_THREADS
        LOG("IO worker thread running\n");
#endif

	while(true){o->WorkPermanent();}

	return NULL;
}

//return thread from pool
thread_handle* Thread::getOSThread()
{
    return gThreadPool.get();
}

/********Debug Interface**********/

//store currently running tasks
tbb::mutex thread_mutex;

std::map<std::string,thread_handle*>& getThreads()
{
	static std::map<std::string,thread_handle*> threads;
	return threads;
}

thread_handle* Thread::registerThread(char* thread)
{
	tbb::mutex::scoped_lock lock(thread_mutex);

	std::string name(thread);

	std::map<std::string,thread_handle*>::iterator i=getThreads().find(name);

	if(i!=getThreads().end())
		return (*i).second;
	else
	{
		thread_handle* id=new Thread(true);
		getThreads().insert(std::make_pair(name,id));
#ifdef DEBUG_THREADS
		printf("registeredThread(%s):0x%x\n",thread,id);
#endif
		return id;
	}
}


//DEBUG FUNS

std::set<uint32> tasks;
tbb::mutex task_mutex;

void Debug::StartTask(uint32 name)
{
	tbb::mutex::scoped_lock lock(task_mutex);
	tasks.insert(name);
}

void Debug::FinishTask(uint32 name)
{
	tbb::mutex::scoped_lock lock(task_mutex);
	tasks.erase(name);
}

void Debug::Abort()
{
	fprintf(stdout,"\n<<<ABNORMAL TERMINATION>>>\n");
	tbb::mutex::scoped_lock lock(task_mutex);
	if(tasks.size()>0)
	{
		fprintf(stdout,"\n<<<DUMP (%d) RUNNING TASKS>>>\n",(int)tasks.size());

		std::ostream_iterator< int > output( std::cerr, " " );
		std::copy( tasks.begin(), tasks.end(), output );

		fprintf(stdout,"\n<<<(%d)>>>\n",(int)tasks.size());
		fflush(stdout);
	}
	else
	{
		fprintf(stdout,"\n<<<(%d) RUNNING TASKS>>>\n",(int)tasks.size());
		fflush(stdout);
	}
}

void Debug::Exit()
{
#ifdef REFCOUNT_DEBUG
	fprintf(stdout,"\n<<<(%d) LEAKING CONTEXTS>>>\n",RefCountBase::liveContexts.load());
#endif

	fprintf(stdout,"\n<<<CLEAN_EXIT>>>\n");
	fflush(stdout);


}
/*
void Debug::RTAlloc(void* addr,uint32 count,char* name)
{
	tbb::mutex::scoped_lock lock(context_mutex);
	context.insert(std::make_pair(addr,Entry(count,name)));
	fprintf(stdout,"%s: allocated dynamic context %p[%d]\n",name,addr,count);
}

void Debug::RTFree(void* addr,uint32 task)
{
	tbb::mutex::scoped_lock lock(context_mutex);
	std::map<void*,Entry>::iterator i=context.find(addr);
	if(i!=context.end())
	{
		Entry& e=(*i).second;
		if(e.freed.erase(task))
		{
			if(e.freed.empty())
			{
				fprintf(stdout,"%s: freed dynamic context %p\n",e.name,addr);
				context.erase(addr);
			}
		}
		else
			fprintf(stdout,"Task(%d) not registered to free context %p (%s)\n",task,addr,e.name);
	}
	else
		fprintf(stdout,"Task(%d) tried to free unknown context 0x%p\n",task,addr);
	fflush(stdout);
}

void Debug::RTRequestFree(void* addr,uint32 task)
{
	tbb::mutex::scoped_lock lock(context_mutex);
	Entry& e=context.at(addr);
	e.freed.insert(task);
}
*/

/********Group Interface**********/

tbb::mutex group_mutex;
std::vector<tbb::concurrent_queue< tbb::task*> > group_queues;
std::vector<tbb::atomic<bool> > group_status; //true if group is free

std::map<std::string,int>& getGroupIds()
{
	static std::map<std::string,int> group_ids;
	return group_ids;
}

int Group::registerGroup(char* group)
{
	tbb::mutex::scoped_lock lock(group_mutex);

	std::string name(group);

	std::map<std::string,int>::iterator i=getGroupIds().find(name);

	if(i!=getGroupIds().end())
		return (*i).second;
	else
	{
		int id=getGroupIds().size();
		getGroupIds().insert(std::make_pair(name,id));
		group_queues.push_back(tbb::concurrent_queue< tbb::task*>());
		group_status.push_back(tbb::atomic<bool>());
		group_status[group_status.size()-1]=true;
		return id;
	}
}

class EmptyTask:public tbb::task
{
public:
	EmptyTask()
	{
	}

	tbb::task* execute()
	{
		return NULL;
	}
};

void Group::aquireGroup(int id)
{
	//tbb::mutex::scoped_lock lock(group_mutex);
	//check if the group is free and lock it (if so)
	bool may_enter=group_status[id].compare_and_swap(false,true);

	tbb::task* t;
	//ensure fairness...
	//if the group is not free or if it is free but somebody else was waiting before us
	if(!may_enter||(group_queues[id].try_pop(t)&&t->decrement_ref_count()>-1))
	{
		//creat empty task to wait for
		tbb::task* t= (new (tbb::task::allocate_additional_child_of(*TaskRoot<>::GetRoot())) EmptyTask());
		t->set_ref_count(2);

		//enque ourselfs
		group_queues[id].push(t);//should be thread safe..push to front (fifo)

		//FIXME: wait fails if current task is non-tbb, can this be called from non-tbb thread at all??
		//and wait
		t->wait_for_all();

		//do refc (as destroy doesn't)
		TaskRoot<>::GetRoot()->decrement_ref_count(); //spawning ref counting ourselfes
		t->set_parent(NULL); //remove parent so that destroy doesn't mess up our refcount

		tbb::task::destroy(*t);
	}
}

void Group::releaseGroup(int id)
{
	//must reset group status first (or we might loose some tasks in the queue)
	group_status[id]=true;

	tbb::task* t;
	if(group_queues[id].try_pop(t))//so if we need to notify anyone...
	{
		t->decrement_ref_count();
	}
}

/********Profile Interface**********/

tbb::mutex profile_mutex;
std::vector<ProfileEntry*> profile_data;

ProfileEntry* Profile::Register(char* name,char* desc,char* stmts,double estimate,double correction)
{
	tbb::mutex::scoped_lock lock(profile_mutex);
	ProfileEntry* entry=new ProfileEntry(name,desc,stmts,estimate,correction,false);
	profile_data.push_back(entry);
	return entry;
}

ProfileEntry* Profile::RegisterCustom(char* name,char* desc,char* stmts)
{
	tbb::mutex::scoped_lock lock(profile_mutex);
	ProfileEntry* entry=new ProfileEntry(name,desc,stmts,0.0,0.0,true);
	profile_data.push_back(entry);
	return entry;
}

void Profile::DumpCustomProfileStats(int runs,float cpt)//dump a .properties file
{
	for(std::vector<ProfileEntry*>::iterator i = profile_data.begin();i!=profile_data.end();i++)
	{
		if((*i)->custom)
			(*i)->Dump(stdout,cpt,runs);
	}

}

void Profile::DumpProfileStats(char* file,int runs,float cpt)//dump a .properties file
{
	tbb::mutex::scoped_lock lock(profile_mutex);

	FILE* results=fopen(file,"w+b");

	fprintf(results,"#measured task times (in clock ticks) for the tasks composed of the statments given in _DESC\n");

	fprintf(results,"TASKS=");
	for(std::vector<ProfileEntry*>::iterator i = profile_data.begin();i!=profile_data.end();i++)
		if(!(*i)->custom)
			fprintf(results,"%s,",(*i)->name);

	fprintf(results,"\n");

	fprintf(results,"RUNS=%d\n",runs);

	float avrg_error=0.f;
	for(std::vector<ProfileEntry*>::iterator i = profile_data.begin();i!=profile_data.end();i++)
	{
		if(!(*i)->custom)
			avrg_error+=(*i)->Dump(results,cpt,runs);
	}

	fclose(results);

	printf("\n\nPROFILING %d RUNs @%.1f clocks per time, error: %.1fe6\n",runs,cpt,avrg_error/profile_data.size()*0.000001);

}



#if defined(__i386__)

static __inline__ unsigned long long rdtsc(void)
{
  unsigned long long int x;
     __asm__ volatile (".byte 0x0f, 0x31" : "=A" (x));
     return x;
}
#elif defined(__x86_64__)

static __inline__ unsigned long long rdtsc(void)
{
  unsigned hi, lo;
  __asm__ __volatile__ ("rdtsc" : "=a"(lo), "=d"(hi));
  return ( (unsigned long long)lo)|( ((unsigned long long)hi)<<32 );
}

#elif defined(__powerpc__)

static __inline__ unsigned long long rdtsc(void)
{
  unsigned long long int result=0;
  unsigned long int upper, lower,tmp;
  __asm__ volatile(
                "0:                  \n"
                "\tmftbu   %0           \n"
                "\tmftb    %1           \n"
                "\tmftbu   %2           \n"
                "\tcmpw    %2,%0        \n"
                "\tbne     0b         \n"
                : "=r"(upper),"=r"(lower),"=r"(tmp)
                );
  result = upper;
  result = result<<32;
  result = result|lower;

  return(result);
}

#else

#error "No tick counter is available!"

#endif


/*  $RCSfile:  $   $Author: kazutomo $
 *  $Revision: 1.6 $  $Date: 2005/04/13 18:49:58 $
 */

}

funky::uint64 tick(funky::uint64 clocks)
{
	//clocks-=30; //overhead
	if(clocks<50) //skip minuscle waits
		return 0;
	else
		clocks-=50; //remove overhead

	funky::uint64 ticks=funky::rdtsc();

	while(funky::rdtsc()-ticks<clocks); //busy waiting clocks cycles

	return ticks;
}

tbb::enumerable_thread_specific<int,tbb::cache_aligned_allocator<int>,tbb::ets_key_usage_type::ets_key_per_instance> workerThreadId;
tbb::atomic<int> usedIds;

int getWorkerThreadID()
{
	if(workerThreadId.empty())
	{
		workerThreadId.local()=(++usedIds);
	}
	return workerThreadId.local();
}

int getCurrentMaxThreadID()
{
	return usedIds;
}

static unsigned long int nextRand = 1;

int rand() // RAND_MAX assumed to be 32767
{
    nextRand = nextRand * 1103515245 + 12345;
    return (unsigned int)(nextRand/65536) % 32768;
}

void srand(unsigned int seed)
{
    nextRand = seed;
}

/*
funky::uint64 tick(funky::uint64 clocks)
{
	//clocks-=900; //overhead
	funky::uint64 n=clocks/7.3f;
	funky::uint64 i=0;

	__asm__ volatile (
		"0:;"
		"incq %[i];"
		"cmpq %[n],%[i];" // GAS syntax
		"jl 0b;"
		: [i] "+m" (i)
		: [n] "r" (n)
		);


	return i;
}
*/
/*

	funky::uint64 n=clocks/1000;
	volatile funky::uint64 i=0;

	__asm__ volatile (
		"0:;"
		"incq %[i];"
		"cmpq %[n],%[i];" // GAS syntax
		"jl 0b;"
		: [i] "+m" (i)
		: [n] "r" (n)
		);
	for(;i<n;i++);

 */


void run_cpuid(uint32_t eax, uint32_t ecx, uint32_t* abcd)
{
#if defined(_MSC_VER)
    __cpuidex(abcd, eax, ecx);
#else
    uint32_t ebx, edx;
# if defined( __i386__ ) && defined ( __PIC__ )
     /* in case of PIC under 32-bit EBX cannot be clobbered */
    __asm__ ( "movl %%ebx, %%edi \n\t cpuid \n\t xchgl %%ebx, %%edi" : "=D" (ebx),
# else
    __asm__ ( "cpuid" : "+b" (ebx),
# endif
              "+a" (eax), "+c" (ecx), "=d" (edx) );
    abcd[0] = eax; abcd[1] = ebx; abcd[2] = ecx; abcd[3] = edx;
#endif
}

int check_HLE()
{
    uint32_t abcd[4];
    uint32_t hle_mask = (1 << 4);

	//CPUID.(EAX=07H, ECX=0H).EBX.HLE[bit 4]==1
	run_cpuid( 7, 0, abcd );
    if ( (abcd[1] & hle_mask) != hle_mask )
        return 0;
    return 1;
}

class StartupCheck
{
public:
	StartupCheck()
	{
		if(check_HLE())
        {
#ifdef LOCK_ELISION
			printf("<<FUNKY RUNTIME>> HLE support detected\n");
#else
			printf("<<FUNKY RUNTIME>> HLE support disabled\n");
#endif
        }
		else
			printf("<<FUNKY RUNTIME>> HLE support missing\n");
	}
};

StartupCheck startup;


