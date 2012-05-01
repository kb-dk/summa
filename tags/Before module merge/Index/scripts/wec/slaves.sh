#!/bin/bash

#
# This script updates all slaves to the latest snapshot of the
# WEC indexing package
#
# To update (and install) configuration scripts on the slaves
# run upload_slave_config.sh.
#

SCRIPT_DIR=`dirname $0`

source $SCRIPT_DIR/wec.config.sh

if [ "$1" == "" ]; then
   echo -e "USAGE:"
   echo -e "\tslaves.sh <command>\n"
   echo -e "The following commands are available:\n"
   echo -e "\tinit\t\tUpload basic configuration scripts to the slaves (you need to call update after this)"
   echo -e "\tupdate\t\tUpdate the WEC distribution on the slaves"
   echo -e "\tstart\t\tStart each slave as a WEC worker"
   echo -e "\trun <command>\tExecute a command on each slave\n"
fi;

if [ "$1" == "update" ]; then

    echo "Updating slaves"

    for slave in $SLAVES
    do
    WECUSER=`echo $slave | sed -e "s/@.*//g"`
    ssh $slave /home/$WECUSER/setup_slave.sh
    done

fi;

if [ "$1" == "start" ]; then

    echo "Starting slaves"

    for slave in $SLAVES
    do
      WECUSER=`echo $slave | sed -e "s/@.*//g"`
      START="/home/$WECUSER/wecindexer/bin/wec.sh worker"
      echo "Running $START on $slave"
      ssh -f $slave $START
    done

fi;

if [ "$1" == "init" ]; then
    echo "Initializing slaves"
    echo "Generating setup_slave.sh"
    cat $SCRIPT_DIR/wec.config.sh > $SCRIPT_DIR/setup_slave.sh
    cat $SCRIPT_DIR/setup_slave.in >> $SCRIPT_DIR/setup_slave.sh
    chmod a+x $SCRIPT_DIR/setup_slave.sh

    for slave in $SLAVES
    do
    echo "Uploading setup_slave.sh to $slave"
    scp -p $SCRIPT_DIR/setup_slave.sh $slave:~/
    done

    echo "Make sure you have a copy of the WEC distribution in public_html/$TARBALL before you run 'slaves.sh update'"
fi;

if [ "$1" == "run" ]; then
    if [ "$2" == "" ]; then
        echo -e "USAGE:\n\tslaves.sh run <command>"
        echo -e "\n<command> must not be empty. Fx. 'killall java'."

    elif [ "$2" != "" ]; then
        echo -e "Running \"$2\" on all slaves"

        for slave in $SLAVES
        do
        echo -e "Running \"$2\" on $slave"
        ssh $slave $2
        done

        echo "Done"
    fi;
fi;
