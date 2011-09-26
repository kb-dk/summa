<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="title">
        <Index:group Index:name="ti" Index:navn="ti">
            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                    <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    <xsl:variable name="previous-subfields" select="following-sibling::mc:subfield[@code='a' or position()=last()][1]/preceding-sibling::mc:subfield"/>
                    <xsl:for-each select="following-sibling::mc:subfield[count(.|$previous-subfields)=count($previous-subfields)]">
                        <xsl:if test="@code='n'">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='o'">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='æ'">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='ø'">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='y'">
                            <xsl:text> - - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                    <xsl:for-each select=".">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='p']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                    <xsl:value-of select="."/>
                    <xsl:variable name="previous-subfields" select="following-sibling::mc:subfield[@code='p' or position()=last()][1]/preceding-sibling::mc:subfield"/>
                    <xsl:for-each select="following-sibling::mc:subfield[count(.|$previous-subfields)=count($previous-subfields)]">
                        <xsl:if test="@code='q'">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='r'">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='x']" >
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                    <xsl:for-each select=".">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='247']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                    <xsl:for-each select=".">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                    <xsl:for-each select=".">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='u']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10" Index:suggest="true">
                    <xsl:for-each select=".">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='239']/mc:subfield[@code='t' or @code='u']">
                <xsl:variable name="titel" select="."/>
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8" Index:suggest="true">
                    <xsl:for-each select="$titel">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="../mc:subfield[@code='ø']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="../mc:subfield[@code='v']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='240']">
                <xsl:if test="mc:subfield[@code='a' or @code='d' or @code='e' or @code='f' or @code='g' or @code='h' or @code='j' or @code='k' or @code='s' or @code='ø' or @code='r' or @code='q' or @code='u']">
                    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8"  Index:suggest="true">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="@code='a'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='d'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='e'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='f'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='g'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='h'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='j'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='k'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='s'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='ø'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='r'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='q'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='u'">
                                    <xsl:value-of select="."/>
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='739']/mc:subfield[@code='t' or @code='ø' or @code='v' or @code='u']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8"  Index:suggest="true">
                    <xsl:for-each select=".">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='740']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                        <xsl:text> </xsl:text>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='s']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='745']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='n']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='o']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='æ']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='ø']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='574']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='247']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                    <xsl:if test="./mc:subfield[@code='s']">
                        <xsl:for-each select="mc:subfield[@code='s']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='n']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='o']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='v']">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='248']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                    <xsl:if test="mc:subfield[@code='s']">
                        <xsl:for-each select="mc:subfield[@code='s']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='n']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='o']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='v']">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='440']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                    <xsl:text>(</xsl:text>
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='æ']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='ø']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='n']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='o']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='v']">
                        <xsl:text> ; </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:text>)</xsl:text>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='840']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                    <xsl:text>(</xsl:text>
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='æ']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='ø']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='n']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='o']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='v']">
                        <xsl:text> ; </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:text>)</xsl:text>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='490']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="5">
                    <xsl:text>(</xsl:text>
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                    </xsl:for-each>
                    <xsl:text>)</xsl:text>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='570']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="5">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='241']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="5"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='ø']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='n']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='o']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='242']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="5"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text>&#32;:&#32;</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='440']/mc:subfield[@code='c' or @code='p' or @code='q' or @code='r' or @code='s']">
                <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                    <xsl:text> </xsl:text>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='795']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="6"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='795']/mc:subfield[@code='p' or @code='u' or @code='v'] ">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="6" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='795']/mc:subfield[@code='æ' or @code='ø' or @code='c' or @code='r' or @code ='s']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='t' or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="6" Index:suggest="true">
                    <xsl:value-of select="."/>
                    <xsl:text> </xsl:text>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='b' or @code='c' or @code='y']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='g']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='247']/mc:subfield[@code='g' or @code='c' or @code='p'or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='g' or @code='c' or @code='p'or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='526']/mc:subfield[@code='t' or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='530']/mc:subfield[@code='a' or @code='t' or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='575']/mc:subfield[@code='t']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='700']/mc:subfield[@code='t']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8" Index:suggest="true">
                    <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='710']/mc:subfield[@code='t']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8" Index:suggest="true">
                    <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='210']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8" Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='222']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="8" Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='512']/mc:subfield[@code='t' or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='520']/mc:subfield[@code='t' or @code='x']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='557']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='æ']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:text> </xsl:text>
                    <xsl:for-each select="mc:subfield[@code='ø']">
                        <xsl:text> / </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4" Index:suggest="true">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='860']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='861']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='863']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='865']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='866']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='867']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='868']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='870']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='871']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='873']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='874']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='879']">
                <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="4"  Index:suggest="true">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
        </Index:group>
    </xsl:template>

</xsl:stylesheet>
