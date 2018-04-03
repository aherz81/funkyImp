#!/bin/bash
if [ "$1" = "" ]
then
    echo "usage: copylibs.sh target_path"
    exit -1
fi

#cp -r ./backend/boehm/bdwgc/include $1/funkyimp/
find ./backend/boehm/bdwgc/include -type f -not -iname '*.svn*' | xargs -I{}  cp '{}' $1/funkyimp/include/ &> /dev/null
cp -r ./backend/boehm/bdwgc/.libs/*.a $1/funkyimp/lib &> /dev/null
cp -r ./backend/boehm/bdwgc/.libs/*.so* $1/funkyimp/lib &> /dev/null
 
#cd ./backend/tbb4/include/ 
#find . -type f -not -iname '*.svn*' | xargs -I{} cp '{}' $1/funkyimp/include/{}
#cd ../../../
cp ./backend/tbb4/include/* $1/funkyimp/include/ &> /dev/null
cp ./backend/tbb4/include/tbb/* $1/funkyimp/include/tbb &> /dev/null

find ./backend/tbb4/build/ -name "*.so*" | xargs -I{} cp {} $1/funkyimp/lib
find ./backend/tbb4/build/ -name "*.dylib*" | xargs -I{} cp {} $1/funkyimp/lib
#find ./backend/tbb4/build/ -name "*.a" | xargs -I{} cp {} $1/funkyimp/lib
find ./javac/langtools/dist/bootstrap/bin/ -type f -not -iname '*.svn*' | xargs -I{} cp '{}' $1/funkyimp/bin/  &> /dev/null

find ./backend/RunTime/include -type f -not -iname '*.svn*' | xargs -I{}  cp '{}' $1/funkyimp/include/  &> /dev/null

find ./backend/RunTime/lib -type f -not -iname '*.svn*' | xargs -I{}  cp '{}' $1/funkyimp/lib/  &> /dev/null


find ./srclib/barvinok-0.35 -name "*.so*" | xargs -I{}  cp '{}' $1/funkyimp/lib/  &> /dev/null
find ./srclib/barvinok-0.35 -name "*.dylib*" | xargs -I{}  cp '{}' $1/funkyimp/lib/  &> /dev/null
find ./srclib/barvinok-0.35 -name "*.a*" | xargs -I{}  cp '{}' $1/funkyimp/lib/  &> /dev/null

find ./srclib/ibarvinok -name "*.so*" | xargs -I{}  cp '{}' $1/funkyimp/lib/  &> /dev/null
find ./srclib/ibarvinok -name "*.dylib*" | xargs -I{}  cp '{}' $1/funkyimp/lib/  &> /dev/null

cp ./javac/langtools/dist/bootstrap/bin/imp $1/funkyimp/bin
cp ./javac/langtools/dist/bootstrap/lib/imp.jar $1/funkyimp/lib

#./mergeRT.sh $1

