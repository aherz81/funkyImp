/*__kernel void matrix_mult(__global float *matrix_A,
                          __global float *matrix_B,
                          __global float *return_matrix,
                          int matrix_A_width,
                          int matrix_B_width)
{
    int current_col = get_global_id(0);
    int current_row = get_global_id(1);
    
    float result = 0;
    for (int i = 0; i < matrix_A_width) {
        value += matrix_A[current_row * matrix_A_width + i] * matrix_B[i * matrix_B_width + current_col];
    }
    return_matrix[current_row * matrix_A_width + current_col] = result;
}*/