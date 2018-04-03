#define CODE_BASE int x = get_global_id(0); \
                  output[x] = input[x];
#define CODE_ADD  int x = get_global_id(0); \
                  output[x] = input[x]+f;
#define CODE_SUB  int x = get_global_id(0); \
                  output[x] = input[x]-f;
#define CODE_MUL  int x = get_global_id(0); \
                  output[x] = input[x]*f;
#define CODE_DIV  int x = get_global_id(0); \
                  output[x] = input[x]/f;

__kernel void basic_op(global float *input, float f, 
                            global float *output)
{
	CODE_<BENCH>;
}
