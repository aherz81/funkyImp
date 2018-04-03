#include <Task.h>


struct EVO : Singular
{
    EVO();
    
    int i /*  = 0 */ ;
    
    CONTEXT(Ev_int_Context,{EVO* self;int x;},{},{});
    
    DECLARE_TASK(Ev_int_Event120,Ev_int_Context);
    
    void Ev(int x);
    
};

