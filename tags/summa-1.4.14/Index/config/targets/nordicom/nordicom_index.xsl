<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:include href="nordicom_short_format.xsl" />
    <xsl:include href="nordicom_author.xsl" />
    <xsl:include href="nordicom_title.xsl" />
    <xsl:include href="nordicom_subject.xsl" />
    <xsl:include href="nordicom_publisher.xsl" />
    <xsl:include href="nordicom_other.xsl" />
    <xsl:include href="nordicom_notes.xsl" />
    <xsl:include href="nordicom_identifiers.xsl" />
    <xsl:include href="nordicom_material.xsl" />
    <xsl:include href="nordicom_lma.xsl" />

    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">

        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                        Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="nordicom">
            <xsl:attribute name="Index:id">
                <xsl:text>ncom_</xsl:text>
                <xsl:value-of select="mc:record/mc:datafield[@tag='001']/mc:subfield[@code='a']" />
            </xsl:attribute>

            <xsl:for-each select="mc:record">
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

                    <xsl:choose>
                        <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">

                            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='l']">
                                <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='041']/mc:subfield[@code='a' or @code='p' or @code='u' or @code='e' or @code='d']">
                                <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='041']/mc:subfield[@code='c']">
                                <Index:field Index:repeat="true" Index:name="original_language" Index:navn="ou" Index:type="token">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='856']/mc:subfield[@code='u']">
                                <Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='440']">
                                <Index:field Index:repeat="false" Index:name="series_normalised" Index:navn="lse" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:for-each select="mc:subfield[@code='a']">
                                        <xsl:value-of select="."/>
                                    </xsl:for-each>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='l']">
                                <Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='041']/mc:subfield[@code='a']">
                                <Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='008']">
                                <xsl:choose>
                                    <xsl:when test="contains(mc:subfield[@code='u'],'?')">
                                        <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                                            <xsl:value-of select="java:dk.statsbiblioteket.sbandex.plugins.YearRange.makeRange(mc:subfield[@code='a'], mc:subfield[@code='z'])"/>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="contains(mc:subfield[@code='u'],'o')">
                                        <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                                            <xsl:value-of select="java:dk.statsbiblioteket.sbandex.plugins.YearRange.makeRange(mc:subfield[@code='a'],'2030')"/>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="contains(mc:subfield[@code='u'],'r')">
                                        <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                                            <xsl:value-of select="java:dk.statsbiblioteket.sbandex.plugins.YearRange.makeRange(mc:subfield[@code='a'],'2030')"/>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:for-each select="mc:subfield[@code='a' or @code='z']">
                                            <xsl:choose>
                                                <xsl:when test="contains(.,'?')">
                                                    <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                                                        <xsl:value-of select="java:dk.statsbiblioteket.sbandex.plugins.YearRange.makeRange(.)"/>
                                                    </Index:field>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                                                        <xsl:value-of select="."/>
                                                    </Index:field>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:for-each>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='008']">
                                <xsl:for-each select="mc:subfield[@code='a' or @code='z']">
                                    <xsl:if test="substring(.,0)!='9999'">
                                        <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="10">
                                            <xsl:value-of select="translate(.,'0123456789?','01234567890')"/>
                                        </Index:field>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z']">
                                <xsl:choose>
                                    <xsl:when test="@code='z'">
                                        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                                            <xsl:value-of select="translate(.,'0123456789?','01234567890')"/>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="@code='a' and not(../mc:subfield[@code='z']) ">
                                        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                                            <xsl:value-of select="translate(.,'0123456789?','01234567890')"/>
                                        </Index:field>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:for-each>
                            <xsl:if test="not(mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z'])">
                                <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:text>0</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z']">
                                <xsl:choose>
                                    <xsl:when test="@code='z'">
                                        <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
                                            <xsl:value-of select="translate(.,'0123456789?','01234567899')"/>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="@code='a' and not(../mc:subfield[@code='z']) ">
                                        <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
                                            <xsl:value-of select="translate(.,'0123456789?','01234567899')"/>
                                        </Index:field>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:for-each>
                            <xsl:if test="not(mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z'])">
                                <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword">
                                    <xsl:text>9999</xsl:text>
                                </Index:field>
                            </xsl:if>

                            <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="100">
                                <xsl:for-each select="mc:datafield[@tag='245']">
                                    <xsl:for-each select="mc:subfield[@code='a' or @code='c']">
                                        <xsl:choose>
                                            <xsl:when test="position()=1">
                                                <xsl:value-of select="."/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:if test="@code='a'">
                                                    <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text>&#32;:&#32;</xsl:text>
                                                        <xsl:value-of select="."/>
                                                    </xsl:if>
                                                    <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text>;</xsl:text>
                                                        <xsl:value-of select="."/>
                                                    </xsl:if>
                                                </xsl:if>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </xsl:for-each>
                            </Index:field>

                            <xsl:for-each select="mc:datafield[@tag='001']/mc:subfield[@code='d']">
                                <Index:field Index:repeat="true" Index:name="op" Index:navn="op" Index:type="token" Index:boostFactor="2" Index:freetext="false">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                         </xsl:when>

                       <!-- Det gamle format -->


                        <xsl:otherwise>
                            <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='b']">
                                <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='b']">
                                <Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='a']">
                                <Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
                                        <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='a']">
                                <Index:field Index:name="year" Index:navn="year" Index:type="number" Index:boostFactor="10">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>

                            <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='a']">
                                <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:if test="not(mc:datafield[@tag='090']/mc:subfield[@code='a'])">
                                <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:text>0</xsl:text>
                                </Index:field>
                            </xsl:if>

                            <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='a']">
                                <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:value-of select="translate(.,'0123456789?','01234567899')"/>
                                </Index:field>
                            </xsl:for-each>
                            <xsl:if test="not(mc:datafield[@tag='090']/mc:subfield[@code='a'])">
                                <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword">
                                    <xsl:text>9999</xsl:text>
                                </Index:field>
                            </xsl:if>

                            <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="100">
                                <xsl:for-each select="mc:datafield[@tag='110']/mc:subfield[@code='a']">
                                        <xsl:choose>
                                            <xsl:when test="position()=1">
                                                <xsl:value-of select="."/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:if test="@code='a'">
                                                    <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text>&#32;:&#32;</xsl:text>
                                                        <xsl:value-of select="."/>
                                                    </xsl:if>
                                                    <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text>;</xsl:text>
                                                        <xsl:value-of select="."/>
                                                    </xsl:if>
                                                </xsl:if>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                </xsl:for-each>
                            </Index:field>

                        </xsl:otherwise>
                    </xsl:choose>

                </Index:fields>
            </xsl:for-each>
        </Index:document>
    </xsl:template>
</xsl:stylesheet>
