<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="lma">

                <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
                    <xsl:for-each select=".">
                        <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                            <xsl:text>Missio Nordica Database</xsl:text>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:choose>
                        <xsl:when test="/marc/mc:record/mc:field[@type='140_00']/mc:subfield[@type='a']">
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>artikel</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>artikel_i_bog</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="/marc/mc:record/mc:field[@type='150_00']/mc:subfield[@type='a']">
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>artikel</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>tss_art</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>bog</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>trykt_bog</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </Index:group>
        
    </xsl:template>
</xsl:stylesheet>