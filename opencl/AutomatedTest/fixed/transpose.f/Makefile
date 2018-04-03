FUNKY_PATH = $(FUNKY_REP_BASE)
LANGTOOLS_PATH = $(FUNKY_PATH)/javac/langtools
FUNKY = $(LANGTOOLS_PATH)/build/bootstrap/bin/imp
CPP = g++  #should be icpc so we can use the alingment hints (for arrays)
export LD_LIBRARY_PATH:=$(FUNKY_LIB_PATH)/lib
export LIBRARY_PATH:=$(FUNKY_LIB_PATH)/lib



compile:
	 mkdir -p tmp
	 $(FUNKY) -PP "" -FORCEGCCLEANUP  -CONFIG "$(FUNKY_PATH)/config/zero.properties" -PROFILE 5 -useopencl -g:none -regression -verbose -CPP -DEPGRAPH -cp $(FUNKY_PATH)/input/current/ -d ./tmp ./cur.f -CC $(CPP) -std=c++0x -I./tmp -I$(FUNKY_PATH)/backend/boehm/bdwgc/include -I$(FUNKY_PATH)/backend/RunTime/include -I$(FUNKY_PATH)/backend/tbb4/include -o./tmp/test.run -g -w {files} -L/user/local/lib64 -framework OpenCL -ltbb -lFUNKY  -lgc -ltbbmalloc

release:
	 mkdir -p tmp
	 $(FUNKY) -PP "" -g:none -useopencl -CONFIG "../../../../../../config/zero.properties" -CPP -cp ../../../../../../input/current/ -d ./tmp ./cur.f -CC $(CPP) -std=c++0x -g -w -O3 -msse4.1 -I$(FUNKY_PATH)/backend/boehm/bdwgc/include -I./tmp -I$(FUNKY_PATH)/backend/RunTime/include -I$(FUNKY_PATH)/backend/tbb4/include -o./tmp/test.run {files} -ltbb -lFUNKY -lgc -ltbbmalloc

clean:
	 rm -rf tmp
	 mkdir -p tmp


