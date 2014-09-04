#!/bin/bash

set -e
DEPLOY=`dirname $0`/..

#
# EDIT HERE
# Variables prepended with ### are suitable for replacement 
# by autogenerated scripts
#

###MAINJAR=
export MAINCLASS=dk.statsbiblioteket.summa.common.SummaRegistry
export CODEBASE_BASEURL="file://$DEPLOY/lib"

export PRINT_CONFIG=
export DEFAULT_CONFIGURATION=$DEPLOY/config/registry.xml
export NAME=summa-registry
###LIBDIRS=
###JAVA_HOME=
#JVM_OPTS="$JVM_OPTS -Dsumma.configuration=$1"
###SECURITY_POLICY=
###ENABLE_JMX=

###JMX_PORT=
###JMX_SSL=
###JMX_ACCESS=
###JMX_PASS=


# Custom code
export CONFIGURATION=$1
if [ "." == ".$CONFIGURATION" -a -f "$DEFAULT_CONFIGURATION" ]; then
    CONFIGURATION="$DEFAULT_CONFIGURATION"
fi
if [ ! -f "$CONFIGURATION" ]; then
    echo "You must specify an exting configuration as first parameter instead of '$CONFIGURATION'" 1>&2
    exit 1
fi

# All is ready, execute!
exec $DEPLOY/bin/generic_start.sh
