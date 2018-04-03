#include "DepGraphPathTest.h"

DepGraphPathTest::DepGraphPathTest()
{
}

BEGIN_TASK(DepGraphPathTest,DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context)
{
    
    context().frame.a = g1(context().params.x);
    context().tasks.DepGraphTest_int_int_Task0->decrement_ref_count();
    context().tasks.DepGraphTest_int_int_Task0->decrement_ref_count();
    
    int b = g1(context().frame.a);
    
    context().frame.d = g1(b);
    context().tasks.DepGraphTest_int_int_Task0->decrement_ref_count();
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context)
{
    
    int i = g3(context().frame.d, context().frame.h, context().frame.g);
    
    context().Release();
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context)
{
    
    int f = g2(context().frame.e, context().frame.a);
    
    context().frame.h = g2(context().frame.e, f);
    context().tasks.DepGraphTest_int_int_Task0->decrement_ref_count();
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context)
{
    
    EVO* evo = new EVO();
    
    context().Release();
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest_int_int_Task0,DepGraphTest_int_int_Context)
{
    
    context().frame.g = g1(context().frame.a);
    context().tasks.DepGraphTest_int_int_Task0->decrement_ref_count();
    
    context().frame.e = g1(context().frame.a);
    context().tasks.DepGraphTest_int_int_Task0->decrement_ref_count();
    
}
END_TASK()

void DepGraphPathTest::DepGraphTest(int x, int y)
{

    const DepGraphTest_int_int_Context* context=new DepGraphTest_int_int_Context(3,true);
    context->params.self=this;
    context->params.x=x;
    context->params.y=y;
    task_handle& self_task=SELF();
    tbb::task_list dep_tasks,join_tasks;
    task_handle* task_instance_DepGraphTest_int_int_Task0=new(self_task.allocate_root()) DepGraphTest_int_int_Task0(context);
    dep_tasks.push_back(*task_instance_DepGraphTest_int_int_Task0);
    task_handle* task_instance_DepGraphTest_int_int_Task0=new(self_task.allocate_root()) DepGraphTest_int_int_Task0(context);
    task_instance_DepGraphTest_int_int_Task0->set_ref_count(3);
    context->tasks.task_instance_DepGraphTest_int_int_Task0=task_instance_DepGraphTest_int_int_Task0
    dep_tasks.push_back(*task_instance_DepGraphTest_int_int_Task0);
    task_handle* task_instance_DepGraphTest_int_int_Task0=new(self_task.allocate_root()) DepGraphTest_int_int_Task0(context);
    task_instance_DepGraphTest_int_int_Task0->set_ref_count(2);
    context->tasks.task_instance_DepGraphTest_int_int_Task0=task_instance_DepGraphTest_int_int_Task0
    dep_tasks.push_back(*task_instance_DepGraphTest_int_int_Task0);
    task_handle* task_instance_DepGraphTest_int_int_Task0=new(self_task.allocate_root()) DepGraphTest_int_int_Task0(context);
    dep_tasks.push_back(*task_instance_DepGraphTest_int_int_Task0);
    task_handle* task_instance_DepGraphTest_int_int_Task0=new(self_task.allocate_root()) DepGraphTest_int_int_Task0(context);
    task_instance_DepGraphTest_int_int_Task0->set_ref_count(1);
    context->tasks.task_instance_DepGraphTest_int_int_Task0=task_instance_DepGraphTest_int_int_Task0
    dep_tasks.push_back(*task_instance_DepGraphTest_int_int_Task0);
    self_task.spawn(dep_tasks);
    context->Release();}

BEGIN_TASK(DepGraphPathTest,DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context)
{
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context)
{
    
    if(context().Return()){context().SetReturn<int>(g2(context().frame.v_1, context().frame.w_1));}
    context().tasks.DepGraphTest2_int_int_Task0->decrement_ref_count();
    context().tasks.DepGraphTest2_int_int_Task0->decrement_ref_count();
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context)
{
    
    context().frame.v = g1(context().params.x);
    context().tasks.DepGraphTest2_int_int_Task0->decrement_ref_count();
    
    context().frame.v_1 = g1(context().frame.v);
    context().tasks.DepGraphTest2_int_int_Task0->decrement_ref_count();
    
}
END_TASK()

BEGIN_TASK(DepGraphPathTest,DepGraphTest2_int_int_Task0,DepGraphTest2_int_int_Context)
{
    int i_1;
    
    int w = g1(context().params.y);
    
    int i = g1(w);
    
    i_1 = g2(context().frame.v, i);
    
    context().frame.w_1 = g2(w, i_1);
    context().tasks.DepGraphTest2_int_int_Task0->decrement_ref_count();
    
}
END_TASK()

int DepGraphPathTest::DepGraphTest2(int x, int y)
{

    const DepGraphTest2_int_int_Context* context=new DepGraphTest2_int_int_Context(1,false);
    context->params.self=this;
    context->params.x=x;
    context->params.y=y;
    task_handle& self_task=SELF();
    self_task.set_ref_count(2);
    tbb::task_list dep_tasks,join_tasks;
    task_handle* task_instance_DepGraphTest2_int_int_Task0=new(self_task.allocate_root()) DepGraphTest2_int_int_Task0(context);
    dep_tasks.push_back(*task_instance_DepGraphTest2_int_int_Task0);
    task_handle* task_instance_DepGraphTest2_int_int_Task0=new(self_task.allocate_root()) DepGraphTest2_int_int_Task0(context);
    task_instance_DepGraphTest2_int_int_Task0->set_ref_count(2);
    context->tasks.task_instance_DepGraphTest2_int_int_Task0=task_instance_DepGraphTest2_int_int_Task0
    join_tasks.push_back(*task_instance_DepGraphTest2_int_int_Task0);
    task_handle* task_instance_DepGraphTest2_int_int_Task0=new(self_task.allocate_root()) DepGraphTest2_int_int_Task0(context);
    dep_tasks.push_back(*task_instance_DepGraphTest2_int_int_Task0);
    task_handle* task_instance_DepGraphTest2_int_int_Task0=new(self_task.allocate_root()) DepGraphTest2_int_int_Task0(context);
    task_instance_DepGraphTest2_int_int_Task0->set_ref_count(1);
    context->tasks.task_instance_DepGraphTest2_int_int_Task0=task_instance_DepGraphTest2_int_int_Task0
    dep_tasks.push_back(*task_instance_DepGraphTest2_int_int_Task0);
    self_task.spawn(dep_tasks);
    self_task.spawn_root_and_wait(join_tasks);
    //if(context->Return()) run final because nobody returned!
    int retval=context->GetReturn<int>();
    return retval;}

int DepGraphPathTest::main()
{
    return 0;
    
}

int DepGraphPathTest::g1(int x)
{
    return x + 1;
    
}

int DepGraphPathTest::g2(int x, int y)
{
    return x + y;
    
}

int DepGraphPathTest::g3(int x, int y, int z)
{
    return x + y + z;
    
}



