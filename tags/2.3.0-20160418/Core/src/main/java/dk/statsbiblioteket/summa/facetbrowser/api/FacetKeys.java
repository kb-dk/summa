/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * Copyright 2007 Statsbiblioteket, Denmark
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Interface defining the search keys used by Summa's {@code BrowserImpl}.
 * </p><p>
 * The FacetBrowser piggy-backs on a DocumentSearcher, from which the query
 * and filter is used. See {@link DocumentKeys} for details.
 * </p><p>
 * Note: in order to enable the generation of docIDs for the FacetBrowser,
 * the DocumentSearcher needs to have the property
 * {@link DocumentKeys#SEARCH_COLLECT_DOCIDS} set to true.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetKeys {

    /**
     * A comma-separated list with the names of the wanted Facets.
     * Optionally, the maximum Tag-count for a given Facet can be specified in
     * parenthesis after the name.
     * Example: "Title, Author (5), City (10), Year".
     * If no maximum Tag-count is specified, the number is taken from the
     * defaults.
     * Optionally, the sort-type for a given Facet can be specified in the same
     * parenthesis. Valid values are {@link FacetStructure#SORT_POPULARITY} and
     * {@link FacetStructure#SORT_ALPHA}. If no sort-type is specified, the
     * number is taken from the defaults.
     * Example: "Title (ALPHA), Author (5 POPULARITY), City"
     * </p><p>
     * This is all optional. If no facets are specified, the default facets
     * are requested.
     */
    // TODO: Add reverse option
    public static final String SEARCH_FACET_FACETS = "search.facet.facets";

    /* The facet range is modelled closely to Solr:
     * https://wiki.apache.org/solr/SimpleFacetParameters#Facet_by_Range
     */

    /**
     * A comma-separated list of fields to perform range faceting on.
     * </p><p>
     * Optional.
     */
    public static final String FACET_RANGE = "facet.range";

    /**
     * Description from Solr:<br/>
     * The lower bound of the ranges.<br/>
     * This parameter can be specified on a per field basis.<br/>
     *
     * Example: {@code f.price.facet.range.start=0.0&f.age.facet.range.start=10}
     */
    public static final String FACET_RANGE_START = "facet.range.start";

    /**
     * Description from Solr:<br/>
     * The upper bound of the ranges.<br/>
     * This parameter can be specified on a per field basis.<br/>
     *
     * Example: {@code f.price.facet.range.end=1000.0&f.age.facet.range.start=99}
     */
    public static final String FACET_RANGE_END = "facet.range.end";

    /**
     * Description from Solr:<br/>
     * The size of each range expressed as a value to be added to the lower bound.
     * For date fields, this should be expressed using the DateMathParser syntax.
     * (ie: facet.range.gap=%2B1DAY, decoded = "+1DAY", URL encoding uses a normal
     * plus sign as a space, so passing a plus to Solr request URL Hex encoding as %2B )
     * </p><p>
     * This parameter can be specified on a per field basis.
     *
     * Example: f.price.facet.range.gap=100&f.age.facet.range.gap=10
     */
    public static final String FACET_RANGE_GAP = "facet.range.gap";

    /**
     * A request for a faceting structure as defined by FacetRequest.xsd in
     * LUCENE-2369 (aka exposed). The return format is FacetResponse.xsd.
     * </p><p>
     * Sample request: {@code
     * <?xml version="1.0" encoding="UTF-8" ?>
     * <facetrequest xmlns="http://lucene.apache.org/exposed/facet/request/1.0" maxtags="30" mincount="1">
     * <query>freetext:"foo &lt;&gt; bar &amp; zoo"</query>
     * <groups>
     * <group name="title" order="locale" locale="da">
     * <fields>
     * <field name="title"/>
     * <field name="subtitle"/>
     * </fields>
     * </group>
     * <group name="author" order="index">
     * <fields>
     * <field name="name"/>
     * </fields>
     * </group>
     * <group name="material" order="count" mincount="0" maxtags="-1">
     * <fields>
     * <field name="materialetype"/>
     * <field name="type"/>
     * </fields>
     * </group>
     * <group name="place">
     * <fields>
     * <field name="position"/>
     * </fields>
     * </group>
     * <!-- A hierarchical group where tags on the first level are sorted by
     * count while tags on subsequent levels are sorted by index order.
     * <p/>
     * -->
     * <group name="depth" mincount="0" order="index" maxtags="5" hierarchical="true" levels="5" delimiter="/">
     * <fields>
     * <field name="classification"/>
     * </fields>
     * <subtags suborder="count" maxtags="10" mintotalcount="1">
     * <subtags suborder="base" maxtags="5"/>
     * </subtags>
     * </group>
     * </groups>
     * </facetrequest>
     * }. Sample response: {@code
     * <?xml version='1.0' encoding='utf-8'?>
     * <facetresponse xmlns="http://lucene.apache.org/exposed/facet/response/1.0" query="all" hits="4911501"
     * totalms="1284" countms="999">
     * <facet name="id" fields="id" order="count" maxtags="5" mincount="0" offset="0" prefix="" extractionms="266">
     * <subtags potentialtags="10000000" totaltags="10000000" count="4911501">
     * <tag count="1" term="00000006"/>
     * <tag count="1" term="00000002"/>
     * <tag count="1" term="00000000"/>
     * <tag count="1" term="00000004"/>
     * <tag count="1" term="09999998"/>
     * </subtags>
     * </facet>
     * <facet name="custom" fields="a" order="locale" locale="da" maxtags="5" mincount="0" offset="0" prefix="a_foo"
     * extractionms="1">
     * <subtags potentialtags="-1" totaltags="-1" count="2311453">
     * <tag count="0" term="a_ fOO01991201"/>
     * <tag count="0" term="a_FOO0qVi Pljvjbæ9v5578717"/>
     * <tag count="0" term="a_FoO1725831"/>
     * <tag count="1" term="a_ foo18a01AøoCf 4518220"/>
     * <tag count="1" term="a_FOO1hzW5450992"/>
     * </subtags>
     * </facet>
     * <facet name="random" fields="evennull" order="locale" locale="da" maxtags="5" mincount="1" offset="0"
     * prefix="" extractionms="18">
     * </facet>
     * <facet name="multi" fields="facet" order="index" maxtags="5" mincount="0" offset="-2" prefix="F"
     * extractionms="0">
     * <subtags potentialtags="-1" totaltags="-1" count="12345678">
     * <tag count="465820" term="D"/>
     * <tag count="467009" term="E"/>
     * <tag count="465194" term="F"/>
     * <tag count="465960" term="G"/>
     * <tag count="464783" term="H"/>
     * </subtags>
     * </facet>
     * <facet name="depth" fields="classification" order="index" maxtags="5" mincount="0" hierarchical="true"
     * levels="5" delimiter="/" extractionms="0">
     * <subtags mincount="0" mintotalcount="10" suborder="count" potentialtags="-1" totaltags="-1" count="20345654">
     * <tag count="465820" term="D"/>
     * <tag count="467009" term="E"/>
     * <tag count="465194" term="F"/>
     * <tag count="465960" term="G">
     * <subtags suborder="base" maxtags="5" totaltags="1" count="460">
     * <tag count="460" term="K"/>
     * </subtags>
     * </tag>
     * <tag count="464783" term="H"/>
     * </subtags>
     * </facet>
     * </facetresponse>
     * }
     */
    public static final String SEARCH_FACET_XMLREQUEST = "search.facet.xmlrequest";
}
