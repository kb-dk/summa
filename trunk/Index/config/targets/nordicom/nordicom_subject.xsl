<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="subject">
        <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
            <xsl:for-each select="mc:field[@type='600_00']">
                <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='610_00']">
                <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="corp"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='630_00']/mc:subfield[@type='a']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                        <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='631_00']/mc:subfield[@type='a']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='633_00']/mc:subfield[@type='a']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                        <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='634_00']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='634_00']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='634_00']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='d']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='645_00']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='645_00']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='645_00']">
                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
        </Index:group>

        
        <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
            <xsl:for-each select="mc:field[@type='600_00']">
                <Index:field Index:repeat="false" Index:name="su_pe" Index:navn="lep" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='610_00']">
                <Index:field Index:repeat="false" Index:name="su_corp" Index:navn="lek" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp"/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='610_00']">
                <Index:field Index:repeat="false" Index:name="su_lc" Index:navn="llcm" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp_subdiv"/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp_subdiv"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='630_00']/mc:subfield[@type='a']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                        <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='631_00']/mc:subfield[@type='a']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='633_00']/mc:subfield[@type='a']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                        <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:field[@type='634_00']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='634_00']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='634_00']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='d']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            
            <xsl:for-each select="mc:field[@type='645_00']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='645_00']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:field[@type='645_00']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@type='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@type='u']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
        </Index:group>
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
    <xsl:template name="person_subdiv">
        <xsl:call-template name="person"/>
        <xsl:for-each select="mc:subfield[@type='t']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='x']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='y']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='z']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='u']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="person_inverted_subdiv">
        <xsl:call-template name="person_inverted"/>
        <xsl:for-each select="mc:subfield[@type='t']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='x']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='y']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='z']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='u']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
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
    <xsl:template name="corp_subdiv">
        <xsl:call-template name="corp"/>
        <xsl:for-each select="mc:subfield[@type='t']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='x']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='y']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='z']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>&#32;</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@type='u']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>

    </xsl:template>
</xsl:stylesheet>