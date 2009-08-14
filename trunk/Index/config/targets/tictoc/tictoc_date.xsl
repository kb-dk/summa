<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:purl="http://purl.org/rss/1.0/"
                xmlns:prism="http://purl.org/rss/1.0/modules/prism/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>


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
        <xsl:for-each select="prism:publicationDate">
            <Index:field Index:repeat="true" Index:name="py" Index:navn="år"  Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="substring(.,1,4)" />
            </Index:field>
            <Index:field Index:repeat="true" Index:name="year" Index:navn="year"  Index:type="number" Index:boostFactor="2">
                <xsl:value-of select="substring(.,1,4)" />
            </Index:field>
        </xsl:for-each>

        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
            <xsl:for-each select="pubDate">
                <xsl:call-template name="year"/>
            </xsl:for-each>
            <xsl:for-each select="prism:publicationDate">
                <xsl:value-of select="substring(.,1,4)" />
            </xsl:for-each>
            <xsl:if test="not(prism:publicationDate) and not(pubDate)">
                <xsl:text>0</xsl:text>
            </xsl:if>
        </Index:field>

        <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
            <xsl:for-each select="pubDate">
                <xsl:call-template name="year"/>
            </xsl:for-each>
            <xsl:for-each select="prism:publicationDate">
                <xsl:value-of select="substring(.,1,4)" />
            </xsl:for-each>
            <xsl:if test="not(prism:publicationDate) and not(pubDate)">
                <xsl:text>9999</xsl:text>
            </xsl:if>
        </Index:field>
    </xsl:template>

    <xsl:template name="year">
        <xsl:choose>
            <xsl:when test='substring(.,string-length(.)-3)=" GMT"'>
                <xsl:value-of select="substring(.,string-length(.)-16,4)" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="substring(.,string-length(.)-3)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>