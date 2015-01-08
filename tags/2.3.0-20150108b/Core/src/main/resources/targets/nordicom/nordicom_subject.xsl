<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="subject">
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">
                <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                    <xsl:for-each select="mc:datafield[@tag='600']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="person"/>
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="person_inverted"/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='610']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="ke" Index:type="token" Index:boostFactor="6">
                            <xsl:call-template name="corp"/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='630']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='631']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='633']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='634']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='634']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='b']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='634']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='c']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='d']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='645']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='645']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='b']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='645']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='c']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                </Index:group>


                <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
                    <xsl:for-each select="mc:datafield[@tag='600']">
                        <Index:field Index:repeat="false" Index:name="su_pe" Index:navn="lep" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="person_inverted"/>
                        </Index:field>
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="person_inverted"/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='610']">
                        <Index:field Index:repeat="false" Index:name="su_corp" Index:navn="lek" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="corp"/>
                        </Index:field>
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="corp"/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='610']">
                        <Index:field Index:repeat="false" Index:name="su_lc" Index:navn="llcm" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="corp_subdiv"/>
                        </Index:field>
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="corp_subdiv"/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='630']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='631']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='633']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='634']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='634']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='b']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='634']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='c']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='d']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='645']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='645']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='b']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='645']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                            <xsl:for-each select="mc:subfield[@code='c']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='u']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                </Index:group>
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>
                <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                    <xsl:for-each select="mc:datafield[@tag='190']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='200']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                </Index:group>
                <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
                    <xsl:for-each select="mc:datafield[@tag='190']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='200']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                </Index:group>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- Diverse templates -->


    <xsl:template name="person_inverted">
        <xsl:for-each select="mc:subfield[@code='a']">
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='h']">
            <xsl:text>,&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='e']">
            <xsl:text>&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='f']">
            <xsl:text>&#32;(</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)&#32;</xsl:text>
        </xsl:for-each>
        <xsl:if test="mc:subfield[@code='c']">
            <xsl:choose>
                <xsl:when test="contains (.,'f. ')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:when test="contains (.,'f.')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='c']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>



    <xsl:template name="person">
        <xsl:for-each select="mc:subfield[@code='h']">
            <xsl:value-of select="."/>
            <xsl:text>&#32;</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='a']">
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='e']">
            <xsl:text>&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='f']">
            <xsl:text>&#32;(</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)&#32;</xsl:text>
        </xsl:for-each>
        <xsl:if test="mc:subfield[@code='c']">
            <xsl:choose>
                <xsl:when test="contains (.,'f. ')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:when test="contains (.,'f.')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='c']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    <xsl:template name="person_subdiv">
        <xsl:call-template name="person"/>
        <xsl:for-each select="mc:subfield[@code='t']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='x']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='y']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='z']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='u']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="person_inverted_subdiv">
        <xsl:call-template name="person_inverted"/>
        <xsl:for-each select="mc:subfield[@code='t']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='x']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='y']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='z']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='u']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="corp">
        <xsl:for-each select="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
            <xsl:choose>
                <xsl:when test="position()=1">
                    <xsl:value-of select="."/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="@code='a'">
                            <xsl:choose>
                                <xsl:when test="contains(.,'¤')">
                                    <xsl:value-of select="substring-after(.,'¤')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@code='s'">
                            <xsl:choose>
                                <xsl:when test="contains(.,'¤')">
                                    <xsl:value-of select="substring-after(.,'¤')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@code='e'">
                            <xsl:text>&#32;(</xsl:text>
                            <xsl:value-of select="." />
                            <xsl:text>)</xsl:text>
                        </xsl:when>
                        <xsl:when test="@code='c'">
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
                        <xsl:when test="@code='i'">
                            <xsl:text>&#32;;&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:when test="@code='k'">
                            <xsl:text>,&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:when test="@code='j'">
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
        <xsl:for-each select="mc:subfield[@code='t']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='x']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='y']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='z']">
            <xsl:text>.&#32;</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>&#32;</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='u']">
            <xsl:text>&#32;:&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>

    </xsl:template>
</xsl:stylesheet>