//
//  main.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 12.08.13.
//  Copyright (c) 2013 Alexander Pöppl. All rights reserved.
//
#include <ostream>
#include <string>
#include <iostream>
#include <fstream>
#include <memory>
#include <functional>

#include "config.h"
#include "BenchmarkRunner.h"
#include "MemoryAccessKernelProvider.h"
#include "MemoryTransferKernelProvider.h"
#include "EmptyKernelBaseCostProvider.h"
#include "AccumulatedReportGenerator.h"
#include "AlternateMeasureKernelProvider.h"
#include "WorkgroupSizeKernelProvider.h"
#include "MultipleMemoryAccessesKernelProvider.h"
#include "MultipleBasicOperationsKernelProvider.h"
#include "MemoryAccessComplexityKernelProvider.h"
#include "MemoryAccessPatternKernelProvider.h"
#include "MemoryAccessComplexityComparisonProvider.h"

using namespace std;

const int samples = 50;
const BenchmarkOutputVerbosityLevel verbosity = HIGH;
const double targetErrorLevel = 0.02;
const int maxMemSize = 12058624;


//Generator functions. Used to obtain the desired memory or work-group sizes
vector<function<int(int)>> generators = {
    [] (int seed) {
        return 1 << seed;
    },
    
    [] (int seed) {
        return 1024 * 256 * (seed + 1) + 16*1024*1024;
    },
    
    [] (int seed) {
        return seed + 1;
    },
    
    [] (int seed) {
        return (seed + 1) * 4096 * 8192;
    },
    
    
    [] (int seed) {
        return (seed + 1) * 256;
    },
    
    [] (int seed) {
        if (seed < 256) {
            return (seed +1) * 512;
        } else {
            return (seed - 254) *128*1024;
        }
    },
    
    [] (int seed) {
        return 1024*512 + 32*1024 * seed;
    },
    
    [] (int seed) {
        return 1+seed;
    },
    
    [] (int seed) {
        return seed;
    },
    
    [] (int seed) {
        return 1024*1024*4 * (seed+1);
    },
};

//These are the benchmarks that are available to the suites
void printCacheSizeInformation(ocl_device *device);
void runBenchmarks(ocl_device *executing_device);
void runBasicBenchmarks(ocl_device *executingDevice);
void runWgBenchmarks(ocl_device *executingDevice);
void runMultipleAccessBenchmarks(ocl_device *executingDevice);
void runMultipleBasicOperationsBenchmarks(ocl_device *executingDevice);
void runMemoryTransferBenchmarks(ocl_device *executingDevice);
void runLinearMemoryTransferBenchmarks(ocl_device *executingDevice);
void runMemoryAccessBenchmarks(ocl_device *executingDevice);
void runMemoryAccessPatternBenchmarks(ocl_device *executingDevice);
void runBasicOperationsBenchmarks(ocl_device *executingDevice);
void runMultipleBasicOperationsBenchmarks(ocl_device *executingDevice);
void runMemoryComplexityOperationsBenchmarks(ocl_device *executingDevice);
void runMemoryComplexityComparisonBenchmark(ocl_device *device);
void runBasicIntegerOperationsBenchmarks(ocl_device *device);
void runMultipleBasicIntegerOperationsBenchmarks(ocl_device *executingDevice);


std::shared_ptr<TableGenerator> baseOperation;

int main(int argc, const char * argv[])
{
    ocl_setup setup;
    setup.findDevices();
    setup.findDeviceInformation();
    ocl_device device = ocl::displayDevices();
    runBenchmarks(&device);
}

void runBenchmarks(ocl_device *device) {
    printCacheSizeInformation(device);
    runBasicBenchmarks(device);
    runWgBenchmarks(device);
    runLinearMemoryTransferBenchmarks(device);
    runMemoryTransferBenchmarks(device);
    runMemoryAccessBenchmarks(device);
    runMemoryAccessPatternBenchmarks(device);
    runMultipleAccessBenchmarks(device);
    runBasicIntegerOperationsBenchmarks(device);
    runBasicOperationsBenchmarks(device);
    runMultipleBasicOperationsBenchmarks(device);
    runMultipleBasicIntegerOperationsBenchmarks(device);
    runMemoryComplexityOperationsBenchmarks(device);
    runMemoryComplexityComparisonBenchmark(device);
 }

