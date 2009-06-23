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
                            <xsl:for-each select="mc:datafield[@tag='245']">
                                <xsl:for-each select="mc:subfield[@code='a' or @code='b' or @code='3' or @code='m' or @code='4' or @code='n' or @code='o' or @code='c' or @code='u' or @code='x' or @code='y' or @code='g']">
                                    <xsl:choose>
                                        <xsl:when test="position()=1">
                                            <xsl:value-of select="."/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:if test="@code='g'">
                                                <xsl:value-of select="."/>
                                                <xsl:text>: </xsl:text>
                                            </xsl:if>
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
                                            <xsl:if test="@code='b'">
                                                <xsl:text>&#32;</xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:if>
                                            <xsl:if test="@code='m'">
                                                <xsl:text>&#32;[</xsl:text>
                                                <xsl:value-of select="."/>
                                                <xsl:text>]</xsl:text>
                                            </xsl:if>
                                            <xsl:if test="@code='4'">
                                                <xsl:text>&#32;[</xsl:text>
                                                <xsl:value-of select="."/>
                                                <xsl:text>]</xsl:text>
                                            </xsl:if>
                                            <xsl:if test="@code='3'">
                                                <xsl:text> / </xsl:text>
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
                                            <xsl:if test="@code='c'">
                                                <xsl:if test="not(preceding-sibling::mc:subfield[@code='c'])">
                                                    <xsl:text> : </xsl:text>
                                                    <xsl:value-of select="."/>
                                                </xsl:if>

                                            </xsl:if>
                                            <xsl:if test="@code='u'">
                                                <xsl:text> : </xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:if>
                                            <xsl:if test="@code='x'">
                                                <xsl:text>. </xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:if>
                                            <xsl:if test="@code='y'">

                                                <br/>
                                                <xsl:text>- -</xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:if>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </xsl:for-each>


                        </dc:title>
                        <xsl:for-each select="mc:datafield[@tag='100']">
                            <dc:creator>
                                <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="mc:subfield[@code='a']//text()"/>


                                <xsl:for-each select="mc:subfield[@code='e']">
                                    <xsl:text>, </xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:for-each>
                                <xsl:for-each select="mc:subfield[@code='f']">
                                    <xsl:text> (</xsl:text>
                                    <xsl:value-of select="."/>
                                    <xsl:text>)</xsl:text>
                                </xsl:for-each>
                                <xsl:if test="mc:subfield[@code='c']">

                                    <xsl:choose>
                                        <xsl:when test="contains (.,'f. ')">
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                                            <xsl:text>-</xsl:text>
                                        </xsl:when>
                                        <xsl:when test="contains (.,'f.')">
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                                            <xsl:text>-</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="mc:subfield[@code='c']"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:if>
                            </dc:creator>
                        </xsl:for-each>
                        <xsl:for-each select="mc:datafield[@tag='110']">
                            <dc:creator>
                                <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                                    <xsl:for-each select="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                                        <xsl:choose>
                                            <xsl:when test="position()=1">
                                                <xsl:value-of select="."/>
                                            </xsl:when>
                                            <xsl:otherwise>

                                                <xsl:choose>
                                                    <xsl:when test="@code='a'">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='s'">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='e'">
                                                        <xsl:text>&#32;(</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>)</xsl:text>
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
                        <xsl:for-each select="mc:datafield[@tag='239']">
                            <dc:creator>

                                <xsl:for-each select="mc:subfield[@code='h']">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:for-each>
                                <xsl:for-each select="mc:subfield[@code='a']">
                                    <xsl:value-of select="."/>
                                </xsl:for-each>
                                <xsl:for-each select="mc:subfield[@code='e']">
                                    <xsl:text> </xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:for-each>
                                <xsl:for-each select="mc:subfield[@code='f']">
                                    <xsl:text> (</xsl:text>
                                    <xsl:value-of select="."/>
                                    <xsl:text>)</xsl:text>
                                </xsl:for-each>
                                <xsl:if test="mc:subfield[@code='c']">
                                    <xsl:choose>
                                        <xsl:when test="contains (.,'f. ')">
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                                            <xsl:text>-</xsl:text>
                                        </xsl:when>
                                        <xsl:when test="contains (.,'f.')">

                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                                            <xsl:text>-</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="mc:subfield[@code='c']"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:if>
                            </dc:creator>
                        </xsl:for-each>
                        <xsl:for-each select="mc:datafield[@tag='700']">
                            <dc:creator>
                                <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="mc:subfield[@code='a']//text()"/>


                                <xsl:for-each select="mc:subfield[@code='e']">
                                    <xsl:text> </xsl:text>
                                    <xsl:value-of select="."/>
                                </xsl:for-each>
                                <xsl:for-each select="mc:subfield[@code='f']">
                                    <xsl:text>(</xsl:text>
                                    <xsl:value-of select="."/>
                                    <xsl:text>)</xsl:text>
                                </xsl:for-each>
                                <xsl:if test="mc:subfield[@code='c']">

                                    <xsl:choose>
                                        <xsl:when test="contains (.,'f. ')">
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                                            <xsl:text>-</xsl:text>
                                        </xsl:when>
                                        <xsl:when test="contains (.,'f.')">
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                                            <xsl:text>-</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="mc:subfield[@code='c']"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:if>
                            </dc:creator>
                        </xsl:for-each>
                        <xsl:for-each select="mc:datafield[@tag='710']">
                            <dc:creator>
                                <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                                    <xsl:for-each select="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                                        <xsl:choose>
                                            <xsl:when test="position()=1">
                                                <xsl:value-of select="."/>
                                            </xsl:when>
                                            <xsl:otherwise>

                                                <xsl:choose>
                                                    <xsl:when test="@code='a'">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='s'">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='e'">
                                                        <xsl:text>&#32;(</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>)</xsl:text>
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
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='v'],'g')">
                                                <xsl:text>radio/tv</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='v'],'v')">
                                                <xsl:text>radio/tv</xsl:text>
                                            </xsl:when>

                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n') and contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:text>e-avis</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                                <xsl:text>avis</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:text>e-tidsskrift</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>tidsskrift</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                                        <xsl:choose>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
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
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xc')">
                                                                <xsl:text>bog og cd</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="
                                                                            contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xa')
																		 or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ia')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ic')
                                                                         or contains(/mc:record/mc:datafield[@tag='009'][position() >1]/mc:subfield[@code='g'],'if')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ih')
                                                                         or contains(/mc:record/mc:datafield[@tag='009'][position() >1]/mc:subfield[@code='g'],'ik')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ip')
                                                                         or contains(/mc:record/mc:datafield[@tag='009'][position() >1]/mc:subfield[@code='g'],'is')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'it')">
                                                                <xsl:text>bog og mikroform</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                                <xsl:text>bog og cd-rom</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tk')">
                                                                <xsl:text>bog og diskette</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xh')">
                                                                <xsl:text>bog og kasettebånd</xsl:text>
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

                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xa') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ia')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ic') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'if')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ih') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ik')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ip') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'is')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'it')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')">
                                                                <xsl:text>bog og mikroform</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:text>mikroform</xsl:text>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>bog</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>

                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'b')">
                                                <xsl:text>håndskrift</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'c')">
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xe')">
                                                                <xsl:text>node og e-node</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xc')">
                                                                <xsl:text>node og cd</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xd')">
                                                                <xsl:text>node og dvd</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                                <xsl:text>node og cd-rom</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:text>node</xsl:text>
                                                            </xsl:otherwise>


                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>node</xsl:text>
                                                    </xsl:otherwise>


                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'d')">
                                                <xsl:text>nodehåndskrift</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'e')">
                                                <xsl:text>kort</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'f')">
                                                <xsl:text>kort (håndskrift)</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'g')">
                                                <xsl:text>billede</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'m')">
                                                <xsl:text>film</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'p')">
                                                <xsl:text>punktskrift</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'r')">
                                                <xsl:text>musik og lyd</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'s')">
                                                <xsl:text>musik og lyd</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'t')">
                                                <xsl:text>elektronisk</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'u')">
                                                <xsl:text>genstand</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'v')">
                                                <xsl:text>sammensat</xsl:text>
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
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='v'],'g')">
                                                <xsl:text>radio/tv</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='v'],'v')">
                                                <xsl:text>radio/tv</xsl:text>
                                            </xsl:when>

                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n') and contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:text>e-newspaper</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                                <xsl:text>newspaper</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:text>e-journal</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>journal</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                                        <xsl:choose>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                        <xsl:text>journal article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                        <xsl:text>newspaper article</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>journal article</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
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
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xc')">
                                                                <xsl:text>book and cd</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xa')
																		 or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ia')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ic')
                                                                         or contains(/mc:record/mc:datafield[@tag='009'][position() >1]/mc:subfield[@code='g'],'if')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ih')
                                                                         or contains(/mc:record/mc:datafield[@tag='009'][position() >1]/mc:subfield[@code='g'],'ik')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'ip')
                                                                         or contains(/mc:record/mc:datafield[@tag='009'][position() >1]/mc:subfield[@code='g'],'is')
                                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'it')">
                                                                <xsl:text>book and microform</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                                <xsl:text>book and cd-rom</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tk')">
                                                                <xsl:text>book and floppy disc</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xh')">
                                                                <xsl:text>book and cassette</xsl:text>
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

                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xa') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ia')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ic') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'if')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ih') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ik')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'ip') or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'is')
                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'it')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')">
                                                                <xsl:text>book and microform</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:text>microform</xsl:text>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>book</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:when>

                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'b')">
                                                <xsl:text>manuscript</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'c')">
                                                <xsl:choose>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xx')">
                                                        <xsl:choose>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xe')">
                                                                <xsl:text>sheet music and e-sheet music</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xc')">
                                                                <xsl:text>sheet music and cd</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xd')">
                                                                <xsl:text>sheet music and dvd</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')">
                                                                <xsl:text>sheet music and cd-rom</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:text>sheet music</xsl:text>
                                                            </xsl:otherwise>


                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>sheet music</xsl:text>
                                                    </xsl:otherwise>


                                                </xsl:choose>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'d')">
                                                <xsl:text>sheet music (manuscipt)</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'e')">
                                                <xsl:text>map</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'f')">
                                                <xsl:text>map (manuscript)</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'g')">
                                                <xsl:text>picture</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'m')">
                                                <xsl:text>film</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'p')">
                                                <xsl:text>braille</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'r')">
                                                <xsl:text>music and sound</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'s')">
                                                <xsl:text>music and sound</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'t')">
                                                <xsl:text>electronic</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'u')">
                                                <xsl:text>object</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'v')">
                                                <xsl:text>composite</xsl:text>
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
                        <!-- Udkommenteret for at få link til materialetypen i den korte visning i eboeger -->
                        <!--<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='a' or @code='e']">
                            <xsl:choose>
                                <xsl:when test="position()=1">
                                    <dc:identifier>
                                        <xsl:text>ISBN </xsl:text>
                                        <xsl:value-of select="translate(.,' -','')"/>
                                    </dc:identifier>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each> -->
                        <xsl:for-each select="mc:datafield[@tag='856']/mc:subfield[@code='u']">
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
                                                        or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xa')
                                                         or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xc')
                                                           or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xd')
                                                             or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'xh')
                                                               or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'mj')
                                                                 or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'nh')
                                                                   or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'np')
                                                                     or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tb')
                                                                       or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'th')
                                                          or contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'][position() >1],'tg')">
                                                <xsl:text>mono</xsl:text>
                                            </xsl:when>

                                            <xsl:otherwise>
                                                <xsl:text>todo</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                                        <xsl:text>journal</xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text>mono</xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>

                            </xsl:for-each>
                        </dc:format>

                        <!--                  <xsl:variable name="fonogram">
                       <xsl:for-each select="mc:datafield[@tag='096']">
                           <xsl:if test="(substring(mc:subfield[@code='z'],1)='SB-dvd' or substring(mc:subfield[@code='z'],1)='SB-fon' or substring(mc:subfield[@code='z'],1)='SB-vid')">
                               true
                           </xsl:if>
                       </xsl:for-each>
                   </xsl:variable>
                   <xsl:if test="not($fonogram='')">
                     <xsl:choose>
                       <xsl:when test="count(mc:datafield[@tag='096']/mc:subfield[@code='z'])>1">

                           <xsl:variable name="blandet">
                               <xsl:for-each select="mc:datafield[@tag='096']">
                                   <xsl:if test="(substring(mc:subfield[@code='z'],1)='SB-dvd' or substring(mc:subfield[@code='z'],1)='SB-fon' or substring(mc:subfield[@code='z'],1)='SB-vid') and starts-with(mc:subfield[@code='a'],'Kla')">
                                       true
                                   </xsl:if>
                               </xsl:for-each>
                           </xsl:variable>
                           <xsl:variable name="esag">
                               <xsl:for-each select="mc:datafield[@tag='096']">
                                   <xsl:if test="not(starts-with(mc:subfield[@code='z'],'SB'))">
                                       true
                                   </xsl:if>
                               </xsl:for-each>
                           </xsl:variable>
                           <xsl:choose>
                               <xsl:when test="not ($blandet='')">
                                <dc:rights>blandet</dc:rights>
                               </xsl:when>
                                <xsl:when test="not ($esag='')">
                                <dc:rights>esag</dc:rights>
                               </xsl:when>
                                <xsl:otherwise>
                                  <dc:rights>spærret</dc:rights>
                               </xsl:otherwise>
                           </xsl:choose>
                       </xsl:when>
                           <xsl:when test="count(mc:datafield[@tag='096']/mc:subfield[@code='z'])=1">
                             <xsl:choose>
                               <xsl:when test="starts-with(mc:datafield[@tag='096']/mc:subfield[@code='a'],'Kla')">
                                   <dc:rights>udlån</dc:rights>
                               </xsl:when>
                                <xsl:otherwise>
                                   <dc:rights>spærret</dc:rights>
                               </xsl:otherwise>
                           </xsl:choose>
                       </xsl:when>
                     </xsl:choose>
                   </xsl:if>     -->
                    </rdf:Description>
                </rdf:RDF>

            </shortrecord>
            <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </Index:field>
    </xsl:template>

</xsl:stylesheet>