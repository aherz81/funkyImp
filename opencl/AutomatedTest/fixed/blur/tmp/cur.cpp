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
                static funky::ProfileEntry* __profile__6020_0=funky::Profile::RegisterCustom("6020","transpose_32_32","Compile");
                tbb::tick_count __TIME__6020_0=tbb::tick_count::now();
                ocl_kernel transpose_32_32_0(&device,"tmp/transpose_32_32_0.cl", "-cl-opt-disable");
                
                
                __profile__6020_0->AddMeasurement((tbb::tick_count::now()-__TIME__6020_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6020_1=funky::Profile::RegisterCustom("6020","transpose_32_32","CopyTo");
                tbb::tick_count __TIME__6020_1=tbb::tick_count::now();
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
                
                __profile__6020_1->AddMeasurement((tbb::tick_count::now()-__TIME__6020_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6020_2=funky::Profile::RegisterCustom("6020","transpose_32_32","GPURun");
                tbb::tick_count __TIME__6020_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_32_32_0 = transpose_32_32_0.getOptimalWgSize(1024);
                int id = transpose_32_32_0.timedRun(__wgtranspose_32_32_0, 1024);
                device.finish();double runtime = transpose_32_32_0.getRunTime(id);
                
                __profile__6020_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6020_3=funky::Profile::RegisterCustom("6020","transpose_32_32","copyBack");
                tbb::tick_count __TIME__6020_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1024,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,32);
                
                
                __profile__6020_3->AddMeasurement((tbb::tick_count::now()-__TIME__6020_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1024,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_32_32_float_two_d_32_32___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 31 + 1,  + ((x)) * 32L, __EXP_TMP0);
                    
                }, 0, 31 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_32_64_float_two_d_32_64___float_two_d_32_64__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6174_0=funky::Profile::RegisterCustom("6174","transpose_32_64","Compile");
                tbb::tick_count __TIME__6174_0=tbb::tick_count::now();
                ocl_kernel transpose_32_64_1(&device,"tmp/transpose_32_64_1.cl", "-cl-opt-disable");
                
                
                __profile__6174_0->AddMeasurement((tbb::tick_count::now()-__TIME__6174_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6174_1=funky::Profile::RegisterCustom("6174","transpose_32_64","CopyTo");
                tbb::tick_count __TIME__6174_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*2048,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=32;
                int __matrix0_dim_1=64;
                int __new_array_0_dim_0=32;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[2048];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2048,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_32_64_1.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6174_1->AddMeasurement((tbb::tick_count::now()-__TIME__6174_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6174_2=funky::Profile::RegisterCustom("6174","transpose_32_64","GPURun");
                tbb::tick_count __TIME__6174_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_32_64_1 = transpose_32_64_1.getOptimalWgSize(2048);
                int id = transpose_32_64_1.timedRun(__wgtranspose_32_64_1, 2048);
                device.finish();double runtime = transpose_32_64_1.getRunTime(id);
                
                __profile__6174_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6174_3=funky::Profile::RegisterCustom("6174","transpose_32_64","copyBack");
                tbb::tick_count __TIME__6174_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2048,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,64);
                
                
                __profile__6174_3->AddMeasurement((tbb::tick_count::now()-__TIME__6174_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_32_64_float_two_d_32_64___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 63 + 1,  + ((x)) * 64L, __EXP_TMP0);
                    
                }, 0, 31 + 1);
                
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
                static funky::ProfileEntry* __profile__6328_0=funky::Profile::RegisterCustom("6328","transpose_64_64","Compile");
                tbb::tick_count __TIME__6328_0=tbb::tick_count::now();
                ocl_kernel transpose_64_64_2(&device,"tmp/transpose_64_64_2.cl", "-cl-opt-disable");
                
                
                __profile__6328_0->AddMeasurement((tbb::tick_count::now()-__TIME__6328_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6328_1=funky::Profile::RegisterCustom("6328","transpose_64_64","CopyTo");
                tbb::tick_count __TIME__6328_1=tbb::tick_count::now();
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
                
                __profile__6328_1->AddMeasurement((tbb::tick_count::now()-__TIME__6328_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6328_2=funky::Profile::RegisterCustom("6328","transpose_64_64","GPURun");
                tbb::tick_count __TIME__6328_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_64_64_2 = transpose_64_64_2.getOptimalWgSize(4096);
                int id = transpose_64_64_2.timedRun(__wgtranspose_64_64_2, 4096);
                device.finish();double runtime = transpose_64_64_2.getRunTime(id);
                
                __profile__6328_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6328_3=funky::Profile::RegisterCustom("6328","transpose_64_64","copyBack");
                tbb::tick_count __TIME__6328_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4096,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,64);
                
                
                __profile__6328_3->AddMeasurement((tbb::tick_count::now()-__TIME__6328_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_64_64_float_two_d_64_64___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 63 + 1,  + ((x)) * 64L, __EXP_TMP0);
                    
                }, 0, 63 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_64_128_float_two_d_64_128___float_two_d_64_128__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6486_0=funky::Profile::RegisterCustom("6486","transpose_64_128","Compile");
                tbb::tick_count __TIME__6486_0=tbb::tick_count::now();
                ocl_kernel transpose_64_128_3(&device,"tmp/transpose_64_128_3.cl", "-cl-opt-disable");
                
                
                __profile__6486_0->AddMeasurement((tbb::tick_count::now()-__TIME__6486_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6486_1=funky::Profile::RegisterCustom("6486","transpose_64_128","CopyTo");
                tbb::tick_count __TIME__6486_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*8192,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=64;
                int __matrix0_dim_1=128;
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[8192];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8192,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_64_128_3.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6486_1->AddMeasurement((tbb::tick_count::now()-__TIME__6486_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6486_2=funky::Profile::RegisterCustom("6486","transpose_64_128","GPURun");
                tbb::tick_count __TIME__6486_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_64_128_3 = transpose_64_128_3.getOptimalWgSize(8192);
                int id = transpose_64_128_3.timedRun(__wgtranspose_64_128_3, 8192);
                device.finish();double runtime = transpose_64_128_3.getRunTime(id);
                
                __profile__6486_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6486_3=funky::Profile::RegisterCustom("6486","transpose_64_128","copyBack");
                tbb::tick_count __TIME__6486_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8192,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,128);
                
                
                __profile__6486_3->AddMeasurement((tbb::tick_count::now()-__TIME__6486_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_64_128_float_two_d_64_128___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 63 + 1);
                
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
                static funky::ProfileEntry* __profile__6650_0=funky::Profile::RegisterCustom("6650","transpose_128_128","Compile");
                tbb::tick_count __TIME__6650_0=tbb::tick_count::now();
                ocl_kernel transpose_128_128_4(&device,"tmp/transpose_128_128_4.cl", "-cl-opt-disable");
                
                
                __profile__6650_0->AddMeasurement((tbb::tick_count::now()-__TIME__6650_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6650_1=funky::Profile::RegisterCustom("6650","transpose_128_128","CopyTo");
                tbb::tick_count __TIME__6650_1=tbb::tick_count::now();
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
                
                __profile__6650_1->AddMeasurement((tbb::tick_count::now()-__TIME__6650_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6650_2=funky::Profile::RegisterCustom("6650","transpose_128_128","GPURun");
                tbb::tick_count __TIME__6650_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_128_128_4 = transpose_128_128_4.getOptimalWgSize(16384);
                int id = transpose_128_128_4.timedRun(__wgtranspose_128_128_4, 16384);
                device.finish();double runtime = transpose_128_128_4.getRunTime(id);
                
                __profile__6650_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6650_3=funky::Profile::RegisterCustom("6650","transpose_128_128","copyBack");
                tbb::tick_count __TIME__6650_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16384,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,128,128);
                
                
                __profile__6650_3->AddMeasurement((tbb::tick_count::now()-__TIME__6650_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_128_128_float_two_d_128_128___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 127 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_256_128_float_two_d_256_128___float_two_d_256_128__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__6814_0=funky::Profile::RegisterCustom("6814","transpose_256_128","Compile");
                tbb::tick_count __TIME__6814_0=tbb::tick_count::now();
                ocl_kernel transpose_256_128_5(&device,"tmp/transpose_256_128_5.cl", "-cl-opt-disable");
                
                
                __profile__6814_0->AddMeasurement((tbb::tick_count::now()-__TIME__6814_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6814_1=funky::Profile::RegisterCustom("6814","transpose_256_128","CopyTo");
                tbb::tick_count __TIME__6814_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*32768,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=256;
                int __matrix0_dim_1=128;
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[32768];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*32768,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_256_128_5.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__6814_1->AddMeasurement((tbb::tick_count::now()-__TIME__6814_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6814_2=funky::Profile::RegisterCustom("6814","transpose_256_128","GPURun");
                tbb::tick_count __TIME__6814_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_256_128_5 = transpose_256_128_5.getOptimalWgSize(32768);
                int id = transpose_256_128_5.timedRun(__wgtranspose_256_128_5, 32768);
                device.finish();double runtime = transpose_256_128_5.getRunTime(id);
                
                __profile__6814_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6814_3=funky::Profile::RegisterCustom("6814","transpose_256_128","copyBack");
                tbb::tick_count __TIME__6814_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(32768,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,128);
                
                
                __profile__6814_3->AddMeasurement((tbb::tick_count::now()-__TIME__6814_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_256_128_float_two_d_256_128___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 127 + 1,  + ((x)) * 128L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                
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
                static funky::ProfileEntry* __profile__6978_0=funky::Profile::RegisterCustom("6978","transpose_256_256","Compile");
                tbb::tick_count __TIME__6978_0=tbb::tick_count::now();
                ocl_kernel transpose_256_256_6(&device,"tmp/transpose_256_256_6.cl", "-cl-opt-disable");
                
                
                __profile__6978_0->AddMeasurement((tbb::tick_count::now()-__TIME__6978_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6978_1=funky::Profile::RegisterCustom("6978","transpose_256_256","CopyTo");
                tbb::tick_count __TIME__6978_1=tbb::tick_count::now();
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
                
                __profile__6978_1->AddMeasurement((tbb::tick_count::now()-__TIME__6978_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__6978_2=funky::Profile::RegisterCustom("6978","transpose_256_256","GPURun");
                tbb::tick_count __TIME__6978_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_256_256_6 = transpose_256_256_6.getOptimalWgSize(65536);
                int id = transpose_256_256_6.timedRun(__wgtranspose_256_256_6, 65536);
                device.finish();double runtime = transpose_256_256_6.getRunTime(id);
                
                __profile__6978_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__6978_3=funky::Profile::RegisterCustom("6978","transpose_256_256","copyBack");
                tbb::tick_count __TIME__6978_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(65536,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,256);
                
                
                __profile__6978_3->AddMeasurement((tbb::tick_count::now()-__TIME__6978_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_256_256_float_two_d_256_256___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 255 + 1,  + ((x)) * 256L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_256_512_float_two_d_256_512___float_two_d_256_512__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7142_0=funky::Profile::RegisterCustom("7142","transpose_256_512","Compile");
                tbb::tick_count __TIME__7142_0=tbb::tick_count::now();
                ocl_kernel transpose_256_512_7(&device,"tmp/transpose_256_512_7.cl", "-cl-opt-disable");
                
                
                __profile__7142_0->AddMeasurement((tbb::tick_count::now()-__TIME__7142_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7142_1=funky::Profile::RegisterCustom("7142","transpose_256_512","CopyTo");
                tbb::tick_count __TIME__7142_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*131072,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=256;
                int __matrix0_dim_1=512;
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[131072];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*131072,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_256_512_7.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7142_1->AddMeasurement((tbb::tick_count::now()-__TIME__7142_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7142_2=funky::Profile::RegisterCustom("7142","transpose_256_512","GPURun");
                tbb::tick_count __TIME__7142_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_256_512_7 = transpose_256_512_7.getOptimalWgSize(131072);
                int id = transpose_256_512_7.timedRun(__wgtranspose_256_512_7, 131072);
                device.finish();double runtime = transpose_256_512_7.getRunTime(id);
                
                __profile__7142_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7142_3=funky::Profile::RegisterCustom("7142","transpose_256_512","copyBack");
                tbb::tick_count __TIME__7142_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(131072,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,512);
                
                
                __profile__7142_3->AddMeasurement((tbb::tick_count::now()-__TIME__7142_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_256_512_float_two_d_256_512___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 511 + 1,  + ((x)) * 512L, __EXP_TMP0);
                    
                }, 0, 255 + 1);
                
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
                static funky::ProfileEntry* __profile__7307_0=funky::Profile::RegisterCustom("7307","transpose_512_512","Compile");
                tbb::tick_count __TIME__7307_0=tbb::tick_count::now();
                ocl_kernel transpose_512_512_8(&device,"tmp/transpose_512_512_8.cl", "-cl-opt-disable");
                
                
                __profile__7307_0->AddMeasurement((tbb::tick_count::now()-__TIME__7307_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7307_1=funky::Profile::RegisterCustom("7307","transpose_512_512","CopyTo");
                tbb::tick_count __TIME__7307_1=tbb::tick_count::now();
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
                
                __profile__7307_1->AddMeasurement((tbb::tick_count::now()-__TIME__7307_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7307_2=funky::Profile::RegisterCustom("7307","transpose_512_512","GPURun");
                tbb::tick_count __TIME__7307_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_512_512_8 = transpose_512_512_8.getOptimalWgSize(262144);
                int id = transpose_512_512_8.timedRun(__wgtranspose_512_512_8, 262144);
                device.finish();double runtime = transpose_512_512_8.getRunTime(id);
                
                __profile__7307_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7307_3=funky::Profile::RegisterCustom("7307","transpose_512_512","copyBack");
                tbb::tick_count __TIME__7307_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(262144,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,512);
                
                
                __profile__7307_3->AddMeasurement((tbb::tick_count::now()-__TIME__7307_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_512_512_float_two_d_512_512___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 511 + 1,  + ((x)) * 512L, __EXP_TMP0);
                    
                }, 0, 511 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_512_1024_float_two_d_512_1024___float_two_d_512_1024__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7475_0=funky::Profile::RegisterCustom("7475","transpose_512_1024","Compile");
                tbb::tick_count __TIME__7475_0=tbb::tick_count::now();
                ocl_kernel transpose_512_1024_9(&device,"tmp/transpose_512_1024_9.cl", "-cl-opt-disable");
                
                
                __profile__7475_0->AddMeasurement((tbb::tick_count::now()-__TIME__7475_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7475_1=funky::Profile::RegisterCustom("7475","transpose_512_1024","CopyTo");
                tbb::tick_count __TIME__7475_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*524288,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=512;
                int __matrix0_dim_1=1024;
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[524288];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*524288,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_512_1024_9.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7475_1->AddMeasurement((tbb::tick_count::now()-__TIME__7475_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7475_2=funky::Profile::RegisterCustom("7475","transpose_512_1024","GPURun");
                tbb::tick_count __TIME__7475_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_512_1024_9 = transpose_512_1024_9.getOptimalWgSize(524288);
                int id = transpose_512_1024_9.timedRun(__wgtranspose_512_1024_9, 524288);
                device.finish();double runtime = transpose_512_1024_9.getRunTime(id);
                
                __profile__7475_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7475_3=funky::Profile::RegisterCustom("7475","transpose_512_1024","copyBack");
                tbb::tick_count __TIME__7475_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(524288,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,1024);
                
                
                __profile__7475_3->AddMeasurement((tbb::tick_count::now()-__TIME__7475_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_512_1024_float_two_d_512_1024___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 511 + 1);
                
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
                static funky::ProfileEntry* __profile__7648_0=funky::Profile::RegisterCustom("7648","transpose_1024_1024","Compile");
                tbb::tick_count __TIME__7648_0=tbb::tick_count::now();
                ocl_kernel transpose_1024_1024_10(&device,"tmp/transpose_1024_1024_10.cl", "-cl-opt-disable");
                
                
                __profile__7648_0->AddMeasurement((tbb::tick_count::now()-__TIME__7648_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7648_1=funky::Profile::RegisterCustom("7648","transpose_1024_1024","CopyTo");
                tbb::tick_count __TIME__7648_1=tbb::tick_count::now();
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
                
                __profile__7648_1->AddMeasurement((tbb::tick_count::now()-__TIME__7648_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7648_2=funky::Profile::RegisterCustom("7648","transpose_1024_1024","GPURun");
                tbb::tick_count __TIME__7648_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_1024_1024_10 = transpose_1024_1024_10.getOptimalWgSize(1048576);
                int id = transpose_1024_1024_10.timedRun(__wgtranspose_1024_1024_10, 1048576);
                device.finish();double runtime = transpose_1024_1024_10.getRunTime(id);
                
                __profile__7648_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7648_3=funky::Profile::RegisterCustom("7648","transpose_1024_1024","copyBack");
                tbb::tick_count __TIME__7648_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1048576,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,1024,1024);
                
                
                __profile__7648_3->AddMeasurement((tbb::tick_count::now()-__TIME__7648_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1048576,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_1024_1024_float_two_d_1024_1024___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 1023 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_2048_1024_float_two_d_2048_1024___float_two_d_2048_1024__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2097152,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__7822_0=funky::Profile::RegisterCustom("7822","transpose_2048_1024","Compile");
                tbb::tick_count __TIME__7822_0=tbb::tick_count::now();
                ocl_kernel transpose_2048_1024_11(&device,"tmp/transpose_2048_1024_11.cl", "-cl-opt-disable");
                
                
                __profile__7822_0->AddMeasurement((tbb::tick_count::now()-__TIME__7822_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7822_1=funky::Profile::RegisterCustom("7822","transpose_2048_1024","CopyTo");
                tbb::tick_count __TIME__7822_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*2097152,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=2048;
                int __matrix0_dim_1=1024;
                int __new_array_0_dim_0=2048;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[2097152];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2097152,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_2048_1024_11.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__7822_1->AddMeasurement((tbb::tick_count::now()-__TIME__7822_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7822_2=funky::Profile::RegisterCustom("7822","transpose_2048_1024","GPURun");
                tbb::tick_count __TIME__7822_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_2048_1024_11 = transpose_2048_1024_11.getOptimalWgSize(2097152);
                int id = transpose_2048_1024_11.timedRun(__wgtranspose_2048_1024_11, 2097152);
                device.finish();double runtime = transpose_2048_1024_11.getRunTime(id);
                
                __profile__7822_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7822_3=funky::Profile::RegisterCustom("7822","transpose_2048_1024","copyBack");
                tbb::tick_count __TIME__7822_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2097152,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,2048,1024);
                
                
                __profile__7822_3->AddMeasurement((tbb::tick_count::now()-__TIME__7822_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2097152,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_2048_1024_float_two_d_2048_1024___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 1023 + 1,  + ((x)) * 1024L, __EXP_TMP0);
                    
                }, 0, 2047 + 1);
                
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
                static funky::ProfileEntry* __profile__7997_0=funky::Profile::RegisterCustom("7997","transpose_2048_2048","Compile");
                tbb::tick_count __TIME__7997_0=tbb::tick_count::now();
                ocl_kernel transpose_2048_2048_12(&device,"tmp/transpose_2048_2048_12.cl", "-cl-opt-disable");
                
                
                __profile__7997_0->AddMeasurement((tbb::tick_count::now()-__TIME__7997_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7997_1=funky::Profile::RegisterCustom("7997","transpose_2048_2048","CopyTo");
                tbb::tick_count __TIME__7997_1=tbb::tick_count::now();
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
                
                __profile__7997_1->AddMeasurement((tbb::tick_count::now()-__TIME__7997_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__7997_2=funky::Profile::RegisterCustom("7997","transpose_2048_2048","GPURun");
                tbb::tick_count __TIME__7997_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_2048_2048_12 = transpose_2048_2048_12.getOptimalWgSize(4194304);
                int id = transpose_2048_2048_12.timedRun(__wgtranspose_2048_2048_12, 4194304);
                device.finish();double runtime = transpose_2048_2048_12.getRunTime(id);
                
                __profile__7997_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__7997_3=funky::Profile::RegisterCustom("7997","transpose_2048_2048","copyBack");
                tbb::tick_count __TIME__7997_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4194304,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,2048,2048);
                
                
                __profile__7997_3->AddMeasurement((tbb::tick_count::now()-__TIME__7997_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4194304,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_2048_2048_float_two_d_2048_2048___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 2047 + 1,  + ((x)) * 2048L, __EXP_TMP0);
                    
                }, 0, 2047 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_4096_2048_float_two_d_4096_2048___float_two_d_4096_2048__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8388608,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8171_0=funky::Profile::RegisterCustom("8171","transpose_4096_2048","Compile");
                tbb::tick_count __TIME__8171_0=tbb::tick_count::now();
                ocl_kernel transpose_4096_2048_13(&device,"tmp/transpose_4096_2048_13.cl", "-cl-opt-disable");
                
                
                __profile__8171_0->AddMeasurement((tbb::tick_count::now()-__TIME__8171_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8171_1=funky::Profile::RegisterCustom("8171","transpose_4096_2048","CopyTo");
                tbb::tick_count __TIME__8171_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*8388608,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=4096;
                int __matrix0_dim_1=2048;
                int __new_array_0_dim_0=4096;
                int __new_array_0_dim_1=2048;
                float *__return_val_0=new float[8388608];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8388608,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_4096_2048_13.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8171_1->AddMeasurement((tbb::tick_count::now()-__TIME__8171_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8171_2=funky::Profile::RegisterCustom("8171","transpose_4096_2048","GPURun");
                tbb::tick_count __TIME__8171_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_4096_2048_13 = transpose_4096_2048_13.getOptimalWgSize(8388608);
                int id = transpose_4096_2048_13.timedRun(__wgtranspose_4096_2048_13, 8388608);
                device.finish();double runtime = transpose_4096_2048_13.getRunTime(id);
                
                __profile__8171_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8171_3=funky::Profile::RegisterCustom("8171","transpose_4096_2048","copyBack");
                tbb::tick_count __TIME__8171_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8388608,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,2048);
                
                
                __profile__8171_3->AddMeasurement((tbb::tick_count::now()-__TIME__8171_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8388608,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_4096_2048_float_two_d_4096_2048___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 2047 + 1,  + ((x)) * 2048L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                
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
                static funky::ProfileEntry* __profile__8345_0=funky::Profile::RegisterCustom("8345","transpose_4096_4096","Compile");
                tbb::tick_count __TIME__8345_0=tbb::tick_count::now();
                ocl_kernel transpose_4096_4096_14(&device,"tmp/transpose_4096_4096_14.cl", "-cl-opt-disable");
                
                
                __profile__8345_0->AddMeasurement((tbb::tick_count::now()-__TIME__8345_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8345_1=funky::Profile::RegisterCustom("8345","transpose_4096_4096","CopyTo");
                tbb::tick_count __TIME__8345_1=tbb::tick_count::now();
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
                
                __profile__8345_1->AddMeasurement((tbb::tick_count::now()-__TIME__8345_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8345_2=funky::Profile::RegisterCustom("8345","transpose_4096_4096","GPURun");
                tbb::tick_count __TIME__8345_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_4096_4096_14 = transpose_4096_4096_14.getOptimalWgSize(16777216);
                int id = transpose_4096_4096_14.timedRun(__wgtranspose_4096_4096_14, 16777216);
                device.finish();double runtime = transpose_4096_4096_14.getRunTime(id);
                
                __profile__8345_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8345_3=funky::Profile::RegisterCustom("8345","transpose_4096_4096","copyBack");
                tbb::tick_count __TIME__8345_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16777216,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,4096);
                
                
                __profile__8345_3->AddMeasurement((tbb::tick_count::now()-__TIME__8345_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16777216,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_4096_4096_float_two_d_4096_4096___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 4095 + 1,  + ((x)) * 4096L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    

}


funky::LinearArray< float >::Version* cur::transpose_4096_8192_float_two_d_4096_8192___float_two_d_4096_8192__(funky::LinearArray< float >::Version* matrix)
{
    
    return [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(33554432,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__8519_0=funky::Profile::RegisterCustom("8519","transpose_4096_8192","Compile");
                tbb::tick_count __TIME__8519_0=tbb::tick_count::now();
                ocl_kernel transpose_4096_8192_15(&device,"tmp/transpose_4096_8192_15.cl", "-cl-opt-disable");
                
                
                __profile__8519_0->AddMeasurement((tbb::tick_count::now()-__TIME__8519_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8519_1=funky::Profile::RegisterCustom("8519","transpose_4096_8192","CopyTo");
                tbb::tick_count __TIME__8519_1=tbb::tick_count::now();
                float *__matrix_0=matrix->toNative();
                ocl_mem __matrix_GPU_0=device.malloc(sizeof(float)*33554432,CL_MEM_READ_ONLY);
                int __matrix0_dim_0=4096;
                int __matrix0_dim_1=8192;
                int __new_array_0_dim_0=4096;
                int __new_array_0_dim_1=8192;
                float *__return_val_0=new float[33554432];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*33554432,CL_MEM_WRITE_ONLY);
                
                __matrix_GPU_0.copyFrom(__matrix_0);
                
                transpose_4096_8192_15.setArgs(__matrix_GPU_0.mem(),&__matrix0_dim_0,&__matrix0_dim_1,&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8519_1->AddMeasurement((tbb::tick_count::now()-__TIME__8519_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8519_2=funky::Profile::RegisterCustom("8519","transpose_4096_8192","GPURun");
                tbb::tick_count __TIME__8519_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_4096_8192_15 = transpose_4096_8192_15.getOptimalWgSize(33554432);
                int id = transpose_4096_8192_15.timedRun(__wgtranspose_4096_8192_15, 33554432);
                device.finish();double runtime = transpose_4096_8192_15.getRunTime(id);
                
                __profile__8519_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8519_3=funky::Profile::RegisterCustom("8519","transpose_4096_8192","copyBack");
                tbb::tick_count __TIME__8519_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(33554432,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,4096,8192);
                
                
                __profile__8519_3->AddMeasurement((tbb::tick_count::now()-__TIME__8519_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(33554432,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_4096_8192_float_two_d_4096_8192___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 8191 + 1,  + ((x)) * 8192L, __EXP_TMP0);
                    
                }, 0, 4095 + 1);
                
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
                static funky::ProfileEntry* __profile__8694_0=funky::Profile::RegisterCustom("8694","transpose_8192_8192","Compile");
                tbb::tick_count __TIME__8694_0=tbb::tick_count::now();
                ocl_kernel transpose_8192_8192_16(&device,"tmp/transpose_8192_8192_16.cl", "-cl-opt-disable");
                
                
                __profile__8694_0->AddMeasurement((tbb::tick_count::now()-__TIME__8694_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8694_1=funky::Profile::RegisterCustom("8694","transpose_8192_8192","CopyTo");
                tbb::tick_count __TIME__8694_1=tbb::tick_count::now();
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
                
                __profile__8694_1->AddMeasurement((tbb::tick_count::now()-__TIME__8694_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8694_2=funky::Profile::RegisterCustom("8694","transpose_8192_8192","GPURun");
                tbb::tick_count __TIME__8694_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgtranspose_8192_8192_16 = transpose_8192_8192_16.getOptimalWgSize(67108864);
                int id = transpose_8192_8192_16.timedRun(__wgtranspose_8192_8192_16, 67108864);
                device.finish();double runtime = transpose_8192_8192_16.getRunTime(id);
                
                __profile__8694_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8694_3=funky::Profile::RegisterCustom("8694","transpose_8192_8192","copyBack");
                tbb::tick_count __TIME__8694_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(67108864,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,8192,8192);
                
                
                __profile__8694_3->AddMeasurement((tbb::tick_count::now()-__TIME__8694_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(67108864,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
                funky::LinearArray<>::Version::forall(
                [&]
                (funky::uint32 x)->void
                {
                    __VAL_TMP0->mapNewVersion_seq(
                    [&]
                    (funky::uint32 y)->float
                    {
                        return blur_8192_8192_float_two_d_8192_8192___int_int_float(matrix, x, y)
                        ;
                        
                    }, 0, 8191 + 1,  + ((x)) * 8192L, __EXP_TMP0);
                    
                }, 0, 8191 + 1);
                
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
                static funky::ProfileEntry* __profile__8896_0=funky::Profile::RegisterCustom("8896","main","Compile");
                tbb::tick_count __TIME__8896_0=tbb::tick_count::now();
                ocl_kernel main_17(&device,"tmp/main_17.cl", "-cl-opt-disable");
                
                
                __profile__8896_0->AddMeasurement((tbb::tick_count::now()-__TIME__8896_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8896_1=funky::Profile::RegisterCustom("8896","main","CopyTo");
                tbb::tick_count __TIME__8896_1=tbb::tick_count::now();
                int __new_array_0_dim_0=32;
                int __new_array_0_dim_1=32;
                float *__return_val_0=new float[1024];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*1024,CL_MEM_WRITE_ONLY);
                
                
                main_17.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__8896_1->AddMeasurement((tbb::tick_count::now()-__TIME__8896_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__8896_2=funky::Profile::RegisterCustom("8896","main","GPURun");
                tbb::tick_count __TIME__8896_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_17 = main_17.getOptimalWgSize(1024);
                int id = main_17.timedRun(__wgmain_17, 1024);
                device.finish();double runtime = main_17.getRunTime(id);
                
                __profile__8896_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__8896_3=funky::Profile::RegisterCustom("8896","main","copyBack");
                tbb::tick_count __TIME__8896_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(1024,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,32);
                
                
                __profile__8896_3->AddMeasurement((tbb::tick_count::now()-__TIME__8896_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(1024,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                static funky::ProfileEntry* __profile__9022_0=funky::Profile::RegisterCustom("9022","main","Compile");
                tbb::tick_count __TIME__9022_0=tbb::tick_count::now();
                ocl_kernel main_18(&device,"tmp/main_18.cl", "-cl-opt-disable");
                
                
                __profile__9022_0->AddMeasurement((tbb::tick_count::now()-__TIME__9022_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9022_1=funky::Profile::RegisterCustom("9022","main","CopyTo");
                tbb::tick_count __TIME__9022_1=tbb::tick_count::now();
                int __new_array_0_dim_0=32;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[2048];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*2048,CL_MEM_WRITE_ONLY);
                
                
                main_18.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9022_1->AddMeasurement((tbb::tick_count::now()-__TIME__9022_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9022_2=funky::Profile::RegisterCustom("9022","main","GPURun");
                tbb::tick_count __TIME__9022_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_18 = main_18.getOptimalWgSize(2048);
                int id = main_18.timedRun(__wgmain_18, 2048);
                device.finish();double runtime = main_18.getRunTime(id);
                
                __profile__9022_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9022_3=funky::Profile::RegisterCustom("9022","main","copyBack");
                tbb::tick_count __TIME__9022_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(2048,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,32,64);
                
                
                __profile__9022_3->AddMeasurement((tbb::tick_count::now()-__TIME__9022_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(2048,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_32_64_float_two_d_32_64___float_two_d_32_64__(matrix_32_64);
    
    
    funky::LinearArray< float >::Version* matrix_64_64 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9148_0=funky::Profile::RegisterCustom("9148","main","Compile");
                tbb::tick_count __TIME__9148_0=tbb::tick_count::now();
                ocl_kernel main_19(&device,"tmp/main_19.cl", "-cl-opt-disable");
                
                
                __profile__9148_0->AddMeasurement((tbb::tick_count::now()-__TIME__9148_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9148_1=funky::Profile::RegisterCustom("9148","main","CopyTo");
                tbb::tick_count __TIME__9148_1=tbb::tick_count::now();
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=64;
                float *__return_val_0=new float[4096];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*4096,CL_MEM_WRITE_ONLY);
                
                
                main_19.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9148_1->AddMeasurement((tbb::tick_count::now()-__TIME__9148_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9148_2=funky::Profile::RegisterCustom("9148","main","GPURun");
                tbb::tick_count __TIME__9148_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_19 = main_19.getOptimalWgSize(4096);
                int id = main_19.timedRun(__wgmain_19, 4096);
                device.finish();double runtime = main_19.getRunTime(id);
                
                __profile__9148_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9148_3=funky::Profile::RegisterCustom("9148","main","copyBack");
                tbb::tick_count __TIME__9148_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(4096,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,64);
                
                
                __profile__9148_3->AddMeasurement((tbb::tick_count::now()-__TIME__9148_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(4096,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                static funky::ProfileEntry* __profile__9277_0=funky::Profile::RegisterCustom("9277","main","Compile");
                tbb::tick_count __TIME__9277_0=tbb::tick_count::now();
                ocl_kernel main_20(&device,"tmp/main_20.cl", "-cl-opt-disable");
                
                
                __profile__9277_0->AddMeasurement((tbb::tick_count::now()-__TIME__9277_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9277_1=funky::Profile::RegisterCustom("9277","main","CopyTo");
                tbb::tick_count __TIME__9277_1=tbb::tick_count::now();
                int __new_array_0_dim_0=64;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[8192];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*8192,CL_MEM_WRITE_ONLY);
                
                
                main_20.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9277_1->AddMeasurement((tbb::tick_count::now()-__TIME__9277_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9277_2=funky::Profile::RegisterCustom("9277","main","GPURun");
                tbb::tick_count __TIME__9277_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_20 = main_20.getOptimalWgSize(8192);
                int id = main_20.timedRun(__wgmain_20, 8192);
                device.finish();double runtime = main_20.getRunTime(id);
                
                __profile__9277_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9277_3=funky::Profile::RegisterCustom("9277","main","copyBack");
                tbb::tick_count __TIME__9277_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(8192,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,64,128);
                
                
                __profile__9277_3->AddMeasurement((tbb::tick_count::now()-__TIME__9277_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(8192,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_64_128_float_two_d_64_128___float_two_d_64_128__(matrix_64_128);
    
    
    funky::LinearArray< float >::Version* matrix_128_128 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9412_0=funky::Profile::RegisterCustom("9412","main","Compile");
                tbb::tick_count __TIME__9412_0=tbb::tick_count::now();
                ocl_kernel main_21(&device,"tmp/main_21.cl", "-cl-opt-disable");
                
                
                __profile__9412_0->AddMeasurement((tbb::tick_count::now()-__TIME__9412_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9412_1=funky::Profile::RegisterCustom("9412","main","CopyTo");
                tbb::tick_count __TIME__9412_1=tbb::tick_count::now();
                int __new_array_0_dim_0=128;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[16384];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*16384,CL_MEM_WRITE_ONLY);
                
                
                main_21.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9412_1->AddMeasurement((tbb::tick_count::now()-__TIME__9412_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9412_2=funky::Profile::RegisterCustom("9412","main","GPURun");
                tbb::tick_count __TIME__9412_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_21 = main_21.getOptimalWgSize(16384);
                int id = main_21.timedRun(__wgmain_21, 16384);
                device.finish();double runtime = main_21.getRunTime(id);
                
                __profile__9412_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9412_3=funky::Profile::RegisterCustom("9412","main","copyBack");
                tbb::tick_count __TIME__9412_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(16384,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,128,128);
                
                
                __profile__9412_3->AddMeasurement((tbb::tick_count::now()-__TIME__9412_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(16384,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                static funky::ProfileEntry* __profile__9548_0=funky::Profile::RegisterCustom("9548","main","Compile");
                tbb::tick_count __TIME__9548_0=tbb::tick_count::now();
                ocl_kernel main_22(&device,"tmp/main_22.cl", "-cl-opt-disable");
                
                
                __profile__9548_0->AddMeasurement((tbb::tick_count::now()-__TIME__9548_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9548_1=funky::Profile::RegisterCustom("9548","main","CopyTo");
                tbb::tick_count __TIME__9548_1=tbb::tick_count::now();
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=128;
                float *__return_val_0=new float[32768];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*32768,CL_MEM_WRITE_ONLY);
                
                
                main_22.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9548_1->AddMeasurement((tbb::tick_count::now()-__TIME__9548_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9548_2=funky::Profile::RegisterCustom("9548","main","GPURun");
                tbb::tick_count __TIME__9548_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_22 = main_22.getOptimalWgSize(32768);
                int id = main_22.timedRun(__wgmain_22, 32768);
                device.finish();double runtime = main_22.getRunTime(id);
                
                __profile__9548_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9548_3=funky::Profile::RegisterCustom("9548","main","copyBack");
                tbb::tick_count __TIME__9548_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(32768,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,128);
                
                
                __profile__9548_3->AddMeasurement((tbb::tick_count::now()-__TIME__9548_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(32768,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_256_128_float_two_d_256_128___float_two_d_256_128__(matrix_256_128);
    
    
    funky::LinearArray< float >::Version* matrix_256_256 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9684_0=funky::Profile::RegisterCustom("9684","main","Compile");
                tbb::tick_count __TIME__9684_0=tbb::tick_count::now();
                ocl_kernel main_23(&device,"tmp/main_23.cl", "-cl-opt-disable");
                
                
                __profile__9684_0->AddMeasurement((tbb::tick_count::now()-__TIME__9684_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9684_1=funky::Profile::RegisterCustom("9684","main","CopyTo");
                tbb::tick_count __TIME__9684_1=tbb::tick_count::now();
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=256;
                float *__return_val_0=new float[65536];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*65536,CL_MEM_WRITE_ONLY);
                
                
                main_23.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9684_1->AddMeasurement((tbb::tick_count::now()-__TIME__9684_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9684_2=funky::Profile::RegisterCustom("9684","main","GPURun");
                tbb::tick_count __TIME__9684_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_23 = main_23.getOptimalWgSize(65536);
                int id = main_23.timedRun(__wgmain_23, 65536);
                device.finish();double runtime = main_23.getRunTime(id);
                
                __profile__9684_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9684_3=funky::Profile::RegisterCustom("9684","main","copyBack");
                tbb::tick_count __TIME__9684_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(65536,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,256);
                
                
                __profile__9684_3->AddMeasurement((tbb::tick_count::now()-__TIME__9684_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(65536,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                static funky::ProfileEntry* __profile__9820_0=funky::Profile::RegisterCustom("9820","main","Compile");
                tbb::tick_count __TIME__9820_0=tbb::tick_count::now();
                ocl_kernel main_24(&device,"tmp/main_24.cl", "-cl-opt-disable");
                
                
                __profile__9820_0->AddMeasurement((tbb::tick_count::now()-__TIME__9820_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9820_1=funky::Profile::RegisterCustom("9820","main","CopyTo");
                tbb::tick_count __TIME__9820_1=tbb::tick_count::now();
                int __new_array_0_dim_0=256;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[131072];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*131072,CL_MEM_WRITE_ONLY);
                
                
                main_24.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9820_1->AddMeasurement((tbb::tick_count::now()-__TIME__9820_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9820_2=funky::Profile::RegisterCustom("9820","main","GPURun");
                tbb::tick_count __TIME__9820_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_24 = main_24.getOptimalWgSize(131072);
                int id = main_24.timedRun(__wgmain_24, 131072);
                device.finish();double runtime = main_24.getRunTime(id);
                
                __profile__9820_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9820_3=funky::Profile::RegisterCustom("9820","main","copyBack");
                tbb::tick_count __TIME__9820_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(131072,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,256,512);
                
                
                __profile__9820_3->AddMeasurement((tbb::tick_count::now()-__TIME__9820_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(131072,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_256_512_float_two_d_256_512___float_two_d_256_512__(matrix_256_512);
    
    
    funky::LinearArray< float >::Version* matrix_512_512 = [&]() ->funky::LinearArray< float >::Version* {
        if (1) {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;
                static funky::ProfileEntry* __profile__9957_0=funky::Profile::RegisterCustom("9957","main","Compile");
                tbb::tick_count __TIME__9957_0=tbb::tick_count::now();
                ocl_kernel main_25(&device,"tmp/main_25.cl", "-cl-opt-disable");
                
                
                __profile__9957_0->AddMeasurement((tbb::tick_count::now()-__TIME__9957_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9957_1=funky::Profile::RegisterCustom("9957","main","CopyTo");
                tbb::tick_count __TIME__9957_1=tbb::tick_count::now();
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=512;
                float *__return_val_0=new float[262144];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*262144,CL_MEM_WRITE_ONLY);
                
                
                main_25.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__9957_1->AddMeasurement((tbb::tick_count::now()-__TIME__9957_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__9957_2=funky::Profile::RegisterCustom("9957","main","GPURun");
                tbb::tick_count __TIME__9957_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_25 = main_25.getOptimalWgSize(262144);
                int id = main_25.timedRun(__wgmain_25, 262144);
                device.finish();double runtime = main_25.getRunTime(id);
                
                __profile__9957_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__9957_3=funky::Profile::RegisterCustom("9957","main","copyBack");
                tbb::tick_count __TIME__9957_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(262144,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,512);
                
                
                __profile__9957_3->AddMeasurement((tbb::tick_count::now()-__TIME__9957_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(262144,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                static funky::ProfileEntry* __profile__10096_0=funky::Profile::RegisterCustom("10096","main","Compile");
                tbb::tick_count __TIME__10096_0=tbb::tick_count::now();
                ocl_kernel main_26(&device,"tmp/main_26.cl", "-cl-opt-disable");
                
                
                __profile__10096_0->AddMeasurement((tbb::tick_count::now()-__TIME__10096_0).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10096_1=funky::Profile::RegisterCustom("10096","main","CopyTo");
                tbb::tick_count __TIME__10096_1=tbb::tick_count::now();
                int __new_array_0_dim_0=512;
                int __new_array_0_dim_1=1024;
                float *__return_val_0=new float[524288];
                ocl_mem __return_val_GPU_0=device.malloc(sizeof(float)*524288,CL_MEM_WRITE_ONLY);
                
                
                main_26.setArgs(&__new_array_0_dim_0,&__new_array_0_dim_1,__return_val_GPU_0.mem());
                
                __profile__10096_1->AddMeasurement((tbb::tick_count::now()-__TIME__10096_1).seconds()*1e6);
                
                static funky::ProfileEntry* __profile__10096_2=funky::Profile::RegisterCustom("10096","main","GPURun");
                tbb::tick_count __TIME__10096_2=tbb::tick_count::now();
                
                //Execute the Kernel!
                size_t __wgmain_26 = main_26.getOptimalWgSize(524288);
                int id = main_26.timedRun(__wgmain_26, 524288);
                device.finish();double runtime = main_26.getRunTime(id);
                
                __profile__10096_2->AddMeasurement(runtime*1e3);
                
                
                static funky::ProfileEntry* __profile__10096_3=funky::Profile::RegisterCustom("10096","main","copyBack");
                tbb::tick_count __TIME__10096_3=tbb::tick_count::now();
                
                __return_val_GPU_0.copyTo(__return_val_0);
                funky::LinearArray<float> *__return_LINARR0=new funky::LinearArray<float>(524288,__return_val_0);
                funky::LinearArray< float >::Version* __return_0=new funky::LinearArray< float >::Version(__return_LINARR0,2,512,1024);
                
                
                __profile__10096_3->AddMeasurement((tbb::tick_count::now()-__TIME__10096_3).seconds()*1e6);
                
                
                return __return_0;
            }) ();
        } else {
            return
            
            ([&]()->funky::LinearArray< float >::Version*
            {
                funky::LinearArray< float >::Version*__VAL_TMP0=
                new funky::LinearArray< float >::Version(new funky::LinearArray< float >(524288,true));
                funky::LinearArray< float >::Version*__EXP_TMP0=__VAL_TMP0;                
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
                
                return __EXP_TMP0;
            }) ();
        }
    }();
    
    transpose_512_1024_float_two_d_512_1024___float_two_d_512_1024__(matrix_512_1024);
    

    
    return 0;
}

void sighandler(int sig){funky::Debug::Abort();exit(-1);}

CONTEXT(int,StartContext,{int argc; char** argv;},{},{});

GLOBAL_TASK(RUN, StartContext){context()->SetReturn(cur::main_int_String_one_d_01___int(context()->params.argc,(new funky::LinearArray<char*>::Version(new funky::LinearArray<char*>(context()->params.argc)))->map([&](funky::uint32 i){return context()->params.argv[i];},0,context()->params.argc)));}END_GLOBAL_TASK()

GLOBAL_TASK(START, StartContext){set_ref_count(2);spawn_and_wait_for_all(*new( allocate_child()) RUN(context()));}END_GLOBAL_TASK()

GLOBAL_TASK(WORKER, StartContext){getWorkerThreadID();usleep(100000);}END_GLOBAL_TASK()

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
    device = ocl::displayDevices();
    
    
    double tsum=0.f;
    double tsq=0.f;
    tbb::tick_count t0;
    for(funky::uint32 i=0;i<1;i++){
    printf("<<PROFILING RUN %d of %d>>\n",i+1,1);
    t0 = tbb::tick_count::now();
    funky::TaskRoot<>::GetRoot() = new(tbb::task::allocate_root()) START(&sp);
    printf("Root(0x%016p)\n",funky::TaskRoot<>::GetRoot());
    tbb::task::spawn_root_and_wait(*funky::TaskRoot<>::GetRoot());
    double val=(tbb::tick_count::now() - t0).seconds();tsum+=val;tsq+=val*val;
    }
    double d=tsum/1;
    printf("\nretval: %d\ntime: %f pm %f [s] %f pm %f [clocks]\n",sp.GetReturn(), d,sqrt(1.0/(1-1)*((double)tsq-tsum*(tsum/(double)1)))/sqrt(1),d*79999.99797903001,sqrt(1.0/(1-1)*((double)tsq-tsum*(tsum/(double)1.f)))*79999.99797903001/sqrt(1.f));
    funky::Profile::DumpCustomProfileStats(1,1.0);funky::Debug::Exit();int retval = sp.GetReturn();
    fprintf(stdout,"Start GC Stats\n");fflush(stdout);
    GC_gcollect();
    GC_gcollect();
    GC_dump();
    return retval;
    
}

