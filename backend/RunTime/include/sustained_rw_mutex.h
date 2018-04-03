/*
 * File:   sustained_rw_lock.h
 * Author: aherz
 *
 * Created on March 6, 2012, 1:14 PM
 */

#ifndef SUSTAINED_RW_LOCK_H
#define	SUSTAINED_RW_LOCK_H

#include <stdio.h>
#include "futex.h"
#include <tbb/spin_mutex.h>

/*
 * - implements multiple reader single writer lock
 * - scoped_upgradeable_lock can be upgraded from reader to writer WITHOUT releasing lock! (unlike tbb::rw_lock)
 * - used to implement events (events first read only and finally write, other reads can be performed during the first part)
 */

namespace funky
{

//#define DBG(TXT,rd,ar) fprintf(stdout,"%s rd:%d, ar:%d\n",TXT,rd,ar);fflush(stdout);
#define DBG(TXT,rd,ar)


class sustained_rw_mutex
{
	//FIXME: was not pointer but static init fails in TBB??
    //use speculative_spin_mutex for lock elision
    tbb::spin_mutex writer __attribute__ ((aligned (16))); //doesn't need fairness, non over-lapping events never conflict
    tbb::atomic<int> readers __attribute__ ((aligned (16)));
    tbb::atomic<int> allow_readers __attribute__ ((aligned (16)));

public:

    class scoped_upgradeable_lock
    {
        sustained_rw_mutex* mutex;
        tbb::spin_mutex::scoped_lock lock;
    public:
        scoped_upgradeable_lock(sustained_rw_mutex & mutex)
        {
            acquire(mutex);
        }

        scoped_upgradeable_lock()
        {
            mutex=NULL;
        }

        bool try_acquire(sustained_rw_mutex & mutex)
        {
            if(lock.try_acquire(mutex.writer))
            {
                this->mutex=&mutex;
                DBG("write: try_acquire",mutex.readers.load(),mutex.allow_readers.load());
                return true;
            }

            return false;
        }

        void acquire(sustained_rw_mutex & mutex)
        {
            this->mutex=&mutex;
            lock.acquire(mutex.writer); //there can be only one upgradeable reader! this ONLY guarantees read access and upgradeability
            DBG("write: acquire",mutex.readers.load(),mutex.allow_readers.load());
        }

        void release()
        {
            if(mutex!=NULL)
            {
                DBG("write: release",mutex->readers.load(),mutex->allow_readers.load());

				//was it upgraded?
                if(!mutex->allow_readers)
                {
					//reallow readers
                    mutex->readers++;
                    mutex->allow_readers=1;
                    futex::wake(&mutex->allow_readers);//wake up potentially waiting readers
                }
                lock.release();
                mutex=NULL;
            }
        }

        ~scoped_upgradeable_lock()
        {
            release();
        }

    };

    class scoped_reader_lock
    {
        sustained_rw_mutex * mutex;
    public:
        scoped_reader_lock(sustained_rw_mutex & mutex)
        {
            acquire(mutex);
        }

        scoped_reader_lock()
        {
            mutex=NULL;
        }

        void acquire(sustained_rw_mutex & mutex)
        {
            this->mutex=&mutex; //mutex shared with other readers and the writer
            while(true)
            {
                if(!mutex.allow_readers)//wait until readers are allowed
                    futex::wait(&mutex.allow_readers,0);

                int rds=mutex.readers.load();//register ourselfes as reader
                if(rds>=0&&rds==mutex.readers.compare_and_swap(rds+1,rds))
                {
                    DBG("read: acquire",mutex.readers.load(),mutex.allow_readers.load());
                    break;
                }
            }
        }

        void release()
        {
            if(mutex!=NULL)
            {
                DBG("read: release",mutex->readers.load(),mutex->allow_readers.load());
                if(--mutex->readers==0)//release reader and notify writer wanting to upgrade
                    futex::wake(&mutex->readers);
                mutex=NULL;
            }
        }

        ~scoped_reader_lock()
        {
            release();
        }

    };

    sustained_rw_mutex():writer()
    {
        readers=0;
        allow_readers=1;
    }

	//FIXME: may only be called from scoped_upgradeable_lock
	//upgrade writer to allow writing
    void upgrade()
    {
        allow_readers=0;//we don't allow additional readers
        while(true)
        {
			//try to dec readers to -1
            if(0!=readers.compare_and_swap(-1,0))
            {
                int rds=readers.load();
                if(rds!=0)
                    futex::wait(&readers,rds);//wait for readers to reach 0
            }
            else
            {
                DBG("write: uprade",readers.load(),allow_readers.load());
                break;//success
            }
        }
    }
};

}

#endif	/* SUSTAINED_RW_LOCK_H */

