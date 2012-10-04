#!/bin/sh

# Checks out Lucene 4 BETA in preparation for patch creation
rm -rf lucene_4

svn checkout https://svn.apache.org/repos/asf/lucene/dev/tags/lucene_solr_4_0_0_BETA/ lucene_4

# Enable this when Lucene/Solr 4.0 goes gold
#svn checkout http://svn.apache.org/repos/asf/lucene/dev/branches/lucene_solr_4_0/ lucene_4
