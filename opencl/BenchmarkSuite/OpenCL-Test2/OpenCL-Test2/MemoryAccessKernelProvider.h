//
//  MemoryAccessKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 15.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MemoryAccessKernelProvider__
#define __OpenCL_Test2__MemoryAccessKernelProvider__

#include <iostream>
#include <vector>

#include "KernelProvider.h"


//This benchmarks is used to compare different kinds of memory accesses and also basic operations on floats and ints.
class MemoryAccessKernelProvider : public KernelProvider {
public:
    static const std::vector<std::string> memoryAccessType;
    static const std::vector<std::string> basicOperationType;
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
protected:
    std::string accessType;
    const OperationType operationType;
public:
    MemoryAccessKernelProvider(std::string accessType, std::string operationName = "", OperationType operationType = FloatType);
    float runKernel(ocl_device *device, int memorySize);
    int getNumberOfOperations();
};

#endif /* defined(__OpenCL_Test2__MemoryAccessKernelProvider__) */
