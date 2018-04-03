//
//  BenchmarkRunner.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 14.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__BenchmarkRunner__
#define __OpenCL_Test2__BenchmarkRunner__

#include <iostream>
#include <memory>
#include <functional>

#include "config.h"
#include "ocl.h"
#include "ResultAnalyzer.h"
#include "TableGenerator.h"
#include "KernelProvider.h"

class BenchmarkRunner {

private:
    int currentRun;
    int memorySize;
    ocl_device *device;
    const std::unique_ptr<KernelProvider> kernelProvider;
public:
    const int maximumNumberOfOperations;
    const int samplesPerOperationLevel;
    const BenchmarkOutputVerbosityLevel verbosity;    
public:
    BenchmarkRunner(ocl_device *runDevice, int maxOps, std::unique_ptr<KernelProvider> kernelProvider, int samplesPerLevel, int memSize, BenchmarkOutputVerbosityLevel verbosity = MINIMAL);
    TableGenerator runBenchmark(std::function<int(int)> generator);
private:
    void runBenchmarkInternal(TableGenerator &tableGen, bool useMemSizeInResult = false);
    ResultAnalyzer runOperationLevel();
};



#endif /* defined(__OpenCL_Test2__BenchmarkRunner__) */
