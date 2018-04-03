#include "scheduler.h"

int main(int argc, char* argv[])
{
    MPI::Init(argc, argv);
    
    Scheduler *s = new Scheduler();
    s->init();
    s->schedule();
    
    MPI::Finalize();
    
    delete s;
    
    return 0;
}
