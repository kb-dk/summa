<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="identifiers">

        <Index:group Index:name="numbers" Index:navn="nr">
            <xsl:if test="contains(marc/mc:record/mc:field[@type='150_00']/mc:subfield[@type='a'],ISSN)">
                <xsl:variable name="issn" select="normalize-space(substring-after(mc:field[@type='150_00']/mc:subfield[@type='a'],'ISSN'))"/>
                <Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="substring($issn,1,9)"/>
                </Index:field>
            </xsl:if>
        </Index:group>

    </xsl:template>

</xsl:stylesheet>