void printCacheSizeInformation(ocl_device *device) {
    cl_ulong globalMemorySize, globalMemoryCacheSize, localMemorySize;
    cl_device_local_mem_type localMemoryType;
    clGetDeviceInfo(device->getDeviceID(), CL_DEVICE_GLOBAL_MEM_SIZE, sizeof(cl_ulong), &globalMemorySize, NULL);
    clGetDeviceInfo(device->getDeviceID(), CL_DEVICE_GLOBAL_MEM_CACHE_SIZE, sizeof(cl_ulong), &globalMemoryCacheSize, NULL);
    clGetDeviceInfo(device->getDeviceID(), CL_DEVICE_LOCAL_MEM_SIZE, sizeof(cl_ulong), &localMemorySize, NULL);
    clGetDeviceInfo(device->getDeviceID(), CL_DEVICE_LOCAL_MEM_TYPE, sizeof(cl_device_local_mem_type), &localMemoryType, NULL);
    cout << "----" << device->getName() << "----" << endl;
    cout << "Global memory size:\t" << globalMemorySize << endl;
    cout << "Global cache size:\t" << globalMemoryCacheSize << endl;
    cout << "Local memory size:\t" << localMemorySize << endl;;
    cout << "Local memory type:\t" << ((localMemoryType == 0x1) ? "local (dedicated)" : "global") << endl;
    cout << "--------" << endl;
}

void runBasicBenchmarks(ocl_device *device) {
    unique_ptr<KernelProvider> emptyKernelKp(new EmptyKernelBaseCostProvider());
    BenchmarkRunner emptyRunner(device, 1, move(emptyKernelKp), samples, 4096*4096, verbosity);
    TableGenerator primary = emptyRunner.runBenchmark(generators[5]);
    baseOperation = make_shared<TableGenerator>(primary);

    AccumulatedReportGenerator reportGen;
    reportGen.addTable(primary);
    ofstream combinedDump, perKernelDump;
    std::cout << "Dumping Runtime for Empty Kernels" << endl;
    combinedDump.open("___baseCombined.log", ios::out | ios::trunc);
    combinedDump << reportGen.generateCombinedTable() << endl;
    combinedDump.close();
    perKernelDump.open("___basePerKernel.log", ios::out | ios::trunc);
    perKernelDump << reportGen.generatePerKernelTable() << endl;
    perKernelDump.close();
}

void runWgBenchmarks(ocl_device *device) {
    unique_ptr<KernelProvider> emptyKernelKp(new WorkgroupSizeKernelProvider());
    BenchmarkRunner emptyRunner(device, 1, move(emptyKernelKp), samples, device->getGroupSize(0), verbosity);
    TableGenerator primary = emptyRunner.runBenchmark(generators[7]);
    
    AccumulatedReportGenerator reportGen;
    reportGen.addTable(primary);
    ofstream combinedDump, perKernelDump;
    std::cout << "Dumping Runtime for Empty Kernels" << endl;
    combinedDump.open("___wgSize.log", ios::out | ios::trunc);
    combinedDump << reportGen.generateCombinedTable() << endl;
    combinedDump.close();
    perKernelDump.open("___wgSizePerKernel.log", ios::out | ios::trunc);
    perKernelDump << reportGen.generatePerKernelTable() << endl;
    perKernelDump.close();
}

