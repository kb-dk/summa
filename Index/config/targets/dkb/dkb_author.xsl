<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="author">
        <Index:group Index:name="au" Index:navn="fo" >
            <xsl:for-each select="mc:field[@type='700_00']">
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='710_00']">
                <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="corp"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="8">
                    <xsl:call-template name="corp"/>
                </Index:field>
            </xsl:for-each>
        </Index:group>

        <xsl:for-each select="mc:field[@type='558_00']/mc:subfield[@type='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:field[@type='700_00']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_inverted"/>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:field[@type='710_00']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="corp"/>
            </Index:field>
        </xsl:for-each>



    </xsl:template>

    <!-- Diverse templates -->

    <xsl:template name="person_inverted">
        <xsl:for-each select="mc:subfield[@type='a']">
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='h']">
            <xsl:text>,&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='e']">
            <xsl:text>&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='f']">
            <xsl:text>&#32;(</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)&#32;</xsl:text>
        </xsl:for-each>
        <xsl:if test="mc:subfield[@type='c']">
            <xsl:choose>
                <xsl:when test="contains (.,'f. ')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@type='c'],'f. ')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:when test="contains (.,'f.')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@type='c'],'f.')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@type='c']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>



    <xsl:template name="person">
        <xsl:for-each select="mc:subfield[@type='h']">
            <xsl:value-of select="."/>
            <xsl:text>&#32;</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='a']">
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='e']">
            <xsl:text>&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='f']">
            <xsl:text>&#32;(</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)&#32;</xsl:text>
        </xsl:for-each>
        <xsl:if test="mc:subfield[@type='c']">
            <xsl:choose>
                <xsl:when test="contains (.,'f. ')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@type='c'],'f. ')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:when test="contains (.,'f.')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@type='c'],'f.')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@type='c']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:template name="corp">
        <xsl:for-each select="mc:subfield[@type='a'or @type='s' or @type='e' or @type='c' or @type='i' or @type='k' or @type='j']">
            <xsl:choose>
                <xsl:when test="position()=1">
                    <xsl:value-of select="."/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="@type='a'">
                            <xsl:choose>
                                <xsl:when test="contains(.,'¤')">
                                    <xsl:value-of select="substring-after(.,'¤')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@type='s'">
                            <xsl:choose>
                                <xsl:when test="contains(.,'¤')">
                                    <xsl:value-of select="substring-after(.,'¤')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@type='e'">
                            <xsl:text>&#32;(</xsl:text>
                            <xsl:value-of select="." />
                            <xsl:text>)</xsl:text>
                        </xsl:when>
                        <xsl:when test="@type='c'">
                            <xsl:if test="position()&gt;1">
                                <xsl:text>.&#32;</xsl:text>
                            </xsl:if>
                            <xsl:choose>
                                <xsl:when test="contains(.,'¤')">
                                    <xsl:value-of select="substring-after(.,'¤')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@type='i'">
                            <xsl:text>&#32;;&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:when test="@type='k'">
                            <xsl:text>,&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:when test="@type='j'">
                            <xsl:text>,&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

    </xsl:template>


</xsl:stylesheet>