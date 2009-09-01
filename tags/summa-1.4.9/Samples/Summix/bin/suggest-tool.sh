#! /bin/bash

DEPLOY=`dirname $0`/..
pushd $DEPLOY > /dev/null
DEPLOY=`pwd`

if [ "$CONFIGURATION" == "" ]; then
    export CONFIGURATION=config/suggest-tool.configuration.xml
fi;

ACTION="$1"
QUERY="$2"
HITCOUNT="$3"
QUERYCOUNT="$4"

if [ -z "$1" -o -z "$2" ]; then
    echo -e "USAGE:" 1>&2
    echo -e "\t$1 <action> <action_args>" 1>&2
    echo -e "Actions:" 1>&2
    echo -e "\tquery\t<prefix>" 1>&2
    echo -e "\tupdate\t<query> <hitcount> [querycount]" 1>&2
    echo -e "\tclear" 1>&2
    echo -e "\timport" 1>&2
    echo -e "\texport" 1>&2
fi

if [ "$ACTION" == "query" ]; then
    ARGS="summa.support.suggest.prefix=\"$QUERY\""
elif [ "$ACTION" == "update" ]; then
    ARGS="summa.support.suggest.update.query=\"$QUERY\""
    ARGS="$ARGS summa.support.suggest.update.hitcount=$HITCOUNT"
    
    if [ ! -z "$QUERYCOUNT" ]; then
        ARGS="$ARGS summa.support.suggest.update.querycount=$QUERYCOUNT"
    fi
elif [ "$ACTION" == "clear" ]; then
    ARGS="summa.support.suggest.clean=true"
elif [ "$ACTION" == "import" ]; then
    ARGS="summa.support.suggest.import=true"
elif [ "$ACTION" == "export" ]; then
    ARGS="summa.support.suggest.export=true"
    
else
    echo "Unknown action '$ACTION'" 1>&2
    popd > /dev/null
    exit 1
fi

eval "bin/search-tool.sh $ARGS"

