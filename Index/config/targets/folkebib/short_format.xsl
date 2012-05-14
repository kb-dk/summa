<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>



    <xsl:template name="shortformat">
        <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
            <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
            <shortrecord>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <rdf:Description>
                        <dc:title>
                            <xsl:choose>
                                <xsl:when test="mc:datafield[@tag='C39']">
                                    <xsl:for-each select="mc:datafield[@tag='239']">
                                        <xsl:for-each select="mc:subfield[@code='a'or @code='h' or @code='e' or @code='f' or @code='c' or @code='7' or @code='ø' or @code='t' or@code='u' or @code='v']">
                                            <xsl:if test="@code='a'">
                                                <xsl:value-of select="translate(.,'¤','')"/>
                                            </xsl:if>

                                            <xsl:if test="@code='h'">
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
                                            <xsl:for-each select="subfield[@code='c']">
                                                <xsl:text>, </xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:for-each>
                                            <xsl:if test="@code='7'">

                                                <xsl:value-of select="."/>

                                            </xsl:if>
                                            <xsl:if test="@code='t'">
                                                <xsl:choose>
                                                    <xsl:when test="not(preceding-sibling::mc:subfield[@code='a' or @code='h' or @code='f' or @code='e' or @code='c' or @code='7'])">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(preceding-sibling::mc:subfield[@code='a' or @code='h' or @code='f' or @code='e' or @code='c'])">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                </xsl:choose>
                                            </xsl:if>


                                            <xsl:if test="@code='ø'">

                                                <xsl:choose>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> [</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>]</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                </xsl:choose>

                                            </xsl:if>



                                            <xsl:if test="@code='u'">

                                                <xsl:choose>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                </xsl:choose>

                                            </xsl:if>
                                            <xsl:if test="@code='v'">
                                                <xsl:choose>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="."/>

                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:if test="(preceding-sibling::mc:subfield[@code='v'])">
                                                            <xsl:text> ; </xsl:text>
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>

                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='v'])">
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>
                                                    </xsl:when>
                                                </xsl:choose>

                                            </xsl:if>

                                        </xsl:for-each>

                                    </xsl:for-each>

                                </xsl:when>
                                <xsl:when test="mc:datafield[@tag='239']">
                                    <xsl:for-each select="mc:datafield[@tag='239']">
                                        <xsl:for-each select="mc:subfield[@code='a'or @code='h' or @code='e' or @code='f' or @code='c' or @code='7' or @code='ø' or @code='t' or@code='u' or @code='v']">
                                            <xsl:if test="@code='a'">
                                                <xsl:value-of select="translate(.,'¤','')"/>
                                            </xsl:if>

                                            <xsl:if test="@code='h'">
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
                                            <xsl:for-each select="subfield[@code='c']">
                                                <xsl:text>, </xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:for-each>
                                            <xsl:if test="@code='7'">

                                                <xsl:value-of select="."/>

                                            </xsl:if>
                                            <xsl:if test="@code='t'">
                                                <xsl:choose>
                                                    <xsl:when test="not(preceding-sibling::mc:subfield[@code='a' or @code='h' or @code='f' or @code='e' or @code='c' or @code='7'])">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(preceding-sibling::mc:subfield[@code='a' or @code='h' or @code='f' or @code='e' or @code='c'])">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                </xsl:choose>
                                            </xsl:if>


                                            <xsl:if test="@code='ø'">

                                                <xsl:choose>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> [</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>]</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                </xsl:choose>

                                            </xsl:if>



                                            <xsl:if test="@code='u'">

                                                <xsl:choose>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                </xsl:choose>

                                            </xsl:if>
                                            <xsl:if test="@code='v'">
                                                <xsl:choose>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=0">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="."/>

                                                    </xsl:when>
                                                    <xsl:when test="(count(preceding-sibling::mc:subfield[@code='7']) mod 2)=1">
                                                        <xsl:if test="(preceding-sibling::mc:subfield[@code='v'])">
                                                            <xsl:text> ; </xsl:text>
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>

                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='v'])">
                                                            <xsl:value-of select="."/>
                                                        </xsl:if>
                                                    </xsl:when>
                                                </xsl:choose>

                                            </xsl:if>

                                        </xsl:for-each>

                                    </xsl:for-each>

                                </xsl:when>

                                <xsl:when test="mc:datafield[@tag='C45']">
                                    <xsl:for-each select="mc:datafield[@tag='C45']">
                                        <xsl:for-each select="mc:subfield[@code='a' or @code='b' or @code='æ' or @code='m' or @code='ø' or @code='n' or @code='o' or @code='c' or @code='u' or @code='x' or @code='y' or @code='g']">
                                            <xsl:choose>
                                                <xsl:when test="position()=1">
                                                    <xsl:value-of select="translate(.,'¤\','')"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:if test="@code='g'">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                        <xsl:text>: </xsl:text>
                                                    </xsl:if>
                                                    <xsl:if test="@code='a'">
                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                            <xsl:text> : </xsl:text>
                                                            <xsl:value-of select="translate(.,'¤\','')"/>
                                                        </xsl:if>
                                                        <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                            <xsl:text>;</xsl:text>
                                                            <xsl:value-of select="translate(.,'¤','')"/>
                                                        </xsl:if>
                                                    </xsl:if>
                                                    <xsl:if test="@code='b'">
                                                        <xsl:text> </xsl:text>
                                                        <xsl:value-of select="."/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='m'">
                                                        <xsl:text> [</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>]</xsl:text>
                                                    </xsl:if>
                                                    <xsl:if test="@code='ø'">
                                                        <xsl:text> [</xsl:text>
                                                        <xsl:value-of select="translate(.,'¤\','')"/>
                                                        <xsl:text>]</xsl:text>
                                                    </xsl:if>
                                                    <xsl:if test="@code='æ'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤\','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='n'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='o'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='c'">
                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='c'])">
                                                            <xsl:text> : </xsl:text>
                                                            <xsl:value-of select="translate(.,'¤\','')"/>
                                                        </xsl:if>

                                                    </xsl:if>
                                                    <xsl:if test="@code='u'">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='x'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='y'">

                                                        <br/>
                                                        <xsl:text>- -</xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:for-each>
                                    </xsl:for-each>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:for-each select="mc:datafield[@tag='245']">
                                        <xsl:for-each select="mc:subfield[@code='a' or @code='b' or @code='æ' or @code='m' or @code='ø' or @code='n' or @code='o' or @code='c' or @code='u' or @code='x' or @code='y' or @code='g']">
                                            <xsl:choose>
                                                <xsl:when test="position()=1">
                                                    <xsl:value-of select="translate(.,'¤','')"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:if test="@code='g'">
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                        <xsl:text>: </xsl:text>
                                                    </xsl:if>
                                                    <xsl:if test="@code='a'">
                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                            <xsl:text> : </xsl:text>
                                                            <xsl:value-of select="translate(.,'¤','')"/>
                                                        </xsl:if>
                                                        <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                            <xsl:text>;</xsl:text>
                                                            <xsl:value-of select="translate(.,'¤','')"/>
                                                        </xsl:if>
                                                    </xsl:if>
                                                    <xsl:if test="@code='b'">
                                                        <xsl:text> </xsl:text>
                                                        <xsl:value-of select="."/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='m'">
                                                        <xsl:text> [</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>]</xsl:text>
                                                    </xsl:if>
                                                    <xsl:if test="@code='ø'">
                                                        <xsl:text> [</xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                        <xsl:text>]</xsl:text>
                                                    </xsl:if>
                                                    <xsl:if test="@code='æ'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='n'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='o'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='c'">
                                                        <xsl:if test="not(preceding-sibling::mc:subfield[@code='c'])">
                                                            <xsl:text> : </xsl:text>
                                                            <xsl:value-of select="translate(.,'¤','')"/>
                                                        </xsl:if>

                                                    </xsl:if>
                                                    <xsl:if test="@code='u'">
                                                        <xsl:text> : </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='x'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                    <xsl:if test="@code='y'">

                                                        <br/>
                                                        <xsl:text>- -</xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:if>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:for-each>
                                    </xsl:for-each>
                                </xsl:otherwise>
                            </xsl:choose>


                        </dc:title>
                        <xsl:choose>
                            <xsl:when test="mc:datafield[@tag='B00']">
                                <xsl:for-each select="mc:datafield[@tag='B00']">
                                    <xsl:if test="not(substring(mc:subfield[@code='a'],1)='-')">
                                    <dc:creator>
                                        <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                        <xsl:text> </xsl:text>
                                        <xsl:for-each select="mc:subfield[@code='a']//text()">

                                            <xsl:value-of select="translate(.,'¤','')"/>


                                        </xsl:for-each>

                                        <xsl:for-each select="mc:subfield[@code='e']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='f']">
                                            <xsl:text> (</xsl:text>
                                            <xsl:value-of select="."/>
                                            <xsl:text>)</xsl:text>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='c']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                    </dc:creator>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="mc:datafield[@tag='B10']">
                                 <xsl:if test="not(substring(mc:subfield[@code='a'],1)='-')">
                                <xsl:for-each select="mc:datafield[@tag='B10']">
                                    <dc:creator>
                                        <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                                            <xsl:for-each select="mc:subfield">
                                                <xsl:choose>
                                                    <xsl:when test="@code='a'">
                                                        <xsl:value-of select="translate(.,'¤','')"/>

                                                    </xsl:when>
                                                    <xsl:when test="@code='s'">

                                                        <xsl:value-of select="translate(.,'¤','')"/>


                                                    </xsl:when>
                                                    <xsl:when test="@code='e'">
                                                        <xsl:text> (</xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                        <xsl:text>)</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="@code='c'">
                                                        <xsl:if test="position()&gt;1">
                                                            <xsl:text>. </xsl:text>
                                                        </xsl:if>

                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='i'">
                                                        <xsl:text>; </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='k'">
                                                        <xsl:text>, </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='j'">
                                                        <xsl:text>, </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>

                                                </xsl:choose>
                                            </xsl:for-each>

                                        </xsl:if>
                                    </dc:creator>
                                </xsl:for-each>
                                 </xsl:if>
                            </xsl:when>
                            <xsl:when test="mc:datafield[@tag='C39']">
                                 <xsl:if test="not(substring(mc:subfield[@code='a'],1)='-')">
                                <xsl:for-each select="mc:datafield[@tag='C39']">
                                    <dc:creator>
                                        <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                        <xsl:text>&#160;</xsl:text>
                                        <xsl:for-each select="mc:subfield[@code='a']//text()">

                                            <xsl:value-of select="translate(.,'¤','')"/>


                                        </xsl:for-each>

                                        <xsl:for-each select="mc:subfield[@code='e']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='f']">
                                            <xsl:text> (</xsl:text>
                                            <xsl:value-of select="."/>
                                            <xsl:text>)</xsl:text>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='c']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                    </dc:creator>
                                </xsl:for-each>
                                   </xsl:if>
                            </xsl:when>
                            <xsl:when test="mc:datafield[@tag='100']">
                                <xsl:for-each select="mc:datafield[@tag='100']">
                                    <dc:creator>
                                        <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                        <xsl:text> </xsl:text>
                                        <xsl:for-each select="mc:subfield[@code='a']//text()">

                                            <xsl:value-of select="translate(.,'¤','')"/>


                                        </xsl:for-each>

                                        <xsl:for-each select="mc:subfield[@code='e']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='f']">
                                            <xsl:text> (</xsl:text>
                                            <xsl:value-of select="."/>
                                            <xsl:text>)</xsl:text>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='c']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                    </dc:creator>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="mc:datafield[@tag='239']">
                                <xsl:for-each select="mc:datafield[@tag='239']">
                                    <dc:creator>
                                        <xsl:value-of select="mc:subfield[@code='h']//text()"/>
                                        <xsl:text> </xsl:text>
                                        <xsl:for-each select="mc:subfield[@code='a']//text()">

                                            <xsl:value-of select="translate(.,'¤','')"/>


                                        </xsl:for-each>

                                        <xsl:for-each select="mc:subfield[@code='e']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='f']">
                                            <xsl:text> (</xsl:text>
                                            <xsl:value-of select="."/>
                                            <xsl:text>)</xsl:text>
                                        </xsl:for-each>
                                        <xsl:for-each select="mc:subfield[@code='c']">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                    </dc:creator>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="mc:datafield[@tag='110']">
                                <xsl:for-each select="mc:datafield[@tag='110']">
                                    <dc:creator>
                                        <xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                                            <xsl:for-each select="mc:subfield">
                                                <xsl:choose>
                                                    <xsl:when test="@code='a'">
                                                        <xsl:value-of select="translate(.,'¤','')"/>

                                                    </xsl:when>
                                                    <xsl:when test="@code='s'">

                                                        <xsl:value-of select="translate(.,'¤','')"/>


                                                    </xsl:when>
                                                    <xsl:when test="@code='e'">
                                                        <xsl:text> (</xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                        <xsl:text>)</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="@code='c'">
                                                        <xsl:if test="position()&gt;1">
                                                            <xsl:text>. </xsl:text>
                                                        </xsl:if>

                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='i'">
                                                        <xsl:text>; </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='k'">
                                                        <xsl:text>, </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>
                                                    <xsl:when test="@code='j'">
                                                        <xsl:text>, </xsl:text>
                                                        <xsl:value-of select="translate(.,'¤','')"/>
                                                    </xsl:when>

                                                </xsl:choose>
                                            </xsl:for-each>

                                        </xsl:if>
                                    </dc:creator>
                                </xsl:for-each>
                            </xsl:when>
                        </xsl:choose>

                        <dc:type xml:lang="da">
                            <xsl:for-each select=".">
                                <xsl:choose>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')
    or contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>
                                            <xsl:when
                                                    test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n') and contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:value-of select="'netdokument (avis)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:value-of select="'netdokument (tidsskrift)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                                <xsl:value-of select="'avis'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                                <xsl:value-of select="'årbog'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                                <xsl:value-of select="'tidsskrift'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                                <xsl:value-of select="'serie'"/>
                                            </xsl:when>

                                            <xsl:otherwise>
                                                <xsl:value-of select="'periodicum'"/>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')
            or contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'a')">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                                <xsl:value-of select="'sang'"/>
                                            </xsl:when>
                                            <xsl:otherwise>


                                                <xsl:choose>

                                                    <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                                        <xsl:choose>
                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                        and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                                <xsl:value-of
                                                                        select="'anmeldelse (tidsskrift)'"/>
                                                            </xsl:when>

                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                        and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                                <xsl:value-of
                                                                        select="'anmeldelse (avis)'"/>
                                                            </xsl:when>
                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                                <xsl:value-of
                                                                        select="'tidsskriftartikel'"/>
                                                            </xsl:when>
                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                                <xsl:value-of
                                                                        select="'avisartikel'"/>

                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of
                                                                        select="'tidsskriftartikel'"/>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                                        <xsl:value-of select="'artikel i bog'"/>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                        <xsl:value-of select="'netdokument (artikel)'"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of select="'artikel'"/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:choose>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='A09'] and
                    not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'td'))
                    and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'te'))">
                                                <xsl:for-each select="/mc:record/mc:datafield[@tag='A09'][position()=1]">
                                                    <xsl:choose>
                                                        <xsl:when test="contains(mc:subfield[@code='a'][position()=1],'p')">
                                                            <xsl:variable name="code"><xsl:call-template name="code_1_letter"/></xsl:variable>
                                                            <xsl:value-of select="$code"/>

                                                        </xsl:when>
                                                        <xsl:when test="mc:subfield[@code='g']">
                                                            <xsl:for-each select="mc:subfield[@code='g']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_2_letters"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:for-each select="mc:subfield[@code='a']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_1_letter"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:for-each>
                                            </xsl:when>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='009']">
                                                <xsl:for-each select="/mc:record/mc:datafield[@tag='009'][position()=1]">
                                                    <xsl:choose>
                                                        <xsl:when test="contains(mc:subfield[@code='a'][position()=1],'p')
                                     or contains(mc:subfield[@code='a'][position()=1],'c')
                                     or contains(mc:subfield[@code='a'][position()=1],'e')">
                                                            <xsl:for-each select="mc:subfield[@code='a'][position()=1]">
                                                                <xsl:variable name="code"><xsl:call-template name="code_1_letter"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>
                                                            </xsl:for-each>
                                                            <xsl:for-each select="mc:subfield[@code='g']">
                                                                <xsl:if test="not(contains(.,'xx'))">
                                                                    <xsl:text> + </xsl:text>
                                                                    <xsl:variable name="code"><xsl:call-template name="code_2_letters"/></xsl:variable>
                                                                    <xsl:value-of select="$code"/>
                                                                </xsl:if>
                                                            </xsl:for-each>

                                                        </xsl:when>
                                                        <xsl:when test="mc:subfield[@code='g']">
                                                            <xsl:for-each select="mc:subfield[@code='g']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_2_letters"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:for-each select="mc:subfield[@code='a']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_1_letter"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:for-each>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="'bog'"/>

                                            </xsl:otherwise>

                                        </xsl:choose>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>
                        </dc:type>
                        <dc:type xml:lang="en">
                            <xsl:for-each select=".">
                                <xsl:choose>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')
                or contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>
                                            <xsl:when
                                                    test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n') and contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:value-of select="'net document (newspaper)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                <xsl:value-of select="'net document (journal)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                                <xsl:value-of select="'newspaper'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                                <xsl:value-of select="'yearbook'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                                <xsl:value-of select="'journal'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                                <xsl:value-of select="'serial'"/>
                                            </xsl:when>

                                            <xsl:otherwise>
                                                <xsl:value-of select="'periodical'"/>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')
                        or contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'a')">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                                <xsl:value-of select="'song'"/>
                                            </xsl:when>
                                            <xsl:otherwise>


                                                <xsl:choose>

                                                    <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                                        <xsl:choose>
                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                                    and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                                <xsl:value-of
                                                                        select="'review (journal)'"/>
                                                            </xsl:when>

                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                                    and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                                <xsl:value-of
                                                                        select="'rteview (newspaper)'"/>
                                                            </xsl:when>
                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                                <xsl:value-of
                                                                        select="'newspaper article'"/>
                                                            </xsl:when>
                                                            <xsl:when
                                                                    test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                                <xsl:value-of
                                                                        select="'newspaper article'"/>

                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of
                                                                        select="'journal article'"/>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                                        <xsl:value-of select="'article in book'"/>
                                                    </xsl:when>
                                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                                        <xsl:value-of select="'net document (article)'"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of select="'article'"/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:choose>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='A09'] and
                    not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'td'))
                    and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'te'))">
                                                <xsl:for-each select="/mc:record/mc:datafield[@tag='A09'][position()=1]">
                                                    <xsl:choose>
                                                        <xsl:when test="contains(mc:subfield[@code='a'][position()=1],'p')">
                                                            <xsl:variable name="code"><xsl:call-template name="code_1_letter_en"/></xsl:variable>
                                                            <xsl:value-of select="$code"/>

                                                        </xsl:when>
                                                        <xsl:when test="mc:subfield[@code='g']">
                                                            <xsl:for-each select="mc:subfield[@code='g']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_2_letters_en"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:for-each select="mc:subfield[@code='a']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_1_letter_en"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:for-each>
                                            </xsl:when>
                                            <xsl:when test="/mc:record/mc:datafield[@tag='009']">
                                                <xsl:for-each select="/mc:record/mc:datafield[@tag='009'][position()=1]">
                                                    <xsl:choose>
                                                        <xsl:when test="contains(mc:subfield[@code='a'][position()=1],'p')
                                                 or contains(mc:subfield[@code='a'][position()=1],'c')
                                                 or contains(mc:subfield[@code='a'][position()=1],'e')">
                                                            <xsl:for-each select="mc:subfield[@code='a'][position()=1]">
                                                                <xsl:variable name="code"><xsl:call-template name="code_1_letter_en"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>
                                                            </xsl:for-each>
                                                            <xsl:for-each select="mc:subfield[@code='g']">
                                                                <xsl:if test="not(contains(.,'xx'))">
                                                                    <xsl:text> + </xsl:text>
                                                                    <xsl:variable name="code"><xsl:call-template name="code_2_letters_en"/></xsl:variable>
                                                                    <xsl:value-of select="$code"/>
                                                                </xsl:if>
                                                            </xsl:for-each>

                                                        </xsl:when>
                                                        <xsl:when test="mc:subfield[@code='g']">
                                                            <xsl:for-each select="mc:subfield[@code='g']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_2_letters_en"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:for-each select="mc:subfield[@code='a']">

                                                                <xsl:variable name="code"><xsl:call-template name="code_1_letter_en"/></xsl:variable>
                                                                <xsl:value-of select="$code"/>

                                                                <xsl:if test="position()!=last()">
                                                                    <xsl:text> + </xsl:text>
                                                                </xsl:if>
                                                            </xsl:for-each>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:for-each>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="'book'"/>

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
                        <xsl:choose>
                            <xsl:when test="contains(mc:datafield[@tag='856'or @tag='I56']/mc:subfield[@code='u'],'http') and contains(mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                <dc:identifier>
                                    <xsl:value-of select="mc:datafield[@tag='856'or @tag='I56']/mc:subfield[@code='u']"/>
                                </dc:identifier>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='a' or @code='e']">
                                    <xsl:choose>
                                        <xsl:when test="position()=1">
                                            <dc:identifier>
                                                <xsl:text>ISBN </xsl:text>
                                                <xsl:value-of select="translate(.,' -','')"/>
                                            </dc:identifier>
                                        </xsl:when>
                                    </xsl:choose>
                                </xsl:for-each>
                            </xsl:otherwise>

                        </xsl:choose>
                    </rdf:Description>
                </rdf:RDF>

            </shortrecord>
            <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </Index:field>


    </xsl:template>
    <xsl:template name="code_2_letters">
        <xsl:choose>
            <xsl:when test="substring(.,1)='xx'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'bi')
                                or contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'Billedbog')
                                or contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'Billedbog')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'Billedbog')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'Billedbog')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'billedbog')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'billedbog')">
                        <xsl:value-of select="'billedbog'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'tegneserie')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'tegneserie')">
                        <xsl:value-of select="'tegneserie'"/>
                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'a')
                              or substring(mc:subfield[@code='a'],'a')">
                        <xsl:choose>
                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='m'],'1')
                                      or contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna print')
                                       or contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna Print')">
                                <xsl:value-of select="'bog stor skrift'"/>

                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'bog'"/>

                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'e')
                              or substring(mc:subfield[@code='a'],'e')">
                        <xsl:value-of select="'kort'"/>
                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'c')
                               or substring(mc:subfield[@code='a'],'c')">
                        <xsl:value-of select="'node'"/>

                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'p')
                                or substring(mc:subfield[@code='a'],'p')">
                        <xsl:value-of select="'blindskrift'"/>

                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'v')">
                        <xsl:value-of select="'sammensat materiale'"/>

                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'bog'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="substring(.,1)='xa'">
                <xsl:value-of select="'mikroform'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='xb'">
                <xsl:value-of select="'dias'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xc'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')
                               and (contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'p')
                               or contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'q'))">
                        <xsl:value-of select="'lydbog (mp3)'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'p')
                               or contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'q') ">
                        <xsl:value-of select="'lydbog (cd)'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                        <xsl:value-of select="'mp3'"/>

                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'cd'"/>

                    </xsl:otherwise>
                </xsl:choose>


            </xsl:when>
            <xsl:when test="substring(.,1)='xd'">
                <xsl:value-of select="'dvd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ga'">
                <xsl:value-of select="'akvarel'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ha'">
                <xsl:value-of select="'arkitekturtegning'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='kb'">
                <xsl:value-of select="'billedbånd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hb'">
                <xsl:value-of select="'billedkort'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gb'">
                <xsl:value-of select="'billedtæppe'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='tg'">
                <xsl:value-of select="'cd-i'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='tb'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'lu ray')
                               or contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'lu-ray')">
                        <xsl:value-of select="'blu ray disc'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                        <xsl:value-of select="'mp3'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'game boy')">
                        <xsl:value-of select="'gameboy-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'wii')">
                        <xsl:value-of select="'wii-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'xbox')">
                        <xsl:value-of select="'xbox-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation 2')">
                        <xsl:value-of select="'playstation2-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation')">
                        <xsl:value-of select="'playstation-spil'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'cd-rom'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="substring(.,1)='xi'">
                <xsl:value-of select="'dcc-bånd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='tk'">
                <xsl:value-of select="'diskette'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='th'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'PSP')">
                        <xsl:value-of select="'psp-film'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'dvd'"/>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="substring(.,1)='to'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'blu ray')">
                        <xsl:value-of select="'blu ray disc'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                        <xsl:value-of select="'mp3'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'game boy')">
                        <xsl:value-of select="'gameboy-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'wii')">
                        <xsl:value-of select="'wii-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'xbox')">
                        <xsl:value-of select="'xbox-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation 2')">
                        <xsl:value-of select="'playstation2-spil'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation')">
                        <xsl:value-of select="'playstation-spil'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'dvd-rom'"/>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="substring(.,1)='ue'">
                <xsl:value-of select="'emnekasse'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xl'">
                <xsl:value-of select="'fastplade'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xy'">
                <xsl:value-of select="'uspecificeret medie'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='mj'">
                <xsl:value-of select="'filmspole'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hl'">
                <xsl:value-of select="'flipover'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hg'">
                <xsl:value-of select="'flonellograf'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hf'">
                <xsl:value-of select="'foto'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ti'">
                <xsl:value-of select="'foto-cd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hr'">
                <xsl:value-of select="'fotoreproduktion'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gg'">
                <xsl:value-of select="'grafisk blad'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xk'">
                <xsl:value-of select="'grammofonplade'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xh'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'p')
                               or contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'q') ">
                        <xsl:value-of select="'lydbog (bånd)'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'kassettelydbånd'"/>

                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="substring(.,1)='gr'">
                <xsl:value-of select="'kunstreproduktion'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ub'">
                <xsl:value-of select="'laborativt materiale'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ut'">
                <xsl:value-of select="'legetøj'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xn'">
                <xsl:value-of select="'lydspor'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ua'">
                <xsl:value-of select="'måleapparat'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gm'">
                <xsl:value-of select="'maleri'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ui'">
                <xsl:value-of select="'materiale til indlæringsapparat'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ia'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ic'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='if'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ih'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ik'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ip'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='is'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='it'">
                <xsl:value-of select="'mikroform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xg'">
                <xsl:value-of select="'mini disc'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xe'">
                <xsl:for-each select="../mc:subfield[@code='a']">
                    <xsl:if test="substring(.,1)='a'">
                        <xsl:choose>
                            <xsl:when test="/mc:record/mc:datafield[@tag='A08']">
                                <xsl:choose>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>

                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'n')">
                                                <xsl:value-of select="'netdokument (avis)'"/>

                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'z')">
                                                <xsl:value-of select="'netdokument (årbog)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'p')">
                                                <xsl:value-of select="'netdokument (tidsskrift)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'m')">
                                                <xsl:value-of select="'netdokument (periodicum)'"/>
                                            </xsl:when>

                                            <xsl:otherwise>
                                                <xsl:value-of select="'netdokument (tidsskrift)'"/>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                    </xsl:when>
                                </xsl:choose>
                            </xsl:when>

                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                                <xsl:choose>

                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                        <xsl:value-of select="'netdokument (avis)'"/>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                        <xsl:value-of select="'netdokument (årbog)'"/>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                        <xsl:value-of select="'netdokument (tidsskrift)'"/>


                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                        <xsl:value-of select="'netdokument (periodicum)'"/>
                                    </xsl:when>

                                    <xsl:otherwise>
                                        <xsl:value-of select="'netdokument (tidsskrift)'"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                                <xsl:choose>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                        <xsl:value-of select="'netdokument (sang)'"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="'netdokument (artikel)'"/>

                                    </xsl:otherwise>
                                </xsl:choose>

                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'netdokument (bog)'"/>


                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='c'">
                        <xsl:value-of select="'netdokument (node)'"/>


                    </xsl:if>
                    <xsl:if test="substring(.,1)='e'">
                        <xsl:value-of select="'netdokument (kort)'"/>


                    </xsl:if>
                    <xsl:if test="substring(.,1)='g'">
                        <xsl:value-of select="'netdokument (billede)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='m'">
                        <xsl:value-of select="'netdokument (film)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='r'">
                        <xsl:value-of select="'netdokument (lyd)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='s'">
                        <xsl:value-of select="'netdokument (musik)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='t'">
                        <xsl:value-of select="'netdokument (elektronisk materiale)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='v'">
                        <xsl:value-of select="'netdokument (sammensat materiale)'"/>


                    </xsl:if>
                </xsl:for-each>

            </xsl:when>
            <xsl:when test="substring(.,1)='hd'">
                <xsl:value-of select="'ordkort'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gk'">
                <xsl:value-of select="'originalkunst'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gp'">
                <xsl:value-of select="'plakat'"/>
            </xsl:when>

            <xsl:when test="substring(.,1)='hp'">
                <xsl:value-of select="'planche'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ho'">
                <xsl:value-of select="'postkort'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='uu'">
                <xsl:value-of select="'puslespil'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ul'">
                <xsl:value-of select="'spil'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xj'">
                <xsl:value-of select="'spolelydbånd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hy'">
                <xsl:value-of select="'symbolkort'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ud'">
                <xsl:value-of select="'teaterdukke'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='wt'">
                <xsl:value-of select="'teateropførelse'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gt'">
                <xsl:value-of select="'tegning'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ht'">
                <xsl:value-of select="'teknisk tegning'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='kt'">
                <xsl:value-of select="'transparent'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='wu'">
                <xsl:value-of select="'udstilling'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='us'">
                <xsl:value-of select="'udstillingsmontage'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='nh'">
                <xsl:value-of select="'video'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='uv'">
                <xsl:value-of select="'øvelsesmodel'"/>
            </xsl:when>

        </xsl:choose>
    </xsl:template>
    <xsl:template name="code_1_letter">
        <xsl:choose>
            <xsl:when test="substring(.,1)='a'">
                <xsl:value-of select="'tekst'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='b'">
                <xsl:value-of select="'håndskrift'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='c'">
                <xsl:value-of select="'node'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='d'">
                <xsl:value-of select="'node'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='e'">
                <xsl:value-of select="'kort'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='e'">
                <xsl:value-of select="'billede'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='m'">
                <xsl:value-of select="'film'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='p'">
                <xsl:value-of select="'punktskrift'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='r'">
                <xsl:value-of select="'lyd'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='s'">
                <xsl:value-of select="'musik'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='t'">
                <xsl:value-of select="'elektronisk materiale'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='u'">
                <xsl:value-of select="'genstand'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='v'">
                <xsl:value-of select="'sammensat materiale'"/>

            </xsl:when>


        </xsl:choose>
    </xsl:template>
    <xsl:template name="code_2_letters_en">
        <xsl:choose>
            <xsl:when test="substring(.,1)='xx'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'bi')
                                or contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'Billedbog')
                                or contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'Billedbog')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'Billedbog')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'Billedbog')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'billedbog')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'billedbog')">
                        <xsl:value-of select="'picture book'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'Tegneserie')
                                or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'tegneserie')
                                 or contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='s'],'tegneserie')">
                        <xsl:value-of select="'comics'"/>
                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'a')
                              or substring(mc:subfield[@code='a'],'a')">
                        <xsl:choose>
                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='m'],'1')
                                      or contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna print')
                                       or contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna Print')">
                                <xsl:value-of select="'large font books'"/>

                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'book'"/>

                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'e')
                              or substring(mc:subfield[@code='a'],'e')">
                        <xsl:value-of select="'map'"/>
                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'c')
                               or substring(mc:subfield[@code='a'],'c')">
                        <xsl:value-of select="'sheet music'"/>

                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'p')
                                or substring(mc:subfield[@code='a'],'p')">
                        <xsl:value-of select="'braille'"/>

                    </xsl:when>
                    <xsl:when test="substring(preceding-sibling::mc:subfield[@code='a'],'v')">
                        <xsl:value-of select="'composite material'"/>

                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'book'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="substring(.,1)='xa'">
                <xsl:value-of select="'microform'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='xb'">
                <xsl:value-of select="'dias'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xc'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')
                               and (contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'p')
                               or contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'q'))">
                        <xsl:value-of select="'audiobook (mp3)'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'p')
                               or contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'q') ">
                        <xsl:value-of select="'audiobook (cd)'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                        <xsl:value-of select="'audiobook (mp3)'"/>

                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'cd'"/>

                    </xsl:otherwise>
                </xsl:choose>


            </xsl:when>
            <xsl:when test="substring(.,1)='xd'">
                <xsl:value-of select="'dvd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ga'">
                <xsl:value-of select="'water color'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ha'">
                <xsl:value-of select="'architectural drawing'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='kb'">
                <xsl:value-of select="'film strip'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hb'">
                <xsl:value-of select="'picture card'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gb'">
                <xsl:value-of select="'tapestry'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='tg'">
                <xsl:value-of select="'cd-i'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='tb'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'blu ray')">
                        <xsl:value-of select="'blu ray disc'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                        <xsl:value-of select="'mp3'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'game boy')">
                        <xsl:value-of select="'gameboy game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'wii')">
                        <xsl:value-of select="'wii game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'xbox')">
                        <xsl:value-of select="'xbox game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation 2')">
                        <xsl:value-of select="'playstation2 game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation')">
                        <xsl:value-of select="'playstation game'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'cd-rom'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="substring(.,1)='xi'">
                <xsl:value-of select="'dcc-tape'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='tk'">
                <xsl:value-of select="'floppy disc'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='th'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'PSP')">
                        <xsl:value-of select="'psp-film'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'dvd'"/>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="substring(.,1)='to'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'blu ray')">
                        <xsl:value-of select="'blu ray disc'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                        <xsl:value-of select="'mp3'"/>

                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'game boy')">
                        <xsl:value-of select="'gameboy game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'wii')">
                        <xsl:value-of select="'wii game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'xbox')">
                        <xsl:value-of select="'xbox game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation 2')">
                        <xsl:value-of select="'playstation2 game'"/>
                    </xsl:when>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'laystation')">
                        <xsl:value-of select="'playstation game'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'dvd-rom'"/>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="substring(.,1)='ue'">
                <xsl:value-of select="'subject box'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xl'">
                <xsl:value-of select="'plate'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xy'">
                <xsl:value-of select="'unspecified'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='mj'">
                <xsl:value-of select="'film'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hl'">
                <xsl:value-of select="'flipover'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hg'">
                <xsl:value-of select="'flanel board material'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hf'">
                <xsl:value-of select="'photo'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ti'">
                <xsl:value-of select="'photo cd'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hr'">
                <xsl:value-of select="'photo reproduction'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gg'">
                <xsl:value-of select="'graphic sheet'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xk'">
                <xsl:value-of select="'gramophone record'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xh'">
                <xsl:choose>
                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'p')
                               or contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],'q') ">
                        <xsl:value-of select="'audiobook (tape)'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'cassette tape'"/>

                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="substring(.,1)='gr'">
                <xsl:value-of select="'art reproduction'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ub'">
                <xsl:value-of select="'laborative kit'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ut'">
                <xsl:value-of select="'toys'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xn'">
                <xsl:value-of select="'sound track'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ua'">
                <xsl:value-of select="'measuring apparatus'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gm'">
                <xsl:value-of select="'painting'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ui'">
                <xsl:value-of select="'material for educational device'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ia'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ic'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='if'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ih'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ik'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ip'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='is'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='it'">
                <xsl:value-of select="'microform'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xg'">
                <xsl:value-of select="'mini disc'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xe'">
                <xsl:for-each select="../mc:subfield[@code='a']">
                    <xsl:if test="substring(.,1)='a'">
                        <xsl:choose>
                            <xsl:when test="/mc:record/mc:datafield[@tag='A08']">
                                <xsl:choose>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>

                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'n')">
                                                <xsl:value-of select="'net document (newspaper)'"/>

                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'z')">
                                                <xsl:value-of select="'net document (yearbook)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'p')">
                                                <xsl:value-of select="'net document (journal)'"/>
                                            </xsl:when>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'m')">
                                                <xsl:value-of select="'net document (periodical)'"/>
                                            </xsl:when>

                                            <xsl:otherwise>
                                                <xsl:value-of select="'net document (journal)'"/>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                    </xsl:when>
                                </xsl:choose>
                            </xsl:when>

                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                                <xsl:choose>

                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                        <xsl:value-of select="'net document (newspaper)'"/>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                        <xsl:value-of select="'net document (yearbook)'"/>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                        <xsl:value-of select="'net document (journal)'"/>


                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                        <xsl:value-of select="'net document (periodical)'"/>
                                    </xsl:when>

                                    <xsl:otherwise>
                                        <xsl:value-of select="'net document (journal)'"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                                <xsl:choose>
                                    <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                        <xsl:value-of select="'net document (song)'"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="'net document (article)'"/>

                                    </xsl:otherwise>
                                </xsl:choose>

                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'net document (book)'"/>


                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='c'">
                        <xsl:value-of select="'net document (sheet music)'"/>


                    </xsl:if>
                    <xsl:if test="substring(.,1)='e'">
                        <xsl:value-of select="'net document (map)'"/>


                    </xsl:if>
                    <xsl:if test="substring(.,1)='g'">
                        <xsl:value-of select="'net document (picture)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='m'">
                        <xsl:value-of select="'net document (film)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='r'">
                        <xsl:value-of select="'net document (sound)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='s'">
                        <xsl:value-of select="'net document (music)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='t'">
                        <xsl:value-of select="'net document (electronic material)'"/>

                    </xsl:if>
                    <xsl:if test="substring(.,1)='v'">
                        <xsl:value-of select="'net document (composite material)'"/>


                    </xsl:if>
                </xsl:for-each>

            </xsl:when>
            <xsl:when test="substring(.,1)='hd'">
                <xsl:value-of select="'word card'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gk'">
                <xsl:value-of select="'original art'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gp'">
                <xsl:value-of select="'poster'"/>
            </xsl:when>

            <xsl:when test="substring(.,1)='hp'">
                <xsl:value-of select="'plate'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ho'">
                <xsl:value-of select="'post cards'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='uu'">
                <xsl:value-of select="'jigsaw puzzle'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ul'">
                <xsl:value-of select="'game'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='xj'">
                <xsl:value-of select="'reel-to-reel tape'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='hy'">
                <xsl:value-of select="'symbol card'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ud'">
                <xsl:value-of select="'theatre puppet'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='wt'">
                <xsl:value-of select="'theatre performance'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='gt'">
                <xsl:value-of select="'drawing'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='ht'">
                <xsl:value-of select="'technical drawing'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='kt'">
                <xsl:value-of select="'transparent'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='wu'">
                <xsl:value-of select="'exhibition'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='us'">
                <xsl:value-of select="'exhibition montage'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='nh'">
                <xsl:value-of select="'video'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='uv'">
                <xsl:value-of select="'training model'"/>
            </xsl:when>

        </xsl:choose>
    </xsl:template>
    <xsl:template name="code_1_letter_en">
        <xsl:choose>
            <xsl:when test="substring(.,1)='a'">
                <xsl:value-of select="'text'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='b'">
                <xsl:value-of select="'manuscript'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='c'">
                <xsl:value-of select="'sheet music'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='d'">
                <xsl:value-of select="'sheet music'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='e'">
                <xsl:value-of select="'map'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='e'">
                <xsl:value-of select="'picture'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='m'">
                <xsl:value-of select="'film'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='p'">
                <xsl:value-of select="'braille'"/>
            </xsl:when>
            <xsl:when test="substring(.,1)='r'">
                <xsl:value-of select="'sound'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='s'">
                <xsl:value-of select="'music'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='t'">
                <xsl:value-of select="'electronic material'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='u'">
                <xsl:value-of select="'object'"/>

            </xsl:when>
            <xsl:when test="substring(.,1)='v'">
                <xsl:value-of select="'composite material'"/>

            </xsl:when>


        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
