//
//  AlternateMeasureKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 12.02.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__AlternateMeasureKernelProvider__
#define __OpenCL_Test2__AlternateMeasureKernelProvider__

#include <iostream>

#include "KernelProvider.h"



//This benchmark is used to determine if the times gathered with the profiling methods integrated in OpenCL are accurate.
//As it turns out, they are. The benchmark remains in place, but is not executed any more.
class AlternateMeasureKernelProvider : public KernelProvider {

    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
public:
    AlternateMeasureKernelProvider();
    float runKernel(ocl_device *device, int memorySize);
};

#endif /* defined(__OpenCL_Test2__AlternateMeasureKernelProvider__) */
