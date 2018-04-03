#!/bin/bash
if [ "$1" = "" ]
then
    echo "usage: copystdlib.sh target_path"
    exit -1
fi

cp -r ./input/current/domains $1/funkyimp/dev/domains/
cp -r ./input/current/ffi $1/funkyimp/dev/ffi/
cp -r ./input/current/stdlib $1/funkyimp/dev/stdlib/

cp -r ./config $1/funkyimp/config/

