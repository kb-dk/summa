Getting Started
---------------
The Exposed faceting system can be tested with the standard Solr example the following way:

1. Run 'ant run-example' in the solr folder.


2. Start Solr from the example folder with
   java -jar -start.jar


3. Use the Tcl script facet_samples.tcl from contrib/exposed to create sample data:
tcl ./facet_samples.tcl 100000 2 2 4 > 100000.csv


4. Index the sample data with curl:
curl "http://localhost:8983/solr/update/csv?commit=true&optimize=true" --data-binary @100000.csv -H 'Content-type:text/csv; charset=utf-8'


5. Verify that the data is indexed with Solr's standard non-hierarchical faceting
http://localhost:8983/solr/select/?facet=true&facet.field=path_ss&q=*%3A*&fl=id&indent=on
A large facet output for path_sss hould be present.


6. Verify that exposed faceting works without hierarchical
http://localhost:8983/solr/exposed?efacet=true&efacet.field=path_ss&q=*%3A*&fl=id&indent=on
A large efacet output for path_sss hould be present.


7. Verify that exposed faceting works with hierarchical:
http://localhost:8983/solr/exposed?efacet=true&efacet.field=path_ss&efacet.hierarchical=true&q=*%3A*&fl=id&indent=on
A large hierarchical efacet output for path_sss hould be present.



-----------------------------------------
TODO
-----------------------------------------

This patch is slowly getting into shape, but should still be seen primarily as a proof of
concept.

Proper integration with Solr is ongoing. Known problems are
* No field-specific parameters, even though the JavaDoc says that they work.


- Toke Eskildsen, te@statsbiblioteket.dk
