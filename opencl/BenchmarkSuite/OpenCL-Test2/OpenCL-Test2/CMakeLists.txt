cmake_minimum_required(VERSION 2.6)

set(
	CMAKE_MODULE_PATH 
	${CMAKE_MODULE_PATH} 
	"${CMAKE_CURRENT_SOURCE_DIR}/cmake/Modules/"
)

project(open_cl_bench)

add_definitions("-std=c++11")

set(
	CMAKE_CXX_FLAGS 
	"${CMAKE_CXX_FLAGS} -std=c++11"
)

find_package(OpenCL REQUIRED)

include_directories(
	${OPENCL_INCLUDE_DIRS}
	${CMAKE_CURRENT_SOURCE_DIR}
)

link_directories(
	${OPENCL_LIBRARIES}
	${CMAKE_CURRENT_SOURCE_DIR}	
)

add_library(
	ocl_lib
	ocl.cpp
)

add_executable(
	cl_bench
	AccumulatedReportGenerator.cpp
	AlternateMeasureKernelProvider.cpp
	BenchmarkRunner.cpp
	EmptyKernelBaseCostProvider.cpp
	KernelProvider.cpp
	MemoryAccessComplexityComparisonProvider.cpp
	MemoryAccessComplexityKernelProvider.cpp
	MemoryAccessKernelProvider.cpp
	MemoryAccessPatternKernelProvider.cpp
	MemoryTransferKernelProvider.cpp
	MultipleBasicOperationsKernelProvider.cpp
	MultipleMemoryAccessesKernelProvider.cpp
	ResultAnalyzer.cpp
	TableGenerator.cpp
	Utilities.cpp
	WorkgroupSizeKernelProvider.cpp
	main.cpp
)

target_link_libraries(cl_bench ocl_lib ${OPENCL_LIBRARIES})
