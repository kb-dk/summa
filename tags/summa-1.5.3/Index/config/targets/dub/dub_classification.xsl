<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="classification">
        <Index:group Index:name="cl" Index:navn="cl" Index:suggest="true">
            <xsl:for-each select="mc:field[@type='652_00']/mc:subfield[@type='p']">
                <Index:field Index:repeat="true" Index:name="dk" Index:navn="dk" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="." />
                </Index:field>
            </xsl:for-each>
        </Index:group>
    </xsl:template>

</xsl:stylesheet>