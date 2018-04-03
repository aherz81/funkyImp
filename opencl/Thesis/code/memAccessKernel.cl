
#define BENCH_CST     output[x] = input[0x3ff];
#define BENCH_CNT     output[x] = input[x];
#define BENCH_IVL     output[x] = input[x & 0x3ff];
#define BENCH_IVLCST  output[x] = input[0x7ff] + input[x&0x3ff];
#define BENCH_CNTCST  output[x] = input[x] + input[0x3ff];
#define BENCH_CNTCNT  output[x] = input[x] + input[x];
#define BENCH_CNTCNT2 output[x] = input[x] + input[x>>1];

__kernel void memory_access(global float *input, float f, 
                            global float *output)
{
	int x = get_global_id(0);
	CODE_<BENCH>;
}
