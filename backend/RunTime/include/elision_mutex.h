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
    //use speculative_spin_mutex for lock elision
    tbb::speculative_spin_mutex writer __attribute__ ((aligned (16))); //doesn't need fairness, non over-lapping events never conflict

public:

    class scoped_upgradeable_lock
    {
        sustained_rw_mutex* mutex;
        tbb::speculative_spin_mutex::scoped_lock lock;
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
                lock.release();
                mutex=NULL;
            }
        }

        ~scoped_upgradeable_lock()
        {
            release();
        }

    };
    
#define scoped_reader_lock scoped_upgradeable_lock

    sustained_rw_mutex():writer()
    {
    }

    void upgrade()
    {
    }
};

}

#endif	/* SUSTAINED_RW_LOCK_H */

