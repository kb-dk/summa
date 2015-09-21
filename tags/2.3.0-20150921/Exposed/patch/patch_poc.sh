#!/bin/sh

java -cp lucene_4/lucene/build/core/lucene-core-4.0-SNAPSHOT.jar:lucene_4/lucene/build/queries/lucene-queries-4.0-SNAPSHOT.jar:lucene_4/lucene/build/queryparser/lucene-queryparser-4.0-SNAPSHOT.jar:lucene_4/lucene/build/analysis/common/lucene-analyzers-common-4.0-SNAPSHOT.jar:lucene_4/lucene/build/analysis/icu/lucene-analyzers-icu-4.0-SNAPSHOT.jar:lucene_4/lucene/build/exposed/lucene-exposed-4.0-SNAPSHOT.jar:lucene_4/lucene/analysis/icu/lib/icu4j-49.1.jar org.apache.lucene.search.exposed.poc.ExposedPOC $@
