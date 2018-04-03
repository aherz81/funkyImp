#ifndef PTHREAD_REDIRECT
#define PTHREAD_REDIRECT

#pragma message("pthread redirected to boehm gc")

//include pthread.h BEFORE gc_cpp.h so that the odd specifier (throw()) is discarded

#include "/usr/include/pthread.h"

#define GC_THREADS
#define GC_NAMESPACE
#include "../boehm/bdwgc/include/gc_cpp.h"

#endif //PTHREAD_REDIRECT
