//
//  TableGenerator.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 14.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__TableGenerator__
#define __OpenCL_Test2__TableGenerator__

#include <iostream>
#include "ResultAnalyzer.h"
//#include "AccumulatedReportGenerator.h"

class TableGenerator {
    std::vector<ResultAnalyzer> results;
    std::vector<int> operations;
    const int numberOfOperations;
    friend class AccumulatedReportGenerator;
    
public:
    TableGenerator(int numberOfOperations = 1);
    void addResult(int operationSize, ResultAnalyzer result);
    double getTimeForOperation();
    
    std::string generateTable();
    std::string generateDifferenceTable(TableGenerator &other);
    std::string generateTablePerOperation();
};

#endif /* defined(__OpenCL_Test2__TableGenerator__) */
