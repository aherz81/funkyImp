//
//  MultipleMemoryAccessesKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.04.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MultipleMemoryAccessesKernelProvider.h"

#include <memory>
#include <sstream>

using namespace std;


static const size_t benchmarkMemSize = 1024*1024;

//RAW STRING
static const string kernelString = R"LIM(
__kernel void wg_kernel(global float *memory) {
    int work_item = get_global_id(0);
    memory[work_item] = 42.f)LIM";
//END

    
//RAW STRING
static const string kernelEnd = R"LIM(;
})LIM";
//END
    

MultipleMemoryAccessesKernelProvider::MultipleMemoryAccessesKernelProvider() :
KernelProvider("", "Number of Memory Accesses") {};

string MultipleMemoryAccessesKernelProvider::getKernelCodeBegin() {
    return kernelString;
}

string MultipleMemoryAccessesKernelProvider::getKernelCodeEnd() {
    return kernelEnd;
}

float MultipleMemoryAccessesKernelProvider::runKernel(ocl_device *device, int numberOfAccesses) {
    ocl_kernel kernel(device, getKernelString(numberOfAccesses));
    
    const int actualMemorySize = getMemorySize(numberOfAccesses);
    
    unique_ptr<float[]> memory(new float[actualMemorySize]);
    for (int i = 0; i < actualMemorySize; i++) {
        memory[i] = i * 42.f;
    }
    
    ocl_mem deviceMemory = device->malloc(actualMemorySize*sizeof(float));
    deviceMemory.copyFrom(memory.get());
    kernel.setArgs(deviceMemory.mem());
    int wgSize = getWorkgroupSize(device, benchmarkMemSize);
    // Execute using N-sized work-groups with a total of N work-items
    int i = kernel.timedRun(wgSize, benchmarkMemSize);
    
    // Wait until the kernel is done executing
    device->finish();
    float runtime = kernel.getRunTime(i);
    return runtime;
}

int MultipleMemoryAccessesKernelProvider::getMemorySize(int numberOfAccesses) {
    return benchmarkMemSize + numberOfAccesses;
}

std::string MultipleMemoryAccessesKernelProvider::getKernelString(int operations) {
    stringstream buf;
    buf << this->getKernelCodeBegin();
    for (int i = 0; i < operations; i++) {
        buf << "+ memory[work_item + " << i << "] ";
    }
    buf << this->getKernelCodeEnd();
    return buf.str();
}