void runMemoryTransferBenchmarks(ocl_device *device) {
    auto baseCostGenerator = generators[2];
    auto perElementGenerator = generators[3];
    
    unique_ptr<KernelProvider> toDeviceBaseKp(new MemoryTransferKernelProvider(TransferType::TO_DEVICE));
    BenchmarkRunner toDeviceBaseBench(device, 1, move(toDeviceBaseKp), samples, 1, verbosity);
    TableGenerator toLatency = toDeviceBaseBench.runBenchmark(baseCostGenerator);
    
    unique_ptr<KernelProvider> fromDeviceBaseKp(new MemoryTransferKernelProvider(TransferType::FROM_DEVICE));
    BenchmarkRunner fromDeviceBaseBench(device, 1, move(fromDeviceBaseKp), samples, 1, verbosity);
    TableGenerator fromLatency = fromDeviceBaseBench.runBenchmark(baseCostGenerator);
    
    unique_ptr<KernelProvider> toDeviceKp(new MemoryTransferKernelProvider(TransferType::TO_DEVICE));
    BenchmarkRunner toDeviceBench(device, 1, move(toDeviceKp), samples, 8192*8192, verbosity);
    TableGenerator toPerElement = toDeviceBench.runBenchmark(perElementGenerator);
    
    unique_ptr<KernelProvider> fromDeviceKp(new MemoryTransferKernelProvider(TransferType::FROM_DEVICE));
    BenchmarkRunner fromDeviceBench(device, 1, move(fromDeviceKp), samples*10, 8192*8192, verbosity);
    TableGenerator fromPerElement = fromDeviceBench.runBenchmark(perElementGenerator);
    
    std::cout << "Dumping Runtime for Empty Kernels" << endl;
    ofstream transferDump;
    transferDump.open("___transfer.log", ios::out | ios::trunc);
    transferDump << "To device: \t" << toLatency.getTimeForOperation() << "\t" << toPerElement.getTimeForOperation() << endl;
    transferDump << "From Device: \t" << fromLatency.getTimeForOperation() << "\t"<< fromPerElement.getTimeForOperation() << endl;
    transferDump.close();
}

void runLinearMemoryTransferBenchmarks(ocl_device *device) {
    auto baseCostGenerator = generators[9];
    AccumulatedReportGenerator reportGen;
    unique_ptr<KernelProvider> toDeviceBaseKp(new MemoryTransferKernelProvider(TransferType::TO_DEVICE));
    BenchmarkRunner toDeviceBaseBench(device, 1, move(toDeviceBaseKp), samples, 1024*1024*64, verbosity);
    TableGenerator toLatency = toDeviceBaseBench.runBenchmark(baseCostGenerator);
    
    unique_ptr<KernelProvider> fromDeviceBaseKp(new MemoryTransferKernelProvider(TransferType::FROM_DEVICE));
    BenchmarkRunner fromDeviceBaseBench(device, 1, move(fromDeviceBaseKp), samples, 1024*1024*64, verbosity);
    TableGenerator fromLatency = fromDeviceBaseBench.runBenchmark(baseCostGenerator);
    
    reportGen.addTable(toLatency);
    reportGen.addTable(fromLatency);
    std::cout << "Dumping Runtime for Empty Kernels" << endl;
    ofstream transferDump;
    transferDump.open("___transferLinear.log", ios::out | ios::trunc);
    transferDump << reportGen.generateCombinedTable();
    transferDump.close();
}

void runMemoryAccessBenchmarks(ocl_device *device) {
    std::shared_ptr<TableGenerator> basicBaseOp = nullptr;
    auto generatorFunction = generators[5];
    AccumulatedReportGenerator reportGenerator;
    for (std::string op : MemoryAccessKernelProvider::memoryAccessType) {
        unique_ptr<KernelProvider> kernelProvider(new MemoryAccessKernelProvider(op, op));
        BenchmarkRunner runner(device, 1, move(kernelProvider), samples, maxMemSize , verbosity);
        if (basicBaseOp == nullptr) {
            basicBaseOp = std::make_shared<TableGenerator>(runner.runBenchmark(generatorFunction));
            reportGenerator.setBaseOperation(basicBaseOp);
            reportGenerator.addTable(*basicBaseOp);
        } else {
            reportGenerator.addTable(runner.runBenchmark(generatorFunction));
        }
    }
    ofstream accessCombined, accessOperation, accessPerKernel, accessPerKernelOp;
    std::cout << "Dumping Memory Accesses: none private global" << endl;
    accessCombined.open("___accessCombined.log", ios::out | ios::trunc);
    accessCombined << reportGenerator.generateCombinedTable() << endl;
    accessCombined.close();
    accessOperation.open("___accessOperation.log", ios::out | ios::trunc);
    accessOperation << reportGenerator.generateOperationTable() << endl;
    accessOperation.close();
    accessPerKernel.open("___accessPerKernel.log", ios::out | ios::trunc);
    accessPerKernel << reportGenerator.generatePerKernelTable() << endl;
    accessPerKernel.close();
    accessPerKernelOp.open("___accessPerKernelOp.log", ios::out | ios::trunc);
    accessPerKernelOp << reportGenerator.generateOperationPerKernelTable() << endl;
    accessPerKernelOp.close();
}

