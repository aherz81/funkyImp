/*
 * File:   futexmvar.h
 * Author: aherz
 *
 * Created on December 8, 2011, 4:55 PM
 */

#ifndef FSIGNAL_H__
#define	FSIGNAL_H__

//ALEX: careful, boehmgc uses signal.h and some structure called signal..should namespace this!!

#include <stdint.h>
#include <assert.h>

#include "futex.h"

#include <unistd.h>
#include <limits>

#include <errno.h>
#include <tbb/atomic.h>

namespace funky
{

//FIXME: non simple signal deadlocks???
#define SIMPLE_SIGNAL //non simple signal better for low grain tasks
#define SPIN 0

//contains a state and allows to wait on state update
class change
{
    tbb::atomic<int> m_state __attribute__ ((aligned (16)));

public:

    change(int state=0)
    {
        m_state = state;
    }

	inline void increment()
	{
		++m_state;
		futex::wake(&m_state); //someone might have been waiting on set
	}

	inline int state()
	{
		return m_state.load();
	}

	void wait(int old)
	{
        while (true)
        {
            if(old!=m_state.load())
            {
                return;
            }
            else
            {
                futex::wait(&m_state, old); //wait if signal still not set
            }
        }

	}
};

class unsetsignal
{
    tbb::atomic<int> m_state __attribute__ ((aligned (16)));
public:

    unsetsignal(int state=0)
    {
        m_state = state;
    }

    inline void reset()
    {
        m_state = 0;
    }

    inline void set()
    {
        m_state = 1;
    }

    inline bool isset()
    {
        return m_state>0;
    }

#ifdef SIMPLE_SIGNAL
 //8s
    inline void tryfire()
    {
		if(0==m_state.compare_and_swap(1,0))
		{
			futex::wake(&m_state); //someone might have been waiting on set
			return;
		}
    }

    void fire()
    {
        while (true)
        {
//            if (__sync_bool_compare_and_swap(&m_state, 0, 1))
            if(0==m_state.compare_and_swap(1,0))
            {
                futex::wake(&m_state); //someone might have been waiting on set
                return;
            }
            else
            {
                futex::wait(&m_state, 1); //wait
            }
        }
    }

    void wait()
    {
        while (true)
        {
//            if (__sync_bool_compare_and_swap(&m_state, 1, 0))
            if(1==m_state.compare_and_swap(0,1))
            {
                futex::wake(&m_state); //someone might have been waiting to set
                return;
            }
            else
            {
                futex::wait(&m_state, 0); //wait if signal still not set
            }
        }
    }
#else

    inline void fire()
    {
        while (true)
        {
            again:
            //if(__sync_bool_compare_and_swap(&m_state, -1, 1))
            if(-1==m_state.compare_and_swap(1,-1))
            {
                futex::wake(&m_state);
                return;
            }
            //if (__sync_bool_compare_and_swap(&m_state, 0, 1))
            if (0==m_state.compare_and_swap(1,0))
            {
                //futex::wake(&m_state); //someone might have been waiting on set
                return;
            }

            for(int i=0;i<SPIN;i++)
                if(m_state<=0)goto again;

            //if(__sync_bool_compare_and_swap(&m_state, 1, 2))
            if(1==m_state.compare_and_swap(2,1))
            {
                futex::wait(&m_state, 2); //wait
            }
        }
    }

    inline void wait()
    {
        while (true)
        {
            again:
//            if(__sync_bool_compare_and_swap(&m_state, 2, 0))
            if(2==m_state.compare_and_swap(0,2))
            {
                futex::wake(&m_state);
                return;
            }
//            if (__sync_bool_compare_and_swap(&m_state, 1, 0))
            if (1==m_state.compare_and_swap(0,1))
            {
                //futex::wake(&m_state); //someone might have been waiting on set
                return;
            }

            for(int i=0;i<SPIN;i++)
                if(m_state>0)goto again;

            //if(__sync_bool_compare_and_swap(&m_state, 0, -1))
            if(0==m_state.compare_and_swap(-1,0))
            {
                futex::wait(&m_state, -1); //wait
            }
        }
    }

#endif

};

class setsignal:public unsetsignal
{
public:
    setsignal(int state=1):unsetsignal(state){}
};

}

#endif	/* FUTEXMVAR_H */

