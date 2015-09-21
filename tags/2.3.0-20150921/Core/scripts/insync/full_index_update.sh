#!/bin/bash

#
# full_index_update.sh
#
# Run a full cycle as described in README.sync
#

#
# Constants
#
INDEX_ROOT=~/persistentFiles
INDEX_LOCATOR=$HOME/index.locator
INDEX_RUNNING_FILE=$HOME/index.update.running
CLUSTER_CALCULATOR="summa@kredit"

#
# Variables
#
NEW_INDEX=
NEW_INDEX_REMOTE=

function prepare_index_cycle () {
    if [ -e "$INDEX_RUNNING_FILE" ]; then
        echo -e "The index cycle appears to be in progress.\nRemove the file $INDEX_RUNNING_FILE if you are sure the index cycle is not in progress.\nBailing out." 2>&1
        exit 1
    fi
    
    touch $INDEX_RUNNING_FILE
}

function do_ingest () { 
    $HOME/ingestDataService/prepare_ingest.sh
    if [ "$?" != "0" ]; then
        echo "`hostname`: ERROR: Failed ingestion. Bailing out." 1>&2
        clean_up
        exit 1
    fi;
}

function do_index () {
    # Clean out all temporary files on all slaves
    echo "`hostname`: Removing temporary files on all slaves." >> $LOGPIPE
    $HOME/wecindexer/bin/slaves.sh run "rm -rf ~/tmp"

    # Start the actual indexing
    $HOME/wecindexer/wec-index.sh
    if [ "$?" != "0" ]; then
        echo "Failed indexing. Bailing out." 1>&2
        clean_up
        exit 1
    fi;

    if [ ! -e "$INDEX_LOCATOR" ]; then
        echo "`hostname`: FATAL: Index locator $INDEX_LOCATOR not found. Bailing out." 1>&2
        clean_up
        exit 1
    fi;
    NEW_INDEX=`cat $INDEX_LOCATOR`

    if [ ! -d "$NEW_INDEX" ]; then
        echo "`hostname`: FATAL: $NEW_INDEX is not a directory. Bailing out." 1>&2
        clean_up
        exit 1
    fi;
}

function start_cluster_calculator () {
    # TODO (IMPORTANT): Make sure NEW_INDEX_REMOTE doesn't exist before pusing to the location
    #                    This whole synching problem would better do as totally sandboxed 
    echo "`hostname`: Pushing index to cluster calculator: $CLUSTER_CALCULATOR"
    NEW_INDEX_REMOTE=tmp/`basename $NEW_INDEX`
    ssh $CLUSTER_CALCULATOR "mkdir -p ~/tmp"
    scp -r $NEW_INDEX $CLUSTER_CALCULATOR:~/$NEW_INDEX_REMOTE
    if [ "$?" != 0 ]; then
        echo "`hostname`: FATAL: Failed to copy index to $CLUSTER_CALCULATOR. Bailing out." 1>&2
        clean_up 
        exit 1
    fi
    ssh $CLUSTER_CALCULATOR "echo /home/summa/$NEW_INDEX_REMOTE > /home/summa/index.locator"
    
    echo "`hostname`: Starting cluster calculator"
    ssh $CLUSTER_CALCULATOR "~/bin/deploy_index.sh"
}

function clean_up () {
    if [ ! -e "$INDEX_RUNNING_FILE" ]; then
        echo -e "`hostname`: FATAL: The index cycle indicator file $INDEX_RUNNING_FILE has been removed during the cycle." 1>&2        
        exit 1
    fi
    rm $INDEX_RUNNING_FILE
    
    echo "`hostname`: Index cycle complete."
}

#
# Workflow
#
prepare_index_cycle
do_ingest
do_index
start_cluster_calculator
clean_up
