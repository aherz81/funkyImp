//
//  MemoryAccessPatternKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.06.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MemoryAccessPatternKernelProvider.h"
//
//  MemoryAccessKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 15.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MemoryAccessPatternKernelProvider.h"
#include <memory>
#include <sstream>
#include <random>

#include "Utilities.h"

using namespace std;


//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!

static const string kernelBegin = R"LIM(
__kernel void memory_access(global float *input, global int *pattern, global float *output)
{
    int work_item = get_global_id(0);
    output[work_item] = input[pattern[work_item]];
})LIM";

const std::vector<AccessPatternType> MemoryAccessPatternKernelProvider::memoryAccessType = {
    AccessPatternTypeConstant,
    AccessPatternTypeContinuous,
    AccessPatternTypeIntervals,
    AccessPatternTypeColumns,
    AccessPatternTypeRandom
};

MemoryAccessPatternKernelProvider::MemoryAccessPatternKernelProvider(AccessPatternType accessType, std::string opName) :
KernelProvider("", ""),
accessType(accessType) {}

int MemoryAccessPatternKernelProvider::getNumberOfOperations() {
    return 1;
}

std::string MemoryAccessPatternKernelProvider::getKernelCodeBegin() {
    return kernelBegin;
}

std::string MemoryAccessPatternKernelProvider::getKernelCodeEnd() {
    return "";
}

cl_float MemoryAccessPatternKernelProvider::runKernel(ocl_device *device, int memorySize) {
    ocl_kernel kernel(device, getKernelString(1));
    // Create host variables
    const unique_ptr<cl_float[]> input(new cl_float[memorySize]);
    const unique_ptr<cl_float[]> output(new cl_float[memorySize]);
    
    // Setup the values of input
    for(int i=0;i<memorySize; i++) {
        input[i] = i;
    }
    
    // Setup the pattern
    const unique_ptr<cl_int[]> pattern = this->initializePattern(memorySize);
    
    // Allocate memory on the device
    ocl_mem cl_input = device->malloc(memorySize * sizeof(cl_float),CL_MEM_READ_WRITE);
    ocl_mem cl_pattern = device->malloc(memorySize * sizeof(cl_int), CL_MEM_READ_ONLY);
    ocl_mem cl_output = device->malloc(memorySize * sizeof(cl_float),CL_MEM_WRITE_ONLY);
    
    // Copy host variables a and b to the device variables cl_a
    cl_input.copyFrom(input.get(), 0, memorySize);
    cl_pattern.copyFrom(pattern.get(), 0, memorySize);
    
    
    // Set the arguments required for the kernel
    kernel.setArgs(cl_input.mem(), cl_pattern.mem(), cl_output.mem());
    
    // Execute using N-sized work-groups with a total of N work-items
    int i = kernel.timedRun(getWorkgroupSize(device, memorySize), memorySize);
    
    // Wait until the kernel is done executing
    device->finish();
    cl_float runtime = kernel.getRunTime(i);
    
    // Copy device variable cl_c to host variable c
    cl_output.copyTo(output.get(), 0, memorySize);
    return runtime;
}

std::unique_ptr<cl_int[]> MemoryAccessPatternKernelProvider::initializePattern(int memorySize) {
#ifdef WIN32 //Workaround. As of Visual Studio 2013, Microsoft does not support all of the C++11 standard, not std::bind in particular. This provides an alternate implementation.
	auto now = std::chrono::system_clock().now().time_since_epoch();
	auto seed = std::chrono::duration<double>(now).count();
	srand((unsigned int) seed);
	auto generate = [memorySize]() -> int {
		return rand() % (memorySize -1);
	};
#else // WIN32 (not)
	std::default_random_engine engine;
	std::uniform_int_distribution<int> distribution(0, memorySize - 1);
	auto generate = std::bind(distribution, engine);
#endif // WIN32

    std::unique_ptr<cl_int[]> res(new cl_int[memorySize]);
    if (this->accessType == AccessPatternTypeConstant) {
        int constant = generate();
        for (int i = 0; i < memorySize; i++) {
            res[i] = constant;
        }
    } else if (this->accessType == AccessPatternTypeContinuous) {
        for (int i = 0; i < memorySize; i++) {
            res[i] = i;
        }
    } else if (this->accessType == AccessPatternTypeIntervals) {
        for (int i = 0; i < memorySize; i++) {
            res[i] = i % 8192;
        }
    } else if (this->accessType == AccessPatternTypeColumns) {
        for (int i = 0; i < memorySize; i++) {
            res[i] = i / 8192;
        }
    } else if (this->accessType == AccessPatternTypeRandom) {
        for (int i = 0; i < memorySize; i++) {
            res[i] = generate();
        }
    } else {
        for (int i = 0; i < memorySize; i++) {
            res[i] = 0;
        }
    }
    return move(res);
}

