<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:oai="http://www.openarchives.org/OAI/2.0/"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:d="http://fedora.statsbiblioteket.dk/datatypes/digitalObjectBundle/"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:doms="http://www.statsbiblioteket.dk/doms-relations/"
                version="1.0"
                exclude-result-prefixes="xs xsl dc oai oai_dc">
    <!--xmlns:util="http://xml.apache.org/xalan/java/dk.statsbiblioteket.doms.disseminator.Util"-->

    <!-- Default disseminator for digital object bundles.
         This disseminator will make the most trivial dissemintaiton to indexing
         documents:
         Short record elements will be generated from the first digital object
         only.
         All elements in all DomsDC foxml:datastreams will be indexed, for all
         included documents in the bundle. However, only the first object will
         be used for main_titel
    -->
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <xsl:apply-templates select="oai:record/oai:metadata"/>
    </xsl:template>

    <xsl:template match="oai:metadata">
        <xsl:choose>
            <!--- DANSKE AVISER Papers-->
            <xsl:when test="substring(d:digitalObjectBundle/foxml:digitalObject/@PID,0,15)='doms:dda_paper'">
                <xsl:variable name="paperId">
                    <xsl:value-of select="d:digitalObjectBundle/foxml:digitalObject/@PID" />
                </xsl:variable>
                <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                                Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="doms" Index:id="{d:digitalObjectBundle/foxml:digitalObject/@PID}">
                    <Index:fields>
                        <xsl:for-each select="d:digitalObjectBundle">
                            <!-- Short format -->
                            <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                                <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                                <shortrecord>
                                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                                        <rdf:Description rdf:about="{foxml:digitalObject/@PID}">
                                            <dc:title><xsl:value-of select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title"/></dc:title>

                                            <dc:creator>
                                                <xsl:value-of select="substring-after(foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:creator,', ')"/>
                                                <xsl:value-of select="substring-before(foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:creator,',')"/>
                                            </dc:creator>

                                            <xsl:choose>
                                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                                    <!-- TODO: Check format of date -->
                                                    <dc:date><xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 1, 4)"/></dc:date>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                                    <!-- TODO: Check format of date -->
                                                    <dc:date><xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 1, 4)"/></dc:date>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                                    <!-- TODO: Check format of date -->
                                                    <dc:date><xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 1, 4)"/></dc:date>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <dc:date> </dc:date>
                                                </xsl:otherwise>

                                            </xsl:choose>

                                            <xsl:choose>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='da']">
                                                    <dc:type xml:lang="da"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='da']"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]">
                                                    <dc:type xml:lang="da"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type">
                                                    <dc:type xml:lang="da"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type"/></dc:type>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:choose>
                                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:aarbog')">
                                                            <dc:type xml:lang="da">Digital &#xE5;rbog</dc:type>
                                                        </xsl:when>
                                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:dda_paper')">
                                                            <dc:type xml:lang="da">De Danske Aviser</dc:type>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <dc:type xml:lang="da">Netdokument</dc:type>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                            <xsl:for-each select=".">
                                                <dc:format>todo</dc:format>
                                            </xsl:for-each>
                                            <xsl:choose>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='en']">
                                                    <dc:type xml:lang="en"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='en']"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]">
                                                    <dc:type xml:lang="en"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type">
                                                    <dc:type xml:lang="en"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type"/></dc:type>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:choose>
                                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:aarbog')">
                                                            <dc:type xml:lang="en">Digital Yearbook</dc:type>
                                                        </xsl:when>
                                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:dda_paper')">
                                                            <dc:type xml:lang="en">The Danish Papers</dc:type>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <dc:type xml:lang="en">Net document</dc:type>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:otherwise>
                                            </xsl:choose>

                                            <!--TODO: Must be PID in storage-->
                                            <dc:identifier>
                                                <xsl:value-of select="foxml:digitalObject/@PID"/>
                                            </dc:identifier>

                                        </rdf:Description>
                                    </rdf:RDF>
                                </shortrecord>
                                <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                            </Index:field>

                            <!-- Title group -->
                            <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
                                <xsl:for-each select="foxml:digitalObject[position() = 1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title">
                                    <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title">
                                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:alternative">
                                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:title">
                                    <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: se -->
                            </Index:group>

                            <!-- Author group -->
                            <Index:group Index:name="au" Index:navn="fo" Index:suggest="false">
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:creator">
                                    <Index:field Index:name="author_main" Index:repeat="true" Index:navn="po" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:publisher">
                                    <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:contributor">
                                    <Index:field Index:repeat="true" Index:name="au_other" Index:navn="fo_andet" Index:type="token">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:creator">
                                    <Index:field Index:name="author_main" Index:repeat="true" Index:navn="po" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/
                                dc:publisher">
                                    <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: author_person -->
                            </Index:group>

                            <!-- Subject group -->
                            <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                                <!-- TODO: Check for qualified -->
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:subject" >
                                    <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: su_dk, lsu_oai, subject_controlled, subject_dk5, subject_serial_solutions, subject_ulrichs, commercials_subject -->
                            </Index:group>

                            <Index:group Index:name="lsubj" Index:navn="lem" Index:suggest="true">
                                <!-- TODO: Check for qualified -->
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:subject" >
                                    <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                    <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsubject" Index:name="lsubject" Index:repeat="false">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: su_dk, lsu_oai, subject_controlled, subject_dk5, subject_serial_solutions, subject_ulrichs, commercials_subject -->
                            </Index:group>

                            <!-- Material type group -->
                            <Index:group Index:name="lma" Index:navn="lma" Index:suggest="true">
                                <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                                    <xsl:text>doms</xsl:text>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                    <xsl:choose>
                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:aarbog')">
                                            <xsl:value-of select="'digitalaarbog'" />
                                        </xsl:when>
                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:dda_paper')">
                                            <xsl:value-of select="'dedanskeaviser'" />
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="'netdokument'" />
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </Index:field>
                            </Index:group>

                            <!-- Material type group -->
                            <Index:group Index:name="ma" Index:navn="ma" Index:suggest="true">
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="keyword">
                                    <xsl:value-of select="'xe'" />
                                </Index:field>
                            </Index:group>

                            <!-- Potential groups: cl, lsubj, lcl, numbers -->

                            <!-- Single fields -->

                            <!-- Language -->
                            <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                                <xsl:value-of select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:language" />
                            </Index:field>

                            <!-- FreeText -->
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:abstract">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:tableOfContents">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/danskeaviser/titles/text">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:description">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/danskeaviser/text/txt">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <!--
                                <Index:field Index:repeat="true" Index:name="other" Index:navn="other" Index:type="token">
                                    <xsl:value-of select="util:decodeBase64(foxml:digitalObject/foxml:datastream[@ID='CONTENT']/foxml:datastreamVersion/foxml:xmlContent/oai:content)" />
                                 </Index:field>
                            -->

                            <!-- Author normalised -->
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:creator">
                                <Index:field Index:repeat="true" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                                    <!-- TODO: Normalising is probably better defined elsewhere. -->
                                    <xsl:value-of select="translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÁÉÍÓÚÝÑÄÖÜ½§!#¤%/()=?`+£${[]}|^~*,.-;:_\&quot;&amp;&lt;&gt;', 'abcdefghijklmnopqrstuvwxyzæøåáéíóúýñäöü')" />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Sort title -->
                            <xsl:for-each
                                    select="foxml:digitalObject[position() = 1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title">
                                <Index:field Index:repeat="true" Index:name="sort_title" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="10">
                                    <!-- TODO: Normalising is probably better defined elsewhere. -->
                                    <xsl:value-of select="translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÁÉÍÓÚÝÑÄÖÜ½§!#¤%/()=?`+£${[]}|^~*,.-;:_\&quot;&amp;&lt;&gt;', 'abcdefghijklmnopqrstuvwxyzæøåáéíóúýñäöü')" />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Date -->
                            <xsl:for-each select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                <!-- TODO: Check format of date -->
                                <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                <!-- TODO: Check format of date -->
                                <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                <!-- TODO: Check format of date -->
                                <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Date sorting -->
                            <xsl:choose>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 0, 4)"/>
                                    </Index:field>
                                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 0, 4)"/>
                                    </Index:field>
                                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 0, 4)"/>
                                    </Index:field>
                                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>
                            <!-- Date limiting -->
                            <!-- Important: The namespace for created is an estimated guess -->
                            <xsl:choose>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                    <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                    <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                    <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>

                            <!-- Format -->
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:format">
                                <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:extent">
                                <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:medium">
                                <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>

                            <!--
                            Henvisning til firstpage, hvor referencen til png filen findes.
                            Dette er IKKE summa men for at hive URL'en ud et eller andet sted indtil
                            den endelige metode findes.

                            <xsl:for-each select="foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description[@rdf:about=concat('info:fedora/',$paperId)]/doms:firstPage/@rdf:resource">
                                <Index:field Index:name="henvisning-til-firstpage-hvor-png-fil-referencen ligger">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            -->


                        </xsl:for-each>
                    </Index:fields>
                </Index:document>
            </xsl:when>


            <!--- DANSKE AVISER Pages-->
            <!-- Illegal XML for at undgå indeksering af Danske Aviser Pages, der ikke indeholder data, der skal være søgbare -->
            <xsl:when test="substring(d:digitalObjectBundle/foxml:digitalObject/@PID,0,14)='doms:dda_page'">
                <xsl:text>Id: </xsl:text>
                <xsl:value-of select="d:digitalObjectBundle/foxml:digitalObject/@PID" />
                <xsl:text>&#xa;Url: </xsl:text>
                <xsl:value-of select="d:digitalObjectBundle/foxml:digitalObject/foxml:datastream/foxml:datastreamVersion/foxml:contentLocation/@REF" />
            </xsl:when>



            <!-- ÅRBOEGER -->
            <xsl:otherwise>
                <xsl:for-each select="d:digitalObjectBundle/foxml:digitalObject">
                    <xsl:if test="position()=1">
                        <xsl:value-of select="@PID" />
                    </xsl:if>
                </xsl:for-each>
                <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                                Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="doms" Index:id="{foxml:digitalObjectBundle/foxml:digitalObject/@PID}">
                    <Index:fields>
                        <xsl:for-each select="d:digitalObjectBundle">
                            <!-- Short format -->
                            <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                                <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                                <shortrecord>
                                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                                        <rdf:Description rdf:about="{foxml:digitalObject/@PID}">
                                            <dc:title><xsl:value-of select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title"/></dc:title>
                                            <!--                  <dc:creator><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:creator"/></dc:creator> -->
                                            <xsl:choose>
                                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                                    <!-- TODO: Check format of date -->
                                                    <dc:date><xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 1, 4)"/></dc:date>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                                    <!-- TODO: Check format of date -->
                                                    <dc:date><xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 1, 4)"/></dc:date>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                                    <!-- TODO: Check format of date -->
                                                    <dc:date><xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 1, 4)"/></dc:date>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <dc:date> </dc:date>
                                                </xsl:otherwise>

                                            </xsl:choose>

                                            <xsl:choose>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='da']">
                                                    <dc:type xml:lang="da"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='da']"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]">
                                                    <dc:type xml:lang="da"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type">
                                                    <dc:type xml:lang="da"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type"/></dc:type>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:choose>
                                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:aarbog')">
                                                            <dc:type xml:lang="da">Digital &#xE5;rbog</dc:type>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <dc:type xml:lang="da">Netdokument</dc:type>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                            <xsl:for-each select=".">
                                                <dc:format>todo</dc:format>
                                            </xsl:for-each>
                                            <xsl:choose>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='en']">
                                                    <dc:type xml:lang="en"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[@xml:lang='en']"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]">
                                                    <dc:type xml:lang="en"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type[not(@xml:lang)]"/></dc:type>
                                                </xsl:when>
                                                <xsl:when test="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type">
                                                    <dc:type xml:lang="en"><xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:type"/></dc:type>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:choose>
                                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:aarbog')">
                                                            <dc:type xml:lang="en">Digital Yearbook</dc:type>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <dc:type xml:lang="en">Net document</dc:type>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:otherwise>
                                            </xsl:choose>

                                            <!--TODO: Must be PID in storage-->
                                            <dc:identifier><xsl:value-of select="foxml:digitalObject/@PID"/></dc:identifier>

                                        </rdf:Description>
                                    </rdf:RDF>
                                </shortrecord>
                                <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                            </Index:field>

                            <!-- Title group -->
                            <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
                                <xsl:for-each select="foxml:digitalObject[position() = 1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title">
                                    <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title">
                                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:alternative">
                                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: se -->
                            </Index:group>

                            <!-- Author group -->
                            <!--    <Index:group Index:name="au" Index:navn="fo" Index:suggest="false">
                         <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:creator">
                             <Index:field Index:name="author_main" Index:repeat="true" Index:navn="po" Index:type="token" Index:boostFactor="10">
                                 <xsl:value-of select="."/>
                             </Index:field>
                         </xsl:for-each>
                         <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:publisher">
                             <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                                 <xsl:value-of select="."/>
                             </Index:field>
                         </xsl:for-each>
                         <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:contributor">
                             <Index:field Index:repeat="true" Index:name="au_other" Index:navn="fo_andet" Index:type="token">
                                 <xsl:value-of select="."/>
                             </Index:field>
                         </xsl:for-each>  -> -->
                            <!-- Potential fields: author_person -->
                            <!--             </Index:group>   -->

                            <!-- Subject group -->
                            <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                                <!-- TODO: Check for qualified -->
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:subject" >
                                    <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: su_dk, lsu_oai, subject_controlled, subject_dk5, subject_serial_solutions, subject_ulrichs, commercials_subject -->
                            </Index:group>

                            <Index:group Index:name="lsubj" Index:navn="lem" Index:suggest="true">
                                <!-- TODO: Check for qualified -->
                                <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:subject" >
                                    <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                    <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsubject" Index:name="lsubject" Index:repeat="false">
                                        <xsl:value-of select="."/>
                                    </Index:field>
                                </xsl:for-each>
                                <!-- Potential fields: su_dk, lsu_oai, subject_controlled, subject_dk5, subject_serial_solutions, subject_ulrichs, commercials_subject -->
                            </Index:group>

                            <!-- Material type group -->
                            <Index:group Index:name="lma" Index:navn="lma" Index:suggest="true">
                                <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                                    <xsl:text>doms</xsl:text>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                    <xsl:choose>
                                        <xsl:when test="starts-with(foxml:digitalObject/@PID, 'doms:aarbog')">
                                            <xsl:value-of select="'digitalaarbog'" />
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="'netdokument'" />
                                        </xsl:otherwise>
                                    </xsl:choose>
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
                                <xsl:value-of select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:language" />
                            </Index:field>

                            <!-- FreeText -->
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:description">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:abstract">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:tableOfContents">
                                <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <!--
                                               <Index:field Index:repeat="true" Index:name="other" Index:navn="other" Index:type="token">
                                                   <xsl:value-of select="util:decodeBase64(foxml:digitalObject/foxml:datastream[@ID='CONTENT']/foxml:datastreamVersion/foxml:xmlContent/oai:content)" />
                                               </Index:field>
                            -->

                            <!-- Author normalised -->
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:creator">
                                <Index:field Index:repeat="true" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                                    <!-- TODO: Normalising is probably better defined elsewhere. -->
                                    <xsl:value-of select="translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÁÉÍÓÚÝÑÄÖÜ½§!#¤%/()=?`+£${[]}|^~*,.-;:_\&quot;&amp;&lt;&gt;', 'abcdefghijklmnopqrstuvwxyzæøåáéíóúýñäöü')" />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Sort title -->
                            <xsl:for-each
                                    select="foxml:digitalObject[position() = 1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:title">
                                <Index:field Index:repeat="true" Index:name="sort_title" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="10">
                                    <!-- TODO: Normalising is probably better defined elsewhere. -->
                                    <xsl:value-of select="translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅÁÉÍÓÚÝÑÄÖÜ½§!#¤%/()=?`+£${[]}|^~*,.-;:_\&quot;&amp;&lt;&gt;', 'abcdefghijklmnopqrstuvwxyzæøåáéíóúýñäöü')" />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Date -->
                            <xsl:for-each select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                <!-- TODO: Check format of date -->
                                <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                <!-- TODO: Check format of date -->
                                <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject[position()=1]/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                <!-- TODO: Check format of date -->
                                <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="2">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Date sorting -->
                            <xsl:choose>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 0, 4)"/>
                                    </Index:field>
                                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 0, 4)"/>
                                    </Index:field>
                                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 0, 4)"/>
                                    </Index:field>
                                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>
                            <!-- Date limiting -->
                            <!-- Important: The namespace for created is an estimated guess -->
                            <xsl:choose>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued">
                                    <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:issued, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created">
                                    <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dcterms:created, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date">
                                    <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="1">
                                        <xsl:value-of select="substring(foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:date, 0, 4)"/>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>

                            <!-- Format -->
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:format">
                                <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:extent">
                                <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="foxml:digitalObject/foxml:datastream[@ID='DomsDC']/foxml:datastreamVersion/foxml:xmlContent/dcterms:qualifieddc/dc:medium">
                                <Index:field Index:name="format" Index:navn="format" Index:type="token" Index:boostFactor="1">
                                    <xsl:value-of select="." />
                                </Index:field>
                            </xsl:for-each>

                            <!-- Potential fields: author_descr, barcode, barcode_normalised, cluster, collection, collection_normalised, format, ip, l_call, lip, llang, location, location_normalised, lso, no, openUrl, original_language, other, place, pu, series_normalised, year -->
                        </xsl:for-each>
                    </Index:fields>
                </Index:document>

            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>