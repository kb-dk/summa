<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:oai="http://www.openarchives.org/OAI/2.0/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:Index="http://statsbiblioteket.dk/2004/Index"
        xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xalan="http://xml.apache.org/xalan"
        xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl dc oai_dc oai"
        version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">



    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                        Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="http:www.statsbiblioteket.dk/tictoc">
            <xsl:attribute name="Index:id">
                <xsl:value-of select="rss/channel/item/link"/>
            </xsl:attribute>

            <xsl:attribute name="Index:resolver">
                <xsl:value-of select="'tictoc'"/>
            </xsl:attribute>
            <xsl:for-each select="rss/channel/item">
                <Index:fields>
                    <xsl:call-template name="shortformat" />
                    <xsl:call-template name="author" />
                    <xsl:call-template name="title" />
                    <xsl:call-template name="date" />
                    <xsl:call-template name="notes" />
                    <xsl:call-template name="identifiers" />
                    <xsl:call-template name="ma" />
                    <!--
                    <xsl:call-template name="language" />
                    <xsl:call-template name="su" />



           <xsl:call-template name="publisher" />       -->



                    <Index:field Index:name="openUrl" Index:navn="openUrl"  Index:type="stored" Index:freetext="false">id=<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.OpenUrlEscape.escape(link)" /></Index:field>

                </Index:fields>
            </xsl:for-each>


        </Index:document>
    </xsl:template>

    <!-- Shortformat -->
    <xsl:template name="shortformat">

        <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
            <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
            <shortrecord>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <rdf:Description>
                        <xsl:for-each select="title">
                            <dc:title>
                                <xsl:value-of select="."/>
                            </dc:title>
                        </xsl:for-each>

                        <xsl:for-each select="author">
                            <dc:creator>
                                <xsl:value-of select="."/>
                            </dc:creator>
                        </xsl:for-each>

                        <xsl:for-each select="pubDate">
                            <dc:date>
                                <xsl:value-of select="substring(.,string-length(.)-3)" />
                            </dc:date>
                        </xsl:for-each>

                        <dc:type xml:lang="da">
                            <xsl:text>netdokument</xsl:text>
                        </dc:type>
                        <dc:type xml:lang="da">
                            <xsl:text>net document</xsl:text>
                        </dc:type>

                        <xsl:for-each select="link">
                            <dc:identifier>
                                <xsl:value-of select="."/>
                            </dc:identifier>
                        </xsl:for-each>

                    </rdf:Description>
                </rdf:RDF>
            </shortrecord>
            <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </Index:field>

    </xsl:template>

    <!-- Author -->
    <xsl:template name="author">
        <Index:group Index:name="au" Index:navn="fo">

            <xsl:for-each select="author">
                <Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

        </Index:group>

        <xsl:for-each select="author">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                <xsl:value-of select="substring-after(.,' ')"/>
                <xsl:text>, </xsl:text>
                <xsl:value-of select="substring-before(.,' ')"/>
            </Index:field>
        </xsl:for-each>
    </xsl:template>

    <!-- Title -->
    <xsl:template name="title">

        <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
            <xsl:for-each select="title">
                <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

        </Index:group>
        <Index:field Index:name="sort_title" Index:sortLocale="da" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="6">

            <xsl:for-each select="title [position()=1]">
                <xsl:choose>
                    <xsl:when test="starts-with(.,'The ') and (starts-with(../../language,'en'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'The ') and (starts-with(../../language,'En'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'A ') and (starts-with(../../language,'en'))">
                        <xsl:value-of select="substring(.,3)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'A ') and (starts-with(../../language,'En'))">
                        <xsl:value-of select="substring(.,3)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'An ') and (starts-with(../../language,'en'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'An ') and (starts-with(../../language,'En'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'La ') and (starts-with(../../language,'fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'La ') and (starts-with(../../language,'Fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Le ') and (starts-with(../../language,'fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Le ') and (starts-with(../../language,'Fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Les ') and (starts-with(../../language,'fr'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Les ') and (starts-with(../../language,'Fr'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Der ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Der ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Der ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Die ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Die ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Die ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Das ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Das ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Das ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Ein ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Ein ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Ein ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Eine ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,6)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Eine ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,6)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Eine ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,6)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Las ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Las ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Las ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Los ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Los ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Los ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Un ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Un ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Un ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Una ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Una ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Una ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>

        </Index:field>

    </xsl:template>

    <!-- Year -->
    <xsl:template name="date">
        <xsl:for-each select="pubDate">
            <Index:field Index:repeat="true" Index:name="py" Index:navn="år"  Index:type="token" Index:boostFactor="2">
                <xsl:call-template name="year"/>
            </Index:field>
            <Index:field Index:repeat="true" Index:name="year" Index:navn="year"  Index:type="number" Index:boostFactor="2">
                <xsl:call-template name="year"/>
            </Index:field>
        </xsl:for-each>

        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
            <xsl:for-each select="pubDate">
                <xsl:call-template name="year"/>
            </xsl:for-each>
            <xsl:if test="not(pubDate)">
                <xsl:text>0</xsl:text>
            </xsl:if>
        </Index:field>

        <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
            <xsl:for-each select="pubDate">
                <xsl:call-template name="year"/>
            </xsl:for-each>
            <xsl:if test="not(pubDate)">
                <xsl:text>9999</xsl:text>
            </xsl:if>
        </Index:field>
    </xsl:template>

    <xsl:template name="year">
        <xsl:value-of select="substring(.,string-length(.)-3)" />
    </xsl:template>

    <!-- Notes -->
    <xsl:template name="notes">
        <xsl:for-each select="description">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
    </xsl:template>

    <!-- Identifiers -->
    <xsl:template name="identifiers">
        <Index:group Index:name="numbers" Index:navn="nr">
            <xsl:for-each select="link">
                <Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
        </Index:group>
    </xsl:template>

    <!-- Material type -->
    <xsl:template name="ma">
        <Index:field Index:repeat="true" Index:name="format" Index:navn="format" Index:type="token">
            <xsl:value-of select="'format??'"/>
        </Index:field>
        <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:text>xe</xsl:text>
            </Index:field>
            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:text>tictoc</xsl:text>
            </Index:field>
        </Index:group>
        <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
            <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                <xsl:text>tictoc</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                <xsl:text>netdokument</xsl:text>
            </Index:field>
        </Index:group>
    </xsl:template>


</xsl:stylesheet>
