//
//  MultipleBasicOperationsKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.04.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MultipleBasicOperationsKernelProvider.h"

#include <memory>
#include <sstream>

#include "Utilities.h"

using namespace std;


static const size_t benchmarkMemSize = 256*1024;

//RAW STRING
static const string kernelString = R"LIM(
__kernel void wg_kernel(global float *memory) {
    int work_item = get_global_id(0);
    float i = 2147483648 + memory[work_item];
    )LIM";
//END
    
    
//RAW STRING
static const string kernelEnd = R"LIM(
    memory[work_item] = i;
})LIM";
//END


MultipleBasicOperationsKernelProvider::MultipleBasicOperationsKernelProvider(OperationType operationType, string op) :
KernelProvider("", "Number of Basic Operations"),
operationType(operationType),
op(op){};

string MultipleBasicOperationsKernelProvider::getKernelCodeBegin() {
    return kernelString;
}

string MultipleBasicOperationsKernelProvider::getKernelCodeEnd() {
    return kernelEnd;
}

template <typename T>
float runKernelInternal(ocl_device *device, ocl_kernel &kernel, int wgSize, int numberOfAccesses) {
    unique_ptr<T[]> memory(new T[benchmarkMemSize]);
    for (int i = 0; i < benchmarkMemSize; i++) {
        memory[i] = i * 42;
    }
    
    ocl_mem deviceMemory = device->malloc(benchmarkMemSize*sizeof(T));
    deviceMemory.copyFrom(memory.get());
    kernel.setArgs(deviceMemory.mem());
    // Execute using N-sized work-groups with a total of N work-items
    int i = kernel.timedRun(wgSize, benchmarkMemSize);
    
    // Wait until the kernel is done executing
    device->finish();
    float runtime = kernel.getRunTime(i);
    return runtime;
}


float MultipleBasicOperationsKernelProvider::runKernel(ocl_device *device, int numberOfAccesses) {
    ocl_kernel kernel(device, getKernelString(numberOfAccesses));
    int wgSize = getWorkgroupSize(device, benchmarkMemSize);
    return runKernelInternal<cl_int>(device, kernel, wgSize, numberOfAccesses);
}

std::string MultipleBasicOperationsKernelProvider::getKernelString(int operations) {
    stringstream buf;
    buf << this->getKernelCodeBegin();
    for (int i = 0; i < operations; i++) {
        buf << "i = i" << this->op << "7" << ((operationType == IntegerType) ? "" : ".f") << ";\n\t";
    }
    buf << this->getKernelCodeEnd();
    if (operationType == FloatType) {
        return buf.str();
    } else {
        return util::replaceString(buf.str(), "float", "int");
    }
}