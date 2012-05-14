#!/bin/bash

DEFAULT_JAVA_HOME="/usr/lib/jvm/java"
JAVA_ARGS=
SCRIPT_DIR=`dirname $0`
JARFILE=summaIndex.jar

# The WEC_ROOT variable is needed for sub-scripts
if [ "$WEC_ROOT" == "" ]; then
    # Assume this script is in WEC_ROOT/bin
    WEC_ROOT=$SCRIPT_DIR/..
fi
echo "Using WEC_ROOT $WEC_ROOT"

source $SCRIPT_DIR/wec.config.sh

if [ "$JAVA_HOME" == "" ]; then
JAVA_HOME=$DEFAULT_JAVA_HOME
echo "No JAVA_HOME set. Using default: $JAVA_HOME"
fi;

if [ "$1" == "employer" ]; then
    JAVA_ARGS=$EMPLOYER_ARGS
    echo "Starting employer"
fi;

if [ "$1" == "consumer" ]; then
    JAVA_ARGS=$CONSUMER_ARGS
    echo "Starting consumer"
fi;

if [ "$1" == "worker" ]; then
    JAVA_ARGS=$WORKER_ARGS
    if [ "$DEDICATED" == "" ]; then
        JAVA_HOME="nice -n 10 $JAVA_HOME"
        echo "Starting sleeping worker"
    else
        echo "Starting dedicated worker"
    fi;
fi;

if [ "$1" == "merge" ]; then
    JAVA_ARGS=-Xmx2048m
    echo "Doing merge"
fi;



COMMAND="$JAVA_HOME/bin/java $JAVA_ARGS -jar $WEC_ROOT/$JARFILE $*"
echo $COMMAND
$COMMAND