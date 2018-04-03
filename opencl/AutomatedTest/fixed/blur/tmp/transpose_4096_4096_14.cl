//FUNCTION HEADER DECLS
float  __s_float_blur_4096_4096_float_p_int_int(global float *  __matrix, int  __x, int  __y);

//FUNCTION BODY DECLS
float  __s_float_blur_4096_4096_float_p_int_int(global float *  __matrix, int  __x, int  __y)
{
    if (__x<(1)||__y<(1)||__x==4096-(1)||__y==4096-(1))
        
        {
            return 0.0F;
        }
        
    else
        
        {
            return ((__matrix)[(__y-(1)) + (__x-(1)) * 4096L])+((__matrix)[(__y) + (__x-(1)) * 4096L])+((__matrix)[(__y+(1)) + (__x-(1)) * 4096L])+((__matrix)[(__y-(1)) + (__x) * 4096L])+((__matrix)[(__y) + (__x) * 4096L])+((__matrix)[(__y+(1)) + (__x) * 4096L])+((__matrix)[(__y-(1)) + (__x+(1)) * 4096L])+((__matrix)[(__y) + (__x+(1)) * 4096L])+((__matrix)[(__y-(1)) + (__x-(1)) * 4096L]);
        }
        
    
}


//KERNEL CODE
__kernel void transpose_4096_4096_14(__global float *__matrix_0, int __matrix0_dim_0, int __matrix0_dim_1, int __new_array_0_dim_0, int __new_array_0_dim_1, __global float *__return_0)
{
    size_t __CUR_POS_0=get_global_id(0);
    unsigned int __x_0=(__CUR_POS_0/4096)%4096;
    unsigned int __y_0=(__CUR_POS_0)%4096;
    __return_0[__CUR_POS_0]=__s_float_blur_4096_4096_float_p_int_int(__matrix_0, __x_0, __y_0);
}