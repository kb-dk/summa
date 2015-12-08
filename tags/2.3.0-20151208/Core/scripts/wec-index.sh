#!/bin/bash

#
# Launch a full distributed indexing session
# This script blocks until the final index is ready.
#

export JAVA_HOME=/usr/java/jdk1.5.0_07
export WEC_ROOT=$HOME/wecindexer

echo -e "\n ** Starting Employer:"
$WEC_ROOT/bin/wec.sh employer &
EMPLOYER_PID=$!
sleep 10

echo -e "\n ** Starting Consumer:"
$WEC_ROOT/bin/wec.sh consumer &
CONSUMER_PID=$!
sleep 10

echo -e "\n ** Starting slaves:"
$WEC_ROOT/bin/slaves.sh start &
sleep 10

echo -e "Wating for consumer to finish"
wait $CONSUMER_PID

echo -e "Consumer done. Killing employer with pid $EMPLOYER_PID"
kill $EMPLOYER_PID
