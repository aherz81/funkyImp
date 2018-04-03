
__kernel void multiple_access(global float *memory) {
    int work_item = get_global_id(0);
    memory[work_item] = 42.f + memory[work_item + 0]
                        /* + memory[work_item + 1] + ... */;
}