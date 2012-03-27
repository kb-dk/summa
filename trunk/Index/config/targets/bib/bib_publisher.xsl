<?xml version="1.0" encoding="UTF-8"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                                                      xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                                                      xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                                                      xmlns:xalan="http://xml.apache.org/xalan"
                                                      xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                                                      exclude-result-prefixes="java xs xalan xsl"
                                                      version="1.0">

    <xsl:template name="publication_data">

                <xsl:for-each select="mc:field[@type='260_00']/mc:subfield[@type='b']">
                    <Index:field Index:repeat="true" Index:name="pu" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='260_00']/mc:subfield[@type='c']">
                    <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="4">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='008_00']/mc:subfield[@type='a' or @type='z']">
                    <Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="4">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='260_00']/mc:subfield[@type='a']">
                    <Index:field Index:repeat="true" Index:name="place" Index:navn="pu" Index:type="token">
                        <xsl:for-each select=".">
                            <xsl:value-of select="."/>
                            <xsl:text>&#32;</xsl:text>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>

                <xsl:for-each select="mc:field[@type='558_00']/mc:subfield[@type='h']">
                    <Index:field Index:repeat="true" Index:name="place" Index:navn="pu" Index:type="token">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>


    </xsl:template>
</xsl:stylesheet>