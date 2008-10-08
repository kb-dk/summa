#!/bin/bash

BASEDIR="$HOME/tmp/summa-nat-fetcher/data/"

NATBASES="all-fys all-geo all-geo-tm all-geo-tp all-itb all-kem all-kem-tm all-kem-tp all-mat all-vid" # what about atrette?
NATBASESDIFF="all-fys all-geo all-itb all-kem all-mat all-vid"

CURRENTDATE=`date +%Y%m%d`
BASEURL="http://www.daimi.au.dk/~bnielsen/statsbib/"
LASTFULLRUN=$BASEDIR"lastfullrun"

if [ -f "$LASTFULLRUN" ]; then
    LASTFULLRUNDATE=`cat $LASTFULLRUN`
else
    LASTFULLRUNDATE=""
fi

if [ "$1" == "full" ]; then
    if [ -d $BASEDIR$CURRENTDATE ]; then
	    echo "$BASEDIR$CURRENTDATE already exists";
	    echo "bailing out..."
	    exit
    else
	    echo "fetching full dump, saving data to $BASEDIR$CURRENTDATE"
	    mkdir -p $BASEDIR$CURRENTDATE
	    # TODO: only update lastfullrun file when a full dump has actually been downloaded
	    echo $CURRENTDATE > $LASTFULLRUN
	    pushd . > /dev/null
	    cd $BASEDIR$CURRENTDATE
        
	    for base in $NATBASES ; do
	        echo "fetching $base"
	        wget -q -c $BASEURL$base ;
	    done # fetch all full dump files
	    for base in $NATBASES ; do mv $base $base.data ; done # rename all full dump files

	    popd > /dev/null
    fi
fi

if [ "$1" == "diff" ]; then
    if [ "$LASTFULLRUNDATE" == "" ]; then
	    echo "lastfullrun date not found. Please run get_nat.sh all"
    	echo "bailing out..."
    	exit
    else
    	echo "fetching diffs to $BASEDIR starting from $LASTFULLRUNDATE"
    	pushd . > /dev/null
    	cd $BASEDIR
    	DATE=$LASTFULLRUNDATE
    	CURRENTDATEPLUSONE=`date -d "$CURRENTDATE 1 day" +%Y%m%d`
    	while [[ "$DATE"<"$CURRENTDATEPLUSONE" ]]; do # as long as we are not at the current date keep fetching diffs
    	    for base in $NATBASESDIFF ; do # run through all possible bases
        		DIFFNAME="diff-"$base"-"$DATE # filename to check
        		DIFFNAMEDONE=$DIFFNAME".done" # filename to check
        		if [[ !( (-f $DIFFNAME) || (-f $DIFFNAMEDONE) ) ]]; then # if the diff doesn't already exist then fetch it
        		    echo "Getting diff for $DATE $BASEURL$DIFFNAME"
        		    wget -q $BASEURL$DIFFNAME
        		fi
	        done
	        DATE=`date -d "$DATE 1 day" +%Y%m%d` # increment date
	    done
    	popd > /dev/null
	    echo "done fetching diffs"
    fi
fi

if [ "$1" == "symlink" ]; then
    pushd . > /dev/null
    cd $BASEDIR

    if [ "$LASTFULLRUNDATE" == "" ]; then
	    echo "lastfullrun date not found. Please run get_nat.sh all"
    	echo "bailing out..."
	    exit
    else
    	for base in $NATBASES ; do rm $base.data ; done # remove all full dump symlinks
            # TODO: check they are actually symlinks before removing
    	for base in $NATBASES ; do ln -s $LASTFULLRUNDATE/$base.data $base.data ; done # establish symlinks to the latest versions
	    popd > /dev/null
   fi
fi
