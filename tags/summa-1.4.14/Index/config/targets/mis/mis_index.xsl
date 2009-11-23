<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:include href="mis_short_format.xsl" />
    <xsl:include href="mis_author.xsl" />
    <xsl:include href="mis_title.xsl" />
    <xsl:include href="mis_subject.xsl" />
    <xsl:include href="mis_publisher.xsl" />
    <xsl:include href="mis_other.xsl" />
    <xsl:include href="mis_notes.xsl" />
    <xsl:include href="mis_identifiers.xsl" />
    <xsl:include href="mis_material.xsl" />
    <xsl:include href="mis_lma.xsl" />

    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">

        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                        Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="mis">
            <xsl:attribute name="Index:id">
                <xsl:text>mis_</xsl:text>
                <xsl:value-of select="marc/mc:record/mc:field[@type='001_00']/mc:subfield[@type='a']" />
            </xsl:attribute>

            <xsl:for-each select="marc/mc:record">
                <Index:fields>
                    <xsl:call-template name="shortformat" />
                    <xsl:call-template name="author" />
                    <xsl:call-template name="title" />
                    <xsl:call-template name="subject" />
                    <xsl:call-template name="publication_data" />
                    <xsl:call-template name="other" />
                    <xsl:call-template name="notes" />
                    <xsl:call-template name="identifiers" />
                    <xsl:call-template name="material" />
                    <xsl:call-template name="lma" />



                    <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='b']">
                        <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='b']">
                        <Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='a']">
                        <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='a']">
                        <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='a']">
                        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:if test="not(mc:field[@type='090_00']/mc:subfield[@type='a'])">
                        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                            <xsl:text>0</xsl:text>
                        </Index:field>
                    </xsl:if>

                    <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='a']">
                        <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="translate(.,'0123456789?','01234567899')"/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:if test="not(mc:field[@type='090_00']/mc:subfield[@type='a'])">
                        <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword">
                            <xsl:text>9999</xsl:text>
                        </Index:field>
                    </xsl:if>

                    <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="100">
                        <xsl:for-each select="mc:field[@type='110_00']/mc:subfield[@type='a']">
                            <xsl:choose>
                                <xsl:when test="position()=1">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:if test="@type='a'">
                                        <xsl:if test="not(preceding-sibling::mc:subfield[@type='a'])">
                                            <xsl:text>&#32;:&#32;</xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:if>
                                        <xsl:if test="(preceding-sibling::mc:subfield[@type='a'])">
                                            <xsl:text>;</xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:if>
                                    </xsl:if>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>

                </Index:fields>
            </xsl:for-each>
        </Index:document>
    </xsl:template>
</xsl:stylesheet>
