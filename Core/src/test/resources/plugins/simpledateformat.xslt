<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/summa/2008/Document"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                xmlns:dummy="http://example.com"
                xmlns:javadateParser="http://xml.apache.org/xalan/java/java.text.SimpleDateFormat"
                exclude-result-prefixes="java xs xalan xsl" version="1.0">
    <xsl:output method="xml" indent="yes" xalan:indent-amount="4"/>

    <xsl:template match="/dummy:dates">
        <xsl:text>foo</xsl:text>
        <xsl:variable name="apos">'</xsl:variable>
        <xsl:variable name="solrtime" select="concat('YYYY-MM-dd',$apos,'T',$apos,'HH:mm:ss')"/>
        <xsl:variable name="da">Europe/Copenhagen</xsl:variable>

        <xsl:variable name="parser" select="java:java.text.SimpleDateFormat.new(concat('yyyy-MM-dd',$apos,'T',$apos,'HH:mm:ssX'))"/>


        <xsl:variable name="printerYear" select="java:java.text.SimpleDateFormat.new('yyyy')"/>
        <xsl:variable name="printerDate" select="java:java.text.SimpleDateFormat.new('yyyy-MM-dd')"/>
        <xsl:variable name="printerDateTime" select="java:java.text.SimpleDateFormat.new('dd-MM-yyyy HH:mm')"/>
        <xsl:variable name="printerTime" select="java:java.text.SimpleDateFormat.new('HH:mm:ss')"/>

        <xsl:variable name="parseTest1" select="java:java.text.SimpleDateFormat.parse($parser, '2017-11-15T11:21:00Z')"/>
        <!--    <xsl:variable name="parseTest1" select="javadateParser:parse($parser, concat('2017-11-15',$apos,'T',$apos,'11:21:00X'))"/>-->

        <Index:SummaDocument>
            <Index:fields>
                <xsl:for-each select="dummy:date">
                    <Index:field Index:name="parsed">
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.DateTime.format(.,$solrtime, $da)"/><xsl:text>Z</xsl:text>
                        <!--                <xsl:value-of select="$parseTest1"/>-->
                    </Index:field>
                </xsl:for-each>
            </Index:fields>
        </Index:SummaDocument>
    </xsl:template>
</xsl:stylesheet>