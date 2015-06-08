<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">
    <xsl:template name="material">

                <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
                    <xsl:choose>
                        <xsl:when test="/marc/mc:record/mc:field[@type='140_00']/mc:subfield[@type='a'] or /marc/mc:record/mc:field[@type='150_00']/mc:subfield[@type='a']">
                            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                <xsl:text>an</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                <xsl:text>te</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                <xsl:text>mo</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </Index:group>

                <xsl:choose>
                    <xsl:when test="/marc/mc:record/mc:field[@type='140_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>artikel</xsl:text>
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>bog art</xsl:text>
                        </Index:field>
                    </xsl:when>
                    <xsl:when test="/marc/mc:record/mc:field[@type='150_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>artikel</xsl:text>
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>tss art</xsl:text>
                        </Index:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>bog</xsl:text>
                        </Index:field>
                    </xsl:otherwise>
                </xsl:choose>

    </xsl:template>
</xsl:stylesheet>