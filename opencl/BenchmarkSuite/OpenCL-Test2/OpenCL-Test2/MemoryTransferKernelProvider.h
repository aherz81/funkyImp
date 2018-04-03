//
//  File.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 20.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MemoryTransferKernelProvider__
#define __OpenCL_Test2__MemoryTransferKernelProvider__

#include <iostream>

#include "KernelProvider.h"

enum class TransferType {
    TO_DEVICE, FROM_DEVICE
};


//This benchmark measures the time spent on transferring memory to and from the GPU.
class MemoryTransferKernelProvider : public KernelProvider {
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
private:
    TransferType transferType;
public:
    MemoryTransferKernelProvider(TransferType transferType);
    float runKernel(ocl_device *device, int memorySize);

};

#endif /* defined(__OpenCL_Test2__MemoryTransferKernelProvider__) */
