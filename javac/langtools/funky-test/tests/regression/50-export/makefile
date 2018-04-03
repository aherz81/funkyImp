LANGTOOLS_PATH = ../../../..
FUNKY_PATH = ../../../../../..
FUNKY = $(LANGTOOLS_PATH)/build/bootstrap/bin/imp
CPP = g++  #should be icpc so we can use the alingment hints (for arrays)
export LD_LIBRARY_PATH:=$(FUNKY_LIB_PATH)/lib
export LIBRARY_PATH:=$(FUNKY_LIB_PATH)/lib

lib:
	 #echo $(PWD)
	 #create lib from c/c++
	 mkdir -p tmp 
	 $(FUNKY) -PP "" -g:none -regression -verbose -CPP -d $(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/tmp $(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/Export.funky -CC $(CPP)  -std=c++0x -I$(FUNKY_PATH)/backend/boehm/bdwgc/include -I$(FUNKY_PATH)/backend/RunTime/include -I$(FUNKY_PATH)/backend/tbb4/include -o$(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/tmp/exported.o -c -g {files}
	 cp ./tmp/TestExport.f .
	 ar rcs ./tmp/libexported.a ./tmp/exported.o
    
import:
	 $(FUNKY) -PP "" -classpath $(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/ -g:none -regression -verbose -CPP -d $(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/tmp $(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/Import.funky -CC $(CPP) -std=c++0x -I./tmp -I$(FUNKY_PATH)/backend/boehm/bdwgc/include -I$(FUNKY_PATH)/backend/RunTime/include -I$(FUNKY_PATH)/backend/tbb4/include -o$(LANGTOOLS_PATH)/funky-test/tests/regression/50-export/tmp/imported.run -g -w -L./tmp {files} -ltbb -ltbbmalloc -lrt -lFUNKY -lpthread -lgccpp -lgc -lexported

clean:
	 rm -rf ./tmp
	 mkdir -p ./tmp
	 rm -f ./*.h
	 rm -f ./*.cpp
	 rm -f ./TestExport.f

