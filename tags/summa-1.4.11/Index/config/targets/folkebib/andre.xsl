<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="aj">



        <xsl:for-each select="mc:datafield[@tag='001']">
            <Index:field Index:repeat="true" Index:name="aj" Index:navn="aj" Index:type="token" Index:boostFactor="2" Index:freetext="false">

           <xsl:choose>
               <xsl:when test="mc:subfield[@code='c']">
                   <xsl:value-of select="substring(mc:subfield[@code='c'],1,8)"/>
               </xsl:when>
               <xsl:otherwise>
                   <xsl:value-of select="mc:subfield[@code='d']"/>
               </xsl:otherwise>
           </xsl:choose>

            </Index:field>

                    </xsl:for-each>
    </xsl:template>
    <xsl:template name="km">



        <xsl:for-each select="mc:datafield[@tag='034' or @tag='A34']/mc:subfield">
                       <Index:field Index:repeat="true" Index:name="km"  Index:freetext="false" Index:navn="km" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
          <xsl:for-each select="mc:datafield[@tag='247' or @tag='C47' or @tag='248' or @tag='C48']/mc:subfield[@code='b']">
                       <Index:field Index:repeat="true" Index:name="km"  Index:freetext="false" Index:navn="km" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
         <xsl:for-each select="mc:datafield[@tag='256' or @tag='C56']/mc:subfield">
                       <Index:field Index:repeat="true" Index:name="km"  Index:freetext="false" Index:navn="km" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='530' or @tag='F30']/mc:subfield[@code='m']">
                       <Index:field Index:repeat="true" Index:name="km"  Index:freetext="false" Index:navn="km" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="mo">
        <xsl:for-each select="mc:datafield[@tag='039' or @tag='A39']/subfield[@code='a' or @code='b']">
                   <Index:field Index:repeat="true" Index:name="mo" Index:navn="mo" Index:type="token" Index:freetext="false">
                            <xsl:value-of select="."/>

                    </Index:field>
        </xsl:for-each>
    </xsl:template>
 <xsl:template name="nm">
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='A05']">
                <xsl:for-each select="mc:subfield[@code='k']">
                  <xsl:call-template name="musikopst"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                      <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='k']">
                           <xsl:call-template name="musikopst"/>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
    <xsl:template name="musikopst">
         <xsl:if test="contains(.,'a')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>nd</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>noder</xsl:text>
                     </Index:field>
                        </xsl:if>
        <xsl:if test="contains(.,'b')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>bc</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>becifring</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'c')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>so</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>solmisation</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'d')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>bc</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>tekst med becifring</xsl:text>
                     </Index:field>
                        </xsl:if>
          <xsl:if test="contains(.,'e')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>tl</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>tal</xsl:text>
                     </Index:field>
                        </xsl:if>
          <xsl:if test="contains(.,'f')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>bg</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>bogstaver</xsl:text>
                     </Index:field>
                        </xsl:if>
        <xsl:if test="contains(.,'g')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>ak</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>akkorddiagrammer (strengeinstrumenter)</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'h')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>ak</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>akkorddiagrammer (klaviaturinstrumenter)</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'i')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>sd</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>spillediagrammer</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'j')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>tu</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>tabulatur</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'k')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>ne</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>neumer</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'l')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>me</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>mensuralnotation</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'m')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>gr</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>grafisk/optisk notation</xsl:text>
                     </Index:field>
                        </xsl:if>
         <xsl:if test="contains(.,'z')">
                    <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>nx</xsl:text>
                     </Index:field>
                        <Index:field Index:repeat="true" Index:name="nm" Index:navn="nm" Index:type="token" Index:freetext="false">
                         <xsl:text>andre notationsformer</xsl:text>
                     </Index:field>
                        </xsl:if>
    </xsl:template>
    <xsl:template name="no">
         <xsl:for-each select="mc:datafield[@tag='247' or @tag='C47' or @code='248' or @code='C28']/mc:subfield[@code='l']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='501' or @tag='F01']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                            <xsl:text> </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="."/>
                            <xsl:text> </xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='502' or @tag='F02']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='504' or @tag='F04']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='505' or @tag='F05']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='506' or @tag='F06']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='507' or @tag='F07']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='508' or @tag='F08']/mc:subfield[@code='a']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='509' or @tag='F09']/mc:subfield[@code='a' or @code='b' or @code='8']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='512' or @tag='F12']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='517' or @tag='F17']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='518' or @tag='F18']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='520' or @tag='F20']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='523' or @tag='F23']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield">
                    <xsl:if test="@code='a'">
                        <xsl:value-of select="."/>
                    </xsl:if>
                    <xsl:if test="@code='u'">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:if>
                    <xsl:if test="@code='y'">
                        <xsl:text> (</xsl:text>
                        <xsl:value-of select="."/>
                        <xsl:text>)</xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='525' or @tag='F25']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='526' or @tag='F26']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='529' or @tag='F29']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:if test="@code='i'">
                                <xsl:text>:</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='a'">
                                <xsl:text>Indekseres i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>Beskrevet i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='c'">
                                <xsl:text>Anmeldt i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:text>Omtalt i: </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='z'">
                                <xsl:text>. - ISRC </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>

                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='c'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='z'">
                                <xsl:text>. - ISRC </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>

                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='530' or @tag='F30']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='m'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='z'">
                                <xsl:text>. - ISRC </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='532' or @tag='F32']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield">
                    <xsl:if test="@code='a'">
                        <xsl:value-of select="."/>
                    </xsl:if>
                    <xsl:if test="@code='u'">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:if>
                    <xsl:if test="@code='y'">
                        <xsl:text> (</xsl:text>
                        <xsl:value-of select="."/>
                        <xsl:text>)</xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='534' or @tag='F34']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="position()='1'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='i'">
                                <xsl:value-of select="."/>
                                <xsl:text>: </xsl:text>
                            </xsl:if>
                            <xsl:if test="@code='t'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='e'">
                                <xsl:choose>
                                    <xsl:when test="position()='1'">
                                        <xsl:text> / </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='d'">
                                <xsl:choose>
                                    <xsl:when test="position()=last()">
                                        <xsl:text>: </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text> ; </xsl:text>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="@code='x'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='b'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='538' or @tag='F38']/mc:subfield[@code='a' or @code='i' or @code='s']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select=".">
                    <xsl:choose>
                        <xsl:when test="@code='i'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='a'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='s'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='555' or @tag='F57']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>

                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='557' or @tag='F57']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:text>Artikel i: </xsl:text>
                <xsl:for-each select="mc:subfield">
                    <xsl:choose>
                        <xsl:when test="@code='a'">
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='æ'">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='ø'">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>)</xsl:text>
                        </xsl:when>
                        <xsl:when test="@code='b'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='h'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='i'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='j'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='l'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='v'">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:when test="@code='k'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='558' or @tag='F58']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:choose>
                    <xsl:when test="position()='1'">
                        <xsl:text>Artikel i: </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="@code='a'">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='e'">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='g'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='w'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='h'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='i'">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='j'">
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='l'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='s'">
                            <xsl:text>. - </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                        <xsl:if test="@code='v'">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='559' or @tag='F59']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="2">
                <xsl:for-each select="mc:subfield">
                    <xsl:if test="@code='a'">
                        <xsl:value-of select="."/>
                    </xsl:if>
                      <xsl:if test="@code='u'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> (</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='560' or @tag='F60']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='565' or @tag='F65']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='566' or @tag='F66']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='573' or @tag='F73']">
            <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </Index:field>
        </xsl:for-each>

    </xsl:template>
    <xsl:template name="op">



        <xsl:for-each select="mc:datafield[@tag='001']">
            <Index:field Index:repeat="true" Index:name="op" Index:navn="op" Index:type="token" Index:boostFactor="2" Index:freetext="false">

                   <xsl:value-of select="mc:subfield[@code='d']"/>

            </Index:field>

                    </xsl:for-each>
    </xsl:template>
    <xsl:template name="ts">


          <xsl:choose>
              <xsl:when test="mc:datafield[@tag='D00']">
                  <xsl:for-each select="mc:subfield[@code='e']">
                      <Index:field Index:repeat="true" Index:name="ts" Index:navn="ts" Index:type="token" Index:boostFactor="2" Index:freetext="false">

                             <xsl:value-of select="."/>

                      </Index:field>

                  </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                     <xsl:for-each select="mc:datafield[@tag='300']/mc:subfield[@code='e']">
                      <Index:field Index:repeat="true" Index:name="ts" Index:navn="ts" Index:type="token" Index:boostFactor="2" Index:freetext="false">

                             <xsl:value-of select="."/>

                      </Index:field>

                  </xsl:for-each>
              </xsl:otherwise>
          </xsl:choose>
    </xsl:template>
    <xsl:template name="other">
         <xsl:for-each select="mc:datafield[@tag='250' or @tag='C50']/mc:subfield[@code='a' or @code='b' or @code='p' or @code='x']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='255' or @tag='C55']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='256' or @tag='C56']/mc:subfield[@code='a'or @code='b' or @code='c' or @code='d' or @code='f']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='257' or @tag='C57']/mc:subfield[@code='a'or @code='p']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='260' or @tag='C60']/mc:subfield[@code='k']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='259' or @tag='C59']/mc:subfield[@code='a'or @code='b']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>

					<xsl:for-each select="mc:datafield[@tag='300' or @tag='D00']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="position()=1">
										<xsl:value-of select="."/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:if test="@code='n'">
											<xsl:text> + </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='a'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:if>
										<xsl:if test="@code='b'">
											<xsl:text> : </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='c'">
											<xsl:text> ; </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='d'">
											<xsl:text> + </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='e'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:if>
										<xsl:if test="@code='l'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:if>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='539' or @tag='F39']/mc:subfield[@code='a']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='555' or @tag='F55']/mc:subfield[@code='a']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='247' or @tag='C47']/mc:subfield[@code='l']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>


                  <xsl:for-each select="mc:datafield[@tag='248' or @tag='C48']/mc:subfield[@code='l']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='e']">
						<Index:field Index:repeat="true" Index:name="bh" Index:navn="bh" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='x']">
						<Index:field Index:repeat="true" Index:name="stregkode" Index:navn="stregkode" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
          <xsl:for-each select="mc:datafield[@tag='x96']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='e']">
						<Index:field Index:repeat="true" Index:name="bh" Index:navn="bh" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='x96']/mc:subfield[@code='x']">
						<Index:field Index:repeat="true" Index:name="stregkode" Index:navn="stregkode" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
    </xsl:template>
    

</xsl:stylesheet>
