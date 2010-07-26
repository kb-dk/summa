<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="notes">
        <xsl:for-each select="mc:datafield[@tag='501']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="."/>
                            <xsl:text> </xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='502']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='504']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='505']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='506']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='507']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='508']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='509']/mc:subfield[@code='a' or @code='b' or @code='8']">
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
                            <xsl:if test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='517']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='518']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='520']">
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
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='520']/mc:subfield[@code='n' or @code='r' or @code='z']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="keyword" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='523']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='525']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='526']">
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
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='529']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:if test="@code='i'">
                                <xsl:text>:</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='a'">
                                <xsl:text>Indekseres i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>Beskrevet i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='c'">
                                <xsl:text>Anmeldt i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:text>Omtalt i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='c'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
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
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='m'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
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
        <xsl:for-each select="mc:datafield[@tag='534']">
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
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='a' or @code='i' or @code='s']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select=".">
                    <xsl:choose>
                        <xsl:when test="@code='i'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='a'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='s'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='557']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:text>Artikel i: </xsl:text>
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="@code='a'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='æ'">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='ø'">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>)</xsl:text>
                        </xsl:when>
                        <xsl:when test="@code='b'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='h'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='i'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='j'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='l'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='v'">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='k'">
                            <xsl:text>, </xsl:text>
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
                        <xsl:text>Artikel i: </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="@code='a'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='e'">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='g'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='w'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='h'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='i'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='j'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='l'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='s'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='v'">
                            <xsl:text> ; </xsl:text>
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
        <xsl:for-each select="mc:datafield[@tag='560']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='565']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='566']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='573']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='860' or @tag='861' or @tag='863' or @tag='865' or @tag='866' or @tag='867' or @tag='868' or @tag='870' or @tag='871' or @tag=873 or @tag='874' or @tag='879']/mc:subfield[@code='i']/..">
            <Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="@code='i'">
                            <xsl:value-of select="."/>
                            <xsl:text>: </xsl:text>
                        </xsl:when>
                        <xsl:when test="@code='t'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='c'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='887']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
                <xsl:for-each select="mc:subfield[@code='2']">
                    <xsl:text>,&#32; </xsl:text>
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
    </xsl:template>


</xsl:stylesheet>
