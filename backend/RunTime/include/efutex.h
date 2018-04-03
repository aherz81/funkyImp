/*
 * File:   efutex.h
 * Author: aherz
 *
 * Created on September 3, 2013, 9:49 AM
 */

#ifndef EFUTEX_H
#define	EFUTEX_H

#include <queue>
#include <map>
#include <tbb/mutex.h>
#include <tbb/atomic.h>
#include <tbb/queuing_mutex.h>
#include <condition_variable>

//emulate futex (or at least the part we're using) for systems that do not support it..not the fastest but w.t.f.
//this is virtually untested (only ran it on FiloIO.f) and not very performant, so theer are probably races and all
//kind of nasty problems, use at your own risk!

namespace funky
{
struct eWait
{
	std::condition_variable condition; //must use C++11 std cond var for predicate
	std::mutex mtx;
};

class futex
{
	std::queue<eWait*> queue;
	tbb::atomic<bool> signaled;
	tbb::atomic<bool> waiterSignaled;
	tbb::mutex mtx;
	static tbb::mutex& getMutex()
	{
		static tbb::mutex mtx;
		return mtx;
	}

	static std::map<tbb::atomic<int>*,futex*>& getMap()
	{
		static std::map<tbb::atomic<int>*,futex*> map;
		return map;
	}
public:
	futex()
	{
		signaled=false;
		waiterSignaled=false;
	}

	void esignal(tbb::atomic<int>* mem)
	{
		tbb::mutex::scoped_lock ul( mtx ); //we lock the whole futex

		if(queue.empty()) //nobody is waiting..just note that we received the signal
		{
//			printf("presignal fut:%016p\n",this);
//			fflush(stdout);
			signaled=true; //we cache up to one signal that was not waited for
		}
		else
		{
			eWait *w=queue.front(); //get first waiter
			queue.pop();

			{
				std::unique_lock<std::mutex> m(w->mtx);
				waiterSignaled=true;
				w->condition.notify_one(); //try to notify
			}
/*
			if(queue.empty())
			{
				tbb::mutex::scoped_lock ul( getMutex() );
				getMap().erase(mem);
			}
*/
		}
	}

	void ewait(tbb::atomic<int>* mem,int val)
	{
		if(signaled.compare_and_swap(false,true)) //signaled before wait? just grab the signal
		{
//			printf("presignaled wake fut:%016p\n",this);
//			fflush(stdout);

			return;
		}

		tbb::mutex::scoped_lock sl;

		sl.acquire(mtx);//lock futex

		eWait* w=new eWait(); //get new wait struct

		queue.push(w); //store in queue

		{
			std::unique_lock<std::mutex> m( w->mtx ); //lock wait's cond var

			sl.release(); //free futex!!!

			do
			{
				w->condition.wait(m, [&] { return *mem!=val; }); //block only if mem==val, notify only if mem!=val (is this atomic?))
			}
			while(waiterSignaled.compare_and_swap(false,true)!=true); //guard against spurious wakeups
		}

		delete w;

//		printf("wake fut:%016p\n",this);
//		fflush(stdout);

	}

	static futex* get(tbb::atomic<int>* mem) //get or create futex in map
	{
		tbb::mutex::scoped_lock ul( getMutex() );
		std::map<tbb::atomic<int>*,futex*>::iterator it = getMap().find(mem);
		futex *f;
		if(it==getMap().end())
		{
			f=new futex(); //note: these are NEVER deleted (until app shut down))
//			printf("create fut:%016p\n",f);
//			fflush(stdout);
			getMap().insert(std::make_pair(mem,f));
		}
		else
		{
			f=(*it).second;
//			printf("access fut:%016p\n",f);
//			fflush(stdout);

		}
		return f;
	}

	static void wait(tbb::atomic<int>* mem,int val)
	{
		if(*mem!=val) //do we need to bother?
			return;

		futex* f=get(mem);
		f->ewait(mem,val);
	}

	static void wake(tbb::atomic<int>* mem)
	{
		futex* f=get(mem);
		f->esignal(mem);
	}
};
}
#endif	/* EFUTEX_H */

