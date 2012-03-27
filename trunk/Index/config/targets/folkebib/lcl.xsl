<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">
    <xsl:template name="lcl">
        <Index:group Index:suggest="true" Index:navn="lcl" Index:name="lcl">

            <xsl:for-each select="mc:datafield[@tag='652'  or @tag='655' or @tag='G52' or @tag='G55']">
                <Index:field Index:repeat="false" Index:name="ldk" Index:navn="ldk" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield">
                        <xsl:if test="@code='m'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='p'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='i'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='n'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='o'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='q'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='r'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='v'">
                            <xsl:text>:</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='z'">
                            <xsl:text>-</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='a'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:if>
                        <xsl:if test="@code='h'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='c'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='e'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='f'">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>)</xsl:text>
                        </xsl:if>
                        <xsl:if test="@code='t'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='b'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>

                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='654'  or @tag='G54']">
                <Index:field Index:repeat="false" Index:name="lgd" Index:navn="lgd" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield">
                        <xsl:if test="@code='m'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='p'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='i'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='n'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='o'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='q'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='r'">
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='v'">
                            <xsl:text>:</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='z'">
                            <xsl:text>-</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='a'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:if>
                        <xsl:if test="@code='h'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='c'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='e'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='f'">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>)</xsl:text>
                        </xsl:if>
                        <xsl:if test="@code='t'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='b'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>

                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:if test="mc:datafield[@tag='652'or @tag='G52']/mc:subfield[@code='o' or @code='p']">
                <Index:field Index:repeat="false" Index:name="lok" Index:navn="lok" Index:type="keyword" Index:boostFactor="10">
                    <xsl:choose>
                        <xsl:when test="mc:datafield[@tag='G52']">
                            <xsl:for-each select="mc:datafield[@tag='G52']/mc:subfield[@code='m' or @code='o']">
                                <xsl:if test="@code='m'">
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='o'">
                                    <xsl:value-of select="."/>
                                </xsl:if>

                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="mc:datafield[@tag='652']">
                            <xsl:for-each select="mc:datafield[@tag='652']/mc:subfield[@code='m' or @code='o']">
                                <xsl:if test="@code='m'">
                                    <xsl:value-of select="."/>
                                </xsl:if>
                                <xsl:if test="@code='o'">
                                    <xsl:value-of select="."/>
                                </xsl:if>

                            </xsl:for-each>

                        </xsl:when>
                    </xsl:choose>
                </Index:field>
            </xsl:if>

        </Index:group>
    </xsl:template>
</xsl:stylesheet>
