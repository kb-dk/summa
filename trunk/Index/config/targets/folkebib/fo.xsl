<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="fo">
        <Index:group Index:name="fo" Index:navn="fo">
            <xsl:choose>
                <xsl:when test="mc:datafield[@tag='100' or @tag='B00']">
                    <xsl:for-each select="mc:datafield[@tag='100' or @tag='B00']">
                        <Index:field Index:name="pe" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="person_name"/>
                        </Index:field>

                        <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="person_name_inverted"/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='700' or @tag='H00']">
                        <xsl:choose>
                            <xsl:when test="position ()&lt;3">
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_name"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_name_inverted"/>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_name"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_name_inverted"/>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:when>

                <xsl:when test="mc:datafield[@tag='110' or@tag='B10']">
                    <xsl:for-each select="mc:datafield[@tag='110' or @tag='B10']">
                        <Index:field Index:repeat="true" Index:name="ko" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="corporate_name"/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='700' or @tag='H00']">
                        <xsl:choose>
                            <xsl:when test="position ()&lt;3">
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_name"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_name_inverted"/>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_name"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_name_inverted"/>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:for-each select="mc:datafield[@tag='700' or @tag='H00']">
                        <xsl:choose>
                            <xsl:when test="position ()&lt;4">
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_name"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_name_inverted"/>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_name"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_name_inverted"/>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="mc:datafield[@tag='710' or @tag='H10']">
                <xsl:choose>
                    <xsl:when test="position ()=1">
                        <Index:field Index:repeat="true" Index:name="ko" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="corporate_name"/>
                        </Index:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <Index:field Index:repeat="true" Index:name="ko" Index:navn="ko" Index:type="token" Index:boostFactor="8">
                            <xsl:call-template name="corporate_name"/>
                        </Index:field>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='239' or @tag='C39']">
                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person_name"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='770' or @tag='H70']">
                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="person_name"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='900' or @tag='J00']">
                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="4">
                    <xsl:call-template name="person_name"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="pe" Index:navn="pe" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='780' or @tag='H80']">
                <Index:field Index:repeat="true" Index:name="ko" Index:navn="ko" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="corporate_name"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='980' or @tag='J80']">
                <Index:field Index:repeat="true" Index:name="ko" Index:navn="ko" Index:type="token" Index:boostFactor="4">
                    <xsl:call-template name="corporate_name"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='100' or @tag='B00']">
                <Index:field Index:repeat="true" Index:name="po" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person_name"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="po" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='110' or @tag='B10']">
                <Index:field Index:repeat="true" Index:name="po" Index:navn="po" Index:type="token">
                    <xsl:call-template name="corporate_name"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='239' or @tag='C39']">
                <Index:field Index:repeat="true" Index:name="po" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person_name"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="po" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
            </xsl:for-each>
        </Index:group>


        <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='j' or @code='k' or @code='t' or @code='æ']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='247']/mc:subfield[@code='e' or @code='f' or @code='t']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='e' or @code='f' or @code='t']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='250']/mc:subfield[@code='c' or @code='d' or @code='t']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='440' or @tag='840']/mc:subfield[@code='e' or @code='t' or @code='æ']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:choose>
                    <xsl:when test="contains(.,'¤')">
                        <xsl:value-of select="substring-after(.,'¤')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='512']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='513']/mc:subfield[@code='a' or @code='e' or @code='f' or @code='i' or @code='j']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='520']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='526']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='530']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='540']/mc:subfield[@code='a']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='557']/mc:subfield[@code='æ']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='e']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='571']/mc:subfield[@code='a' or @code='x']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='572']/mc:subfield[@code='a']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='745']/mc:subfield[@code='æ']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='795']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='j' or @code='k' or @code='t' or @code='æ']">
            <Index:field Index:name="fb" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>


    </xsl:template>
    <xsl:template name="person_name">
        <xsl:for-each select="mc:subfield[@code='h']">
            <xsl:value-of select="."/>
            <xsl:text> </xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='a']">
            <xsl:choose>
                <xsl:when test="contains(.,'¤')">
                    <xsl:value-of select="substring-after(.,'¤')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='e']">
            <xsl:text> </xsl:text>
            <xsl:choose>
                <xsl:when test="contains(.,'¤')">
                    <xsl:value-of select="substring-after(.,'¤')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='f']">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='c']">
            <xsl:text>, </xsl:text>
            <xsl:value-of select="." />

        </xsl:for-each>
    </xsl:template>
    <xsl:template name="person_name_inverted">
        <xsl:for-each select="mc:subfield[@code='a']">
            <xsl:choose>
                <xsl:when test="contains(.,'¤')">
                    <xsl:value-of select="substring-after(.,'¤')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='h']">
            <xsl:text>, </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='e']">
            <xsl:text> </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='f']">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='c']">
            <xsl:text>, </xsl:text>
            <xsl:value-of select="." />
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="corporate_name">
        <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
            <xsl:for-each select="mc:subfield">
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
                        <xsl:text> (</xsl:text>
                        <xsl:value-of select="." />
                        <xsl:text>)</xsl:text>
                    </xsl:when>
                    <xsl:when test="@code='c'">
                        <xsl:if test="position()&gt;1">
                            <xsl:text>. </xsl:text>
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
                        <xsl:text> ; </xsl:text>
                        <xsl:value-of select="." />
                    </xsl:when>
                    <xsl:when test="@code='k'">
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="." />
                    </xsl:when>
                    <xsl:when test="@code='j'">
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="." />
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
