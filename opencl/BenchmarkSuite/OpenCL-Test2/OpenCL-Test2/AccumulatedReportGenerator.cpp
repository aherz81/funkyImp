//
//  AccumulatedReportGenerator.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 04.02.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "AccumulatedReportGenerator.h"

#include <algorithm>
#include <ostream>
#include <sstream>

using namespace std;


AccumulatedReportGenerator::AccumulatedReportGenerator() :
numberOfRows(-1) {}

void AccumulatedReportGenerator::setBaseOperation(std::shared_ptr<TableGenerator> baseOperation) {
    this->baseOperation = baseOperation;
}

void AccumulatedReportGenerator::addTable(TableGenerator tableGen) {
    if (tables.size() == 0) {
        tables.push_back(tableGen);
        numberOfRows = tableGen.operations.size();
    } else if (numberOfRows == tableGen.operations.size()) {
        tables.push_back(tableGen);
    } else {
        throw exception();
    }
}

string AccumulatedReportGenerator::generateCombinedTable() {
    stringstream result;
    for (int i = 0; i < numberOfRows; i++) {
        bool firstTableGen = true;
        for (TableGenerator &tableGen : tables) {
            if (firstTableGen) {
                result << tableGen.operations[i];
                firstTableGen = false;
            }
            ResultAnalyzer analyzer = tableGen.results[i];
            result << "\t" << analyzer.getAverage() << "\t" << analyzer.getStandardDeviation() << "\t" << analyzer.getStandardError();
        }
        result << endl;
    }
    string res = result.str();
    return res;
}

string AccumulatedReportGenerator::generateOperationUsingBaseCostTable(double baseCost) {
    stringstream result;
    for (int i = 0; i < numberOfRows; i++) {
        bool firstTableGen = true;
        for (TableGenerator &tableGen : tables) {
            if (firstTableGen) {
                result << tableGen.operations[i];
                firstTableGen = false;
            }
            ResultAnalyzer analyzer = tableGen.results[i];
            result << "\t" << analyzer.getAverage() - baseCost << "\t" << analyzer.getStandardDeviation() << "\t" << analyzer.getStandardError();
        }
        result << endl;
    }
    string res = result.str();
    return res;
}

string AccumulatedReportGenerator::generatePerKernelTable() {
    stringstream result;
    for (int i = 0; i < numberOfRows; i++) {
        bool firstTableGen = true;
        for (TableGenerator &tableGen : tables) {
            if (firstTableGen) {
                result << tableGen.operations[i];
                firstTableGen = false;
            }
            ResultAnalyzer analyzer = tableGen.results[i];
            result <<  "\t" << analyzer.getAverage() / tableGen.operations[i];
        }
        result << endl;
    }
    string res = result.str();
    return res;
}

string AccumulatedReportGenerator::generateOperationTable() {
    if (!baseOperation) {
        return "";
    } else {
        stringstream result;
        for (int i = 0; i < numberOfRows; i++) {
            bool firstTableGen = true;
            for (TableGenerator &tableGen : tables) {
                if (firstTableGen) {
                    result << tableGen.operations[i];
                    firstTableGen = false;
                }
                ResultAnalyzer &analyzer = tableGen.results[i];
                ResultAnalyzer &otherAnalyzer = baseOperation->results[i];
                result  <<  "\t" << ((analyzer.getAverage() - otherAnalyzer.getAverage()) / tableGen.numberOfOperations)
                << "\t" << max(analyzer.getStandardError(), analyzer.getStandardError());
            }
            result << endl;
        }
        string res = result.str();
        return res;
    }
}

string AccumulatedReportGenerator:: generateOperationPerKernelTable() {
    if (!baseOperation) {
        return "";
    } else {
        stringstream result;
        for (int i = 0; i < numberOfRows; i++) {
            bool firstTableGen = true;
            for (TableGenerator &tableGen : tables) {
                if (firstTableGen) {
                    result << tableGen.operations[i];
                    firstTableGen = false;
                }
                ResultAnalyzer &analyzer = tableGen.results[i];
                ResultAnalyzer &otherAnalyzer = baseOperation->results[i];
                result  <<  "\t" << (analyzer.getAverage() - otherAnalyzer.getAverage()) / (tableGen.operations[i] * tableGen.numberOfOperations);
            }
            result << endl;
        }
        string res = result.str();
        return res;
    }
}

string AccumulatedReportGenerator:: generateOperationPerKernelTableUsingBaseCost(double baseCost) {
    stringstream result;
    for (int i = 0; i < numberOfRows; i++) {
        bool firstTableGen = true;
        for (TableGenerator &tableGen : tables) {
            if (firstTableGen) {
                result << tableGen.operations[i];
                firstTableGen = false;
            }
            ResultAnalyzer &analyzer = tableGen.results[i];
            result  <<  "\t" << (analyzer.getAverage() - baseCost) / (tableGen.operations[i] * tableGen.numberOfOperations);
        }
        result << endl;
    }
    string res = result.str();
    return res;
    
}
