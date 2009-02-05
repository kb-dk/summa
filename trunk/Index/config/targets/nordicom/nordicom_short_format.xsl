<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>


    <xsl:template name="shortformat">
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">
                <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                    <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                    <shortrecord>
                        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                 xmlns:dc="http://purl.org/dc/elements/1.1/">
                            <rdf:Description>
                                <dc:title>
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
                                                            <xsl:text>&#32;;&#32;</xsl:text>
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>
                                                    </xsl:if>
                                                    <xsl:if test="@code='c'">
                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='c'])">
                                                            <xsl:text>&#32;:&#32;</xsl:text>
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>
                                                    </xsl:if>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:for-each>
                                    </xsl:for-each>
                                </dc:title>

                                <xsl:for-each select="mc:datafield[@tag='700']">
                                    <dc:creator>
                                        <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                        <xsl:text>&#32;</xsl:text>
                                        <xsl:value-of select="mc:subfield[@code='a']//text()"/>
                                    </dc:creator>
                                </xsl:for-each>

                                <xsl:for-each select="mc:datafield[@tag='710']">
                                    <dc:creator>
                                        <xsl:if test="mc:subfield[@code='a'or @code='c' or @code='i' or @code='k' or @code='j']">
                                            <xsl:for-each select="mc:subfield[@code='a' or @code='c' or @code='i' or @code='k' or @code='j']">
                                                <xsl:choose>
                                                    <xsl:when test="position()=1">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:choose>
                                                            <xsl:when test="@code='a'">
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@code='c'">
                                                                <xsl:if test="position()&gt;1">
                                                                    <xsl:text>.&#32;</xsl:text>
                                                                </xsl:if>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@code='i'">
                                                                <xsl:text>;&#32;</xsl:text>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@code='k'">
                                                                <xsl:text>,&#32;</xsl:text>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@code='j'">
                                                                <xsl:text>,&#32;</xsl:text>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                        </xsl:choose>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:for-each>
                                        </xsl:if>
                                    </dc:creator>
                                </xsl:for-each>

                                <dc:type xml:lang="da">
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">
                                                <xsl:choose>
                                                    <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                                        <xsl:text>artikel i bog</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                        <xsl:text>e-artikel</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>tidsskrifthefte</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'a')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xx')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xe')">
                                                                        <xsl:text>bog og e-bog</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                                        <xsl:text>bog og cd-rom</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <xsl:text>bog</xsl:text>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')">
                                                                        <xsl:text>bog og e-bog</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <xsl:text>e-bog</xsl:text>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:text>bog</xsl:text>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'m')">
                                                        <xsl:text>film</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'r')">
                                                        <xsl:text>musik og lyd</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>bog</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:type>

                                <dc:type xml:lang="en">
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">
                                                <xsl:choose>
                                                    <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                                        <xsl:text>journal article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                                        <xsl:text>book article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                        <xsl:text>e-article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>journal issue</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'a')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xx')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xe')">
                                                                        <xsl:text>book and e-book</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                                        <xsl:text>book and cd-rom</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <xsl:text>book</xsl:text>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')">
                                                                        <xsl:text>book and e-book</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <xsl:text>e-book</xsl:text>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:text>book</xsl:text>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'m')">
                                                        <xsl:text>film</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'r')">
                                                        <xsl:text>music and sound</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>book</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:type>

                                <xsl:for-each select="mc:datafield[@tag='260']/mc:subfield[@code='c']">
                                    <dc:date>
                                        <xsl:choose>
                                            <xsl:when test="contains(.,'@UD8')">
                                                <xsl:value-of select="translate(substring-before(.,'@UD8'),'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.','')"/>
                                                <xsl:text>-</xsl:text>
                                                <xsl:value-of select="translate(substring-after(.,'@UD8'),'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.','')"/>
                                            </xsl:when>
                                            <xsl:when test="contains(.,'{')">
                                                <xsl:value-of select="translate(substring-after(substring-before(.,'}'),'{'),'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.','')"/>
                                            </xsl:when>
                                            <xsl:when test="contains(.,', p')">
                                                <xsl:value-of select="translate(substring-before(.,', p'),'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.','')"/>
                                            </xsl:when>
                                            <xsl:when test="contains(.,'-')">
                                                <xsl:value-of select="translate(substring-before(.,'-'),'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.','')"/>
                                                <xsl:text>-</xsl:text>
                                                <xsl:value-of select="translate(substring-after(.,'-'),'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.','')"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="substring(translate(.,'abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ [](),.',''),1,4)"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </dc:date>
                                </xsl:for-each>

                                <xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='a']">
                                    <xsl:choose>
                                        <xsl:when test="position()=1">
                                            <dc:identifier>
                                                <xsl:text>ISBN&#32;</xsl:text>
                                                <xsl:value-of select="translate(.,' -','')"/>
                                            </dc:identifier>
                                        </xsl:when>
                                    </xsl:choose>
                                </xsl:for-each>
                                <xsl:for-each select="mc:datafield[@tag='856']">
                                    <xsl:if test="contains(../mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                        <dc:identifier>
                                            <xsl:value-of select="."/>
                                        </dc:identifier>
                                    </xsl:if>
                                </xsl:for-each>

                                <dc:format>
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position()=1],'xe')">
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')
                                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                        <xsl:text>mono</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>todo</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>mono</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:format>

                            </rdf:Description>
                        </rdf:RDF>

                    </shortrecord>
                    <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                </Index:field>
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>

                <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                    <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                    <shortrecord>
                        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                 xmlns:dc="http://purl.org/dc/elements/1.1/">
                            <rdf:Description>
                                <dc:title>
                                    <xsl:for-each select="mc:datafield[@tag='110']/mc:subfield[@code='a']">
                                        <xsl:value-of select="."/>
                                    </xsl:for-each>
                                </dc:title>

                                <xsl:for-each select="mc:datafield[@tag='100']/mc:subfield[@code='a']">
                                    <dc:creator>
                                        <xsl:call-template name="author_forward"/>    <!-- Author template ligger i nordicom_author.xsl-->
                                    </dc:creator>
                                </xsl:for-each>

                                <dc:type xml:lang="da">
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='150']">
                                                <xsl:text>tidsskriftartikel</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='140']">
                                                <xsl:text>artikel i bog</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>bog</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:type>

                                <dc:type xml:lang="en">
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='150']">
                                                <xsl:text>journal article</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='140']">
                                                <xsl:text>book article</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>book</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:type>

                                <xsl:for-each select="mc:datafield[@tag='090']/mc:subfield[@code='a']">
                                    <dc:date>
                                        <xsl:value-of select="."/>
                                    </dc:date>
                                </xsl:for-each>

                                <dc:format>
                                    <xsl:for-each select=".">
                                        <xsl:text>todo</xsl:text>
                                    </xsl:for-each>
                                </dc:format>

                            </rdf:Description>
                        </rdf:RDF>
                    </shortrecord>
                    <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                </Index:field>

            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>