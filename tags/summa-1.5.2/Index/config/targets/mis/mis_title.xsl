<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="title">

                <Index:group Index:name="ti" Index:navn="ti" >
                    <xsl:for-each select="mc:field[@type='110_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='120_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='110_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:for-each select=".">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                </Index:group>

    </xsl:template>

</xsl:stylesheet>
