//FUNCTION HEADER DECLS

//FUNCTION BODY DECLS

//KERNEL CODE
__kernel void main_29(int __new_array_0_dim_0, int __new_array_0_dim_1, __global float *__return_0)
{
    size_t __CUR_POS_0=get_global_id(0);
    unsigned int __x_0=(__CUR_POS_0/2048)%2048;
    unsigned int __y_0=(__CUR_POS_0)%2048;
    __return_0[__CUR_POS_0]=((__x_0*(10)))+((9-(__y_0)));
}