//
//  MemoryAccessComplexityComparisonProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 07.05.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MemoryAccessComplexityComparisonProvider__
#define __OpenCL_Test2__MemoryAccessComplexityComparisonProvider__

#include <iostream>
#include "KernelProvider.h"

//This benchmark is done as an experiment to find out how to classify memory accesses. To do it, random index expressions of varying length
//are generated. Each node count has 20 examples.
class MemoryAccessComplexityComparisonProvider : public KernelProvider {
public:
    static const std::vector<std::string> memoryComplexityClasses;
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
protected:
    std::string accessType;
public:
    MemoryAccessComplexityComparisonProvider(std::string accessType, std::string operationName = "");
    float runKernel(ocl_device *device, int memorySize);
    int getNumberOfOperations();
    virtual std::string getKernelString(int memsize);
};

#endif /* defined(__OpenCL_Test2__MemoryAccessComplexityComparisonProvider__) */
