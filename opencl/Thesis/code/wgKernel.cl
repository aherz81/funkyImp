__kernel void wg_kernel(global float *memory) {
    int work_item = get_global_id(0);
    memory[work_item] = memory[work_item] / 42.f;
}