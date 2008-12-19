<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="other">
        <xsl:choose>
            <xsl:when test="mc:field[@type='001_00']/mc:subfield[@type='f']='new'">
                <xsl:for-each select="mc:field[@type='250_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='300_00']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="position()=1">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@type='a'">
                                        <xsl:text>&#32;(</xsl:text>
                                        <xsl:value-of select="."/>
                                        <xsl:text>)</xsl:text>
                                    </xsl:if>
                                    <xsl:if test="@type='b'">
                                        <xsl:text>&#32;:&#32;</xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:if>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>

                <xsl:if test="/mc:record/mc:field[@type='130_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:value-of select="mc:field[@type='130_00']/mc:subfield[@type='a']"/>
                    </Index:field>
               </xsl:if>
                
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

</xsl:stylesheet>
