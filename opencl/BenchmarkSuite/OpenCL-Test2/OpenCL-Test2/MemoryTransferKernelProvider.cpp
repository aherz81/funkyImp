//
//  File.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 20.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MemoryTransferKernelProvider.h"

#include <memory>
#include <chrono>

using namespace std;

//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!
static const std::string kernelCodeBegin = R"LIM(
__kernel void transpose(__global float *input_matrix, int memorySize, __global float *output_matrix)
{
    int current_item = get_global_id(0);
    )LIM";
    
    static const std::string kernelCodeContinue = R"LIM( float arr[2];
    arr[0] = 446e34f;
    arr[1] = 87e33f;
    for (int i = 0; i < 1000; i++) {
        )LIM";
        
        
        //END SECOND CONSTANT!!
        static const std::string kernelCodeEnd = R"LIM(
    }
    
    output_matrix[current_item] = arr[1];
})LIM";
//END OF STRINGS!

MemoryTransferKernelProvider::MemoryTransferKernelProvider(TransferType transferType) :
KernelProvider("", (transferType == TransferType::TO_DEVICE) ? "TO_DEVICE" : "FROM_DEVICE"),
transferType(transferType)
{

}

std::string MemoryTransferKernelProvider::getKernelCodeBegin() {
    return "";
}

std::string MemoryTransferKernelProvider::getKernelCodeEnd() {
    return kernelCodeBegin + kernelCodeContinue + kernelCodeEnd;
}

float MemoryTransferKernelProvider::runKernel(ocl_device *device, int memorySize) {
    ocl_kernel kernel(device, getKernelString(1));
    
    // Create host variables
    const unique_ptr<cl_float[]> a(new cl_float[memorySize]);
    const unique_ptr<cl_float[]> c(new cl_float[memorySize]);
    
    // Setup the values of a
    for(int i=0;i<memorySize; i++) {
        a[i] = i+1;
    }
    
    // Allocate memory on the device
    // Copy host variables a and b to the device variables cl_a
    ocl_mem cl_a = device->malloc(memorySize * sizeof(cl_float),CL_MEM_READ_ONLY);

    
    auto aCopyBase = std::chrono::high_resolution_clock::now();
    cl_a.copyFrom(a.get());
    ocl_mem cl_c = device->malloc(memorySize * sizeof(cl_float),CL_MEM_WRITE_ONLY);
    auto aCopyAfter = std::chrono::high_resolution_clock::now();
    // Set the arguments required for the kernel
    kernel.setArgs(cl_a.mem(), &memorySize, cl_c.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int wgSize = getWorkgroupSize(device, memorySize);
    kernel.timedRun(wgSize, memorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    
    // Copy device variable cl_c to host variable c
    auto cCopyBase = std::chrono::high_resolution_clock::now();
    cl_c.copyTo(c.get(), 0, memorySize);
    auto cCopyAfter = std::chrono::high_resolution_clock::now();

    auto copyFromDuration = chrono::duration_cast<chrono::duration<double>>(cCopyAfter-cCopyBase);
    auto copyToDuration = chrono::duration_cast<chrono::duration<double>>(aCopyAfter-aCopyBase);
    auto runtime = ((transferType == TransferType::TO_DEVICE) ? copyToDuration.count() : copyFromDuration.count()) * 1e6;
    return runtime;
}