//FUNCTION HEADER DECLS

//FUNCTION BODY DECLS

//KERNEL CODE
__kernel void transpose_256_256_6(__global float *__matrix_0, int __matrix0_dim_0, int __matrix0_dim_1, int __new_array_0_dim_0, int __new_array_0_dim_1, __global float *__return_0)
{
    size_t __CUR_POS_0=get_global_id(0);
    unsigned int __x_0=(__CUR_POS_0/256)%256;
    unsigned int __y_0=(__CUR_POS_0)%256;
    __return_0[__CUR_POS_0]=(__matrix_0)[(__x_0) + (__y_0) * 256L];
}