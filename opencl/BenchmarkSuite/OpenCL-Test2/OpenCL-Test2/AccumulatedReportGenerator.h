//
//  AccumulatedReportGenerator.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 04.02.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__AccumulatedReportGenerator__
#define __OpenCL_Test2__AccumulatedReportGenerator__

#include <iostream>
#include <memory>

#include "TableGenerator.h"


class AccumulatedReportGenerator {
    
private:
    std::vector<TableGenerator> tables;
    std::shared_ptr<TableGenerator> baseOperation;
    size_t numberOfRows;
    
public:
    AccumulatedReportGenerator();
    void setBaseOperation(std::shared_ptr<TableGenerator> baseOperation);
    void addTable(TableGenerator tableGen);
    std::string generateCombinedTable();
    std::string generateOperationTable();
    std::string generateOperationUsingBaseCostTable(double baseCost);
    std::string generatePerKernelTable();
    std::string generateOperationPerKernelTable();
    std::string generateOperationPerKernelTableUsingBaseCost(double baseCost);
};

#endif /* defined(__OpenCL_Test2__AccumulatedReportGenerator__) */