void runMemoryAccessPatternBenchmarks(ocl_device *device) {
    std::shared_ptr<TableGenerator> basicBaseOp = nullptr;
    auto generatorFunction = generators[5];
    AccumulatedReportGenerator reportGenerator;
    for (AccessPatternType type : MemoryAccessPatternKernelProvider::memoryAccessType) {
        unique_ptr<KernelProvider> kernelProvider(new MemoryAccessPatternKernelProvider(type));
        BenchmarkRunner runner(device, 1, move(kernelProvider), samples, maxMemSize , verbosity);
        if (basicBaseOp == nullptr) {
            basicBaseOp = std::make_shared<TableGenerator>(runner.runBenchmark(generatorFunction));
            reportGenerator.setBaseOperation(basicBaseOp);
            reportGenerator.addTable(*basicBaseOp);
        } else {
            reportGenerator.addTable(runner.runBenchmark(generatorFunction));
        }
    }
    ofstream accessCombined, accessOperation, accessPerKernel, accessPerKernelOp;
    std::cout << "Dumping Memory Accesses..." << endl;
    accessCombined.open("___accessPatternsCombined.log", ios::out | ios::trunc);
    accessCombined << reportGenerator.generateCombinedTable() << endl;
    accessCombined.close();
    accessOperation.open("___accessPatternsOperation.log", ios::out | ios::trunc);
    accessOperation << reportGenerator.generateOperationTable() << endl;
    accessOperation.close();
    accessPerKernel.open("___accessPatternsPerKernel.log", ios::out | ios::trunc);
    accessPerKernel << reportGenerator.generatePerKernelTable() << endl;
    accessPerKernel.close();
    accessPerKernelOp.open("___accessPatternsPerKernelOp.log", ios::out | ios::trunc);
    accessPerKernelOp << reportGenerator.generateOperationPerKernelTable() << endl;
    accessPerKernelOp.close();
}

void runBasicOperationsBenchmarks(ocl_device *device) {
    std::shared_ptr<TableGenerator> basicBaseOp = nullptr;
    auto generatorFunction = generators[5];
    AccumulatedReportGenerator reportGenerator;
    for (std::string op : MemoryAccessKernelProvider::basicOperationType) {
        unique_ptr<KernelProvider> kernelProvider(new MemoryAccessKernelProvider(op, op));
        BenchmarkRunner runner(device, 1, move(kernelProvider), samples, maxMemSize, verbosity);//, baseOperation);
        if (basicBaseOp == nullptr) {
            basicBaseOp = std::make_shared<TableGenerator>(runner.runBenchmark(generatorFunction));
            reportGenerator.setBaseOperation(basicBaseOp);
        } else {
            reportGenerator.addTable(runner.runBenchmark(generatorFunction));
        }
    }
    ofstream operationCombined, operationOperation, operationPerKernel, operationPerKernelOp;
    std::cout << "Dumping basic floating point operations..." << endl;
    operationCombined.open("___operationCombined.log", ios::out | ios::trunc);
    operationCombined << reportGenerator.generateCombinedTable() << endl;
    operationCombined.close();
    operationOperation.open("___operationOperation.log", ios::out | ios::trunc);
    operationOperation << reportGenerator.generateOperationTable() << endl;
    operationOperation.close();
    operationPerKernel.open("___operationPerKernel.log", ios::out | ios::trunc);
    operationPerKernel << reportGenerator.generatePerKernelTable() << endl;
    operationPerKernel.close();
    operationPerKernelOp.open("___operationPerKernelOp.log", ios::out | ios::trunc);
    operationPerKernelOp << reportGenerator.generateOperationPerKernelTable() << endl;
    operationPerKernelOp.close();
}

