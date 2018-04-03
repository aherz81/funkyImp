#include "cur.h"



float cur::blur_32_32_float_two_d_32_32___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 32 - 1 || y == 32 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 32L) + matrix->access(((y)) + ((x - 1)) * 32L) + matrix->access(((y + 1)) + ((x - 1)) * 32L) + matrix->access(((y - 1)) + ((x)) * 32L) + matrix->access(((y)) + ((x)) * 32L) + matrix->access(((y + 1)) + ((x)) * 32L) + matrix->access(((y - 1)) + ((x + 1)) * 32L) + matrix->access(((y)) + ((x + 1)) * 32L) + matrix->access(((y - 1)) + ((x - 1)) * 32L);
        

        

    }
    

}


float cur::blur_32_64_float_two_d_32_64___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 32 - 1 || y == 64 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 64L) + matrix->access(((y)) + ((x - 1)) * 64L) + matrix->access(((y + 1)) + ((x - 1)) * 64L) + matrix->access(((y - 1)) + ((x)) * 64L) + matrix->access(((y)) + ((x)) * 64L) + matrix->access(((y + 1)) + ((x)) * 64L) + matrix->access(((y - 1)) + ((x + 1)) * 64L) + matrix->access(((y)) + ((x + 1)) * 64L) + matrix->access(((y - 1)) + ((x - 1)) * 64L);
        

        

    }
    

}


float cur::blur_64_64_float_two_d_64_64___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 64 - 1 || y == 64 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 64L) + matrix->access(((y)) + ((x - 1)) * 64L) + matrix->access(((y + 1)) + ((x - 1)) * 64L) + matrix->access(((y - 1)) + ((x)) * 64L) + matrix->access(((y)) + ((x)) * 64L) + matrix->access(((y + 1)) + ((x)) * 64L) + matrix->access(((y - 1)) + ((x + 1)) * 64L) + matrix->access(((y)) + ((x + 1)) * 64L) + matrix->access(((y - 1)) + ((x - 1)) * 64L);
        

        

    }
    

}


float cur::blur_64_128_float_two_d_64_128___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 64 - 1 || y == 128 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 128L) + matrix->access(((y)) + ((x - 1)) * 128L) + matrix->access(((y + 1)) + ((x - 1)) * 128L) + matrix->access(((y - 1)) + ((x)) * 128L) + matrix->access(((y)) + ((x)) * 128L) + matrix->access(((y + 1)) + ((x)) * 128L) + matrix->access(((y - 1)) + ((x + 1)) * 128L) + matrix->access(((y)) + ((x + 1)) * 128L) + matrix->access(((y - 1)) + ((x - 1)) * 128L);
        

        

    }
    

}


float cur::blur_128_128_float_two_d_128_128___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 128 - 1 || y == 128 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 128L) + matrix->access(((y)) + ((x - 1)) * 128L) + matrix->access(((y + 1)) + ((x - 1)) * 128L) + matrix->access(((y - 1)) + ((x)) * 128L) + matrix->access(((y)) + ((x)) * 128L) + matrix->access(((y + 1)) + ((x)) * 128L) + matrix->access(((y - 1)) + ((x + 1)) * 128L) + matrix->access(((y)) + ((x + 1)) * 128L) + matrix->access(((y - 1)) + ((x - 1)) * 128L);
        

        

    }
    

}


float cur::blur_256_128_float_two_d_256_128___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 256 - 1 || y == 128 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 128L) + matrix->access(((y)) + ((x - 1)) * 128L) + matrix->access(((y + 1)) + ((x - 1)) * 128L) + matrix->access(((y - 1)) + ((x)) * 128L) + matrix->access(((y)) + ((x)) * 128L) + matrix->access(((y + 1)) + ((x)) * 128L) + matrix->access(((y - 1)) + ((x + 1)) * 128L) + matrix->access(((y)) + ((x + 1)) * 128L) + matrix->access(((y - 1)) + ((x - 1)) * 128L);
        

        

    }
    

}


float cur::blur_256_256_float_two_d_256_256___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 256 - 1 || y == 256 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 256L) + matrix->access(((y)) + ((x - 1)) * 256L) + matrix->access(((y + 1)) + ((x - 1)) * 256L) + matrix->access(((y - 1)) + ((x)) * 256L) + matrix->access(((y)) + ((x)) * 256L) + matrix->access(((y + 1)) + ((x)) * 256L) + matrix->access(((y - 1)) + ((x + 1)) * 256L) + matrix->access(((y)) + ((x + 1)) * 256L) + matrix->access(((y - 1)) + ((x - 1)) * 256L);
        

        

    }
    

}


float cur::blur_256_512_float_two_d_256_512___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 256 - 1 || y == 512 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 512L) + matrix->access(((y)) + ((x - 1)) * 512L) + matrix->access(((y + 1)) + ((x - 1)) * 512L) + matrix->access(((y - 1)) + ((x)) * 512L) + matrix->access(((y)) + ((x)) * 512L) + matrix->access(((y + 1)) + ((x)) * 512L) + matrix->access(((y - 1)) + ((x + 1)) * 512L) + matrix->access(((y)) + ((x + 1)) * 512L) + matrix->access(((y - 1)) + ((x - 1)) * 512L);
        

        

    }
    

}


float cur::blur_512_512_float_two_d_512_512___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 512 - 1 || y == 512 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 512L) + matrix->access(((y)) + ((x - 1)) * 512L) + matrix->access(((y + 1)) + ((x - 1)) * 512L) + matrix->access(((y - 1)) + ((x)) * 512L) + matrix->access(((y)) + ((x)) * 512L) + matrix->access(((y + 1)) + ((x)) * 512L) + matrix->access(((y - 1)) + ((x + 1)) * 512L) + matrix->access(((y)) + ((x + 1)) * 512L) + matrix->access(((y - 1)) + ((x - 1)) * 512L);
        

        

    }
    

}


float cur::blur_512_1024_float_two_d_512_1024___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 512 - 1 || y == 1024 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 1024L) + matrix->access(((y)) + ((x - 1)) * 1024L) + matrix->access(((y + 1)) + ((x - 1)) * 1024L) + matrix->access(((y - 1)) + ((x)) * 1024L) + matrix->access(((y)) + ((x)) * 1024L) + matrix->access(((y + 1)) + ((x)) * 1024L) + matrix->access(((y - 1)) + ((x + 1)) * 1024L) + matrix->access(((y)) + ((x + 1)) * 1024L) + matrix->access(((y - 1)) + ((x - 1)) * 1024L);
        

        

    }
    

}


float cur::blur_1024_1024_float_two_d_1024_1024___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 1024 - 1 || y == 1024 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 1024L) + matrix->access(((y)) + ((x - 1)) * 1024L) + matrix->access(((y + 1)) + ((x - 1)) * 1024L) + matrix->access(((y - 1)) + ((x)) * 1024L) + matrix->access(((y)) + ((x)) * 1024L) + matrix->access(((y + 1)) + ((x)) * 1024L) + matrix->access(((y - 1)) + ((x + 1)) * 1024L) + matrix->access(((y)) + ((x + 1)) * 1024L) + matrix->access(((y - 1)) + ((x - 1)) * 1024L);
        

        

    }
    

}


float cur::blur_2048_1024_float_two_d_2048_1024___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 2048 - 1 || y == 1024 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 1024L) + matrix->access(((y)) + ((x - 1)) * 1024L) + matrix->access(((y + 1)) + ((x - 1)) * 1024L) + matrix->access(((y - 1)) + ((x)) * 1024L) + matrix->access(((y)) + ((x)) * 1024L) + matrix->access(((y + 1)) + ((x)) * 1024L) + matrix->access(((y - 1)) + ((x + 1)) * 1024L) + matrix->access(((y)) + ((x + 1)) * 1024L) + matrix->access(((y - 1)) + ((x - 1)) * 1024L);
        

        

    }
    

}


float cur::blur_2048_2048_float_two_d_2048_2048___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 2048 - 1 || y == 2048 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 2048L) + matrix->access(((y)) + ((x - 1)) * 2048L) + matrix->access(((y + 1)) + ((x - 1)) * 2048L) + matrix->access(((y - 1)) + ((x)) * 2048L) + matrix->access(((y)) + ((x)) * 2048L) + matrix->access(((y + 1)) + ((x)) * 2048L) + matrix->access(((y - 1)) + ((x + 1)) * 2048L) + matrix->access(((y)) + ((x + 1)) * 2048L) + matrix->access(((y - 1)) + ((x - 1)) * 2048L);
        

        

    }
    

}


float cur::blur_4096_2048_float_two_d_4096_2048___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 4096 - 1 || y == 2048 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 2048L) + matrix->access(((y)) + ((x - 1)) * 2048L) + matrix->access(((y + 1)) + ((x - 1)) * 2048L) + matrix->access(((y - 1)) + ((x)) * 2048L) + matrix->access(((y)) + ((x)) * 2048L) + matrix->access(((y + 1)) + ((x)) * 2048L) + matrix->access(((y - 1)) + ((x + 1)) * 2048L) + matrix->access(((y)) + ((x + 1)) * 2048L) + matrix->access(((y - 1)) + ((x - 1)) * 2048L);
        

        

    }
    

}


