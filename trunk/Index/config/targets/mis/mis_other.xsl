<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="other">

                <xsl:for-each select="mc:field[@type='130_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                        <xsl:value-of select="mc:field[@type='130_00']/mc:subfield[@type='a']"/>
                    </Index:field>
               </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>
