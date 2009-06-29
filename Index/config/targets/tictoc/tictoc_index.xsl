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
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:admin="http://webns.net/mvcb/"
        xmlns:prism="http://purl.org/rss/1.0/modules/prism/"
        xmlns:syn="http://purl.org/rss/1.0/modules/syndication/"
        xmlns:taxo="http://purl.org/rss/1.0/modules/taxonomy/"
        xmlns:purl="http://purl.org/rss/1.0/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">

    <xsl:include href="tictoc_shortformat.xsl" />
    <xsl:include href="tictoc_author.xsl" />
    <xsl:include href="tictoc_ma.xsl" />
    <xsl:include href="tictoc_identifiers.xsl" />
    <xsl:include href="tictoc_date.xsl" />
    <xsl:include href="tictoc_title.xsl" />
    <xsl:include href="tictoc_notes.xsl" />
    <!--<xsl:include href="oai_publisher.xsl" />   -->

    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                        Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="http:www.statsbiblioteket.dk/tictoc">
            <xsl:attribute name="Index:id">
                <xsl:for-each select="rss/channel/item/link">
                    <xsl:value-of select="."/>
                </xsl:for-each>
                <xsl:for-each select="rdf:RDF/purl:channel/prism:issn">
                    <xsl:value-of select="."/>
                </xsl:for-each>
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
                    <xsl:call-template name="publisher" />
                    -->
                    <Index:field Index:name="openUrl" Index:navn="openUrl"  Index:type="stored" Index:freetext="false">id=<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.OpenUrlEscape.escape(link)" /></Index:field>
                </Index:fields>
            </xsl:for-each>

            <xsl:for-each select="rdf:RDF/purl:item">
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
                    <xsl:call-template name="publisher" />
                    -->
                    <Index:field Index:name="openUrl" Index:navn="openUrl"  Index:type="stored" Index:freetext="false">id=<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.OpenUrlEscape.escape(link)" /></Index:field>
                </Index:fields>
            </xsl:for-each>




        </Index:document>
    </xsl:template>

</xsl:stylesheet>
