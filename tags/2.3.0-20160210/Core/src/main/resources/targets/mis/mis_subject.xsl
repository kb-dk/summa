<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="subject">

                <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                    <xsl:for-each select="mc:field[@type='190_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='200_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                </Index:group>
                <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
                    <xsl:for-each select="mc:field[@type='190_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='200_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="." />
                        </Index:field>
                    </xsl:for-each>
                </Index:group>  

    </xsl:template>
</xsl:stylesheet>