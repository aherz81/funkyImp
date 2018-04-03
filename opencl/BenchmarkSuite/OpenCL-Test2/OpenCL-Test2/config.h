//
//  config.h
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 21.01.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#ifndef OpenCL_Test2_config_h
#define OpenCL_Test2_config_h

#include "ocl.h"

enum BenchmarkOutputVerbosityLevel {
    MINIMAL = 1, LOW = 2, HIGH = 3, EXTRA_VERBOSE = 4
};

extern const int samples;
extern const BenchmarkOutputVerbosityLevel verbosity;
extern const double targetErrorLevel;

#endif
