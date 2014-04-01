#!/bin/sh

# Checks out Lucene 4 in preparation for patch creation
rm -rf lucene_4

svn checkout https://svn.apache.org/repos/asf/lucene/dev/tags/lucene_solr_4_6_1/ lucene_4

#svn checkout https://svn.apache.org/repos/asf/lucene/dev/tags/lucene_solr_4_0_0_BETA/ lucene_4

