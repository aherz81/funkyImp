#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=f95
AS=as

# Macros
CND_PLATFORM=GNU-Linux-x86
CND_DLIB_EXT=so
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/source/Scheduler.o \
	${OBJECTDIR}/source/ocl.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=-std=c++11 -Ofast -fstrict-aliasing -fno-exceptions -I/usr/include/x86_64-linux-gnu/c++/4.8
CXXFLAGS=-std=c++11 -Ofast -fstrict-aliasing -fno-exceptions -I/usr/include/x86_64-linux-gnu/c++/4.8

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/../lib/libFUNKY.a

${CND_DISTDIR}/../lib/libFUNKY.a: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/../lib
	${RM} ${CND_DISTDIR}/../lib/libFUNKY.a
	${AR} -rv ${CND_DISTDIR}/../lib/libFUNKY.a ${OBJECTFILES} 

${OBJECTDIR}/source/Scheduler.o: nbproject/Makefile-${CND_CONF}.mk source/Scheduler.cpp 
	${MKDIR} -p ${OBJECTDIR}/source
	${RM} "$@.d"
	$(COMPILE.cc) -g -D_DEBUG -I../tbb4/include -I../boehm/bdwgc/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/source/Scheduler.o source/Scheduler.cpp

${OBJECTDIR}/source/ocl.o: nbproject/Makefile-${CND_CONF}.mk source/ocl.cpp 
	${MKDIR} -p ${OBJECTDIR}/source
	${RM} "$@.d"
	$(COMPILE.cc) -g -D_DEBUG -I../tbb4/include -I../boehm/bdwgc/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/source/ocl.o source/ocl.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/../lib/libFUNKY.a

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
