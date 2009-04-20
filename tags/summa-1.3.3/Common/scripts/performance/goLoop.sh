#!/bin/bash

# Simple loop for testing combinations of Lucene versions, threads and shared searchers.
# Note that we need to alternate between drives in order to ensure that the disk-cache
# is flushed between tests.

LOCATION[0]=/flash01/full20080109
LOCATION_NAME[0]=SandiskSSD_RAID0
LOCATION[1]=/flash02/full20080109
LOCATION_NAME[1]=MTRONSSD_RAID0
LOCATION[2]=/home/summa/full20080109
LOCATION_NAME[2]=15000RPM_RAID1
LOCATION[3]=/scratch01/full20080109
LOCATION_NAME[3]=10000RPM_RAID0

QUERIES=allQueries20070815.txt
MEM=8GB
ISIZE=37

for lucene in 23; do
    if [ ! -f "/flash01/full20080109/$lucene" ]; then
	echo "Copying Lucene $lucene index to Sandisk SSD"
	rm /flash01/full20080109/*
	cp /scratch01/full20080109_$lucene/* /flash01/full20080109
	echo "Copying Lucene $lucene index to MTRON SSD"
	rm /flash02/full20080109/*
	cp /scratch01/full20080109_$lucene/* /flash02/full20080109
	echo "Copying Lucene $lucene index to 15000 RPM"
	rm /home/summa/full20080109/*
	cp /scratch01/full20080109_$lucene/* /home/summa/full20080109
    fi

    echo "Starting tests"
    for threads in 1 2 3 4 5; do
	for shared in "no" "yes"; do
	    for loc in 0 1 2; do
		location=${LOCATION[$loc]}
		location_name=${LOCATION_NAME[$loc]}
		echo "***** Lucene:$lucene Threads:$threads Shared:$shared Storage:$location_name"
		if [ $shared = "yes" ]; then
		    if [ $threads = 1 ] || [ $threads = 5 ]; then
			echo "Skipping threads $threads and shared yes"
		    else
			./go$lucene\.sh -u -t $threads $location\_$lucene $QUERIES > metis_$location_name\_$MEM\_i$ISIZE\_v$lucene\_t$threads\u_l$lucene.log
		    fi
		else
			./go$lucene\.sh -t $threads $location\_$lucene $QUERIES > metis_$location_name\_$MEM\_i$ISIZE\_v$lucene\_t$threads\_l$lucene.log
		fi
	    done
	done
    done
done
echo "Finished tests"
