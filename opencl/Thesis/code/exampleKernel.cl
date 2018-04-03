//FUNCTION HEADER DECLS
int  __s_int_g_int(int  __x);

//FUNCTION BODY DECLS
int  __s_int_g_int(int  __x)
{
    return 2*(__x);
}


//KERNEL CODE
__kernel void f_0(__global int *__ma_0, int __ma0_dim_0, 
	int __ma0_dim_1, int __ma0_dim_2, 
	__global int *__return_0)
{
    size_t __CUR_POS_0=get_global_id(0);
    int __a_0=(__CUR_POS_0/65536)%256;
    int __b_0=(__CUR_POS_0/256)%256;
    int __c_0=(__CUR_POS_0/1)%256;
    __return_0[__CUR_POS_0]=((__ma_0)[(__b_0) + (__a_0) 
    	* 256L + (__c_0) * 65536L]) 
    	+ (__s_int_g_int(__a_0));
}