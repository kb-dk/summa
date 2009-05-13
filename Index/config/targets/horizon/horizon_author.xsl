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
            <xsl:choose>
                <xsl:when test="mc:datafield[@tag='100']">
                    <xsl:for-each select="mc:datafield[@tag='100']">
                        <Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="person"/>
                        </Index:field>

                        <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="person_inverted"/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='700']">
                        <xsl:choose>
                            <xsl:when test="position ()&lt;3">
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_inverted"/>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_inverted"/>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:when>

                <xsl:when test="mc:datafield[@tag='110']">
                    <xsl:for-each select="mc:datafield[@tag='110']">
                        <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="corp"/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='700']">
                        <xsl:choose>
                            <xsl:when test="position ()&lt;3">
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_inverted"/>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_inverted"/>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:for-each select="mc:datafield[@tag='700']">
                        <xsl:choose>
                            <xsl:when test="position ()&lt;4">
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                    <xsl:call-template name="person_inverted"/>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person"/>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
                                    <xsl:call-template name="person_inverted"/>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="mc:datafield[@tag='710']">
                <xsl:choose>
                    <xsl:when test="position ()=1">
                        <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                            <xsl:call-template name="corp"/>
                        </Index:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="8">
                            <xsl:call-template name="corp"/>
                        </Index:field>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='239']">
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='770']">
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='780']">
                <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="6">
                    <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                        <xsl:call-template name="corp"/>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='100']">
                <Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='110']">
                <Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
                    <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                        <xsl:call-template name="corp"/>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='239']">
                <Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>
        </Index:group>

        <!-- Forfatterbeskrivelser på dansk -->
        <xsl:if test="mc:datafield[@tag='100']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='100']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='110']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='110']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='239']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='239']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='700']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='700']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='710']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='710']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='720']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='720']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='o']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='739']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='739']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_da_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>


        <!-- Forfatterbeskrivelser på engelsk -->
        <xsl:if test="mc:datafield[@tag='100']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='100']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='110']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='110']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='239']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='239']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='700']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='700']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='710']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='710']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

        <xsl:if test="mc:datafield[@tag='720']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='720']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='o']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>

         <xsl:if test="mc:datafield[@tag='739']/mc:subfield[@code='9']">
            <xsl:for-each select="mc:datafield[@tag='739']">
                <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                    <xsl:call-template name="author_en_description">
                        <xsl:with-param name="func_code" select="mc:subfield[@code='9']" />
                    </xsl:call-template>
                    <xsl:if test="mc:subfield[@code='h']">
                        <xsl:text>&#32;</xsl:text>
                        <xsl:value-of select="mc:subfield[@code='h']">
                        </xsl:value-of>
                    </xsl:if>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='a']">
                    </xsl:value-of>
                </Index:field>
            </xsl:for-each>
        </xsl:if>


        <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='j' or @code='k' or @code='t' or @code='3']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='247']/mc:subfield[@code='e' or @code='f' or @code='t']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='e' or @code='f' or @code='t']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='250']/mc:subfield[@code='c' or @code='d' or @code='t']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='440' or @tag='840']/mc:subfield[@code='e' or @code='t' or @code='3']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='512']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='513']/mc:subfield[@code='a' or @code='e' or @code='f' or @code='i' or @code='j']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='520']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='526']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='530']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='d' or @code='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='540']/mc:subfield[@code='a']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='557']/mc:subfield[@code='3']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='e']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='571']/mc:subfield[@code='a' or @code='x']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='572']/mc:subfield[@code='a']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='745']/mc:subfield[@code='3']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='795']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='j' or @code='k' or @code='t' or @code='3']">
            <Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='100']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_inverted"/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='700']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_inverted"/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='770']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_inverted"/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='239']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_inverted"/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='110' or @tag='710'or @tag='780']">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="corp"/>
            </Index:field>
        </xsl:for-each>

    </xsl:template>


    <xsl:template name="author_da_description">
        <xsl:param name="func_code" />
        <xsl:if test="$func_code = 'act'">
            <xsl:text>Skuespiller</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aft'">
            <xsl:text>Forfatter til efterord, efterskrift etc.</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'anm'">
            <xsl:text>Animator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ant'">
            <xsl:text>Forfatter til forlæg</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'arr'">
            <xsl:text>Arrangør</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'art'">
            <xsl:text>Kunstner</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aud'">
            <xsl:text>Dialogforfatter</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aui'">
            <xsl:text>Forfatter til forord, introduktion etc.</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aus'">
            <xsl:text>Manuskriptforfatter</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aut'">
            <xsl:text>Forfatter</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ccp'">
            <xsl:text>Idemager</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'chr'">
            <xsl:text>Koreograf</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cli'">
            <xsl:text>For</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cll'">
            <xsl:text>Kalligraf</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cmm'">
            <xsl:text>Kommentator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cmp'">
            <xsl:text>Komponist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cnd'">
            <xsl:text>Dirigent</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cng'">
            <xsl:text>Filmfotograf</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'com'">
            <xsl:text>Udgiver</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cre'">
            <xsl:text>Skaber</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ctb'">
            <xsl:text>Bidragsyder</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ctg'">
            <xsl:text>Korttegner</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cwt'">
            <xsl:text>Forfatter til tekstkommentar</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dnc'">
            <xsl:text>Danser</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'drm'">
            <xsl:text>Tegner</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'drt'">
            <xsl:text>Instruktør</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dte'">
            <xsl:text>Festskriftsmodtager</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'edt'">
            <xsl:text>Redaktør</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ill'">
            <xsl:text>Illustrator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'inv'">
            <xsl:text>Udvikler</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'itr'">
            <xsl:text>Instrumentalist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ive'">
            <xsl:text>Interviewede</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ivr'">
            <xsl:text>Interviewer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'lbt'">
            <xsl:text>Librettist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ltg'">
            <xsl:text>Litograf</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'lyr'">
            <xsl:text>Sangskriver</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'mus'">
            <xsl:text>Musiker</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'nrt'">
            <xsl:text>Fortæller</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'orm'">
            <xsl:text>Mødearrangør</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'oth'">
            <xsl:text>Medarbejder</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'pht'">
            <xsl:text>Fotograf</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ppt'">
            <xsl:text>Dukkefører</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'prd'">
            <xsl:text>Produktionspersonale</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'prf'">
            <xsl:text>Performer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'prg'">
            <xsl:text>Programmør</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'pro'">
            <xsl:text>Producer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'rce'">
            <xsl:text>Tekniker</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'res'">
            <xsl:text>Researcher</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'rev'">
            <xsl:text>Anmelder</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'scl'">
            <xsl:text>Skulptør</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'sng'">
            <xsl:text>Sanger</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'stl'">
            <xsl:text>Kunstnerisk fortæller</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'trl'">
            <xsl:text>Oversætter</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'wdc'">
            <xsl:text>Træskærer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dkani'">
            <xsl:text>Ansvarlig institution</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dkbea'">
            <xsl:text>Bearbejder</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dkdes'">
            <xsl:text>Designer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dkind'">
            <xsl:text>Indlæser</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dkmed'">
            <xsl:text>Medforfatter</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dkref'">
            <xsl:text>Referent</xsl:text>
        </xsl:if>
    </xsl:template>


    <xsl:template name="author_en_description">
        <xsl:param name="func_code" />
        <xsl:if test="$func_code = 'act'">
            <xsl:text>Actor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aft'">
            <xsl:text>Author of afterword, colophon etc.</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'anm'">
            <xsl:text>Animator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ant'">
            <xsl:text>Bibliographic antecedent</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'arr'">
            <xsl:text>Arranger</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'art'">
            <xsl:text>Artist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aud'">
            <xsl:text>Author of dialog</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aui'">
            <xsl:text>Author of introduction, etc.</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aus'">
            <xsl:text>Author of screenplay</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'aut'">
            <xsl:text>Author</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ccp'">
            <xsl:text>Conceptor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'chr'">
            <xsl:text>Choreographer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cli'">
            <xsl:text>Client</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cll'">
            <xsl:text>Calligrapher</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cmm'">
            <xsl:text>Commentator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cmp'">
            <xsl:text>Composer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cnd'">
            <xsl:text>Conductor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cng'">
            <xsl:text>Cinematographer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'com'">
            <xsl:text>Compiler</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cre'">
            <xsl:text>Creator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ctb'">
            <xsl:text>Contributor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ctg'">
            <xsl:text>Cartographer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'cwt'">
            <xsl:text>Commentator for written text</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dnc'">
            <xsl:text>Dancer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'drm'">
            <xsl:text>Draftsman</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'drt'">
            <xsl:text>Director</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'dte'">
            <xsl:text>Dedicatee</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'edt'">
            <xsl:text>Editor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ill'">
            <xsl:text>Illustrator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'inv'">
            <xsl:text>Inventor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'itr'">
            <xsl:text>Instrumentalist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ive'">
            <xsl:text>Interviwee</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ivr'">
            <xsl:text>Interviewer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'lbt'">
            <xsl:text>Librettist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ltg'">
            <xsl:text>Lithographer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'lyr'">
            <xsl:text>Lyricist</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'mus'">
            <xsl:text>Musician</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'nrt'">
            <xsl:text>Narrator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'orm'">
            <xsl:text>Organizer of meeting</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'oth'">
            <xsl:text>Other</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'pht'">
            <xsl:text>Photographer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'ppt'">
            <xsl:text>Puppeteer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'prd'">
            <xsl:text>Production personnel</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'prf'">
            <xsl:text>Performer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'prg'">
            <xsl:text>Programmer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'pro'">
            <xsl:text>Producer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'rce'">
            <xsl:text>Recording engineer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'res'">
            <xsl:text>Researcher</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'rev'">
            <xsl:text>Reviewer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'scl'">
            <xsl:text>Sculptor</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'sng'">
            <xsl:text>Singer</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'stl'">
            <xsl:text>Storyteller</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'trl'">
            <xsl:text>Translator</xsl:text>
        </xsl:if>
        <xsl:if test="$func_code = 'wdc'">
            <xsl:text>Woodcutter</xsl:text>
        </xsl:if>
    </xsl:template>


</xsl:stylesheet>