float cur::blur_4096_4096_float_two_d_4096_4096___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 4096 - 1 || y == 4096 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 4096L) + matrix->access(((y)) + ((x - 1)) * 4096L) + matrix->access(((y + 1)) + ((x - 1)) * 4096L) + matrix->access(((y - 1)) + ((x)) * 4096L) + matrix->access(((y)) + ((x)) * 4096L) + matrix->access(((y + 1)) + ((x)) * 4096L) + matrix->access(((y - 1)) + ((x + 1)) * 4096L) + matrix->access(((y)) + ((x + 1)) * 4096L) + matrix->access(((y - 1)) + ((x - 1)) * 4096L);
        

        

    }
    

}


float cur::blur_4096_8192_float_two_d_4096_8192___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 4096 - 1 || y == 8192 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 8192L) + matrix->access(((y)) + ((x - 1)) * 8192L) + matrix->access(((y + 1)) + ((x - 1)) * 8192L) + matrix->access(((y - 1)) + ((x)) * 8192L) + matrix->access(((y)) + ((x)) * 8192L) + matrix->access(((y + 1)) + ((x)) * 8192L) + matrix->access(((y - 1)) + ((x + 1)) * 8192L) + matrix->access(((y)) + ((x + 1)) * 8192L) + matrix->access(((y - 1)) + ((x - 1)) * 8192L);
        

        

    }
    

}


float cur::blur_8192_8192_float_two_d_8192_8192___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y)
{

    
        
    if (x < 1 || y < 1 || x == 8192 - 1 || y == 8192 - 1)
    {
                
        
                
        return 0.0F;
        

        

    }
    else
    {
                
        
                
        return matrix->access(((y - 1)) + ((x - 1)) * 8192L) + matrix->access(((y)) + ((x - 1)) * 8192L) + matrix->access(((y + 1)) + ((x - 1)) * 8192L) + matrix->access(((y - 1)) + ((x)) * 8192L) + matrix->access(((y)) + ((x)) * 8192L) + matrix->access(((y + 1)) + ((x)) * 8192L) + matrix->access(((y - 1)) + ((x + 1)) * 8192L) + matrix->access(((y)) + ((x + 1)) * 8192L) + matrix->access(((y - 1)) + ((x - 1)) * 8192L);
        

        

    }
    

}


