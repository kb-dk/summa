<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="identifiers">
        <Index:group Index:name="numbers" Index:navn="nr">
            <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='a']">
                <Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="translate(.,'- ','')"/>
                </Index:field>
            </xsl:for-each>
            
            <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='a']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="translate(.,'- ','')"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='558_00']/mc:subfield[@type='z']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="translate(.,'- ','')"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='x']">
                <Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="translate(.,'- ','')"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='x']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="translate(.,'- ','')"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='440_00']/mc:subfield[@type='z']">
                <Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='440_00']/mc:subfield[@type='z']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='557_00']/mc:subfield[@type='z']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='a']">
                <Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='558_00']/mc:subfield[@type='z']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
	    
            <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='x']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='440_00']/mc:subfield[@type='z']">
                <Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='557_00']/mc:subfield[@type='z']">
                <Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>

        </Index:group>
    </xsl:template>

</xsl:stylesheet>