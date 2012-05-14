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
                <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                    <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                    <shortrecord>
                        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                 xmlns:dc="http://purl.org/dc/elements/1.1/">
                            <rdf:Description>
                                <dc:title>
                                    <xsl:for-each select="mc:field[@type='245_00']">
                                        <xsl:for-each select="mc:subfield[@type='a' or @type='c']">
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
                                                            <xsl:text>&#32;;&#32;</xsl:text>
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>
                                                    </xsl:if>
                                                    <xsl:if test="@type='c'">
                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@type='c'])">
                                                            <xsl:text>&#32;:&#32;</xsl:text>
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>
                                                    </xsl:if>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:for-each>
                                    </xsl:for-each>
                                </dc:title>

                                <xsl:for-each select="mc:field[@type='700_00']">
                                    <dc:creator>
                                        <xsl:value-of select="mc:subfield[@type='h']//text()"/>
                                        <xsl:text>&#32;</xsl:text>
                                        <xsl:value-of select="mc:subfield[@type='a']//text()"/>
                                    </dc:creator>
                                </xsl:for-each>

                                <xsl:for-each select="mc:field[@type='710_00']">
                                    <dc:creator>
                                        <xsl:if test="mc:subfield[@type='a'or @type='c' or @type='i' or @type='k' or @type='j']">
                                            <xsl:for-each select="mc:subfield[@type='a' or @type='c' or @type='i' or @type='k' or @type='j']">
                                                <xsl:choose>
                                                    <xsl:when test="position()=1">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:choose>
                                                            <xsl:when test="@type='a'">
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@type='c'">
                                                                <xsl:if test="position()&gt;1">
                                                                    <xsl:text>.&#32;</xsl:text>
                                                                </xsl:if>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@type='i'">
                                                                <xsl:text>;&#32;</xsl:text>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@type='k'">
                                                                <xsl:text>,&#32;</xsl:text>
                                                                <xsl:value-of select="."/>
                                                            </xsl:when>
                                                            <xsl:when test="@type='j'">
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
                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='008_00']/mc:subfield[@type='t'],'a')">
                                                <xsl:choose>
                                                    <xsl:when test="/marc/mc:record/mc:field[@type='557_00']">
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="/marc/mc:record/mc:field[@type='558_00']">
                                                        <xsl:text>artikel i bog</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'],'xe')">
                                                        <xsl:text>e-artikel</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>tidsskrifthefte</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='a'],'a')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'],'xx')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'xe')">
                                                                        <xsl:text>bog og e-bog</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'tb')">
                                                                        <xsl:text>bog og cd-rom</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <xsl:text>bog</xsl:text>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'],'xe')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'xx')">
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
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='a'],'m')">
                                                        <xsl:text>film</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='a'],'r')">
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
                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='008_00']/mc:subfield[@type='t'],'a')">
                                                <xsl:choose>
                                                    <xsl:when test="/marc/mc:record/mc:field[@type='557_00']">
                                                        <xsl:text>journal article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="/marc/mc:record/mc:field[@type='558_00']">
                                                        <xsl:text>book article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'],'xe')">
                                                        <xsl:text>e-article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>journal issue</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='a'],'a')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'],'xx')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'xe')">
                                                                        <xsl:text>book and e-book</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'tb')">
                                                                        <xsl:text>book and cd-rom</xsl:text>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                        <xsl:text>book</xsl:text>
                                                                    </xsl:otherwise>
                                                                </xsl:choose>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'],'xe')">
                                                                <xsl:choose>
                                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'xx')">
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
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='a'],'m')">
                                                        <xsl:text>film</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='a'],'r')">
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

                                <xsl:for-each select="mc:field[@type='260_00']/mc:subfield[@type='c']">
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

                                <xsl:for-each select="mc:field[@type='021_00']/mc:subfield[@type='a']">
                                    <xsl:choose>
                                        <xsl:when test="position()=1">
                                            <dc:identifier>
                                                <xsl:text>ISBN&#32;</xsl:text>
                                                <xsl:value-of select="translate(.,' -','')"/>
                                            </dc:identifier>
                                        </xsl:when>
                                    </xsl:choose>
                                </xsl:for-each>
                                <xsl:for-each select="mc:field[@type='856_00']">
                                    <xsl:if test="contains(../mc:field[@type='009_00']/mc:subfield[@type='g'],'xe')">
                                        <dc:identifier>
                                            <xsl:value-of select="."/>
                                        </dc:identifier>
                                    </xsl:if>
                                </xsl:for-each>

                                <dc:format>
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position()=1],'xe')">
                                                <xsl:choose>
                                                    <xsl:when test="contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'xx')
                                                        or contains(/marc/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g'][position() >1],'tb')">
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
    </xsl:template>

</xsl:stylesheet>