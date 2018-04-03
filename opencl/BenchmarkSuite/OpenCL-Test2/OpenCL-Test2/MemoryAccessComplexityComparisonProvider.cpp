//
//  MemoryAccessComplexityComparisonProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 07.05.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MemoryAccessComplexityComparisonProvider.h"

#include <memory>
#include <sstream>

using namespace std;


//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!

static const string kernelBegin = R"LIM(
__kernel void memory_access(global float *input, float f, global float *output)
{
    int work_item = get_global_id(0);
	)LIM";
    
    static const string kernelEnd =R"LIM(
})LIM";

static const std::string accessBegin = "output[work_item] = input[";
static const string xDef = "unsigned int x = (work_item>>11) & 2047;\n\t";
static const string yDef = "unsigned int y = work_item & 2047;\n\t";
static const std::string accessEnd = "];";

const std::vector<std::string> MemoryAccessComplexityComparisonProvider::memoryComplexityClasses = {
    "((x + (415397 + x)) * (785 * ((x + 705) * (y * ((x + 95) * x)))))",
    "(x + 944)",
    "work_item",
    "32" 
};


MemoryAccessComplexityComparisonProvider::MemoryAccessComplexityComparisonProvider(std::string accessType, std::string opName) :
KernelProvider("", accessType),
accessType(accessType) {}

int MemoryAccessComplexityComparisonProvider::getNumberOfOperations() {
    return 1;
}

std::string MemoryAccessComplexityComparisonProvider::getKernelCodeBegin() {
    return kernelBegin;
}

std::string MemoryAccessComplexityComparisonProvider::getKernelCodeEnd() {
    return this->accessType + kernelEnd;
}

cl_float MemoryAccessComplexityComparisonProvider::runKernel(ocl_device *device, int memorySize) {
    ocl_kernel runKernel(device, getKernelString(memorySize));
    
    // Create host variables
    const unique_ptr<cl_float[]> input(new cl_float[memorySize]);
    const unique_ptr<cl_float[]> output(new cl_float[memorySize]);
    float f = 3.f;
    
    // Setup the values of a
    for(int i=0;i<memorySize; i++) {
        input[i] = i;
    }
    
    
    // Allocate memory on the device
    ocl_mem cl_input = device->malloc(memorySize * sizeof(cl_float),CL_MEM_READ_WRITE);
    ocl_mem cl_output = device->malloc(memorySize * sizeof(cl_float),CL_MEM_WRITE_ONLY);
    
    // Copy host variables a and b to the device variables cl_a
    cl_input.copyFrom(input.get(), 0, memorySize);
    
    
    // Set the arguments required for the kernel
    runKernel.setArgs(cl_input.mem(), &f, cl_output.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int wgSize = getWorkgroupSize(device, memorySize);
    int i = runKernel.timedRun(wgSize, memorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    cl_float runtime = runKernel.getRunTime(i);
    
    // Copy device variable cl_c to host variable c
    //cl_output.copyTo(output.get(), 0, memorySize);
    
    return runtime;
}

string MemoryAccessComplexityComparisonProvider::getKernelString(int currentMemorySize) {
    stringstream buf;
    buf << getKernelCodeBegin();
    if (accessType.find('x') != string::npos) {
        buf << xDef;
    }
    if (accessType.find('y') != string::npos) {
        buf << yDef;
    }
    buf << accessBegin << accessType << "%" << currentMemorySize << accessEnd;
    buf << kernelEnd;
    return buf.str();
}