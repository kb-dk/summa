#!/bin/bash

#
# sync_index.sh
#
# Update the index from a remote server,
# rotate the backups, and restart the tomcats
#
#


#
# CONFIG
#
INDEX_ROOT=$HOME/persistentFiles
NEW_INDEX=$INDEX_ROOT/index_new
NEW_CLUSTERMAP=$INDEX_ROOT/completeClustermap_new

#
# Functions
#

function print_usage () {
    echo -e "USAGE:\n\tsync_index.sh <user@remote:/path/to/index>\n" 1>&2
}

function retrieve_index () {
    if [ -e "$NEW_INDEX" ]; then
        echo "`hostname`: ERROR: Index $NEW_INDEX already exists. Bailing out." 1>&2
        exit 1
    fi
    
    if [ -e "$NEW_CLUSTERMAP" ]; then
        echo "`hostname`: ERROR: Cluster map $NEW_CLUSTERMAP already exists. Bailing out." 1>&2
        exit 1
    fi

    echo "`hostname`: Fetching remote index: $1"        
    scp -r $1:~/persistentFiles/index_new $NEW_INDEX
    if [ "$?" != "0" ]; then
        echo "`hostname`: Failed to retrieve index. Bailing out." 1>&2
        exit 1
    fi
    
    echo "`hostname`: Fetching clustermap"
    scp -r $1:~/persistentFiles/completeClustermap_new $NEW_CLUSTERMAP
    if [ "$?" != "0" ]; then
        echo "`hostname`: Failed to retrieve clustermap. Bailing out." 1>&2
        exit 1
    fi
}

function rotate_indexes () {
    echo "`hostname`: Rotating indexes"
    rm -rf $INDEX_ROOT/index_old2
    mv $INDEX_ROOT/index_old $INDEX_ROOT/index_old2
    mv $INDEX_ROOT/index $INDEX_ROOT/index_old
    mv $NEW_INDEX $INDEX_ROOT/index
    
    echo "`hostname`: Rotating clustermaps"
    rm -rf $INDEX_ROOT/completeClustermap_old2
    mv $INDEX_ROOT/completeClustermap_old $INDEX_ROOT/completeClustermap_old2
    mv $INDEX_ROOT/completeClustermap $INDEX_ROOT/completeClustermap_old
    mv $NEW_CLUSTERMAP $INDEX_ROOT/completeClustermap
}

function restart_tomcats () {
    echo "`hostname`: Restarting ws_tomcat_1 and sleeping 30s"
    $HOME/bin/search.sh restart ws_tomcat_1
    sleep 30
    wget -O /dev/null "http://`hostname`:8480/summaws/services/SummaCompleteClusterSearch?method=getClustersSimple&arg0=hest"
    if [ "$?" != "0" ]; then
        echo "`hostname`: ERROR: Failed to ping ws_tomcat_1" 1>&2
        exit 1
    fi

    echo "`hostname`: Restarting ws_tomcat_2 and sleeping 30s"
    $HOME/bin/search.sh restart ws_tomcat_2
    sleep 30
    wget -O /dev/null "http://`hostname`:8420/summaws/services/SummaCompleteClusterSearch?method=getClustersSimple&arg0=hest"
    if [ "$?" != "0" ]; then
        echo "`hostname`: ERROR: Failed to ping ws_tomcat_2" 1>&2
        exit 1
    fi
}

#
# Workflow
#

if [ $1 == "" ]; then
    print_usage
    exit 1
fi;

retrieve_index $1
#rotate_indexes
#restart_tomcats
