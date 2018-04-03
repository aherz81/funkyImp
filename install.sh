#!/bin/bash
if [ "$1" = "" ]
then
    echo "usage: install.sh target_path"
    exit -1
fi
echo "installing to $1..."
echo "$1" > install_path.txt

make clean

./configure

make default
mkdir -p $1/funkyimp/bin
mkdir -p $1/funkyimp/include/tbb
mkdir -p $1/funkyimp/lib
mkdir -p $1/funkyimp/dev
mkdir -p $1/funkyimp/config

./copylibs.sh $1
./copystdlib.sh $1

#find ./javac/langtools/dist/bootstrap/lib/ -type f -not -iname '*.svn*' | xargs -i cp '{}' $1/funkyimp/lib/

echo "libs and shared libs are stored in: '$1/funkyimp/lib', export FUNKY_LIB_PATH=$1/funkyimp/"
echo "runtime include path is: '$1/funkyimp/include', set -I for backend c++ compiler there"
echo "compiler binary 'imp' and runtime is in: '$1/funkyimp/bin/', make sure this path is in PATH or LD_LIBRARY_PATH when running the final binary"
echo "funky development code (stdlib,ffi,domains) are in: '$1/funkyimp/dev/', set your classpath accordingly when running imp -cp"
echo "should run test.sh to verify that all is well"

