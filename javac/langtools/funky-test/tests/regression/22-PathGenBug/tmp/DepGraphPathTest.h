#include <Task.h>
#include "EVO.h"


struct DepGraphPathTest
{
    DepGraphPathTest();
    
    CONTEXT(DepGraphTest_int_int_Context,{DepGraphPathTest* self;int x;int y;},{int a;int d;int e;int g;int h;},{task_handle* task_instance_DepGraphTest_int_int_Task0;task_handle* task_instance_DepGraphTest_int_int_Task0;task_handle* task_instance_DepGraphTest_int_int_Task0;task_handle* task_instance_DepGraphTest_int_int_Task0;});
    
    DECLARE_TASK(DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context);
    
    DECLARE_TASK(DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context);
    
    DECLARE_TASK(DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context);
    
    DECLARE_TASK(DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context);
    
    DECLARE_TASK(DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context);
    
    void DepGraphTest(int x, int y);
    
    CONTEXT(DepGraphTest2_int_int_Context,{DepGraphPathTest* self;int x;int y;},{int v;int v_1;int w_1;},{task_handle* task_instance_DepGraphTest2_int_int_Task0;task_handle* task_instance_DepGraphTest2_int_int_Task0;task_handle* task_instance_DepGraphTest2_int_int_Task0;});
    
    DECLARE_TASK(DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context);
    
    DECLARE_TASK(DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context);
    
    DECLARE_TASK(DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context);
    
    DECLARE_TASK(DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context);
    
    int DepGraphTest2(int x, int y);
    
    static int main();
    
    int g1(int x);
    
    int g2(int x, int y);
    
    int g3(int x, int y, int z);
    
};

