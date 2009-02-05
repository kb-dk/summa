<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="title">
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">
                <Index:group Index:name="ti" Index:navn="ti" >
                    <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:for-each select=".">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='p']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='745']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8"  Index:suggest="true">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='440']">
                        <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                            <xsl:text>(</xsl:text>
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='v']">
                                <xsl:text>&#32;;&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:text>)</xsl:text>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='241']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="5"  Index:suggest="true">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='ø']">
                                <xsl:text>&#32;</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='c']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='530']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='512']/mc:subfield[@code='t']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='557']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                </Index:group>
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='110']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='120']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                        <xsl:value-of select="."/>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='110']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                        <xsl:for-each select=".">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>
            </xsl:otherwise>

        </xsl:choose>

    </xsl:template>

</xsl:stylesheet>
