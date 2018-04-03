//Kernel file

__kernel void transpose(__global float *input_matrix, int rows, int cols, __global float *output_matrix)
{
    int current_item = get_global_id(0);
    int current_row = current_item / cols;
    int current_col = current_item % cols;
    local int a[2];
    a[0] = 0;
    a[1] = 0;
    int variable = 3;
    for (int i = 0; i < 1000000; i++) {
        a[0] = a[1] + 5;
        a[1] = a[0] + 2;

        variable = variable + 5;
    }
    
    output_matrix[current_col * cols + current_row] = a[1] + variable;
}