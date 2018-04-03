//
//  BenchmarkRunner.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 14.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "BenchmarkRunner.h"

#include <memory>

#include "TableGenerator.h"

using namespace std;

BenchmarkRunner::BenchmarkRunner(ocl_device *runDevice, int maxOps, unique_ptr<KernelProvider> kernelProvider, int samplesPerOpLevel, int memSize, BenchmarkOutputVerbosityLevel verbosity) : 
maximumNumberOfOperations(maxOps),
kernelProvider(move(kernelProvider)),
samplesPerOperationLevel(samplesPerOpLevel),
currentRun(1),
device(runDevice),
memorySize(memSize),
verbosity(verbosity) {}

TableGenerator BenchmarkRunner::runBenchmark(function<int(int)> generator) {
    int maxMemSize = memorySize;
    TableGenerator tableGen(kernelProvider->getNumberOfOperations());
    for (int i = 0, currentMemorySize = generator(i); currentMemorySize > 0 && currentMemorySize <= maxMemSize; currentMemorySize = generator(++i)) {
        this->memorySize = currentMemorySize;
        ResultAnalyzer analyzer = runOperationLevel();
        tableGen.addResult(memorySize, analyzer);
    }
    return tableGen;
}

void BenchmarkRunner::runBenchmarkInternal(TableGenerator &tableGen, bool useMemSizeInResult) {
    for (; currentRun < maximumNumberOfOperations; currentRun++) {
        ResultAnalyzer analysis = runOperationLevel();
        tableGen.addResult(((useMemSizeInResult) ? memorySize : currentRun), analysis);
    }
}

ResultAnalyzer BenchmarkRunner::runOperationLevel() {
    ResultAnalyzer result((std::vector<double>()));
    int dotCount = 0;
    do {
        vector<double> runs;
        for (int sample_run = 0; sample_run < samplesPerOperationLevel; sample_run++) {
            string kernelCode = kernelProvider->getKernelString(currentRun);
            float runtime = kernelProvider->runKernel(device, memorySize);
            runs.push_back(runtime);
            if (verbosity >= HIGH) {
                cout << ".";
                dotCount++;
            }
        }
        ResultAnalyzer analyzer(move(runs));
        result += analyzer;
        if (verbosity == EXTRA_VERBOSE) {
            analyzer.printStatistics();
        }

    } while (result.getStandardErrorPercentage() > targetErrorLevel);
    if (verbosity >= EXTRA_VERBOSE) {
        result.dump();
    }
    if (verbosity >= HIGH) {
        cout << "\t" << memorySize << " (" << dotCount << " samples)" << endl;
    }
    return result;
}