#!/bin/bash

#
# Set up a fresh index complete with 
# clustering and the whole shebang.
#


#
# CONFIG
#

# Whitespace separated list of user@host pairs
MIRRORS="summa@debit"

INDEX_ROOT=~/persistentFiles
NEW_INDEX=$INDEX_ROOT/index_new/full
NEW_CLUSTERMAP=$INDEX_ROOT/completeClustermap_new

CLUSTER_GENERATION=$HOME/cluster/cluster.sh
FACET_GENERATION=$HOME/facetCluster/bin/clustermapCompleteCreate.sh

REMOTE_SYNC_COMMAND=bin/sync_index.sh

#
# Operations
#

function print_usage () {
    echo -e "USAGE:\n\tdeploy_index.sh <user@remote:/path/to/index>\n" 1>&2
}

function retrieve_index () {
    
    echo "`hostname`: Locating index"
    
    if [ -e "~/index.locator" ]; then
        echo "`hostname`: No index locator: ~/index.locator. Bailing out." 1>&2
        exit 1
    fi;
    
    INDEX=`cat ~/index.locator`    
    if [ "$INDEX" == "" ]; then
        echo "`hostname`: Index locator is empty. Bailing out." 1>&2
        exit 1
    fi;
    
    if [ ! -d "$INDEX" ]; then
        echo "`hostname`: $INDEX is not a directory. Bailing out." 1>&2
        exit 1
    fi;
    
    mkdir -p `dirname $NEW_INDEX`
    mv $INDEX $NEW_INDEX
    echo "`hostname`: moved new index from $INDEX to $NEW_INDEX"
}

function create_clusters () {
    echo "`hostname`: Creating clusters"
    
    if [ -e "$NEW_CLUSTERMAP" ]; then
        echo "$NEW_CLUSTERMAP already exists. Bailing out." 1>&2
        exit 1
    fi;
    
    $CLUSTER_GENERATION $NEW_INDEX
    if [ "$?" != "0" ]; then
        echo "`hostname`: Failed cluster generation. Bailing out." 1>&2        
        exit 1
    fi;
}

function create_facets () {
    echo "`hostname`: Creating facets"
    $FACET_GENERATION $NEW_INDEX
    if [ "$?" != "0" ]; then
        echo "`hostname`: Failed facet generation. Bailing out." 1>&2
        exit 1
    fi;
}

function start_sync () {
    for mirror in $MIRRORS
    do
        echo "`hostname`: Synching mirror $mirror"
        REMOTE_USER=`echo $mirror | sed -e "s/@.*//g"`
        ssh $mirror "/home/$REMOTE_USER/$REMOTE_SYNC_COMMAND $USER@`hostname`"
        if [ "$?" != "0" ]; then
            echo "`hostname`: Failed to sync mirror: $mirror" 1>&2
        fi;
    done
}

function rotate_indexes () {
    echo "`hostname`: Rotating indexes"
    rm -rf $INDEX_ROOT/index_old2
    mv $INDEX_ROOT/index_old $INDEX_ROOT/index_old2
    mv $INDEX_ROOT/index $INDEX_ROOT/index_old
    mv $INDEX_ROOT/index_new $INDEX_ROOT/index
            
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

retrieve_index $1
create_clusters
create_facets
start_sync
rotate_indexes
restart_tomcats
