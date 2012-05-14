<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="notes">
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">

                <xsl:for-each select="mc:datafield[@tag='504']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='506']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='512']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="position()='1'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@code='t'">
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

                <xsl:for-each select="mc:datafield[@tag='523']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@code='u']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='530']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="position()='1'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@code='a'">
                                        <xsl:value-of select="."/>
                                    </xsl:if>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='532']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='557']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:text>Artikel i:&#32;</xsl:text>
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="@code='a'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@code='j'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@code='l'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@code='v'">
                                    <xsl:text>&#32;;&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@code='k'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='558']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:choose>
                            <xsl:when test="position()='1'">
                                <xsl:text>Artikel i:&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:if test="@code='a'">
                                    <xsl:text>&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='e'">
                                    <xsl:text>&#32;/&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='g'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='w'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='h'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='i'">
                                    <xsl:text>&#32;:&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='j'">
                                    <xsl:text>,&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='s'">
                                    <xsl:text>. -&#32;</xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='v'">
                                    <xsl:text>&#32;;&#32; </xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:if>
                            </xsl:otherwise>
                        </xsl:choose>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='559']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='140']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:text>Artikel i bog:&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='150']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:text>Artikel i:&#32; </xsl:text>
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                 <xsl:for-each select="mc:datafield[@tag='170']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='180']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
</xsl:stylesheet>