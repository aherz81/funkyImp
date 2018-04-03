//
//  MultipleBasicOperationsKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.04.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MultipleBasicOperationsKernelProvider__
#define __OpenCL_Test2__MultipleBasicOperationsKernelProvider__

#include <iostream>
#include "KernelProvider.h"

//This benchmark measures the impact of having multiple basic operations in a single kernel. Again, it is possible to use it for ints and floats.
class MultipleBasicOperationsKernelProvider : public KernelProvider {
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
    const OperationType operationType;
    const std::string op;
public:
    MultipleBasicOperationsKernelProvider(OperationType operationType, std::string op);
    float runKernel(ocl_device *device, int memorySize);
    std::string getKernelString(int operations);
};

#endif /* defined(__OpenCL_Test2__MultipleBasicOperationsKernelProvider__) */
