//
//  KernelStringProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 15.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "KernelProvider.h"

//WHAT FOLLOWS ARE ACTUALLY RAW STRINGS! XCODE MESSES THIS UP!
std::string kernelCodeBegin = R"LIM(
__kernel void transpose(__global float *input_matrix, int memorySize, __global float *output_matrix)
{
    int current_item = get_global_id(0);
    float2 a = (float2) (23.4234e43, 234.2356e54);
    for (int i = 0; i < 1000; i++) {)LIM";
        
//END FIRST CONSTANT!
std::string kernel_code_add_operation = R"LIM(
        a.x = 42 + sqrt(a.x);)LIM";
        
//END SECOND CONSTANT!!
std::string kernelCodeEnd = R"LIM(
    }
    
    output_matrix[current_item] = a.x;
})LIM";
//END OF STRINGS!

KernelProvider::KernelProvider(std::string computeIntensiveOperation, std::string name) :
computeIntensiveOperation(computeIntensiveOperation),
operationName(name) {}


int KernelProvider::getNumberOfOperations() {
    return 1;
}

std::string KernelProvider::getKernelString(int operations) {
    std::string finalString = getKernelCodeBegin();
    for (int i = 0; i < operations; i++) {
        finalString += computeIntensiveOperation;
    }
    finalString += getKernelCodeEnd();
    return finalString;
    
}

int KernelProvider::getWorkgroupSize(ocl_device *device, int memorySize) {
    int maxSize = device->getGroupSize(0);
#ifdef __APPLE__
    if (device->getDeviceType() & CL_DEVICE_TYPE_CPU ) {
        maxSize = 1;
    }
#endif
    while (maxSize > 1 && memorySize % maxSize != 0) {
        maxSize--;
    }
    return maxSize;
}