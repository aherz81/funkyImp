#include "cur.h"

int cur::g_int_int(int x)
{
    return 2 * x;
}

funky::LinearArray< int >::Version* 
  cur::f_int_three_d_256_256_256___int_three_d_256_256_256__
  (funky::LinearArray< int >::Version* ma)
{
  return [&]() ->funky::LinearArray< int >::Version* {
    if (1) {
      return
        ([&]()->funky::LinearArray< int >::Version*
        {
          funky::LinearArray< int >::Version*__VAL_TMP0=ma;
          funky::LinearArray< int >::Version*__EXP_TMP0
            = __VAL_TMP0->getNewVersion();
          
          // Compile the kernel
          ocl_kernel f_0(&device,"tmp/f_0.cl");
          
          // Translate ma into a format
          // that is understandable by OpenCL
          int *__ma_0=ma->toNative();
          ocl_mem __ma_GPU_0 
            = device.malloc(sizeof(int)*16777216,
                                CL_MEM_READ_ONLY);
          int __ma0_dim_0=256;
          int __ma0_dim_1=256;
          int __ma0_dim_2=256;

          // Create the return value
          int *__return_val_0=new int[16777216];
          ocl_mem __return_val_GPU_0 
            = device.malloc(sizeof(int)*16777216,
                                CL_MEM_WRITE_ONLY);
          // Copy ma to the GPU
          __ma_GPU_0.copyFrom(__ma_0);

          // Set the Kernel Arguments
          f_0.setArgs(__ma_GPU_0.mem(),&__ma0_dim_0,
                      &__ma0_dim_1,&__ma0_dim_2,
                      __return_val_GPU_0.mem());

          //Run the kernel and wait for it to be done.
          int id = f_0.timedRun(1024, 16777216);
          device.finish();

          // Translate the return value back to the
          // funkyIMP format.
          __return_val_GPU_0.copyTo(__return_val_0);
          funky::LinearArray<int> *__return_LINARR0
            = new funky::LinearArray<int>(16777216,
                                        __return_val_0);
          funky::LinearArray< int >::Version* __return_0
            = new funky::LinearArray<int>
                    ::Version(__return_LINARR0,
                      3,256,256,256);
                return __return_0;
            }) ();
        } else {
            // Generated CPU iteration
        }
    }();
}


int cur::main_int_String_one_d_01___int
  (int argc, funky::LinearArray< char* >::Version* argv)
{
  funky::LinearArray< int >::Version* test_a;
  funky::LinearArray< int >::Version* test = [&]() ->funky::LinearArray< int >::Version* {
  if (1) {
    return
      ([&]()->funky::LinearArray< int >::Version*
      {
        funky::LinearArray< int >::Version*__VAL_TMP0
          = new funky::LinearArray< int >::Version(
              new funky::LinearArray< int >(16777216,true));
        funky::LinearArray< int >::Version*__EXP_TMP0 
          = __VAL_TMP0;
        ocl_kernel main_1(&device,"tmp/main_1.cl");
        int __new_array_0_dim_0=256;
        int __new_array_0_dim_1=256;
        int __new_array_0_dim_2=256;
        int *__return_val_0 = new int[16777216];
        ocl_mem __return_val_GPU_0
          = device.malloc(sizeof(int)*16777216,CL_MEM_WRITE_ONLY);
        main_1.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,
                        &__new_array_0_dim_2,__return_val_GPU_0.mem());
        int id = main_1.timedRun(1024, 16777216);
        device.finish();
        __return_val_GPU_0.copyTo(__return_val_0);
        funky::LinearArray<int> *__return_LINARR0 
          = new funky::LinearArray<int>(16777216,__return_val_0);
        funky::LinearArray< int >::Version* __return_0
          = new funky::LinearArray< int >::Version(
                    __return_LINARR0,3,256,256,256);
        return __return_0;
      }) ();
    } else {
      return
        ([&]()->funky::LinearArray< int >::Version*
        {
          //CPU iteration
        }) ();
    }
    }();
  
  test_a = f_int_three_d_256_256_256___int_three_d_256_256_256__(test);  
  return 0;
}

CONTEXT(int,StartContext,{int argc; char** argv;},{},{});
GLOBAL_TASK(RUN, StartContext){context()->SetReturn(cur::main_int_String_one_d_01___int(context()->params.argc,(new funky::LinearArray<char*>::Version(new funky::LinearArray<char*>(context()->params.argc)))->map([&](funky::uint32 i){return context()->params.argv[i];},0,context()->params.argc)));}END_GLOBAL_TASK()
GLOBAL_TASK(START, StartContext){set_ref_count(2);spawn_and_wait_for_all(*new( allocate_child()) RUN(context()));}END_GLOBAL_TASK()
GLOBAL_TASK(WORKER, StartContext){usleep(100000);}END_GLOBAL_TASK()
GLOBAL_TASK(START_WORKERS, StartContext){set_ref_count(2);for(funky::uint32 i=0;i<1;i++)spawn(*new (allocate_child()) WORKER(context()));wait_for_all();}END_GLOBAL_TASK()

int main(int argc, char* argv[])
{
    GC_INIT();
    tbb::task_scheduler_init();
    StartContext sp;
    sp.params.argc = argc;
    sp.params.argv = argv;
    tbb::task::spawn_root_and_wait(*new (tbb::task::allocate_root()) START_WORKERS(&sp));
    device = ocl::getDevice(0,2);
    funky::TaskRoot<>::GetRoot() = new(tbb::task::allocate_root()) START(&sp);
    tbb::task::spawn_root_and_wait(*funky::TaskRoot<>::GetRoot());
    return sp.GetReturn();
    
}

