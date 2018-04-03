/*
    library with common constants and functions used for communicating via MPI
*/
#ifndef FUNKYMPI_H
#define FUNKYMPI_H

#include "mpi.h"
/*
    constants
*/
#define SCHEDULER        0
#define NOTIFY_ID_TAG  993
#define REGISTER_TAG   994
#define REQUEST_TAG    995
#define RELEASE_TAG    996
#define RESPONSE_TAG   997
#define DATA_TAG       998 
#define EXIT           999
#define SHUTDOWN      1000

/*
    helper functions
*/

class Util
{
    public:
        static int get_next_free_worker();
};


#endif
