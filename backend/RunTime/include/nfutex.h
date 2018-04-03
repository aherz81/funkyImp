/*
 * File:   nfutex.h
 * Author: aherz
 *
 * Created on September 3, 2013, 10:56 AM
 */

#ifndef NFUTEX_H
#define	NFUTEX_H

//plain calls to native futex on linux

#include <linux/futex.h>
#include <sys/syscall.h>
namespace funky
{
class futex
{
public:
    static void wait(tbb::atomic<int> * on, int when)
    {
        syscall(__NR_futex, on, FUTEX_WAIT_PRIVATE, when, NULL, 0, 0);
    }

    static void wake(tbb::atomic<int> * on)
    {
        syscall(__NR_futex, on, FUTEX_WAKE_PRIVATE, 1, NULL, 0, 0);
    }

};
}

#endif	/* NFUTEX_H */

