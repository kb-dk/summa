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
            <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">
                <xsl:for-each select="mc:datafield[@tag='250']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='300']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="position()=1">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@code='a'">
                                        <xsl:text>&#32;(</xsl:text>
                                        <xsl:value-of select="."/>
                                        <xsl:text>)</xsl:text>
                                    </xsl:if>
                                    <xsl:if test="@code='b'">
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

                <xsl:if test="/mc:record/mc:datafield[@tag='130']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:value-of select="mc:datafield[@tag='130']/mc:subfield[@code='a']"/>
                    </Index:field>
               </xsl:if>
                
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

</xsl:stylesheet>
