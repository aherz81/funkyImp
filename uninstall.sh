#!/bin/bash
target=`cat install_path.txt`
if [ "$target" = "" ]
then
    echo "cannot uninstall before install"
    exit -1
fi
echo "uninstalling from $target"
rm -rf $target
rm install_path.txt
