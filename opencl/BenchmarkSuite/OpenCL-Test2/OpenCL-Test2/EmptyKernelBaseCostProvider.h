//
//  EmptyKernelBaseCostProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 21.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__EmptyKernelBaseCostProvider__
#define __OpenCL_Test2__EmptyKernelBaseCostProvider__

#include <iostream>
#include "KernelProvider.h"


//This benchmark is used to determine the base costs of Kernel execution. Basically, an empty kernel that only contains get_global_id(0) is
//executed.
class EmptyKernelBaseCostProvider : public KernelProvider {
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
public:
    EmptyKernelBaseCostProvider();
    float runKernel(ocl_device *device, int memorySize);
};

#endif /* defined(__OpenCL_Test2__EmptyKernelBaseCostProvider__) */
