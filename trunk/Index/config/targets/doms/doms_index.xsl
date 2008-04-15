<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:oai="http://www.openarchives.org/OAI/2.0/"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                version="1.0"
                exclude-result-prefixes="xs xsl dc oai oai_dc">
    <!--xmlns:util="http://xml.apache.org/xalan/java/dk.statsbiblioteket.doms.disseminator.Util"-->

    <!-- Default disseminator for digital object bundles.
         This disseminator will make the most trivial dissemintaiton to indexing
         documents:
         Short record elements will be generated from the first digital object
         only.
         All elements in all DomsDC oai:datastreams will be indexed, for all
         included documents in the bundle. However, only the first object will
         be used for main_titel
    -->
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <xsl:apply-templates select="oai:record/oai:metadata"/>
    </xsl:template>

    <xsl:template match="oai:metadata">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                        Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="doms" Index:id="{oai:digitalObjectBundle/oai:digitalObject/@PID}">
            <Index:fields>
                <xsl:for-each select="oai:digitalObjectBundle">
                    <!-- Short format -->
                    <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                        <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                        <shortrecord>
                            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                                <rdf:Description rdf:about="{oai:digitalObject/@PID}">
                                    <dc:title><xsl:value-of select="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:title"/></dc:title>
                                    <dc:creator><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:creator"/></dc:creator>
                                    <xsl:choose>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:issued">
                                            <!-- TODO: Check format of date -->
                                            <dc:date><xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:issued, 1, 4)"/></dc:date>
                                        </xsl:when>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:created">
                                            <!-- TODO: Check format of date -->
                                            <dc:date><xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:created, 1, 4)"/></dc:date>
                                        </xsl:when>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:date">
                                            <!-- TODO: Check format of date -->
                                            <dc:date><xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:date, 1, 4)"/></dc:date>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <dc:date></dc:date>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:choose>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[@xml:lang='da']">
                                            <dc:type xml:lang="da"><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[@xml:lang='da']"/></dc:type>
                                        </xsl:when>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[not(@xml:lang)]">
                                            <dc:type xml:lang="da"><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[not(@xml:lang)]"/></dc:type>
                                        </xsl:when>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type">
                                            <dc:type xml:lang="da"><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type"/></dc:type>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <dc:type xml:lang="da">Netdokument</dc:type>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:choose>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[@xml:lang='en']">
                                            <dc:type xml:lang="en"><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[@xml:lang='en']"/></dc:type>
                                        </xsl:when>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[not(@xml:lang)]">
                                            <dc:type xml:lang="en"><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type[not(@xml:lang)]"/></dc:type>
                                        </xsl:when>
                                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type">
                                            <dc:type xml:lang="en"><xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:type"/></dc:type>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <dc:type xml:lang="en">Net document</dc:type>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <!--TODO: Must be PID in storage-->
                                    <dc:identifier><xsl:value-of select="oai:digitalObject/@PID"/></dc:identifier>
                                </rdf:Description>
                            </rdf:RDF>
                        </shortrecord>
                        <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                    </Index:field>

                    <!-- Title group -->
                    <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
                        <xsl:for-each select="oai:digitalObject[position() = 1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:title">
                            <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:title">
                            <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:alternative">
                            <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <!-- Potential fields: se -->
                    </Index:group>

                    <!-- Author group -->
                    <Index:group Index:name="au" Index:navn="fo" Index:suggest="false">
                        <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:creator">
                            <Index:field Index:name="author_main" Index:repeat="true" Index:navn="po" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:publisher">
                            <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:contributor">
                            <Index:field Index:repeat="true" Index:name="au_other" Index:navn="fo_andet" Index:type="token">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <!-- Potential fields: author_person -->
                    </Index:group>

                    <!-- Subject group -->
                    <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                        <!-- TODO: Check for qualified -->
                        <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:subject" >
                            <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <!-- Potential fields: su_dk, lsu_oai, subject_controlled, subject_dk5, subject_serial_solutions, subject_ulrichs, commercials_subject -->
                    </Index:group>

                    <!-- Material type group -->
                    <Index:group Index:name="lma" Index:navn="lma" Index:suggest="true">
                        <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                            <!-- TODO: Check correctness -->
                            <xsl:value-of select="Netdokument" />
                        </Index:field>
                    </Index:group>

                    <!-- Material type group -->
                    <Index:group Index:name="ma" Index:navn="ma" Index:suggest="true">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="keyword">
                            <xsl:value-of select="xe" />
                        </Index:field>
                    </Index:group>

                    <!-- Potential groups: cl, lsubj, lcl, numbers -->

                    <!-- Single fields -->

                    <!-- Language -->
                    <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                        <xsl:value-of select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:language" />
                    </Index:field>

                    <!-- FreeText -->
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:description">
                        <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:abstract">
                        <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:tableOfContents">
                        <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
 <!--
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="other" Index:type="token">
                        <xsl:value-of select="util:decodeBase64(oai:digitalObject/oai:datastream[@ID='CONTENT']/oai:datastreamVersion/oai:xmlContent/oai:content)" />
                    </Index:field>
 -->

                    <!-- Author normalised -->
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:creator">
                        <Index:field Index:repeat="true" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                            <!-- TODO: Normalising is probably better defined elsewhere. -->
                            <xsl:value-of select="translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÁÉÍÓÚÝÑÄÖÜ½§!#¤%/()=?`+£${[]}|^~*,.-;:_\&quot;&amp;&lt;&gt;', 'abcdefghijklmnopqrstuvwxyzæøåáéíóúýñäöü')" />
                        </Index:field>
                    </xsl:for-each>

                    <!-- Sort title -->
                    <xsl:for-each
                            select="oai:digitalObject[position() = 1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:title">
                        <Index:field Index:repeat="true" Index:name="sort_title" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="10">
                            <!-- TODO: Normalising is probably better defined elsewhere. -->
                            <xsl:value-of select="translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÁÉÍÓÚÝÑÄÖÜ½§!#¤%/()=?`+£${[]}|^~*,.-;:_\&quot;&amp;&lt;&gt;', 'abcdefghijklmnopqrstuvwxyzæøåáéíóúýñäöü')" />
                        </Index:field>
                    </xsl:for-each>

                    <!-- Date -->
                    <xsl:for-each select="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:issued">
                        <!-- TODO: Check format of date -->
                        <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:created">
                        <!-- TODO: Check format of date -->
                        <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:date">
                        <!-- TODO: Check format of date -->
                        <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>

                    <!-- Date sorting -->
                    <xsl:choose>
                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:issued">
                            <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                <xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:issued, 0, 4)"/>
                            </Index:field>
                            <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                <xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:issued, 0, 4)"/>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:created">
                            <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                <xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:created, 0, 4)"/>
                            </Index:field>
                            <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                <xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/oai:created, 0, 4)"/>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="oai:digitalObject[position()=1]/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:date">
                            <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                <xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:date, 0, 4)"/>
                            </Index:field>
                            <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                <xsl:value-of select="substring(oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:date, 0, 4)"/>
                            </Index:field>
                        </xsl:when>
                    </xsl:choose>

                    <!-- Format -->
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:format">
                        <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:extent">
                        <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="oai:digitalObject/oai:datastream[@ID='DomsDC']/oai:datastreamVersion/oai:xmlContent/oai:qualifieddc/dc:medium">
                        <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>

                    <!-- Potential fields: author_descr, barcode, barcode_normalised, cluster, collection, collection_normalised, format, ip, l_call, lip, llang, location, location_normalised, lso, no, openUrl, original_language, other, place, pu, series_normalised, year -->
                </xsl:for-each>
            </Index:fields>
        </Index:document>
    </xsl:template>
</xsl:stylesheet>