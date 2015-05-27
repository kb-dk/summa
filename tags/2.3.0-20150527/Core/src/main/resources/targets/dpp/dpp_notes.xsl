<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="notes">

                <xsl:for-each select="mc:field[@type='504_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='506_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='512_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="position()='1'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@type='t'">
                                        <xsl:choose>
                                            <xsl:when test="position()='1'">
                                                <xsl:value-of select="."/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>&#32;;&#32;</xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:if>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='523_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@type='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@type='u']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='530_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="position()='1'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@type='a'">
                                        <xsl:value-of select="."/>
                                    </xsl:if>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='532_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@type='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='557_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:text>Artikel i:&#32;</xsl:text>
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="@type='a'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@type='j'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@type='l'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@type='v'">
                                    <xsl:text>&#32;;&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@type='k'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='558_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:choose>
                            <xsl:when test="position()='1'">
                                <xsl:text>Artikel i:&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:if test="@type='a'">
                                    <xsl:text>&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='e'">
                                    <xsl:text>&#32;/&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='g'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='w'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='h'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='i'">
                                    <xsl:text>&#32;:&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='j'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='s'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@type='v'">
                                    <xsl:text>&#32;;&#32; </xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                            </xsl:otherwise>
                        </xsl:choose>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='559_00']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@type='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

    </xsl:template>
</xsl:stylesheet>