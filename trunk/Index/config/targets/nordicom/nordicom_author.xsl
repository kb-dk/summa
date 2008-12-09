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
            <xsl:for-each select="mc:field[@type='710_00']">
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
</xsl:stylesheet>