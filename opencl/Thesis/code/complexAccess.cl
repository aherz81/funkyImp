__kernel void memory_access(global float *input, 
            float f, global float *output)
{
    int work_item = get_global_id(0);
	unsigned int x = (work_item>>11) & 2047;
	unsigned int y = work_item & 2047;
    output[work_item] = input[((x + (415397 + x))
         * (785 * ((x + 705) 
         * (y * ((x + 95) * x)))))]
}