#!/bin/bash
cat install_path.txt | xargs -I{} -t ./copylibs.sh {}
cat install_path.txt | xargs -I{} -t ./copystdlib.sh {}

