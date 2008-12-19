<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="author">
        <xsl:choose>
            <xsl:when test="mc:field[@type='001_00']/mc:subfield[@type='f']='new'">
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
            </xsl:when>

            <!-- Den gamle visning -->

            <xsl:otherwise>
                <xsl:for-each select="mc:field[@type='100_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                        <xsl:call-template name="author_reverse"/>
                    </Index:field>
                    <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                        <xsl:call-template name="author_forward"/>
                    </Index:field>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Personnavn templates -->

    <!-- En masse flam for at fjerne punktummer efter fornavnet. Hvis 2.sidste bogstav foran et evt sidste punktum
         er et mellemrum, punktum eller ingenting antages det at punktummet er efter en forkortelse af navnet. -->

    <xsl:template name="author_reverse">
        <xsl:variable name="navn" select="." />
        <xsl:choose>
            <xsl:when test="substring($navn, string-length($navn)) = '.'">
                <xsl:choose>
                    <xsl:when test="substring($navn, string-length($navn)-2,1) = ' ' or
                                    substring($navn, string-length($navn)-2,1) = '.' ">
                        <xsl:value-of select="$navn"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring($navn,1,string-length($navn)-1)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$navn"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="author_forward">
        <xsl:variable name="fornavn" select="normalize-space(substring-after(.,','))" />
        <xsl:choose>
            <xsl:when test="substring($fornavn, string-length($fornavn)) = '.'">
                <xsl:choose>
                    <xsl:when test="substring($fornavn, string-length($fornavn)-2,1) = '' or
                                    substring($fornavn, string-length($fornavn)-2,1) = ' ' or 
                                    substring($fornavn, string-length($fornavn)-2,1) = '.' ">
                        <xsl:value-of select="$fornavn"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring($fornavn,1,string-length($fornavn)-1)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$fornavn"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text>&#32;</xsl:text>
        <xsl:value-of select="substring-before(.,',')"/>

    </xsl:template>

</xsl:stylesheet>