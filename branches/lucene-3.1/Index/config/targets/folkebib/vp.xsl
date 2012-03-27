<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="vp">
                <xsl:for-each select="mc:datafield[@tag='557' or @tag='F57']">
                    <Index:field Index:repeat="true" Index:name="vp" Index:navn="vp" Index:type="token" Index:boostFactor="4">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='æ']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:text> </xsl:text>
                        <xsl:for-each select="mc:subfield[@code='ø']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='v']">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='558' or @tag='F58']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="vp" Index:navn="vp" Index:type="token" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                </xsl:for-each>
    </xsl:template>


    </xsl:stylesheet>
