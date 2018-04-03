#include "EVO.h"

EVO::EVO() : i(0)
{
}



BEGIN_TASK(EVO,Ev_int_Event120,Ev_int_Context)
{
    tbb::recursive_mutex::scoped_lock lock(context().params.self->mutex);
        context().params.self->i = context().params.x;
    
    
    context().Release();
}
END_TASK()

void EVO::Ev(int x)
{
    tbb::recursive_mutex::scoped_lock lock;
    for(uint32 spin=0;spin<100;spin++)
    if(lock.try_acquire(mutex))
    {        i = x;
        return;
        
    }
    
    Ev_int_Context stack_context;
    const Ev_int_Context* context=&stack_context
    context->params.self=this;
    context->params.x=x;
    Thread::SpawnTask(new(tbb::task::allocate_root()) Ev_int_Event120(context);
}



