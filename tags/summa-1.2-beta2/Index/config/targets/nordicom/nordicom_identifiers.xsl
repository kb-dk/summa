<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="identifiers">
        <xsl:choose>
            <xsl:when test="mc:field[@type='001_00']/mc:subfield[@type='f']='new'">
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
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>

                <xsl:if test="contains(mc:record/mc:field[@type='150_00']/mc:subfield[@type='a'],ISSN)">
                    <xsl:variable name="issn" select="normalize-space(substring-after(mc:field[@type='150_00']/mc:subfield[@type='a'],'ISSN'))"/>
                    <Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="substring($issn,1,9)"/>
                    </Index:field>
                </xsl:if>

            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>