//
//  MemoryAccessComplexityKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 06.05.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MemoryAccessComplexityKernelProvider__
#define __OpenCL_Test2__MemoryAccessComplexityKernelProvider__

#include <iostream>
#include "KernelProvider.h"

//This benchmark was implemented to gain data about complex memory accesses that produce lots of cache misses. One such complex access is
//compared to a normal continuous access. 
class MemoryAccessComplexityKernelProvider : public KernelProvider {
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
public:
    MemoryAccessComplexityKernelProvider();
    float runKernel(ocl_device *device, int memorySize);
    std::string getKernelString(int operations);
};

#endif /* defined(__OpenCL_Test2__MemoryAccessComplexityKernelProvider__) */