void runBasicIntegerOperationsBenchmarks(ocl_device *device) {
    std::shared_ptr<TableGenerator> basicBaseOp = nullptr;
    auto generatorFunction = generators[5];
    AccumulatedReportGenerator reportGenerator;
    for (std::string op : MemoryAccessKernelProvider::basicOperationType) {
        unique_ptr<KernelProvider> kernelProvider(new MemoryAccessKernelProvider(op, op, IntegerType));
        BenchmarkRunner runner(device, 1, move(kernelProvider), samples, maxMemSize, verbosity);//, baseOperation);
        if (basicBaseOp == nullptr) {
            basicBaseOp = std::make_shared<TableGenerator>(runner.runBenchmark(generatorFunction));
            reportGenerator.setBaseOperation(basicBaseOp);
        } else {
            reportGenerator.addTable(runner.runBenchmark(generatorFunction));
        }
    }
    ofstream operationCombined, operationOperation, operationPerKernel, operationPerKernelOp;
    std::cout << "Dumping basic integer operations..." << endl;
    operationCombined.open("___intOperationCombined.log", ios::out | ios::trunc);
    operationCombined << reportGenerator.generateCombinedTable() << endl;
    operationCombined.close();
    operationOperation.open("___intOperationOperation.log", ios::out | ios::trunc);
    operationOperation << reportGenerator.generateOperationTable() << endl;
    operationOperation.close();
    operationPerKernel.open("___intOperationPerKernel.log", ios::out | ios::trunc);
    operationPerKernel << reportGenerator.generatePerKernelTable() << endl;
    operationPerKernel.close();
    operationPerKernelOp.open("___intOperationPerKernelOp.log", ios::out | ios::trunc);
    operationPerKernelOp << reportGenerator.generateOperationPerKernelTable() << endl;
    operationPerKernelOp.close();
}


void runMultipleAccessBenchmarks(ocl_device *executingDevice) {
    double baseCost = 0.000188876028330378 * 1024*1024 + 3.1621293534031;
    
    unique_ptr<KernelProvider> emptyKernelKp(new MultipleMemoryAccessesKernelProvider());
    BenchmarkRunner emptyRunner(executingDevice, 1, move(emptyKernelKp), samples, 128, verbosity);
    TableGenerator primary = emptyRunner.runBenchmark(generators[7]);
    
    AccumulatedReportGenerator reportGen;
    reportGen.addTable(primary);
    ofstream combinedDump, perKernelDump;
    std::cout << "Dumping Runtime for Multiple Accesses..." << endl;
    combinedDump.open("___multipleAccess.log", ios::out | ios::trunc);
    combinedDump << reportGen.generateOperationUsingBaseCostTable(baseCost) << endl;
    combinedDump.close();
    perKernelDump.open("___multipleAccessPerKernel.log", ios::out | ios::trunc);
    perKernelDump << reportGen.generateOperationPerKernelTableUsingBaseCost(baseCost) << endl;
    perKernelDump.close();
}

void runMultipleBasicOperationsBenchmarks(ocl_device *executingDevice) {
    const vector<string> operators = {"+", "-", "*", "/"};
    AccumulatedReportGenerator reportGen;

    for (auto op : operators) {
        unique_ptr<KernelProvider> emptyKernelKp(new MultipleBasicOperationsKernelProvider(FloatType, op));
        BenchmarkRunner emptyRunner(executingDevice, 1, move(emptyKernelKp), samples, 128, verbosity);
        TableGenerator primary = emptyRunner.runBenchmark(generators[7]);
        reportGen.addTable(primary);
    }
    ofstream combinedDump, perKernelDump;
    std::cout << "Dumping Runtime for Multiple Basic Operations on Floating Point Numbers..." << endl;
    combinedDump.open("___multipleOperations.log", ios::out | ios::trunc);
    combinedDump << reportGen.generateCombinedTable() << endl;
    combinedDump.close();
    perKernelDump.open("___multipleOperationsPerKernel.log", ios::out | ios::trunc);
    perKernelDump << reportGen.generatePerKernelTable() << endl;
    perKernelDump.close();
}

