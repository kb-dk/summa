<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:Index="http://statsbiblioteket.dk/summa/2008/Document" xmlns:xs="http://www.w3.org/2001/XMLSchema-instance" xmlns:xalan="http://xml.apache.org/xalan" xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim" exclude-result-prefixes="java xs xalan xsl" version="1.0">
    <xsl:template match="/">
        <xsl:for-each select="mc:datafield">
            <Index:field Index:name="standard_number" Index:disabled_boost="6">
                <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
            </Index:field>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>