funky::LinearArray< float >::Version* cur::transpose_32_32_float_two_d_32_32___float_two_d_32_32__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1024,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6018_0=funky::Profile::RegisterCustom("6018","transpose_32_32","Compile");
                tbb::tick_count __TIME__6018_0=tbb::tick_count::now();
                ocl_kernel transpose_32_32_0(&device,"tmp/transpose_32_32_0.cl");
                
                
                __profile__6018_0->AddMeasurement((tbb::tick_count::now()-__TIME__6018_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6018_1=funky::Profile::RegisterCustom("6018","transpose_32_32","CopyTo");
                tbb::tick_count __TIME__6018_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*1024,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=32;
                int __matrix0_dim_1=32;
                int __new_array_0_dim_0=32;
                int __new_array_0_dim_1=32;
                float *__return_val_0=new float[1024];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*1024,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_32_32_0.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6018_1->AddMeasurement((tbb::tick_count::now()-__TIME__6018_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6018_2=funky::Profile::RegisterCustom("6018","transpose_32_32","GPURun");
                tbb::tick_count __TIME__6018_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_32_32_0.timedRun(1024, 1024);
                device.finish();double runtime = transpose_32_32_0.getRunTime(id);
                
                __profile__6018_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6018_3=funky::Profile::RegisterCustom("6018","transpose_32_32","copyBack");
                tbb::tick_count __TIME__6018_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1024,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,32);
                
                
                __profile__6018_3->AddMeasurement((tbb::tick_count::now()-__TIME__6018_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1024,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6018_4=funky::Profile::RegisterCustom("6018","transpose_32_32","WholeCPURun");
                tbb::tick_count __TIME__6018_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 32L)
                        ;
                        
                    }, 0, 31 + 1,  + ((x)) * 32L, __EXP_TMP0);
                    
                }, 0, 31 + 1);
                

                __profile__6018_4->AddMeasurement((tbb::tick_count::now()-__TIME__6018_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_32_64_float_two_d_32_64___float_two_d_64_32__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6159_0=funky::Profile::RegisterCustom("6159","transpose_32_64","Compile");
                tbb::tick_count __TIME__6159_0=tbb::tick_count::now();
                ocl_kernel transpose_32_64_1(&device,"tmp/transpose_32_64_1.cl");
                
                
                __profile__6159_0->AddMeasurement((tbb::tick_count::now()-__TIME__6159_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6159_1=funky::Profile::RegisterCustom("6159","transpose_32_64","CopyTo");
                tbb::tick_count __TIME__6159_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*2048,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=32;
                int __matrix0_dim_1=64;
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=32;
                float *__return_val_0=new float[2048];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2048,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_32_64_1.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6159_1->AddMeasurement((tbb::tick_count::now()-__TIME__6159_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6159_2=funky::Profile::RegisterCustom("6159","transpose_32_64","GPURun");
                tbb::tick_count __TIME__6159_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_32_64_1.timedRun(1024, 2048);
                device.finish();double runtime = transpose_32_64_1.getRunTime(id);
                
                __profile__6159_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6159_3=funky::Profile::RegisterCustom("6159","transpose_32_64","copyBack");
                tbb::tick_count __TIME__6159_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2048,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,32);
                
                
                __profile__6159_3->AddMeasurement((tbb::tick_count::now()-__TIME__6159_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6159_4=funky::Profile::RegisterCustom("6159","transpose_32_64","WholeCPURun");
                tbb::tick_count __TIME__6159_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 64L)
                        ;
                        
                    }, 0, 31 + 1,  + ((x)) * 32L, __EXP_TMP0);
                    
                }, 0, 63 + 1);
                

                __profile__6159_4->AddMeasurement((tbb::tick_count::now()-__TIME__6159_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_64_64_float_two_d_64_64___float_two_d_64_64__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6300_0=funky::Profile::RegisterCustom("6300","transpose_64_64","Compile");
                tbb::tick_count __TIME__6300_0=tbb::tick_count::now();
                ocl_kernel transpose_64_64_2(&device,"tmp/transpose_64_64_2.cl");
                
                
                __profile__6300_0->AddMeasurement((tbb::tick_count::now()-__TIME__6300_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6300_1=funky::Profile::RegisterCustom("6300","transpose_64_64","CopyTo");
                tbb::tick_count __TIME__6300_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*4096,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=64;
                int __matrix0_dim_1=64;
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[4096];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*4096,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_64_64_2.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6300_1->AddMeasurement((tbb::tick_count::now()-__TIME__6300_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6300_2=funky::Profile::RegisterCustom("6300","transpose_64_64","GPURun");
                tbb::tick_count __TIME__6300_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_64_64_2.timedRun(1024, 4096);
                device.finish();double runtime = transpose_64_64_2.getRunTime(id);
                
                __profile__6300_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6300_3=funky::Profile::RegisterCustom("6300","transpose_64_64","copyBack");
                tbb::tick_count __TIME__6300_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4096,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,64);
                
                
                __profile__6300_3->AddMeasurement((tbb::tick_count::now()-__TIME__6300_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6300_4=funky::Profile::RegisterCustom("6300","transpose_64_64","WholeCPURun");
                tbb::tick_count __TIME__6300_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 64L)
                        ;
                        
                    }, 0, 63 + 1,  + ((x)) * 64L, __EXP_TMP0);
                    
                }, 0, 63 + 1);
                

                __profile__6300_4->AddMeasurement((tbb::tick_count::now()-__TIME__6300_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_64_128_float_two_d_64_128___float_two_d_128_64__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6445_0=funky::Profile::RegisterCustom("6445","transpose_64_128","Compile");
                tbb::tick_count __TIME__6445_0=tbb::tick_count::now();
                ocl_kernel transpose_64_128_3(&device,"tmp/transpose_64_128_3.cl");
                
                
                __profile__6445_0->AddMeasurement((tbb::tick_count::now()-__TIME__6445_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6445_1=funky::Profile::RegisterCustom("6445","transpose_64_128","CopyTo");
                tbb::tick_count __TIME__6445_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*8192,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=64;
                int __matrix0_dim_1=128;
                int __new_array_0_dim_0=128;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[8192];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8192,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_64_128_3.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6445_1->AddMeasurement((tbb::tick_count::now()-__TIME__6445_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6445_2=funky::Profile::RegisterCustom("6445","transpose_64_128","GPURun");
                tbb::tick_count __TIME__6445_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_64_128_3.timedRun(1024, 8192);
                device.finish();double runtime = transpose_64_128_3.getRunTime(id);
                
                __profile__6445_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6445_3=funky::Profile::RegisterCustom("6445","transpose_64_128","copyBack");
                tbb::tick_count __TIME__6445_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8192,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,128,64);
                
                
                __profile__6445_3->AddMeasurement((tbb::tick_count::now()-__TIME__6445_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6445_4=funky::Profile::RegisterCustom("6445","transpose_64_128","WholeCPURun");
                tbb::tick_count __TIME__6445_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 128L)
                        ;
                        
                    }, 0, 63 + 1,  + ((x)) * 64L, __EXP_TMP0);
                    
                }, 0, 127 + 1);
                

                __profile__6445_4->AddMeasurement((tbb::tick_count::now()-__TIME__6445_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_128_128_float_two_d_128_128___float_two_d_128_128__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6595_0=funky::Profile::RegisterCustom("6595","transpose_128_128","Compile");
                tbb::tick_count __TIME__6595_0=tbb::tick_count::now();
                ocl_kernel transpose_128_128_4(&device,"tmp/transpose_128_128_4.cl");
                
                
                __profile__6595_0->AddMeasurement((tbb::tick_count::now()-__TIME__6595_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6595_1=funky::Profile::RegisterCustom("6595","transpose_128_128","CopyTo");
                tbb::tick_count __TIME__6595_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*16384,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=128;
                int __matrix0_dim_1=128;
                int __new_array_0_dim_0=128;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[16384];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*16384,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_128_128_4.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6595_1->AddMeasurement((tbb::tick_count::now()-__TIME__6595_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6595_2=funky::Profile::RegisterCustom("6595","transpose_128_128","GPURun");
                tbb::tick_count __TIME__6595_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_128_128_4.timedRun(1024, 16384);
                device.finish();double runtime = transpose_128_128_4.getRunTime(id);
                
                __profile__6595_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6595_3=funky::Profile::RegisterCustom("6595","transpose_128_128","copyBack");
                tbb::tick_count __TIME__6595_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16384,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,128,128);
                
                
                __profile__6595_3->AddMeasurement((tbb::tick_count::now()-__TIME__6595_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6595_4=funky::Profile::RegisterCustom("6595","transpose_128_128","WholeCPURun");
                tbb::tick_count __TIME__6595_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 128L)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 127 + 1);
                

                __profile__6595_4->AddMeasurement((tbb::tick_count::now()-__TIME__6595_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_256_128_float_two_d_256_128___float_two_d_128_256__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6744_0=funky::Profile::RegisterCustom("6744","transpose_256_128","Compile");
                tbb::tick_count __TIME__6744_0=tbb::tick_count::now();
                ocl_kernel transpose_256_128_5(&device,"tmp/transpose_256_128_5.cl");
                
                
                __profile__6744_0->AddMeasurement((tbb::tick_count::now()-__TIME__6744_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6744_1=funky::Profile::RegisterCustom("6744","transpose_256_128","CopyTo");
                tbb::tick_count __TIME__6744_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*32768,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=256;
                int __matrix0_dim_1=128;
                int __new_array_0_dim_0=128;
                int __new_array_0_dim_1=256;
                float *__return_val_0=new float[32768];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*32768,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_256_128_5.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6744_1->AddMeasurement((tbb::tick_count::now()-__TIME__6744_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6744_2=funky::Profile::RegisterCustom("6744","transpose_256_128","GPURun");
                tbb::tick_count __TIME__6744_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_256_128_5.timedRun(1024, 32768);
                device.finish();double runtime = transpose_256_128_5.getRunTime(id);
                
                __profile__6744_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6744_3=funky::Profile::RegisterCustom("6744","transpose_256_128","copyBack");
                tbb::tick_count __TIME__6744_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(32768,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,128,256);
                
                
                __profile__6744_3->AddMeasurement((tbb::tick_count::now()-__TIME__6744_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6744_4=funky::Profile::RegisterCustom("6744","transpose_256_128","WholeCPURun");
                tbb::tick_count __TIME__6744_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 128L)
                        ;
                        
                    }, 0, 255 + 1,  + ((x)) * 256L, __EXP_TMP0);
                    
                }, 0, 127 + 1);
                

                __profile__6744_4->AddMeasurement((tbb::tick_count::now()-__TIME__6744_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_256_256_float_two_d_256_256___float_two_d_256_256__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6893_0=funky::Profile::RegisterCustom("6893","transpose_256_256","Compile");
                tbb::tick_count __TIME__6893_0=tbb::tick_count::now();
                ocl_kernel transpose_256_256_6(&device,"tmp/transpose_256_256_6.cl");
                
                
                __profile__6893_0->AddMeasurement((tbb::tick_count::now()-__TIME__6893_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6893_1=funky::Profile::RegisterCustom("6893","transpose_256_256","CopyTo");
                tbb::tick_count __TIME__6893_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*65536,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=256;
                int __matrix0_dim_1=256;
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=256;
                float *__return_val_0=new float[65536];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*65536,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_256_256_6.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6893_1->AddMeasurement((tbb::tick_count::now()-__TIME__6893_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6893_2=funky::Profile::RegisterCustom("6893","transpose_256_256","GPURun");
                tbb::tick_count __TIME__6893_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_256_256_6.timedRun(1024, 65536);
                device.finish();double runtime = transpose_256_256_6.getRunTime(id);
                
                __profile__6893_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6893_3=funky::Profile::RegisterCustom("6893","transpose_256_256","copyBack");
                tbb::tick_count __TIME__6893_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(65536,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,256);
                
                
                __profile__6893_3->AddMeasurement((tbb::tick_count::now()-__TIME__6893_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6893_4=funky::Profile::RegisterCustom("6893","transpose_256_256","WholeCPURun");
                tbb::tick_count __TIME__6893_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 256L)
                        ;
                        
                    }, 0, 255 + 1,  + ((x)) * 256L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                

                __profile__6893_4->AddMeasurement((tbb::tick_count::now()-__TIME__6893_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_256_512_float_two_d_256_512___float_two_d_512_256__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7042_0=funky::Profile::RegisterCustom("7042","transpose_256_512","Compile");
                tbb::tick_count __TIME__7042_0=tbb::tick_count::now();
                ocl_kernel transpose_256_512_7(&device,"tmp/transpose_256_512_7.cl");
                
                
                __profile__7042_0->AddMeasurement((tbb::tick_count::now()-__TIME__7042_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7042_1=funky::Profile::RegisterCustom("7042","transpose_256_512","CopyTo");
                tbb::tick_count __TIME__7042_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*131072,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=256;
                int __matrix0_dim_1=512;
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=256;
                float *__return_val_0=new float[131072];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*131072,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_256_512_7.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7042_1->AddMeasurement((tbb::tick_count::now()-__TIME__7042_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7042_2=funky::Profile::RegisterCustom("7042","transpose_256_512","GPURun");
                tbb::tick_count __TIME__7042_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_256_512_7.timedRun(1024, 131072);
                device.finish();double runtime = transpose_256_512_7.getRunTime(id);
                
                __profile__7042_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7042_3=funky::Profile::RegisterCustom("7042","transpose_256_512","copyBack");
                tbb::tick_count __TIME__7042_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(131072,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,256);
                
                
                __profile__7042_3->AddMeasurement((tbb::tick_count::now()-__TIME__7042_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7042_4=funky::Profile::RegisterCustom("7042","transpose_256_512","WholeCPURun");
                tbb::tick_count __TIME__7042_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 512L)
                        ;
                        
                    }, 0, 255 + 1,  + ((x)) * 256L, __EXP_TMP0);
                    
                }, 0, 511 + 1);
                

                __profile__7042_4->AddMeasurement((tbb::tick_count::now()-__TIME__7042_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_512_512_float_two_d_512_512___float_two_d_512_512__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7192_0=funky::Profile::RegisterCustom("7192","transpose_512_512","Compile");
                tbb::tick_count __TIME__7192_0=tbb::tick_count::now();
                ocl_kernel transpose_512_512_8(&device,"tmp/transpose_512_512_8.cl");
                
                
                __profile__7192_0->AddMeasurement((tbb::tick_count::now()-__TIME__7192_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7192_1=funky::Profile::RegisterCustom("7192","transpose_512_512","CopyTo");
                tbb::tick_count __TIME__7192_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*262144,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=512;
                int __matrix0_dim_1=512;
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[262144];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*262144,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_512_512_8.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7192_1->AddMeasurement((tbb::tick_count::now()-__TIME__7192_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7192_2=funky::Profile::RegisterCustom("7192","transpose_512_512","GPURun");
                tbb::tick_count __TIME__7192_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_512_512_8.timedRun(1024, 262144);
                device.finish();double runtime = transpose_512_512_8.getRunTime(id);
                
                __profile__7192_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7192_3=funky::Profile::RegisterCustom("7192","transpose_512_512","copyBack");
                tbb::tick_count __TIME__7192_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(262144,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,512);
                
                
                __profile__7192_3->AddMeasurement((tbb::tick_count::now()-__TIME__7192_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7192_4=funky::Profile::RegisterCustom("7192","transpose_512_512","WholeCPURun");
                tbb::tick_count __TIME__7192_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 512L)
                        ;
                        
                    }, 0, 511 + 1,  + ((x)) * 512L, __EXP_TMP0);
                    
                }, 0, 511 + 1);
                

                __profile__7192_4->AddMeasurement((tbb::tick_count::now()-__TIME__7192_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_512_1024_float_two_d_512_1024___float_two_d_1024_512__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7345_0=funky::Profile::RegisterCustom("7345","transpose_512_1024","Compile");
                tbb::tick_count __TIME__7345_0=tbb::tick_count::now();
                ocl_kernel transpose_512_1024_9(&device,"tmp/transpose_512_1024_9.cl");
                
                
                __profile__7345_0->AddMeasurement((tbb::tick_count::now()-__TIME__7345_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7345_1=funky::Profile::RegisterCustom("7345","transpose_512_1024","CopyTo");
                tbb::tick_count __TIME__7345_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*524288,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=512;
                int __matrix0_dim_1=1024;
                int __new_array_0_dim_0=1024;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[524288];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*524288,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_512_1024_9.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7345_1->AddMeasurement((tbb::tick_count::now()-__TIME__7345_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7345_2=funky::Profile::RegisterCustom("7345","transpose_512_1024","GPURun");
                tbb::tick_count __TIME__7345_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_512_1024_9.timedRun(1024, 524288);
                device.finish();double runtime = transpose_512_1024_9.getRunTime(id);
                
                __profile__7345_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7345_3=funky::Profile::RegisterCustom("7345","transpose_512_1024","copyBack");
                tbb::tick_count __TIME__7345_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(524288,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,1024,512);
                
                
                __profile__7345_3->AddMeasurement((tbb::tick_count::now()-__TIME__7345_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7345_4=funky::Profile::RegisterCustom("7345","transpose_512_1024","WholeCPURun");
                tbb::tick_count __TIME__7345_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 1024L)
                        ;
                        
                    }, 0, 511 + 1,  + ((x)) * 512L, __EXP_TMP0);
                    
                }, 0, 1023 + 1);
                

                __profile__7345_4->AddMeasurement((tbb::tick_count::now()-__TIME__7345_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_1024_1024_float_two_d_1024_1024___float_two_d_1024_1024__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1048576,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7502_0=funky::Profile::RegisterCustom("7502","transpose_1024_1024","Compile");
                tbb::tick_count __TIME__7502_0=tbb::tick_count::now();
                ocl_kernel transpose_1024_1024_10(&device,"tmp/transpose_1024_1024_10.cl");
                
                
                __profile__7502_0->AddMeasurement((tbb::tick_count::now()-__TIME__7502_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7502_1=funky::Profile::RegisterCustom("7502","transpose_1024_1024","CopyTo");
                tbb::tick_count __TIME__7502_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*1048576,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=1024;
                int __matrix0_dim_1=1024;
                int __new_array_0_dim_0=1024;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[1048576];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*1048576,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_1024_1024_10.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7502_1->AddMeasurement((tbb::tick_count::now()-__TIME__7502_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7502_2=funky::Profile::RegisterCustom("7502","transpose_1024_1024","GPURun");
                tbb::tick_count __TIME__7502_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_1024_1024_10.timedRun(1024, 1048576);
                device.finish();double runtime = transpose_1024_1024_10.getRunTime(id);
                
                __profile__7502_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7502_3=funky::Profile::RegisterCustom("7502","transpose_1024_1024","copyBack");
                tbb::tick_count __TIME__7502_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1048576,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,1024,1024);
                
                
                __profile__7502_3->AddMeasurement((tbb::tick_count::now()-__TIME__7502_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1048576,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7502_4=funky::Profile::RegisterCustom("7502","transpose_1024_1024","WholeCPURun");
                tbb::tick_count __TIME__7502_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 1024L)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 1023 + 1);
                

                __profile__7502_4->AddMeasurement((tbb::tick_count::now()-__TIME__7502_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_2048_1024_float_two_d_2048_1024___float_two_d_1024_2048__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2097152,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7659_0=funky::Profile::RegisterCustom("7659","transpose_2048_1024","Compile");
                tbb::tick_count __TIME__7659_0=tbb::tick_count::now();
                ocl_kernel transpose_2048_1024_11(&device,"tmp/transpose_2048_1024_11.cl");
                
                
                __profile__7659_0->AddMeasurement((tbb::tick_count::now()-__TIME__7659_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7659_1=funky::Profile::RegisterCustom("7659","transpose_2048_1024","CopyTo");
                tbb::tick_count __TIME__7659_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*2097152,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=2048;
                int __matrix0_dim_1=1024;
                int __new_array_0_dim_0=1024;
                int __new_array_0_dim_1=2048;
                float *__return_val_0=new float[2097152];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2097152,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_2048_1024_11.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7659_1->AddMeasurement((tbb::tick_count::now()-__TIME__7659_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7659_2=funky::Profile::RegisterCustom("7659","transpose_2048_1024","GPURun");
                tbb::tick_count __TIME__7659_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_2048_1024_11.timedRun(1024, 2097152);
                device.finish();double runtime = transpose_2048_1024_11.getRunTime(id);
                
                __profile__7659_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7659_3=funky::Profile::RegisterCustom("7659","transpose_2048_1024","copyBack");
                tbb::tick_count __TIME__7659_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2097152,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,1024,2048);
                
                
                __profile__7659_3->AddMeasurement((tbb::tick_count::now()-__TIME__7659_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2097152,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7659_4=funky::Profile::RegisterCustom("7659","transpose_2048_1024","WholeCPURun");
                tbb::tick_count __TIME__7659_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 1024L)
                        ;
                        
                    }, 0, 2047 + 1,  + ((x)) * 2048L, __EXP_TMP0);
                    
                }, 0, 1023 + 1);
                

                __profile__7659_4->AddMeasurement((tbb::tick_count::now()-__TIME__7659_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_2048_2048_float_two_d_2048_2048___float_two_d_2048_2048__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4194304,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7817_0=funky::Profile::RegisterCustom("7817","transpose_2048_2048","Compile");
                tbb::tick_count __TIME__7817_0=tbb::tick_count::now();
                ocl_kernel transpose_2048_2048_12(&device,"tmp/transpose_2048_2048_12.cl");
                
                
                __profile__7817_0->AddMeasurement((tbb::tick_count::now()-__TIME__7817_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7817_1=funky::Profile::RegisterCustom("7817","transpose_2048_2048","CopyTo");
                tbb::tick_count __TIME__7817_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*4194304,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=2048;
                int __matrix0_dim_1=2048;
                int __new_array_0_dim_0=2048;
                int __new_array_0_dim_1=2048;
                float *__return_val_0=new float[4194304];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*4194304,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_2048_2048_12.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7817_1->AddMeasurement((tbb::tick_count::now()-__TIME__7817_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7817_2=funky::Profile::RegisterCustom("7817","transpose_2048_2048","GPURun");
                tbb::tick_count __TIME__7817_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_2048_2048_12.timedRun(1024, 4194304);
                device.finish();double runtime = transpose_2048_2048_12.getRunTime(id);
                
                __profile__7817_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7817_3=funky::Profile::RegisterCustom("7817","transpose_2048_2048","copyBack");
                tbb::tick_count __TIME__7817_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4194304,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,2048,2048);
                
                
                __profile__7817_3->AddMeasurement((tbb::tick_count::now()-__TIME__7817_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4194304,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7817_4=funky::Profile::RegisterCustom("7817","transpose_2048_2048","WholeCPURun");
                tbb::tick_count __TIME__7817_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 2048L)
                        ;
                        
                    }, 0, 2047 + 1,  + ((x)) * 2048L, __EXP_TMP0);
                    
                }, 0, 2047 + 1);
                

                __profile__7817_4->AddMeasurement((tbb::tick_count::now()-__TIME__7817_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_4096_2048_float_two_d_4096_2048___float_two_d_2048_4096__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8388608,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7974_0=funky::Profile::RegisterCustom("7974","transpose_4096_2048","Compile");
                tbb::tick_count __TIME__7974_0=tbb::tick_count::now();
                ocl_kernel transpose_4096_2048_13(&device,"tmp/transpose_4096_2048_13.cl");
                
                
                __profile__7974_0->AddMeasurement((tbb::tick_count::now()-__TIME__7974_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7974_1=funky::Profile::RegisterCustom("7974","transpose_4096_2048","CopyTo");
                tbb::tick_count __TIME__7974_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*8388608,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=4096;
                int __matrix0_dim_1=2048;
                int __new_array_0_dim_0=2048;
                int __new_array_0_dim_1=4096;
                float *__return_val_0=new float[8388608];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8388608,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_4096_2048_13.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7974_1->AddMeasurement((tbb::tick_count::now()-__TIME__7974_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7974_2=funky::Profile::RegisterCustom("7974","transpose_4096_2048","GPURun");
                tbb::tick_count __TIME__7974_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_4096_2048_13.timedRun(1024, 8388608);
                device.finish();double runtime = transpose_4096_2048_13.getRunTime(id);
                
                __profile__7974_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7974_3=funky::Profile::RegisterCustom("7974","transpose_4096_2048","copyBack");
                tbb::tick_count __TIME__7974_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8388608,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,2048,4096);
                
                
                __profile__7974_3->AddMeasurement((tbb::tick_count::now()-__TIME__7974_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8388608,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7974_4=funky::Profile::RegisterCustom("7974","transpose_4096_2048","WholeCPURun");
                tbb::tick_count __TIME__7974_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 2048L)
                        ;
                        
                    }, 0, 4095 + 1,  + ((x)) * 4096L, __EXP_TMP0);
                    
                }, 0, 2047 + 1);
                

                __profile__7974_4->AddMeasurement((tbb::tick_count::now()-__TIME__7974_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_4096_4096_float_two_d_4096_4096___float_two_d_4096_4096__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16777216,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8131_0=funky::Profile::RegisterCustom("8131","transpose_4096_4096","Compile");
                tbb::tick_count __TIME__8131_0=tbb::tick_count::now();
                ocl_kernel transpose_4096_4096_14(&device,"tmp/transpose_4096_4096_14.cl");
                
                
                __profile__8131_0->AddMeasurement((tbb::tick_count::now()-__TIME__8131_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8131_1=funky::Profile::RegisterCustom("8131","transpose_4096_4096","CopyTo");
                tbb::tick_count __TIME__8131_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*16777216,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=4096;
                int __matrix0_dim_1=4096;
                int __new_array_0_dim_0=4096;
                int __new_array_0_dim_1=4096;
                float *__return_val_0=new float[16777216];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*16777216,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_4096_4096_14.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8131_1->AddMeasurement((tbb::tick_count::now()-__TIME__8131_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8131_2=funky::Profile::RegisterCustom("8131","transpose_4096_4096","GPURun");
                tbb::tick_count __TIME__8131_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_4096_4096_14.timedRun(1024, 16777216);
                device.finish();double runtime = transpose_4096_4096_14.getRunTime(id);
                
                __profile__8131_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8131_3=funky::Profile::RegisterCustom("8131","transpose_4096_4096","copyBack");
                tbb::tick_count __TIME__8131_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16777216,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,4096);
                
                
                __profile__8131_3->AddMeasurement((tbb::tick_count::now()-__TIME__8131_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16777216,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8131_4=funky::Profile::RegisterCustom("8131","transpose_4096_4096","WholeCPURun");
                tbb::tick_count __TIME__8131_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 4096L)
                        ;
                        
                    }, 0, 4095 + 1,  + ((x)) * 4096L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                

                __profile__8131_4->AddMeasurement((tbb::tick_count::now()-__TIME__8131_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_4096_8192_float_two_d_4096_8192___float_two_d_8192_4096__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(33554432,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8288_0=funky::Profile::RegisterCustom("8288","transpose_4096_8192","Compile");
                tbb::tick_count __TIME__8288_0=tbb::tick_count::now();
                ocl_kernel transpose_4096_8192_15(&device,"tmp/transpose_4096_8192_15.cl");
                
                
                __profile__8288_0->AddMeasurement((tbb::tick_count::now()-__TIME__8288_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8288_1=funky::Profile::RegisterCustom("8288","transpose_4096_8192","CopyTo");
                tbb::tick_count __TIME__8288_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*33554432,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=4096;
                int __matrix0_dim_1=8192;
                int __new_array_0_dim_0=8192;
                int __new_array_0_dim_1=4096;
                float *__return_val_0=new float[33554432];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*33554432,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_4096_8192_15.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8288_1->AddMeasurement((tbb::tick_count::now()-__TIME__8288_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8288_2=funky::Profile::RegisterCustom("8288","transpose_4096_8192","GPURun");
                tbb::tick_count __TIME__8288_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_4096_8192_15.timedRun(1024, 33554432);
                device.finish();double runtime = transpose_4096_8192_15.getRunTime(id);
                
                __profile__8288_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8288_3=funky::Profile::RegisterCustom("8288","transpose_4096_8192","copyBack");
                tbb::tick_count __TIME__8288_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(33554432,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,8192,4096);
                
                
                __profile__8288_3->AddMeasurement((tbb::tick_count::now()-__TIME__8288_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(33554432,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8288_4=funky::Profile::RegisterCustom("8288","transpose_4096_8192","WholeCPURun");
                tbb::tick_count __TIME__8288_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 8192L)
                        ;
                        
                    }, 0, 4095 + 1,  + ((x)) * 4096L, __EXP_TMP0);
                    
                }, 0, 8191 + 1);
                

                __profile__8288_4->AddMeasurement((tbb::tick_count::now()-__TIME__8288_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_8192_8192_float_two_d_8192_8192___float_two_d_8192_8192__(funky::LinearArray< float >::Version* matrix)
{

    
        
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(67108864,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8446_0=funky::Profile::RegisterCustom("8446","transpose_8192_8192","Compile");
                tbb::tick_count __TIME__8446_0=tbb::tick_count::now();
                ocl_kernel transpose_8192_8192_16(&device,"tmp/transpose_8192_8192_16.cl");
                
                
                __profile__8446_0->AddMeasurement((tbb::tick_count::now()-__TIME__8446_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8446_1=funky::Profile::RegisterCustom("8446","transpose_8192_8192","CopyTo");
                tbb::tick_count __TIME__8446_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*67108864,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=8192;
                int __matrix0_dim_1=8192;
                int __new_array_0_dim_0=8192;
                int __new_array_0_dim_1=8192;
                float *__return_val_0=new float[67108864];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*67108864,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_8192_8192_16.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8446_1->AddMeasurement((tbb::tick_count::now()-__TIME__8446_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8446_2=funky::Profile::RegisterCustom("8446","transpose_8192_8192","GPURun");
                tbb::tick_count __TIME__8446_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = transpose_8192_8192_16.timedRun(1024, 67108864);
                device.finish();double runtime = transpose_8192_8192_16.getRunTime(id);
                
                __profile__8446_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8446_3=funky::Profile::RegisterCustom("8446","transpose_8192_8192","copyBack");
                tbb::tick_count __TIME__8446_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(67108864,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,8192,8192);
                
                
                __profile__8446_3->AddMeasurement((tbb::tick_count::now()-__TIME__8446_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(67108864,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8446_4=funky::Profile::RegisterCustom("8446","transpose_8192_8192","WholeCPURun");
                tbb::tick_count __TIME__8446_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return matrix->access(((x)) + ((y)) * 8192L)
                        ;
                        
                    }, 0, 8191 + 1,  + ((x)) * 8192L, __EXP_TMP0);
                    
                }, 0, 8191 + 1);
                

                __profile__8446_4->AddMeasurement((tbb::tick_count::now()-__TIME__8446_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


int cur::main_int_String_one_d_01___int(int argc, funky::LinearArray< char* >::Version* argv)
{

    
        
    funky::LinearArray< float >::Version* matrix_32_32 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1024,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8633_0=funky::Profile::RegisterCustom("8633","main","Compile");
                tbb::tick_count __TIME__8633_0=tbb::tick_count::now();
                ocl_kernel main_17(&device,"tmp/main_17.cl");
                
                
                __profile__8633_0->AddMeasurement((tbb::tick_count::now()-__TIME__8633_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8633_1=funky::Profile::RegisterCustom("8633","main","CopyTo");
                tbb::tick_count __TIME__8633_1=tbb::tick_count::now();
                int __new_array_0_dim_0=32;
                int __new_array_0_dim_1=32;
                float *__return_val_0=new float[1024];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*1024,CL_MEM_WRITE_ONLY);
                
                
                main_17.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8633_1->AddMeasurement((tbb::tick_count::now()-__TIME__8633_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8633_2=funky::Profile::RegisterCustom("8633","main","GPURun");
                tbb::tick_count __TIME__8633_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_17.timedRun(1024, 1024);
                device.finish();double runtime = main_17.getRunTime(id);
                
                __profile__8633_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8633_3=funky::Profile::RegisterCustom("8633","main","copyBack");
                tbb::tick_count __TIME__8633_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1024,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,32);
                
                
                __profile__8633_3->AddMeasurement((tbb::tick_count::now()-__TIME__8633_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1024,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8633_4=funky::Profile::RegisterCustom("8633","main","WholeCPURun");
                tbb::tick_count __TIME__8633_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 31 + 1,  + ((x)) * 32L, __EXP_TMP0);
                    
                }, 0, 31 + 1);
                

                __profile__8633_4->AddMeasurement((tbb::tick_count::now()-__TIME__8633_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_32_32_float_two_d_32_32___float_two_d_32_32__(matrix_32_32);
    
    
    funky::LinearArray< float >::Version* matrix_32_64 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8759_0=funky::Profile::RegisterCustom("8759","main","Compile");
                tbb::tick_count __TIME__8759_0=tbb::tick_count::now();
                ocl_kernel main_18(&device,"tmp/main_18.cl");
                
                
                __profile__8759_0->AddMeasurement((tbb::tick_count::now()-__TIME__8759_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8759_1=funky::Profile::RegisterCustom("8759","main","CopyTo");
                tbb::tick_count __TIME__8759_1=tbb::tick_count::now();
                int __new_array_0_dim_0=32;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[2048];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2048,CL_MEM_WRITE_ONLY);
                
                
                main_18.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8759_1->AddMeasurement((tbb::tick_count::now()-__TIME__8759_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8759_2=funky::Profile::RegisterCustom("8759","main","GPURun");
                tbb::tick_count __TIME__8759_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_18.timedRun(1024, 2048);
                device.finish();double runtime = main_18.getRunTime(id);
                
                __profile__8759_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8759_3=funky::Profile::RegisterCustom("8759","main","copyBack");
                tbb::tick_count __TIME__8759_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2048,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,64);
                
                
                __profile__8759_3->AddMeasurement((tbb::tick_count::now()-__TIME__8759_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8759_4=funky::Profile::RegisterCustom("8759","main","WholeCPURun");
                tbb::tick_count __TIME__8759_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 63 + 1,  + ((x)) * 64L, __EXP_TMP0);
                    
                }, 0, 31 + 1);
                

                __profile__8759_4->AddMeasurement((tbb::tick_count::now()-__TIME__8759_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_32_64_float_two_d_32_64___float_two_d_64_32__(matrix_32_64);
    
    
    funky::LinearArray< float >::Version* matrix_64_64 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8885_0=funky::Profile::RegisterCustom("8885","main","Compile");
                tbb::tick_count __TIME__8885_0=tbb::tick_count::now();
                ocl_kernel main_19(&device,"tmp/main_19.cl");
                
                
                __profile__8885_0->AddMeasurement((tbb::tick_count::now()-__TIME__8885_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8885_1=funky::Profile::RegisterCustom("8885","main","CopyTo");
                tbb::tick_count __TIME__8885_1=tbb::tick_count::now();
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[4096];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*4096,CL_MEM_WRITE_ONLY);
                
                
                main_19.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8885_1->AddMeasurement((tbb::tick_count::now()-__TIME__8885_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8885_2=funky::Profile::RegisterCustom("8885","main","GPURun");
                tbb::tick_count __TIME__8885_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_19.timedRun(1024, 4096);
                device.finish();double runtime = main_19.getRunTime(id);
                
                __profile__8885_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8885_3=funky::Profile::RegisterCustom("8885","main","copyBack");
                tbb::tick_count __TIME__8885_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4096,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,64);
                
                
                __profile__8885_3->AddMeasurement((tbb::tick_count::now()-__TIME__8885_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8885_4=funky::Profile::RegisterCustom("8885","main","WholeCPURun");
                tbb::tick_count __TIME__8885_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 63 + 1,  + ((x)) * 64L, __EXP_TMP0);
                    
                }, 0, 63 + 1);
                

                __profile__8885_4->AddMeasurement((tbb::tick_count::now()-__TIME__8885_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_64_64_float_two_d_64_64___float_two_d_64_64__(matrix_64_64);
    
    
    funky::LinearArray< float >::Version* matrix_64_128 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9014_0=funky::Profile::RegisterCustom("9014","main","Compile");
                tbb::tick_count __TIME__9014_0=tbb::tick_count::now();
                ocl_kernel main_20(&device,"tmp/main_20.cl");
                
                
                __profile__9014_0->AddMeasurement((tbb::tick_count::now()-__TIME__9014_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9014_1=funky::Profile::RegisterCustom("9014","main","CopyTo");
                tbb::tick_count __TIME__9014_1=tbb::tick_count::now();
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[8192];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8192,CL_MEM_WRITE_ONLY);
                
                
                main_20.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9014_1->AddMeasurement((tbb::tick_count::now()-__TIME__9014_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9014_2=funky::Profile::RegisterCustom("9014","main","GPURun");
                tbb::tick_count __TIME__9014_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_20.timedRun(1024, 8192);
                device.finish();double runtime = main_20.getRunTime(id);
                
                __profile__9014_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9014_3=funky::Profile::RegisterCustom("9014","main","copyBack");
                tbb::tick_count __TIME__9014_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8192,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,128);
                
                
                __profile__9014_3->AddMeasurement((tbb::tick_count::now()-__TIME__9014_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9014_4=funky::Profile::RegisterCustom("9014","main","WholeCPURun");
                tbb::tick_count __TIME__9014_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 63 + 1);
                

                __profile__9014_4->AddMeasurement((tbb::tick_count::now()-__TIME__9014_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_64_128_float_two_d_64_128___float_two_d_128_64__(matrix_64_128);
    
    
    funky::LinearArray< float >::Version* matrix_128_128 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9149_0=funky::Profile::RegisterCustom("9149","main","Compile");
                tbb::tick_count __TIME__9149_0=tbb::tick_count::now();
                ocl_kernel main_21(&device,"tmp/main_21.cl");
                
                
                __profile__9149_0->AddMeasurement((tbb::tick_count::now()-__TIME__9149_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9149_1=funky::Profile::RegisterCustom("9149","main","CopyTo");
                tbb::tick_count __TIME__9149_1=tbb::tick_count::now();
                int __new_array_0_dim_0=128;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[16384];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*16384,CL_MEM_WRITE_ONLY);
                
                
                main_21.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9149_1->AddMeasurement((tbb::tick_count::now()-__TIME__9149_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9149_2=funky::Profile::RegisterCustom("9149","main","GPURun");
                tbb::tick_count __TIME__9149_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_21.timedRun(1024, 16384);
                device.finish();double runtime = main_21.getRunTime(id);
                
                __profile__9149_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9149_3=funky::Profile::RegisterCustom("9149","main","copyBack");
                tbb::tick_count __TIME__9149_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16384,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,128,128);
                
                
                __profile__9149_3->AddMeasurement((tbb::tick_count::now()-__TIME__9149_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9149_4=funky::Profile::RegisterCustom("9149","main","WholeCPURun");
                tbb::tick_count __TIME__9149_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 127 + 1);
                

                __profile__9149_4->AddMeasurement((tbb::tick_count::now()-__TIME__9149_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_128_128_float_two_d_128_128___float_two_d_128_128__(matrix_128_128);
    
    
    funky::LinearArray< float >::Version* matrix_256_128 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9285_0=funky::Profile::RegisterCustom("9285","main","Compile");
                tbb::tick_count __TIME__9285_0=tbb::tick_count::now();
                ocl_kernel main_22(&device,"tmp/main_22.cl");
                
                
                __profile__9285_0->AddMeasurement((tbb::tick_count::now()-__TIME__9285_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9285_1=funky::Profile::RegisterCustom("9285","main","CopyTo");
                tbb::tick_count __TIME__9285_1=tbb::tick_count::now();
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[32768];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*32768,CL_MEM_WRITE_ONLY);
                
                
                main_22.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9285_1->AddMeasurement((tbb::tick_count::now()-__TIME__9285_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9285_2=funky::Profile::RegisterCustom("9285","main","GPURun");
                tbb::tick_count __TIME__9285_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_22.timedRun(1024, 32768);
                device.finish();double runtime = main_22.getRunTime(id);
                
                __profile__9285_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9285_3=funky::Profile::RegisterCustom("9285","main","copyBack");
                tbb::tick_count __TIME__9285_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(32768,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,128);
                
                
                __profile__9285_3->AddMeasurement((tbb::tick_count::now()-__TIME__9285_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9285_4=funky::Profile::RegisterCustom("9285","main","WholeCPURun");
                tbb::tick_count __TIME__9285_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                

                __profile__9285_4->AddMeasurement((tbb::tick_count::now()-__TIME__9285_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_256_128_float_two_d_256_128___float_two_d_128_256__(matrix_256_128);
    
    
    funky::LinearArray< float >::Version* matrix_256_256 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9421_0=funky::Profile::RegisterCustom("9421","main","Compile");
                tbb::tick_count __TIME__9421_0=tbb::tick_count::now();
                ocl_kernel main_23(&device,"tmp/main_23.cl");
                
                
                __profile__9421_0->AddMeasurement((tbb::tick_count::now()-__TIME__9421_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9421_1=funky::Profile::RegisterCustom("9421","main","CopyTo");
                tbb::tick_count __TIME__9421_1=tbb::tick_count::now();
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=256;
                float *__return_val_0=new float[65536];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*65536,CL_MEM_WRITE_ONLY);
                
                
                main_23.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9421_1->AddMeasurement((tbb::tick_count::now()-__TIME__9421_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9421_2=funky::Profile::RegisterCustom("9421","main","GPURun");
                tbb::tick_count __TIME__9421_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_23.timedRun(1024, 65536);
                device.finish();double runtime = main_23.getRunTime(id);
                
                __profile__9421_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9421_3=funky::Profile::RegisterCustom("9421","main","copyBack");
                tbb::tick_count __TIME__9421_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(65536,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,256);
                
                
                __profile__9421_3->AddMeasurement((tbb::tick_count::now()-__TIME__9421_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9421_4=funky::Profile::RegisterCustom("9421","main","WholeCPURun");
                tbb::tick_count __TIME__9421_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 255 + 1,  + ((x)) * 256L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                

                __profile__9421_4->AddMeasurement((tbb::tick_count::now()-__TIME__9421_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_256_256_float_two_d_256_256___float_two_d_256_256__(matrix_256_256);
    
    
    funky::LinearArray< float >::Version* matrix_256_512 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9557_0=funky::Profile::RegisterCustom("9557","main","Compile");
                tbb::tick_count __TIME__9557_0=tbb::tick_count::now();
                ocl_kernel main_24(&device,"tmp/main_24.cl");
                
                
                __profile__9557_0->AddMeasurement((tbb::tick_count::now()-__TIME__9557_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9557_1=funky::Profile::RegisterCustom("9557","main","CopyTo");
                tbb::tick_count __TIME__9557_1=tbb::tick_count::now();
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[131072];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*131072,CL_MEM_WRITE_ONLY);
                
                
                main_24.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9557_1->AddMeasurement((tbb::tick_count::now()-__TIME__9557_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9557_2=funky::Profile::RegisterCustom("9557","main","GPURun");
                tbb::tick_count __TIME__9557_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_24.timedRun(1024, 131072);
                device.finish();double runtime = main_24.getRunTime(id);
                
                __profile__9557_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9557_3=funky::Profile::RegisterCustom("9557","main","copyBack");
                tbb::tick_count __TIME__9557_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(131072,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,512);
                
                
                __profile__9557_3->AddMeasurement((tbb::tick_count::now()-__TIME__9557_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9557_4=funky::Profile::RegisterCustom("9557","main","WholeCPURun");
                tbb::tick_count __TIME__9557_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 511 + 1,  + ((x)) * 512L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                

                __profile__9557_4->AddMeasurement((tbb::tick_count::now()-__TIME__9557_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_256_512_float_two_d_256_512___float_two_d_512_256__(matrix_256_512);
    
    
    funky::LinearArray< float >::Version* matrix_512_512 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9694_0=funky::Profile::RegisterCustom("9694","main","Compile");
                tbb::tick_count __TIME__9694_0=tbb::tick_count::now();
                ocl_kernel main_25(&device,"tmp/main_25.cl");
                
                
                __profile__9694_0->AddMeasurement((tbb::tick_count::now()-__TIME__9694_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9694_1=funky::Profile::RegisterCustom("9694","main","CopyTo");
                tbb::tick_count __TIME__9694_1=tbb::tick_count::now();
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[262144];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*262144,CL_MEM_WRITE_ONLY);
                
                
                main_25.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9694_1->AddMeasurement((tbb::tick_count::now()-__TIME__9694_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9694_2=funky::Profile::RegisterCustom("9694","main","GPURun");
                tbb::tick_count __TIME__9694_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_25.timedRun(1024, 262144);
                device.finish();double runtime = main_25.getRunTime(id);
                
                __profile__9694_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9694_3=funky::Profile::RegisterCustom("9694","main","copyBack");
                tbb::tick_count __TIME__9694_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(262144,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,512);
                
                
                __profile__9694_3->AddMeasurement((tbb::tick_count::now()-__TIME__9694_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9694_4=funky::Profile::RegisterCustom("9694","main","WholeCPURun");
                tbb::tick_count __TIME__9694_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 511 + 1,  + ((x)) * 512L, __EXP_TMP0);
                    
                }, 0, 511 + 1);
                

                __profile__9694_4->AddMeasurement((tbb::tick_count::now()-__TIME__9694_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_512_512_float_two_d_512_512___float_two_d_512_512__(matrix_512_512);
    
    
    funky::LinearArray< float >::Version* matrix_512_1024 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9833_0=funky::Profile::RegisterCustom("9833","main","Compile");
                tbb::tick_count __TIME__9833_0=tbb::tick_count::now();
                ocl_kernel main_26(&device,"tmp/main_26.cl");
                
                
                __profile__9833_0->AddMeasurement((tbb::tick_count::now()-__TIME__9833_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9833_1=funky::Profile::RegisterCustom("9833","main","CopyTo");
                tbb::tick_count __TIME__9833_1=tbb::tick_count::now();
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[524288];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*524288,CL_MEM_WRITE_ONLY);
                
                
                main_26.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9833_1->AddMeasurement((tbb::tick_count::now()-__TIME__9833_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9833_2=funky::Profile::RegisterCustom("9833","main","GPURun");
                tbb::tick_count __TIME__9833_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_26.timedRun(1024, 524288);
                device.finish();double runtime = main_26.getRunTime(id);
                
                __profile__9833_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9833_3=funky::Profile::RegisterCustom("9833","main","copyBack");
                tbb::tick_count __TIME__9833_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(524288,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,1024);
                
                
                __profile__9833_3->AddMeasurement((tbb::tick_count::now()-__TIME__9833_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9833_4=funky::Profile::RegisterCustom("9833","main","WholeCPURun");
                tbb::tick_count __TIME__9833_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 511 + 1);
                

                __profile__9833_4->AddMeasurement((tbb::tick_count::now()-__TIME__9833_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_512_1024_float_two_d_512_1024___float_two_d_1024_512__(matrix_512_1024);
    
    
    funky::LinearArray< float >::Version* matrix_1024_1024 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1048576,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9977_0=funky::Profile::RegisterCustom("9977","main","Compile");
                tbb::tick_count __TIME__9977_0=tbb::tick_count::now();
                ocl_kernel main_27(&device,"tmp/main_27.cl");
                
                
                __profile__9977_0->AddMeasurement((tbb::tick_count::now()-__TIME__9977_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9977_1=funky::Profile::RegisterCustom("9977","main","CopyTo");
                tbb::tick_count __TIME__9977_1=tbb::tick_count::now();
                int __new_array_0_dim_0=1024;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[1048576];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*1048576,CL_MEM_WRITE_ONLY);
                
                
                main_27.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9977_1->AddMeasurement((tbb::tick_count::now()-__TIME__9977_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9977_2=funky::Profile::RegisterCustom("9977","main","GPURun");
                tbb::tick_count __TIME__9977_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_27.timedRun(1024, 1048576);
                device.finish();double runtime = main_27.getRunTime(id);
                
                __profile__9977_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9977_3=funky::Profile::RegisterCustom("9977","main","copyBack");
                tbb::tick_count __TIME__9977_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1048576,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,1024,1024);
                
                
                __profile__9977_3->AddMeasurement((tbb::tick_count::now()-__TIME__9977_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1048576,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9977_4=funky::Profile::RegisterCustom("9977","main","WholeCPURun");
                tbb::tick_count __TIME__9977_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 1023 + 1);
                

                __profile__9977_4->AddMeasurement((tbb::tick_count::now()-__TIME__9977_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_1024_1024_float_two_d_1024_1024___float_two_d_1024_1024__(matrix_1024_1024);
    
    
    funky::LinearArray< float >::Version* matrix_2048_1024 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2097152,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10123_0=funky::Profile::RegisterCustom("10123","main","Compile");
                tbb::tick_count __TIME__10123_0=tbb::tick_count::now();
                ocl_kernel main_28(&device,"tmp/main_28.cl");
                
                
                __profile__10123_0->AddMeasurement((tbb::tick_count::now()-__TIME__10123_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10123_1=funky::Profile::RegisterCustom("10123","main","CopyTo");
                tbb::tick_count __TIME__10123_1=tbb::tick_count::now();
                int __new_array_0_dim_0=2048;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[2097152];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2097152,CL_MEM_WRITE_ONLY);
                
                
                main_28.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10123_1->AddMeasurement((tbb::tick_count::now()-__TIME__10123_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10123_2=funky::Profile::RegisterCustom("10123","main","GPURun");
                tbb::tick_count __TIME__10123_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_28.timedRun(1024, 2097152);
                device.finish();double runtime = main_28.getRunTime(id);
                
                __profile__10123_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10123_3=funky::Profile::RegisterCustom("10123","main","copyBack");
                tbb::tick_count __TIME__10123_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2097152,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,2048,1024);
                
                
                __profile__10123_3->AddMeasurement((tbb::tick_count::now()-__TIME__10123_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2097152,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10123_4=funky::Profile::RegisterCustom("10123","main","WholeCPURun");
                tbb::tick_count __TIME__10123_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 2047 + 1);
                

                __profile__10123_4->AddMeasurement((tbb::tick_count::now()-__TIME__10123_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_2048_1024_float_two_d_2048_1024___float_two_d_1024_2048__(matrix_2048_1024);
    
    
    funky::LinearArray< float >::Version* matrix_2048_2048 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4194304,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10270_0=funky::Profile::RegisterCustom("10270","main","Compile");
                tbb::tick_count __TIME__10270_0=tbb::tick_count::now();
                ocl_kernel main_29(&device,"tmp/main_29.cl");
                
                
                __profile__10270_0->AddMeasurement((tbb::tick_count::now()-__TIME__10270_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10270_1=funky::Profile::RegisterCustom("10270","main","CopyTo");
                tbb::tick_count __TIME__10270_1=tbb::tick_count::now();
                int __new_array_0_dim_0=2048;
                int __new_array_0_dim_1=2048;
                float *__return_val_0=new float[4194304];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*4194304,CL_MEM_WRITE_ONLY);
                
                
                main_29.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10270_1->AddMeasurement((tbb::tick_count::now()-__TIME__10270_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10270_2=funky::Profile::RegisterCustom("10270","main","GPURun");
                tbb::tick_count __TIME__10270_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_29.timedRun(1024, 4194304);
                device.finish();double runtime = main_29.getRunTime(id);
                
                __profile__10270_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10270_3=funky::Profile::RegisterCustom("10270","main","copyBack");
                tbb::tick_count __TIME__10270_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4194304,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,2048,2048);
                
                
                __profile__10270_3->AddMeasurement((tbb::tick_count::now()-__TIME__10270_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4194304,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10270_4=funky::Profile::RegisterCustom("10270","main","WholeCPURun");
                tbb::tick_count __TIME__10270_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 2047 + 1,  + ((x)) * 2048L, __EXP_TMP0);
                    
                }, 0, 2047 + 1);
                

                __profile__10270_4->AddMeasurement((tbb::tick_count::now()-__TIME__10270_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_2048_2048_float_two_d_2048_2048___float_two_d_2048_2048__(matrix_2048_2048);
    
    
    funky::LinearArray< float >::Version* matrix_4096_2048 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8388608,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10416_0=funky::Profile::RegisterCustom("10416","main","Compile");
                tbb::tick_count __TIME__10416_0=tbb::tick_count::now();
                ocl_kernel main_30(&device,"tmp/main_30.cl");
                
                
                __profile__10416_0->AddMeasurement((tbb::tick_count::now()-__TIME__10416_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10416_1=funky::Profile::RegisterCustom("10416","main","CopyTo");
                tbb::tick_count __TIME__10416_1=tbb::tick_count::now();
                int __new_array_0_dim_0=4096;
                int __new_array_0_dim_1=2048;
                float *__return_val_0=new float[8388608];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8388608,CL_MEM_WRITE_ONLY);
                
                
                main_30.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10416_1->AddMeasurement((tbb::tick_count::now()-__TIME__10416_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10416_2=funky::Profile::RegisterCustom("10416","main","GPURun");
                tbb::tick_count __TIME__10416_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_30.timedRun(1024, 8388608);
                device.finish();double runtime = main_30.getRunTime(id);
                
                __profile__10416_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10416_3=funky::Profile::RegisterCustom("10416","main","copyBack");
                tbb::tick_count __TIME__10416_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8388608,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,2048);
                
                
                __profile__10416_3->AddMeasurement((tbb::tick_count::now()-__TIME__10416_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8388608,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10416_4=funky::Profile::RegisterCustom("10416","main","WholeCPURun");
                tbb::tick_count __TIME__10416_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 2047 + 1,  + ((x)) * 2048L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                

                __profile__10416_4->AddMeasurement((tbb::tick_count::now()-__TIME__10416_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_4096_2048_float_two_d_4096_2048___float_two_d_2048_4096__(matrix_4096_2048);
    
    
    funky::LinearArray< float >::Version* matrix_4096_4096 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16777216,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10562_0=funky::Profile::RegisterCustom("10562","main","Compile");
                tbb::tick_count __TIME__10562_0=tbb::tick_count::now();
                ocl_kernel main_31(&device,"tmp/main_31.cl");
                
                
                __profile__10562_0->AddMeasurement((tbb::tick_count::now()-__TIME__10562_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10562_1=funky::Profile::RegisterCustom("10562","main","CopyTo");
                tbb::tick_count __TIME__10562_1=tbb::tick_count::now();
                int __new_array_0_dim_0=4096;
                int __new_array_0_dim_1=4096;
                float *__return_val_0=new float[16777216];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*16777216,CL_MEM_WRITE_ONLY);
                
                
                main_31.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10562_1->AddMeasurement((tbb::tick_count::now()-__TIME__10562_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10562_2=funky::Profile::RegisterCustom("10562","main","GPURun");
                tbb::tick_count __TIME__10562_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_31.timedRun(1024, 16777216);
                device.finish();double runtime = main_31.getRunTime(id);
                
                __profile__10562_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10562_3=funky::Profile::RegisterCustom("10562","main","copyBack");
                tbb::tick_count __TIME__10562_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16777216,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,4096);
                
                
                __profile__10562_3->AddMeasurement((tbb::tick_count::now()-__TIME__10562_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16777216,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10562_4=funky::Profile::RegisterCustom("10562","main","WholeCPURun");
                tbb::tick_count __TIME__10562_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 4095 + 1,  + ((x)) * 4096L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                

                __profile__10562_4->AddMeasurement((tbb::tick_count::now()-__TIME__10562_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_4096_4096_float_two_d_4096_4096___float_two_d_4096_4096__(matrix_4096_4096);
    
    
    funky::LinearArray< float >::Version* matrix_4096_8192 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(33554432,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10708_0=funky::Profile::RegisterCustom("10708","main","Compile");
                tbb::tick_count __TIME__10708_0=tbb::tick_count::now();
                ocl_kernel main_32(&device,"tmp/main_32.cl");
                
                
                __profile__10708_0->AddMeasurement((tbb::tick_count::now()-__TIME__10708_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10708_1=funky::Profile::RegisterCustom("10708","main","CopyTo");
                tbb::tick_count __TIME__10708_1=tbb::tick_count::now();
                int __new_array_0_dim_0=4096;
                int __new_array_0_dim_1=8192;
                float *__return_val_0=new float[33554432];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*33554432,CL_MEM_WRITE_ONLY);
                
                
                main_32.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10708_1->AddMeasurement((tbb::tick_count::now()-__TIME__10708_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10708_2=funky::Profile::RegisterCustom("10708","main","GPURun");
                tbb::tick_count __TIME__10708_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_32.timedRun(1024, 33554432);
                device.finish();double runtime = main_32.getRunTime(id);
                
                __profile__10708_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10708_3=funky::Profile::RegisterCustom("10708","main","copyBack");
                tbb::tick_count __TIME__10708_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(33554432,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,8192);
                
                
                __profile__10708_3->AddMeasurement((tbb::tick_count::now()-__TIME__10708_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(33554432,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10708_4=funky::Profile::RegisterCustom("10708","main","WholeCPURun");
                tbb::tick_count __TIME__10708_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 8191 + 1,  + ((x)) * 8192L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                

                __profile__10708_4->AddMeasurement((tbb::tick_count::now()-__TIME__10708_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_4096_8192_float_two_d_4096_8192___float_two_d_8192_4096__(matrix_4096_8192);
    
    
    funky::LinearArray< float >::Version* matrix_8192_8192 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(67108864,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10855_0=funky::Profile::RegisterCustom("10855","main","Compile");
                tbb::tick_count __TIME__10855_0=tbb::tick_count::now();
                ocl_kernel main_33(&device,"tmp/main_33.cl");
                
                
                __profile__10855_0->AddMeasurement((tbb::tick_count::now()-__TIME__10855_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10855_1=funky::Profile::RegisterCustom("10855","main","CopyTo");
                tbb::tick_count __TIME__10855_1=tbb::tick_count::now();
                int __new_array_0_dim_0=8192;
                int __new_array_0_dim_1=8192;
                float *__return_val_0=new float[67108864];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*67108864,CL_MEM_WRITE_ONLY);
                
                
                main_33.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10855_1->AddMeasurement((tbb::tick_count::now()-__TIME__10855_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10855_2=funky::Profile::RegisterCustom("10855","main","GPURun");
                tbb::tick_count __TIME__10855_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                int id = main_33.timedRun(1024, 67108864);
                device.finish();double runtime = main_33.getRunTime(id);
                
                __profile__10855_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10855_3=funky::Profile::RegisterCustom("10855","main","copyBack");
                tbb::tick_count __TIME__10855_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(67108864,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,8192,8192);
                
                
                __profile__10855_3->AddMeasurement((tbb::tick_count::now()-__TIME__10855_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(67108864,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__10855_4=funky::Profile::RegisterCustom("10855","main","WholeCPURun");
                tbb::tick_count __TIME__10855_4=tbb::tick_count::now();
                                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->int
                    {
                        return (x * 10) + (9 - y)
                        ;
                        
                    }, 0, 8191 + 1,  + ((x)) * 8192L, __EXP_TMP0);
                    
                }, 0, 8191 + 1);
                

                __profile__10855_4->AddMeasurement((tbb::tick_count::now()-__TIME__10855_4).seconds()*1e6);
                                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_8192_8192_float_two_d_8192_8192___float_two_d_8192_8192__(matrix_8192_8192);
    

    
    return 0;
}

void sighandler(int sig){funky::Debug::Abort();exit(-1);}

CONTEXT(int,StartContext,{int argc; char** argv;},{},{});

GLOBAL_TASK(RUN, StartContext){context()->SetReturn(cur::main_int_String_one_d_01___int(context()->params.argc,(new funky::LinearArray<char*>::Version(new funky::LinearArray<char*>(context()->params.argc)))->map([&](funky::uint32 i){return context()->params.argv[i];},0,context()->params.argc)));}END_GLOBAL_TASK()

GLOBAL_TASK(START, StartContext){set_ref_count(2);spawn_and_wait_for_all(*new( allocate_child()) RUN(context()));}END_GLOBAL_TASK()

GLOBAL_TASK(WORKER, StartContext){usleep(100000);}END_GLOBAL_TASK()

GLOBAL_TASK(START_WORKERS, StartContext){set_ref_count(2);for(funky::uint32 i=0;i<1;i++)spawn(*new (allocate_child()) WORKER(context()));wait_for_all();}END_GLOBAL_TASK()

int main(int argc, char* argv[])
{
    GC_INIT();
    tbb::task_scheduler_init();
    signal(SIGABRT, &sighandler);signal(SIGTERM, &sighandler);signal(SIGINT, &sighandler);
    
    StartContext sp;
    sp.params.argc = argc;
    sp.params.argv = argv;
    
    
    tbb::task::spawn_root_and_wait(*new (tbb::task::allocate_root()) START_WORKERS(&sp));//encourage tbb to start worker threads before we start measuring
    device = ocl::getDevice(0,2);
    
    
    tbb::tick_count t0 = tbb::tick_count::now();
    
    for(funky::uint32 i=0;i<5;i++){
    funky::TaskRoot<>::GetRoot() = new(tbb::task::allocate_root()) START(&sp);
    printf("Root(0x%016p)\n",funky::TaskRoot<>::GetRoot());
    tbb::task::spawn_root_and_wait(*funky::TaskRoot<>::GetRoot());}
    tbb::tick_count t1 = tbb::tick_count::now();
    
    float d = (t1 - t0).seconds();d=d/5;
    printf("\nretval: %d\ntime: %f [s] %f [clocks]\n",sp.GetReturn(), d,d*79999.99797903001);
    funky::Profile::DumpProfileStats("profile.properties",5,0.07999999797903001);funky::Profile::DumpCustomProfileStats(1,1.0);funky::Debug::Exit();int retval = sp.GetReturn();
    fprintf(stdout,"Start GC Stats\n");fflush(stdout);
    GC_gcollect();
    GC_gcollect();
    GC_dump();
    return retval;
    
}

