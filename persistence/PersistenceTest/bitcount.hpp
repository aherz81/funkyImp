/* 
 * File:   bitcount.hpp
 * Author: aherz
 *
 * Created on April 1, 2011, 5:55 PM
 */

#ifndef BITCOUNT_HPP
#define	BITCOUNT_HPP

#include "persistence.hpp"

#ifdef ALIGN_DATA
#	define __aligned__ __attribute__((aligned(16)))
#else
#	define __aligned__
#endif

uint32 ssse3_popcount2(uint64* buffer);

#endif	/* BITCOUNT_HPP */

