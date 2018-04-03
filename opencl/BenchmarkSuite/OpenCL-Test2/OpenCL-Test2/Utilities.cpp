//
//  Utilities.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 21.05.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "Utilities.h"


std::string util::replaceString(std::string subject, const std::string& search,
                          const std::string& replace) {
    size_t pos = 0;
    while((pos = subject.find(search, pos)) != std::string::npos) {
        subject.replace(pos, search.length(), replace);
        pos += replace.length();
    }
    return subject;
}