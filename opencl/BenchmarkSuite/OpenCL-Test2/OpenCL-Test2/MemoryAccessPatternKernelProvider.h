//
//  MemoryAccessPatternKernelProvider.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.06.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__MemoryAccessPatternKernelProvider__
#define __OpenCL_Test2__MemoryAccessPatternKernelProvider__

#include <iostream>
#include <vector>
#include <memory>

#include "KernelProvider.h"

enum AccessPatternType {
    AccessPatternTypeRandom,
    AccessPatternTypeColumns,
    AccessPatternTypeIntervals,
    AccessPatternTypeContinuous,
    AccessPatternTypeConstant
};


//This is a benchmark that tries to unify the MemoryAccessBenchmark and the MemoryAccessComplexityComparisonBenchmark. It did not work out.
class MemoryAccessPatternKernelProvider : public KernelProvider {
public:
    static const std::vector<AccessPatternType> memoryAccessType;
protected:
    std::string getKernelCodeBegin();
    std::string getKernelCodeEnd();
protected:
    AccessPatternType accessType;
public:
    MemoryAccessPatternKernelProvider(AccessPatternType accessType, std::string operationName = "");
    float runKernel(ocl_device *device, int memorySize);
    int getNumberOfOperations();
private:
    std::unique_ptr<cl_int[]> initializePattern(int memSize);
};

#endif /* defined(__OpenCL_Test2__MemoryAccessPatternKernelProvider__) */
