//
//  KernelStringProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 15.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__KernelStringProvider__
#define __OpenCL_Test2__KernelStringProvider__

#include <iostream>
#include <vector>
#include "ocl.h"

enum OperationType {
    IntegerType, FloatType
};


// Benchmark base class.
class KernelProvider {
protected:
    std::string computeIntensiveOperation;
public:
    const std::string operationName;
protected:
    virtual std::string getKernelCodeBegin() = 0;
    virtual std::string getKernelCodeEnd() = 0;
    int getWorkgroupSize(ocl_device *device, int memSize);
    
public:
    KernelProvider(std::string computeIntensiveOperation, std::string operationName = "");
    virtual std::string getKernelString(int operations);
    virtual int getNumberOfOperations();
    virtual float runKernel(ocl_device *device, int memorySize) = 0;
};

#endif /* defined(__OpenCL_Test2__KernelStringProvider__) */