void runMultipleBasicIntegerOperationsBenchmarks(ocl_device *executingDevice) {
    const vector<string> operators = {"+", "-", "*", "/"};
    AccumulatedReportGenerator reportGen;
    
    for (auto op : operators) {
        unique_ptr<KernelProvider> emptyKernelKp(new MultipleBasicOperationsKernelProvider(IntegerType, op));
        BenchmarkRunner emptyRunner(executingDevice, 1, move(emptyKernelKp), samples, 128, verbosity);
        TableGenerator primary = emptyRunner.runBenchmark(generators[7]);
        reportGen.addTable(primary);
    }
    ofstream combinedDump, perKernelDump;
    std::cout << "Dumping Runtime for Multiple Basic Operations on Integers..." << endl;
    combinedDump.open("___multipleIntDivisions.log", ios::out | ios::trunc);
    combinedDump << reportGen.generateCombinedTable() << endl;
    combinedDump.close();
    perKernelDump.open("___multipleIntDivisionsPerKernel.log", ios::out | ios::trunc);
    perKernelDump << reportGen.generatePerKernelTable() << endl;
    perKernelDump.close();
}

void runMemoryComplexityOperationsBenchmarks(ocl_device *executingDevice) {
    unique_ptr<KernelProvider> emptyKernelKp(new MemoryAccessComplexityKernelProvider());
    BenchmarkRunner emptyRunner(executingDevice, 1, move(emptyKernelKp), samples, 20*20, verbosity);
    TableGenerator primary = emptyRunner.runBenchmark(generators[7]);
    
    AccumulatedReportGenerator reportGen;
    reportGen.addTable(primary);
    ofstream combinedDump, perKernelDump;
    std::cout << "Dumping Runtime for Memory Complexity Analysis..." << endl;
    combinedDump.open("___memoryComplexity.log", ios::out | ios::trunc);
    combinedDump << reportGen.generateCombinedTable() << endl;
    combinedDump.close();
    perKernelDump.open("___memoryComplexityPerKernel.log", ios::out | ios::trunc);
    perKernelDump << reportGen.generatePerKernelTable() << endl;
    perKernelDump.close();
}

void runMemoryComplexityComparisonBenchmark(ocl_device *device) {
    auto generatorFunction = generators[5];
    AccumulatedReportGenerator reportGenerator;
    for (std::string op : MemoryAccessComplexityComparisonProvider::memoryComplexityClasses) {
        unique_ptr<KernelProvider> kernelProvider(new MemoryAccessComplexityComparisonProvider(op, op));
        BenchmarkRunner runner(device, 1, move(kernelProvider), samples, maxMemSize , verbosity);
        reportGenerator.addTable(runner.runBenchmark(generatorFunction));
    }
    ofstream accessCombined, accessOperation, accessPerKernel, accessPerKernelOp;
    std::cout << "Dumping Runtime for Memory Access Complexity Comparison..." << endl;
    accessCombined.open("___complexityCombined.log", ios::out | ios::trunc);
    accessCombined << reportGenerator.generateCombinedTable() << endl;
    accessCombined.close();
    accessOperation.open("___complexityOperation.log", ios::out | ios::trunc);
    accessOperation << reportGenerator.generateOperationTable() << endl;
    accessOperation.close();
    accessPerKernel.open("___complexityPerKernel.log", ios::out | ios::trunc);
    accessPerKernel << reportGenerator.generatePerKernelTable() << endl;
    accessPerKernel.close();
    accessPerKernelOp.open("___complexityPerKernelOp.log", ios::out | ios::trunc);
    accessPerKernelOp << reportGenerator.generateOperationPerKernelTable() << endl;
    accessPerKernelOp.close();
}