#!/bin/sh

# 
# No assumptions patch creator: Just run & wait (takes quite a while)
#

./patch_setup.sh
./patch_copy.sh
# Build not needed for patch creation
#./patch_build.sh
./patch_create.sh
