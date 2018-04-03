#ifndef __cur__INCLUDE_GUARD
#define __cur__INCLUDE_GUARD
#define OPENCLBACKEND
#include <Task.h>

ocl_device device;

struct cur : virtual boehmgc::gc
{
        
    
    
    static float blur_32_32_float_two_d_32_32___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_32_64_float_two_d_32_64___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_64_64_float_two_d_64_64___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_64_128_float_two_d_64_128___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_128_128_float_two_d_128_128___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_256_128_float_two_d_256_128___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_256_256_float_two_d_256_256___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_256_512_float_two_d_256_512___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_512_512_float_two_d_512_512___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_512_1024_float_two_d_512_1024___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_1024_1024_float_two_d_1024_1024___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_2048_1024_float_two_d_2048_1024___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_2048_2048_float_two_d_2048_2048___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_4096_2048_float_two_d_4096_2048___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_4096_4096_float_two_d_4096_4096___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_4096_8192_float_two_d_4096_8192___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static float blur_8192_8192_float_two_d_8192_8192___int_int_float(funky::LinearArray< float >::Version* matrix, int x, int y);
    
    
    static funky::LinearArray< float >::Version* transpose_32_32_float_two_d_32_32___float_two_d_32_32__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_32_64_float_two_d_32_64___float_two_d_32_64__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_64_64_float_two_d_64_64___float_two_d_64_64__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_64_128_float_two_d_64_128___float_two_d_64_128__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_128_128_float_two_d_128_128___float_two_d_128_128__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_256_128_float_two_d_256_128___float_two_d_256_128__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_256_256_float_two_d_256_256___float_two_d_256_256__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_256_512_float_two_d_256_512___float_two_d_256_512__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_512_512_float_two_d_512_512___float_two_d_512_512__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_512_1024_float_two_d_512_1024___float_two_d_512_1024__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_1024_1024_float_two_d_1024_1024___float_two_d_1024_1024__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_2048_1024_float_two_d_2048_1024___float_two_d_2048_1024__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_2048_2048_float_two_d_2048_2048___float_two_d_2048_2048__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_4096_2048_float_two_d_4096_2048___float_two_d_4096_2048__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_4096_4096_float_two_d_4096_4096___float_two_d_4096_4096__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_4096_8192_float_two_d_4096_8192___float_two_d_4096_8192__(funky::LinearArray< float >::Version* matrix);
    
    
    static funky::LinearArray< float >::Version* transpose_8192_8192_float_two_d_8192_8192___float_two_d_8192_8192__(funky::LinearArray< float >::Version* matrix);
    
    
    static int main_int_String_one_d_01___int(int argc, funky::LinearArray< char* >::Version* argv);
    

};
#endif //__cur__INCLUDE_GUARD


