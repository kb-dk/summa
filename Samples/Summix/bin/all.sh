#! /bin/bash

set -e

#
# Run all Summix scripts and set up a complete Summa
#

#
# Command used for detaching a process from the parent shell
#
DETACH="setsid"
STORAGE="bin/summa-storage.sh config/storage.xml"
INGEST="bin/summa-filter.sh config/ingest.xml"
INDEX="bin/summa-filter.sh config/indexer.xml"
SEARCHER="bin/summa-searcher.sh config/searcher.xml"
SUGGEST="bin/summa-searcher.sh config/suggest.xml"

pushd "$(dirname $0)/.." > /dev/null

#
# $1: Name
# $2: Command
#
function await() {
    echo -n "[$(date)] Waiting for $1 ... "
    if [ "$?" == "0" ]; then
        echo "OK"
    else
        echo "FAILED"
        exit 2
    fi        
}

#
# $1: Name
# $2: Command
#
function detach() {
    echo -n "[$(date)] Starting $1 ... "
    $DETACH $2 &
    PID=$!
    echo -n " (PID $PID) "

    # Sleep a bit and see if PID is still running...
    sleep 10
    if [ -e /proc/$PID ]; then
        echo "OK"
    else
        echo "FAILED"
        exit 2
    fi
}

function all_good() {
    echo "All good. Run 'bin/search-tool.sh search.document.query=bar' as a test query"
}

detach storage "$STORAGE"
detach suggest "$SUGGEST"
await ingest "$INGEST"
await index "$INDEX"
detach searcher "$SEARCHER"

all_good

