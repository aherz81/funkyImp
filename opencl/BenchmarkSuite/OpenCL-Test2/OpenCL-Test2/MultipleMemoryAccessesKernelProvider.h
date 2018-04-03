//
//  MultipleMemoryAccessesKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.04.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MultipleMemoryAccessesKernelProvider__
#define __OpenCL_Test2__MultipleMemoryAccessesKernelProvider__

#include <iostream>
#include "KernelProvider.h"

//This benchmark measures the impact of having multiple memory accesses in a single kernel. Again, it is possible to use it for ints and floats.
class MultipleMemoryAccessesKernelProvider : public KernelProvider {
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
public:
    MultipleMemoryAccessesKernelProvider();
    float runKernel(ocl_device *device, int memorySize);
    std::string getKernelString(int operations);
private:
    int getMemorySize(int numberOfAccesses);
};

#endif /* defined(__OpenCL_Test2__MultipleMemoryAccessesKernelProvider__) */
