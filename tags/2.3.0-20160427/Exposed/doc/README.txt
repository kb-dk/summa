Getting Started
---------------
The Exposed faceting system can be tested with the standard Solr example the following way:

1. Run 'ant example' in the solr folder.
1.1 mkdir example/solr/lib
1.2 cp dist/apache-solr-exposed-4.0-SNAPSHOT.jar example/solr/lib/


2. Start Solr from the example folder with
   java -jar -start.jar


3. Use the Tcl script facet_samples.tcl from contrib/exposed to create sample data:
tclsh ./facet_samples.tcl 100000 2 2 -u 100 100 > 100000.csv


4. Index the sample data with curl:

curl "http://localhost:8983/solr/update/csv?commit=true&optimize=true" --data-binary @100000.csv -H 'Content-type:text/plain; charset=utf-8'


5. Verify that exposed faceting works without hierarchical

http://localhost:8983/solr/select/?qt=exprh&efacet=true&efacet.field=path_ss&q=*%3A*&fl=id&version=2.2&start=0&rows=10&indent=on


10. Verify that exposed faceting works with hierarchical:

http://localhost:8983/solr/select/?qt=exprh&efacet=true&efacet.field=path_ss&efacet.hierarchical=true&q=*%3A*&fl=id&version=2.2&start=0&rows=10&indent=on

11. Play around, but note the show stopper described below.

-----------------------------------------
TODO
-----------------------------------------

This patch is extremely rough around the edges and is more a proof of concept than a real
Solr plugin. While the Lucene-part seems fairly solid, all the Solr-parts are hacked together.

Proper integration with Solr is ongoing. The show stopper as of march 2011 is
* No detection of index changes: Updating the index means that everything fails.
  Relevant wiki pages seems to be
  http://wiki.apache.org/solr/SolrCaching and http://wiki.apache.org/solr/SolrPlugins#CacheRegenerator

Other problems are
* No field-specific parameters, even though the JavaDoc says that they work.
* Bad build scripting as I do not understand the ant files.

- Toke Eskildsen, te@statsbiblioteket.dk
