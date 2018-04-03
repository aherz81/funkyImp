#!/bin/bash
if [ "$FUNKY_LIB_PATH" = "" ]
then
    echo "FUNKY_LIB_PATH not set, abborting"
    exit -1
fi

cd javac/langtools/make

ant funkyreg-javac

cd ../../../
