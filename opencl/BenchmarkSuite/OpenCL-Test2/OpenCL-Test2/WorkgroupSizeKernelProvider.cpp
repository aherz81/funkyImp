//
//  WorkgroupSizeKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 26.03.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "WorkgroupSizeKernelProvider.h"

#include <memory>

using namespace std;


static const size_t benchmarkMemSize = 1024*1024;

//RAW STRING!
static const string kernelString = R"LIM(
__kernel void wg_kernel(global float *memory) {
    int work_item = get_global_id(0);
    memory[work_item] = memory[work_item] / 42.f;
})LIM";
//END RAW STRING!


WorkgroupSizeKernelProvider::WorkgroupSizeKernelProvider() :
KernelProvider("", "Workgroup Sizes") {};

string WorkgroupSizeKernelProvider::getKernelCodeBegin() {
    return kernelString;
}

string WorkgroupSizeKernelProvider::getKernelCodeEnd() {
    return "";
}

float WorkgroupSizeKernelProvider::runKernel(ocl_device *device, int wgSize) {
    ocl_kernel kernel(device, getKernelString(1));
    
    const int actualMemorySize = getMemorySize(wgSize);
    
    unique_ptr<float[]> memory(new float[actualMemorySize]);
    for (int i = 0; i < actualMemorySize; i++) {
        memory[i] = i * 42.f;
    }
    
    ocl_mem deviceMemory = device->malloc(actualMemorySize*sizeof(float));
    deviceMemory.copyFrom(memory.get());
    kernel.setArgs(deviceMemory.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int i = kernel.timedRun(wgSize, actualMemorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    float runtime = kernel.getRunTime(i);
    return runtime;
}

//The memory size needs to be evenly divisible by the number of work items in a work-group. This method tries to get the closest fit.
int WorkgroupSizeKernelProvider::getMemorySize(int wgSize) {
    const int remainder = benchmarkMemSize % wgSize;
    if (remainder == 0) {
        return benchmarkMemSize;
    } else if (remainder > wgSize / 2) {
        return benchmarkMemSize + (wgSize-remainder);
    } else {
        return benchmarkMemSize-remainder;
    }
}