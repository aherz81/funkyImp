//
//  ResultAnalyzer.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 14.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef __OpenCL_Test2__ResultAnalyzer__
#define __OpenCL_Test2__ResultAnalyzer__

#include <iostream>
#include <vector>
#include <memory>


class ResultAnalyzer {
    std::vector<double> runtimes;
    float average;
    float standardDeviation;
    float standardError;
    size_t numberOfNans;
    
public:
    ResultAnalyzer(std::vector<double> runtimes);
    float getAverage();
    float getStandardDeviation();
    float getStandardError();
    float getStandardErrorPercentage();
    void printStatistics();
    void dump();
    ResultAnalyzer operator+ (ResultAnalyzer &other);
    ResultAnalyzer operator+= (ResultAnalyzer &other);
    
private:
    void filter();
    void calculateAverage();
    void calculateStandardDeviation();
    void calculateStandardError();
};

#endif /* defined(__OpenCL_Test2__ResultAnalyzer__) */
