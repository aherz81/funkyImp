/*
 * File:   futex.h
 * Author: aherz
 *
 * Created on September 3, 2013, 11:31 AM
 */

#ifndef FUTEX_H
#define	FUTEX_H

#if __linux__ 
	#define NATIVE_FUTEX
#endif

#ifdef NATIVE_FUTEX

	#include "nfutex.h"

#else
//isset is defined as a macro on osx!
#undef isset
	#include "efutex.h"

#endif

#endif	/* FUTEX_H */

