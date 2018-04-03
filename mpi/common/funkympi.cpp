#include "funkympi.h"
#include <unistd.h>

int Util::get_next_free_worker()
{
    int tmp=0;
    MPI::COMM_WORLD.Ssend(&tmp, 1, MPI::INT, SCHEDULER, REQUEST_TAG);
    int next = 0;
    //blocks until worker is available
    MPI::COMM_WORLD.Recv(&next, 1, MPI::INT, SCHEDULER, RESPONSE_TAG);

    return next;
}
