<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/summa/2008/Document"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                xmlns:dummy="http://example.com"
                exclude-result-prefixes="java xs xalan xsl" version="1.0">
    <xsl:output method="xml" indent="yes" xalan:indent-amount="4"/>

    <xsl:template match="/dummy:dates">
        <xsl:text>foo</xsl:text>
        <xsl:variable name="apos">'</xsl:variable>
        <xsl:variable name="solrtime" select="concat('YYYY-MM-dd',$apos,'T',$apos,'HH:mm:ss',$apos,'Z',$apos)"/>
        <xsl:variable name="printer"  select="concat('d. MMMM yyyy ',$apos,'kl',$apos,'. HH:mm')"/>
        <xsl:variable name="printerYear" select="'YYYY'"/>
        <xsl:variable name="printerDate" select="'YYYY-MM-dd'"/>
        <xsl:variable name="printerTime" select="'HH:mm:ss'"/>
        <xsl:variable name="da">Europe/Copenhagen</xsl:variable>

        <Index:SummaDocument>
            <Index:fields>
                <xsl:for-each select="dummy:date">
                    <Index:field Index:name="parsed">
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.DateTime.isoToCustom($solrtime,$da,.)"/><xsl:text> *** </xsl:text>
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.DateTime.isoToCustom($printer,$da,.)"/><xsl:text> *** </xsl:text>
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.DateTime.isoToCustom($printerYear,$da,.)"/><xsl:text> *** </xsl:text>
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.DateTime.isoToCustom($printerDate,$da,.)"/><xsl:text> *** </xsl:text>
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.DateTime.isoToCustom($printerTime,$da,.)"/>
                        <!--                <xsl:value-of select="$parseTest1"/>-->
                    </Index:field>
                </xsl:for-each>
            </Index:fields>
        </Index:SummaDocument>
    </xsl:template>
</xsl:stylesheet>