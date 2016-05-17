#!/bin/sh

PATCH=LUCENE-2369_SOLR-2412.patch

cd lucene_4
svn diff > $PATCH
mv $PATCH ../
