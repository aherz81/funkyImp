//
//  EmptyKernelBaseCostProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 21.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "EmptyKernelBaseCostProvider.h"
#include <memory>

using namespace std;

//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!
static const std::string kernelCode = R"LIM(
__kernel void transpose(global float *memory)
{
    get_global_id(0);
})LIM";
//END OF STRINGS!


EmptyKernelBaseCostProvider::EmptyKernelBaseCostProvider() :
KernelProvider("", "EMPTY_KERNEL") {
}


string EmptyKernelBaseCostProvider::getKernelCodeBegin() {
    return kernelCode;
}

string EmptyKernelBaseCostProvider::getKernelCodeEnd() {
    return "";
}

float EmptyKernelBaseCostProvider::runKernel(ocl_device *device, int memorySize) {
    ocl_kernel kernel(device, getKernelString(1));
    
    std::unique_ptr<float[]> hostMemory(new float[memorySize]);
    for (int i = 0; i < memorySize; i++) {
        hostMemory[i] = i;
    }
    
    ocl_mem deviceMemory = device->malloc(memorySize*sizeof(float));
    deviceMemory.copyFrom(hostMemory.get());
    kernel.setArgs(deviceMemory.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int wgSize = getWorkgroupSize(device, memorySize);
    int i = kernel.timedRun(wgSize, memorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    float runtime = kernel.getRunTime(i);
    return runtime;
}
