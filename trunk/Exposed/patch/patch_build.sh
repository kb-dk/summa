#!/bin/sh

cd lucene_4/lucene

# Do we really need to do this?
ant dist
cd ../solr
ant example

