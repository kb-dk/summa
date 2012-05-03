<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="notes">

                <xsl:for-each select="mc:field[@type='140_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:text>Artikel i bog:&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:field[@type='150_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:text>Artikel i:&#32; </xsl:text>
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                 <xsl:for-each select="mc:field[@type='170_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:field[@type='180_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

    </xsl:template>
</xsl:stylesheet>