//
//  AlternateMeasureKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 12.02.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "AlternateMeasureKernelProvider.h"

#include <chrono>
#include <memory>

using namespace std;

//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!
static const std::string kernelCode = R"LIM(
__kernel void transpose(__global float *input_matrix, int memorySize, __global float *output_matrix)
{
    
})LIM";
//END OF STRINGS!


AlternateMeasureKernelProvider::AlternateMeasureKernelProvider() :
KernelProvider("", "EMPTY_KERNEL") {
}


string AlternateMeasureKernelProvider::getKernelCodeBegin() {
    return kernelCode;
}

string AlternateMeasureKernelProvider::getKernelCodeEnd() {
    return "";
}

float AlternateMeasureKernelProvider::runKernel(ocl_device *device, int memorySize) {
    ocl_kernel runKernel(device, getKernelString(1));
    
    // Create host variables
    std::unique_ptr<cl_float[]> a(new cl_float[memorySize]);
    std::unique_ptr<cl_float[]> c(new cl_float[memorySize]);
    
    // Setup the values of a
    for(int i=0;i<memorySize; i++) {
        a[i] = i;
    }
    
    // Allocate memory on the device
    ocl_mem cl_a = device->malloc(memorySize * sizeof(cl_float),CL_MEM_READ_ONLY);
    ocl_mem cl_c = device->malloc(memorySize * sizeof(cl_float),CL_MEM_WRITE_ONLY);
    
    // Copy host variables a and b to the device variables cl_a
    cl_a.copyFrom(a.get(), 0 , memorySize);
    
    // Set the arguments required for the kernel
    runKernel.setArgs(cl_a.mem(), &memorySize, cl_c.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int wgSize = getWorkgroupSize(device, memorySize);
    
    auto runStartTime = std::chrono::high_resolution_clock::now();
    runKernel.run(wgSize, memorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    auto runEndTime = std::chrono::high_resolution_clock::now();
    
    // Copy device variable cl_c to host variable c
    cl_c.copyTo(c.get(), 0, memorySize);
    
    return std::chrono::duration_cast<std::chrono::duration<double>>(runEndTime - runStartTime).count() * 1e6;
}
