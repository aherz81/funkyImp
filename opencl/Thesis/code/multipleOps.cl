#define TYPE float
#define OP + 
#define OPERATION i = i OP 7

__kernel void multiple_ops(global TYPE *memory) {
    int work_item = get_global_id(0);
    TYPE i = 2147483648;
    OPERATION;
    ...
    OPERATION;
    memory[work_item] = i;
}