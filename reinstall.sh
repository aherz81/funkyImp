#!/bin/bash
cat install_path.txt | xargs -I{} -t ./install.sh {}

