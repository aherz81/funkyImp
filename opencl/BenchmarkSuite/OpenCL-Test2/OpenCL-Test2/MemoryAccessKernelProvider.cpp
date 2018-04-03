//
//  MemoryAccessKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 15.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MemoryAccessKernelProvider.h"
#include <memory>
#include <sstream>

#include "Utilities.h"

using namespace std;


//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!

static const string kernelBegin = R"LIM(

__kernel void memory_access(global float *input, float f, global float *output)
{
    
	)LIM";

static const string kernelEnd =R"LIM(
})LIM";
    
const std::vector<std::string> MemoryAccessKernelProvider::memoryAccessType = {
    "get_global_id(0);",
    "int x = get_global_id(0);\n\tf = x;",
    "int x = get_global_id(0);\n\tf = input[x];",
    "int x = get_global_id(0);\n\toutput[x] = x;",
    "int x = get_global_id(0);\n\tlocal float mem[2];\n\toutput[x] = mem[(~x)&0x1];",
    "int x = get_global_id(0);\n\tlocal float mem[2];\n\tmem[x&0x1] = x;\n\toutput[x] = mem[(~x)&0x1];",
    "int x = get_global_id(0);\n\toutput[x] = input[0x1ff];",
    "int x = get_global_id(0);\n\toutput[x] = input[0x7ff] + input[x & 0x3ff];",
    "int x = get_global_id(0);\n\toutput[x] = input[x & 0x1ff];",
    "int x = get_global_id(0);\n\toutput[x] = input[x];",
    "int x = get_global_id(0);\n\toutput[x] = input[x] + input[0x3ff];",
    "int x = get_global_id(0);\n\toutput[x] = input[x] + input[x];",
    "int x = get_global_id(0);\n\toutput[x] = input[x] + input[x>>1];"
};
    
    
const std::vector<std::string> MemoryAccessKernelProvider::basicOperationType = {
    "int x = get_global_id(0);\n\toutput[x] = input[x];",
    "int x = get_global_id(0);\n\toutput[x] = input[x]+f;",
    "int x = get_global_id(0);\n\toutput[x] = input[x]-f;",
    "int x = get_global_id(0);\n\toutput[x] = input[x]*f;",
    "int x = get_global_id(0);\n\toutput[x] = input[x]/f;",
};

MemoryAccessKernelProvider::MemoryAccessKernelProvider(std::string accessType, std::string opName, OperationType operationsType) :
KernelProvider("", accessType),
operationType(operationsType),
accessType(accessType) {}

int MemoryAccessKernelProvider::getNumberOfOperations() {
    return 1;
}

std::string MemoryAccessKernelProvider::getKernelCodeBegin() {
    return (this->operationType == FloatType) ? kernelBegin : util::replaceString(kernelBegin, "float", "int");
}

std::string MemoryAccessKernelProvider::getKernelCodeEnd() {
    return this->accessType + kernelEnd;
}

template <typename T>
cl_float runKernelInt(ocl_device *device, ocl_kernel &kernel, int memorySize, int wgSize) {
    // Create host variables
    const unique_ptr<T[]> input(new T[memorySize]);
    const unique_ptr<T[]> output(new T[memorySize]);
    float f = 3.f;
    
    // Setup the values of a
    for(int i=0;i<memorySize; i++) {
        input[i] = i;
    }
    
    
    // Allocate memory on the device
    ocl_mem cl_input = device->malloc(memorySize * sizeof(T),CL_MEM_READ_WRITE);
    ocl_mem cl_output = device->malloc(memorySize * sizeof(T),CL_MEM_WRITE_ONLY);
    
    // Copy host variables a and b to the device variables cl_a
    cl_input.copyFrom(input.get(), 0, memorySize);
    
    
    // Set the arguments required for the kernel
    kernel.setArgs(cl_input.mem(), &f, cl_output.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int i = kernel.timedRun(wgSize, memorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    cl_float runtime = kernel.getRunTime(i);
    
    // Copy device variable cl_c to host variable c
    cl_output.copyTo(output.get(), 0, memorySize);
    return runtime;
}

cl_float MemoryAccessKernelProvider::runKernel(ocl_device *device, int memorySize) {
    ocl_kernel runKernel(device, getKernelString(1));
    if (this->operationType == IntegerType) {
        return runKernelInt<cl_int>(device, runKernel, memorySize, getWorkgroupSize(device, memorySize));
    } else {
        return runKernelInt<cl_float>(device, runKernel, memorySize, getWorkgroupSize(device, memorySize));
    }
}