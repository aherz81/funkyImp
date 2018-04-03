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
	${OBJECTDIR}/ibarvinoc.o \
	${OBJECTDIR}/isl_obj_list.o \
	${OBJECTDIR}/isl_obj_str.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-L/usr/local/lib -L../barvinok-0.35/cloog/.libs -L../barvinok-0.35/.libs -L../barvinok-0.35/isl/.libs -L../barvinok-0.35/isl-polylib/.libs -L../barvinok-0.35/polylib/.libs

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libibarvinok.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libibarvinok.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libibarvinok.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -lcloog-isl -lbarvinok -lisl -lisl-polylib -lpolylibgmp -lgmp -lntl -lstdc++ -shared -fPIC

${OBJECTDIR}/ibarvinoc.o: ibarvinoc.c 
	${MKDIR} -p ${OBJECTDIR}
	${RM} $@.d
	$(COMPILE.c) -g -I../barvinok-0.35/isl/include -I../barvinok-0.35 -I../barvinok-0.35/isl-polylib/include -I../barvinok-0.35/polylib/include -I../barvinok-0.35/cloog/include -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/ibarvinoc.o ibarvinoc.c

${OBJECTDIR}/isl_obj_list.o: isl_obj_list.c 
	${MKDIR} -p ${OBJECTDIR}
	${RM} $@.d
	$(COMPILE.c) -g -I../barvinok-0.35/isl/include -I../barvinok-0.35 -I../barvinok-0.35/isl-polylib/include -I../barvinok-0.35/polylib/include -I../barvinok-0.35/cloog/include -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/isl_obj_list.o isl_obj_list.c

${OBJECTDIR}/isl_obj_str.o: isl_obj_str.c 
	${MKDIR} -p ${OBJECTDIR}
	${RM} $@.d
	$(COMPILE.c) -g -I../barvinok-0.35/isl/include -I../barvinok-0.35 -I../barvinok-0.35/isl-polylib/include -I../barvinok-0.35/polylib/include -I../barvinok-0.35/cloog/include -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/isl_obj_str.o isl_obj_str.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libibarvinok.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
