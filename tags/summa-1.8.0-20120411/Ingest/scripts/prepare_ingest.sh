#!/usr/bin/env bash

#
# The purpose of this script is to prepare new or updated data for ingestion
# into the storage system, and ingest it if applicable.
#
# It scans IN_DATA_DIR and extracts any gzipped files found in any subdirectory
# of IN_DATA_DIR to a subdir of the same name in DATA_DIR. If the subdir in question
# already exists in DATA_DIR it will be assumed that this updated already has been
# deployed, and the item will be skipped.
#

#
# CONSTANTS
#
DO_INGEST=$HOME/ingestDataService/ingest.sh
IN_DATA_DIR=$HOME/data_in/horizon_update
DATA_DIR=$HOME/data/horizon

#
# VARIABLES
#
CURRENT_IN_DATA_DIR=
CURRENT_TARGET_DIR=
TOTAL_NUM_INGESTS=0

function sanity_check_current_in_data_dir () {
    dir_count=0
    for f in `ls $CURRENT_IN_DATA_DIR`
    do
        let dir_count=$dir_count+1
        IS_GZIP=`file $CURRENT_IN_DATA_DIR/$f | grep gzip`
        if [ "$IS_GZIP" == "" ]; then
            echo "ERROR: Non-gzip file in input dir: $CURRENT_IN_DATA_DIR/$f" 1>&2
            exit 1
        fi;
    done;
    
    if [ $dir_count == 0 ]; then
        echo "WARNING: Empty input dir: $CURRENT_IN_DATA_DIR" 1>&2
        return 1
    fi;
    
    echo "Found $dir_count gzip files in $CURRENT_IN_DATA_DIR"
    
    return 0
}

function prepare_current_in_data_for_ingestion () {   
    if [ -d $CURRENT_TARGET_DIR ]; then
        echo "Data dir $CURRENT_TARGET_DIR already exists. Skipping"
        return
    fi;
    
    mkdir -p $CURRENT_TARGET_DIR
    
    # iterate over gzip files in CURRENT_IN_DATA_DIR and extract them to target_dir
    for orig in `ls $CURRENT_IN_DATA_DIR`
    do
        output=`basename $CURRENT_IN_DATA_DIR/$orig .gz`
        output=$CURRENT_TARGET_DIR/$output
        echo "Extracting $CURRENT_IN_DATA_DIR/$orig to $output"
        if [ -e $output ]; then
            echo "ERROR: Output file $output already exists" 1>&2
            exit 1
        fi
        
        # IMPORATANT: The -c switch makes sure gunzip keeps the original around
        gunzip -c $CURRENT_IN_DATA_DIR/$orig > $output
    done;
    
    $DO_INGEST

    if [ "$?" != "0" ]; then
        echo "`hostname`: Fatal error during ingest. Bailing out." 1>&2
        exit 1
    fi;

    let TOTAL_NUM_INGESTS=$TOTAL_NUM_INGESTS+1 
}

function prepare_updates_for_ingestion () {
    for d in `ls $IN_DATA_DIR`
    do
        CURRENT_IN_DATA_DIR=$IN_DATA_DIR/$d
        if [ ! -d $CURRENT_IN_DATA_DIR ]; then
            echo "WARN: Garbage in input dir: $CURRENT_IN_DATA_DIR" 1>&2
        else
            echo "Found data dir $CURRENT_IN_DATA_DIR"
            CURRENT_TARGET_DIR=$DATA_DIR/`basename $CURRENT_IN_DATA_DIR`
                        
            sanity_check_current_in_data_dir
            if [ "$?" == "0" ]; then
                prepare_current_in_data_for_ingestion
            else            
                echo "WARN: Skipping input dir $CURRENT_IN_DATA_DIR" 1>&2
            fi;
        fi;
    done;
}

function exit_if_nothing_ingested () {
    if [ "$TOTAL_NUM_INGESTS" == "0" ]; then
        echo "Nothing ingested. Stopping." 2>&1
        exit 1
    fi
}

prepare_updates_for_ingestion
exit_if_nothing_ingested