//FUNCTION HEADER DECLS
float  __s_float_blur_512_1024_float_p_int_int(global float *  __matrix, int  __x, int  __y);

//FUNCTION BODY DECLS
float  __s_float_blur_512_1024_float_p_int_int(global float *  __matrix, int  __x, int  __y)
{
    if (__x<(1)||__y<(1)||__x==512-(1)||__y==1024-(1))
        
        {
            return 0.0F;
        }
        
    else
        
        {
            return ((__matrix)[(__y-(1)) + (__x-(1)) * 1024L])+((__matrix)[(__y) + (__x-(1)) * 1024L])+((__matrix)[(__y+(1)) + (__x-(1)) * 1024L])+((__matrix)[(__y-(1)) + (__x) * 1024L])+((__matrix)[(__y) + (__x) * 1024L])+((__matrix)[(__y+(1)) + (__x) * 1024L])+((__matrix)[(__y-(1)) + (__x+(1)) * 1024L])+((__matrix)[(__y) + (__x+(1)) * 1024L])+((__matrix)[(__y-(1)) + (__x-(1)) * 1024L]);
        }
        
    
}


//KERNEL CODE
__kernel void transpose_512_1024_9(__global float *__matrix_0, int __matrix0_dim_0, int __matrix0_dim_1, int __new_array_0_dim_0, int __new_array_0_dim_1, __global float *__return_0)
{
    size_t __CUR_POS_0=get_global_id(0);
    unsigned int __x_0=(__CUR_POS_0/1024)%512;
    unsigned int __y_0=(__CUR_POS_0)%1024;
    __return_0[__CUR_POS_0]=__s_float_blur_512_1024_float_p_int_int(__matrix_0, __x_0, __y_0);
}