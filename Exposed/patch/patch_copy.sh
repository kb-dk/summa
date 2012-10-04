#!/bin/bash

# Copies Exposed files from Summa to Lucene 4 BETA checkout
EXP=..

echo "Copying and moving Lucene files"
LUCENE=lucene_4/lucene/exposed
rm -rf $LUCENE
mkdir -p $LUCENE
cp -r $EXP/src $LUCENE/
find $LUCENE/ -name ".svn" -exec rm -rf {} \; 2> /dev/null
rm -rf $LUCENE/src/main/java/org/apache/solr
pushd $LUCENE/src > /dev/null
mv main/* .
rm -r main
cd test
mv java/* .
mv resources ../test-files
rm -r java
rm -rf org/apache/solr/
popd > /dev/null
svn add --depth infinity $LUCENE

echo "Copying and moving Solr files"
SOLR=lucene_4/solr/contrib/exposed
rm -rf $SOLR
mkdir -p $SOLR
cp -r $EXP/src $SOLR/
find $SOLR/ -name ".svn" -exec rm -rf {} \;  2> /dev/null
rm -rf $SOLR/src/main/java/org/apache/lucene
pushd $SOLR/src > /dev/null
mv main/* .
rm -r main
cd test
mv java/* .
mv resources ../test-files
rm -r java
popd > /dev/null
svn add --depth infinity $SOLR

# We need to prune .svn
TMP=tmp_delete
rm -rf $TMP
mkdir $TMP
cp -r -f build_files/* $TMP/
find $TMP/ -name ".svn" -exec rm -rf {} \;  2> /dev/null
cp -r -f $TMP/* lucene_4/
rm -rf $TMP

cp ../scripts/facet_samples.tcl lucene_4/solr/example/
cp ../README_EXPOSED_SOLR.txt lucene_4/solr/contrib/exposed/README.txt

echo "Patching build-files for Lucene and Solr"
# Insert appropriate calls in the common-build.xml for Lucene
pushd $LUCENE > /dev/null
cd ..
patch -N module-build.xml patch_module-build 
popd > /dev/null

# Insert appropriate calls in the build.xml and common-build.xml for solr
pushd $SOLR > /dev/null
cd ../..
# lucene_4/solr
patch -N common-build.xml patch_common-build
patch -N build.xml patch_build

echo "Patching Solr example solrconfig file"
cd example/solr/collection1/conf
pwd
patch -N solrconfig.xml patch_solrconfig
