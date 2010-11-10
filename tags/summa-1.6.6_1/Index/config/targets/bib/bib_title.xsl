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
                    <xsl:for-each select="mc:field[@type='245_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='245_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:for-each select=".">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='245_00']/mc:subfield[@type='p']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='745_00']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8"  Index:suggest="true">
                            <xsl:for-each select="mc:subfield[@type='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='440_00']">
                        <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                            <xsl:text>(</xsl:text>
                            <xsl:for-each select="mc:subfield[@type='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@type='v']">
                                <xsl:text>&#32;;&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:text>)</xsl:text>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='241_00']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="5"  Index:suggest="true">
                            <xsl:for-each select="mc:subfield[@type='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@type='ø']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='245_00']/mc:subfield[@type='c']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='530_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='512_00']/mc:subfield[@type='t']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='557_00']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:for-each select="mc:subfield[@type='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='558_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                </Index:group>


    </xsl:template>

</xsl:stylesheet>
