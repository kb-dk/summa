<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="sort">

        <xsl:for-each select="mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='a' or @code='z']">
                                <xsl:choose>
                                    <xsl:when test="@code='z'">
                                        <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword"  Index:boostFactor="10">
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
                            <xsl:if test="not(mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='a' or @code='z'])">
                                <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:text>0</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:for-each select="mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='a' or @code='z']">
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
                            <xsl:if test="not(mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='a' or @code='z'])">
                                <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword">
                                    <xsl:text>9999</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:type="keyword" Index:sortLocale="da" Index:boostFactor="100">
                            <xsl:choose>
                            <xsl:when test ="mc:datafield[@tag='C45']">
                            <xsl:for-each select="mc:datafield[@tag='C45']">

                                    <xsl:for-each select="mc:subfield[@code ='A' or @code='a' or @code='b' or  @code='n' or @code='o' or  @code='x' or @code='y' or @code='g']">
                                        <xsl:choose>

                                            <xsl:when test="position()=1">
                                            <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                            </xsl:when>

                                            <xsl:otherwise>
                     <xsl:if test="@code='g'">
                                                    <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                                    <xsl:text>: </xsl:text>
                                                </xsl:if>
                                                <xsl:if test="@code='a'">
                                                    <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                                    </xsl:if>
                                                    <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text>;</xsl:text>
                                                    <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                                    </xsl:if>
                                                </xsl:if>
                                                <xsl:if test="@code='b'">
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                                <xsl:if test="@code='n'">
                                                    <xsl:text>. </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='o'">
                                                    <xsl:text>. </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                                <xsl:if test="@code='x'">
                                                    <xsl:text>. </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='y'">

                                                    <xsl:text>- -</xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>

                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                            <xsl:for-each select="mc:datafield[@tag='245']">

                                    <xsl:for-each select="mc:subfield[@code ='A' or @code='a' or @code='b' or  @code='n' or @code='o' or  @code='x' or @code='y' or @code='g']">
                                        <xsl:choose>

                                            <xsl:when test="position()=1">
                                            <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                            </xsl:when>

                                            <xsl:otherwise>
                     <xsl:if test="@code='g'">
                                                    <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                                    <xsl:text>: </xsl:text>
                                                </xsl:if>
                                                <xsl:if test="@code='a'">
                                                    <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                                    </xsl:if>
                                                    <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                        <xsl:text>;</xsl:text>
                                                    <xsl:choose>
                                            <xsl:when test="contains(.,'¤')">
                                <xsl:value-of select="substring-after(.,'¤')"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                        </xsl:otherwise>
                             </xsl:choose>
                                                    </xsl:if>
                                                </xsl:if>
                                                <xsl:if test="@code='b'">
                                                    <xsl:text> </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                                <xsl:if test="@code='n'">
                                                    <xsl:text>. </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='o'">
                                                    <xsl:text>. </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                                <xsl:if test="@code='x'">
                                                    <xsl:text>. </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='y'">

                                                    <xsl:text>- -</xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>

                                </xsl:for-each>
                                </xsl:otherwise>
                                </xsl:choose>
                            </Index:field>



					
    </xsl:template>
</xsl:stylesheet>
