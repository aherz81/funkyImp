//
//  ResultAnalyzer.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 14.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "ResultAnalyzer.h"
#include <math.h>

using namespace std;

ResultAnalyzer::ResultAnalyzer(std::vector<double> runs) :
runtimes(runs), average(0), standardDeviation(0), standardError(0) {
    //filter();
    calculateAverage();
    calculateStandardDeviation();
    calculateStandardError();
}

void ResultAnalyzer::filter() {
    int minIndex = -1;
    double minVal = INFINITY;
    int maxIndex = -1;
    double maxVal = 0;
    for (int i = 0; i < runtimes.size(); i++) {
        double value = runtimes[i];
        if (value < minVal) {
            minVal = value;
            minIndex = i;
        } else if (value > maxVal) {
            maxVal = value;
            maxIndex = i;
        }
    }
    if (runtimes.size() > 0) {
        runtimes[minIndex] = NAN;
        runtimes[maxIndex] = NAN;
    }
}

void ResultAnalyzer::calculateAverage() {
    numberOfNans = 0;
    for (double runtime : runtimes) {
        if (runtime == runtime) {
            average += runtime;
        } else {
            numberOfNans++;
        }
    }
    average /= (runtimes.size() - numberOfNans);
}

void ResultAnalyzer::calculateStandardDeviation() {
    double mean = average;
    double sum = 0.0;
    
    for (auto run : runtimes) {
        if (run == run) {
            sum += (run - mean) * (run - mean);
        }
    }
    double variance = sum / (runtimes.size() - numberOfNans - 1);
    standardDeviation = sqrt(variance);
}

void ResultAnalyzer::calculateStandardError() {
    standardError = standardDeviation / sqrt(runtimes.size());
}

float ResultAnalyzer::getAverage() {
    return average;
}

float ResultAnalyzer::getStandardDeviation() {
    return standardDeviation;
}

float ResultAnalyzer::getStandardError() {
    return standardError;
}

float ResultAnalyzer::getStandardErrorPercentage() {
    return (standardError / average);
}

void ResultAnalyzer::printStatistics() {
    cout << "Performed " << runtimes.size() << " runs. " << endl;
    cout << "Average:\t" << average << endl;
    cout << "StdDev:\t" << standardDeviation << endl;
    cout << "StdErr:\t" << standardError << "\t in percent: " << getStandardErrorPercentage() << "%" << endl;
}

ResultAnalyzer ResultAnalyzer::operator+(ResultAnalyzer &other) {
    auto runtimes = std::vector<double>();
    runtimes.insert(runtimes.end(), this->runtimes.begin(), this->runtimes.end());
    runtimes.insert(runtimes.end(), other.runtimes.begin(), other.runtimes.end());
    return ResultAnalyzer(runtimes);
}

ResultAnalyzer ResultAnalyzer::operator+=(ResultAnalyzer &other) {
    this->runtimes.insert(runtimes.end(), other.runtimes.begin(), other.runtimes.end());
    average = 0;
    standardDeviation = 0;
    standardError = 0;
    calculateAverage();
    calculateStandardDeviation();
    calculateStandardError();
    return *this;
}

void ResultAnalyzer::dump() {
    for (double f : this->runtimes) {
        std::cout << f << " ";
    }
    std::cout << endl;
}
