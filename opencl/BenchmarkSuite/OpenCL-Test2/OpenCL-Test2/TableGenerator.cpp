//
//  TableGenerator.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 14.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "TableGenerator.h"
#include <sstream>
#include <algorithm>

using namespace std;

TableGenerator::TableGenerator(int numberOfOperations) :
numberOfOperations(numberOfOperations) {}

void TableGenerator::addResult(int operationSize, ResultAnalyzer result) {
    operations.push_back(operationSize);
    results.push_back(result);
}

string TableGenerator::generateTable() {
    ostringstream result;
    for (int i = 0; i < results.size(); i++) {
        ResultAnalyzer analyzer = results[i];
        result << operations[i] << "\t" << analyzer.getAverage() << "\t" << analyzer.getStandardDeviation() << "\t" << analyzer.getStandardError() << endl;
    }
    string res = result.str();
    std::replace(res.begin(), res.end(), '.', ',');
    return res;
}

string TableGenerator::generateTablePerOperation()
{
    ostringstream result;
    for (int i = 0; i < results.size(); i++) {
        ResultAnalyzer analyzer = results[i];
        result << operations[i] <<  "\t" << analyzer.getAverage() / (operations[i] * numberOfOperations) << endl;
    }
    string res = result.str();
    std::replace(res.begin(), res.end(), '.', ',');
    return res;
}

string TableGenerator::generateDifferenceTable(TableGenerator &other) {
    ostringstream result;
    for (int i = 0; i < results.size(); i++) {
        ResultAnalyzer analyzer = results[i];
        ResultAnalyzer otherAnalyzer = other.results[i];
        result  << operations[i]
                <<  "\t" << analyzer.getAverage() - otherAnalyzer.getAverage()
                << "\t" << max(analyzer.getStandardError(), analyzer.getStandardError())
                << endl;
    }
    string res = result.str();
    std::replace(res.begin(), res.end(), '.', ',');
    return res;
}

double TableGenerator::getTimeForOperation() {
    double totalAverage = 0;
    int totalOps = 0;
    for (int i = 0; i < results.size(); i++) {
        float average = results[i].getAverage();
        int numOps = operations[i];
        totalAverage += (average);
        totalOps += numOps;
    }
    return totalAverage / ((totalOps == 0) ? 1 : totalOps);
}