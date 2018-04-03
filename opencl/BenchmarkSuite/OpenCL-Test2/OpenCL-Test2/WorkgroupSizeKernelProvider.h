//
//  WorkgroupSizeKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 26.03.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__WorkgroupSizeKernelProvider__
#define __OpenCL_Test2__WorkgroupSizeKernelProvider__

#include <iostream>

#include "KernelProvider.h"

//This benchmark measures the impact the size of the work-group has on the execution speed.
class WorkgroupSizeKernelProvider : public KernelProvider {
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
public:
    WorkgroupSizeKernelProvider();
    float runKernel(ocl_device *device, int memorySize);
private:
    int getMemorySize(int wgSize);
};

#endif /* defined(__OpenCL_Test2__WorkgroupSizeKernelProvider__) */
