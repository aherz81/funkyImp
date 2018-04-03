CC=g++
name = example.o

COMPILER = /opt/intel/composer_xe_2013/bin/icpc
export CPLUS_INCLUDE_PATH=/usr/include/x86_64-linux-gnu/c++/4.8
GXX=g++-4.8

ICPCBASEFLAGS=-I/usr/include/c++/4.8/
ICPCFLAGS=-Ofast -inline-forceinline -fstrict-aliasing -fno-exceptions -xHost -restrict $(ICPCBASEFLAGS)


all: $(name) 

$(name): 
	$(COMPILER) $(GXXFLAGS) $(ICPCFLAGS) -std=c++0x main.cpp -o $(name)

clean:
	@-rm -f *.o
	@-rm -f $(name)
	@-rm -Rf output.xyz



