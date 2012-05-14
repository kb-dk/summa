<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="lti">

                <xsl:for-each select="mc:datafield[@tag='245' or @tag='C45']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10"  >
                        <xsl:value-of select="translate(.,'¤','')"/>
                        <xsl:variable name="previous-subfields" select="following-sibling::mc:subfield[@code='a' or position()=last()][1]/preceding-sibling::mc:subfield"/>
                        <xsl:for-each select="following-sibling::mc:subfield[count(.|$previous-subfields)=count($previous-subfields)]">
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
                                <xsl:value-of select="translate(.,'¤','')"/>
                            </xsl:if>
                            <xsl:if test="@code='æ'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="translate(.,'¤','')"/>
                            </xsl:if>
                            <xsl:if test="@code='ø'">
                                <xsl:text>. </xsl:text>
                                <xsl:value-of select="translate(.,'¤','')"/>
                            </xsl:if>
                            <xsl:if test="@code='y'">
                                <xsl:text> - - </xsl:text>
                                <xsl:value-of select="translate(.,'¤','')"/>
                            </xsl:if>
                        </xsl:for-each>
                    </Index:field>
               <xsl:if test="contains(.,'¤')">
                   <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                       <xsl:value-of select="substring-after(.,'¤')"/>
                       <xsl:variable name="previous-subfields" select="following-sibling::mc:subfield[@code='a' or position()=last()][1]/preceding-sibling::mc:subfield"/>
                       <xsl:for-each select="following-sibling::mc:subfield[count(.|$previous-subfields)=count($previous-subfields)]">
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
                               <xsl:value-of select="translate(.,'¤','')"/>
                           </xsl:if>
                           <xsl:if test="@code='æ'">
                               <xsl:text>. </xsl:text>
                               <xsl:value-of select="translate(.,'¤','')"/>
                           </xsl:if>
                           <xsl:if test="@code='ø'">
                               <xsl:text>. </xsl:text>
                               <xsl:value-of select="translate(.,'¤','')"/>
                           </xsl:if>
                           <xsl:if test="@code='y'">
                               <xsl:text> - - </xsl:text>
                               <xsl:value-of select="translate(.,'¤','')"/>
                           </xsl:if>
                       </xsl:for-each>
                   </Index:field>

               </xsl:if>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='245' or @tag='C45']/mc:subfield[@code='a']">
                    <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                   <xsl:if test="contains(.,'¤')">
                          <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </xsl:for-each>
                    </Index:field>
                   </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='245' or @tag='C45']/mc:subfield[@code='p']">
                    <Index:field Index:repeat="false" Index:name="lpa" Index:navn="lpa" Index:type="keyword" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'¤','')"/>
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
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'¤','')"/>
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
               <xsl:if test="contains(.,'¤')">
                     <Index:field Index:repeat="false" Index:name="lpa" Index:navn="lpa" Index:type="keyword" Index:boostFactor="10">
                        <xsl:value-of select="substring-after(.,'¤')"/>
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
                   <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                      <xsl:value-of select="substring-after(.,'¤')"/>
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

               </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='245' or @tag='C45']/mc:subfield[@code='x']" >
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                <xsl:if test="contains(.,'¤')">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </xsl:for-each>
                    </Index:field>

                </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='247' or @tag='C47']/mc:subfield[@code='a' or @tag='248' or @tag='C48']">
                    <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>

                    <xsl:if test="contains(.,'¤')">
                            <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </xsl:for-each>
                    </Index:field>
                        <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select=".">
                        <xsl:value-of select="substring-after(.,'¤')"/>
                    </xsl:for-each>
                </Index:field>

                    </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='245' or @tag='C45']/mc:subfield[@code='u']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                            <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select=".">
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </xsl:for-each>
                    </Index:field>
                    </xsl:if>
                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='239' or @tag='C39']/mc:subfield[@code='t' or @code='u']">
                    <xsl:variable name="titel" select="."/>
                    <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="$titel">
                            <xsl:value-of select="translate(.,'¤','')"/>
                             </xsl:for-each>
                        <xsl:for-each select="../mc:subfield[@code='ø']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="../mc:subfield[@code='v']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="$titel">
                            <xsl:value-of select="translate(.,'¤','')"/>
                             </xsl:for-each>
                        <xsl:for-each select="../mc:subfield[@code='ø']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="../mc:subfield[@code='v']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>

                     <xsl:if test="contains($titel,'¤')">
                            <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="10">
                        <xsl:for-each select="$titel">
                            <xsl:value-of select="substring-after($titel,'¤')"/>
                        </xsl:for-each>
                                  <xsl:for-each select="../mc:subfield[@code='ø']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="../mc:subfield[@code='v']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                         <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                     <xsl:for-each select="$titel">
                         <xsl:value-of select="substring-after($titel,'¤')"/>
                     </xsl:for-each>
                               <xsl:for-each select="../mc:subfield[@code='ø']">
                         <xsl:text>. </xsl:text>
                         <xsl:value-of select="translate(.,'¤','')"/>
                     </xsl:for-each>
                     <xsl:for-each select="../mc:subfield[@code='v']">
                         <xsl:text>. </xsl:text>
                         <xsl:value-of select="translate(.,'¤','')"/>
                     </xsl:for-each>
                 </Index:field>
                    </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='240' or @tag='C40']">
                    <xsl:if test="mc:subfield[@code='a' or @code='d' or @code='e' or @code='f' or @code='g' or @code='h' or @code='j' or @code='k' or @code='s' or @code='ø' or @code='r' or @code='q' or @code='u']">
                        <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="8"  >
                            <xsl:for-each select="mc:subfield">
                                <xsl:choose>
                                    <xsl:when test="@code='a'">
                                        <xsl:value-of select="translate(.,'¤','')"/>
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
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                        <xsl:text> </xsl:text>
                                    </xsl:when>
                                    <xsl:when test="@code='ø'">
                                        <xsl:value-of select="translate(.,'¤','')"/>
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
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                        <xsl:text> </xsl:text>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:for-each>
                        </Index:field>
                        <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8"  >
                            <xsl:for-each select="mc:subfield">
                                <xsl:choose>
                                    <xsl:when test="@code='a'">
                                        <xsl:value-of select="translate(.,'¤','')"/>
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
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                        <xsl:text> </xsl:text>
                                    </xsl:when>
                                    <xsl:when test="@code='ø'">
                                        <xsl:value-of select="translate(.,'¤','')"/>
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
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                        <xsl:text> </xsl:text>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:for-each>
                        </Index:field>

                    </xsl:if>
                       <xsl:if test="contains(mc:subfield[@code='a'],'¤')">
                        <Index:field Index:repeat="false" Index:name="lht" Index:navn="lht" Index:type="keyword" Index:boostFactor="8">
                            <xsl:for-each select="mc:subfield">
                                <xsl:choose>
                                    <xsl:when test="@code='a'">
                                        <xsl:value-of select="substring-after(.,'¤')"/>
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
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                        <xsl:text> </xsl:text>
                                    </xsl:when>
                                    <xsl:when test="@code='ø'">
                                        <xsl:value-of select="translate(.,'¤','')"/>
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
                                        <xsl:value-of select="translate(.,'¤','')"/>
                                        <xsl:text> </xsl:text>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:for-each>
                        </Index:field>
                           <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                               <xsl:for-each select="mc:subfield">
                                   <xsl:choose>
                                       <xsl:when test="@code='a'">
                                           <xsl:value-of select="substring-after(.,'¤')"/>
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
                                           <xsl:value-of select="translate(.,'¤','')"/>
                                           <xsl:text> </xsl:text>
                                       </xsl:when>
                                       <xsl:when test="@code='ø'">
                                           <xsl:value-of select="translate(.,'¤','')"/>
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
                                           <xsl:value-of select="translate(.,'¤','')"/>
                                           <xsl:text> </xsl:text>
                                       </xsl:when>
                                   </xsl:choose>
                               </xsl:for-each>
                           </Index:field>

                    </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='739' or @tag='H39']/mc:subfield[@code='t' or @code='ø' or @code='v' or @code='u']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
               <xsl:if test="contains(.,'¤')">
                   <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                       <xsl:for-each select=".">
                           <xsl:value-of select="substring-after(.,'¤')"/>
                       </xsl:for-each>
                   </Index:field>

               </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='740' or @tag='H40']/mc:subfield[@code='a' or @code='s']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select=".">
                            <xsl:value-of select="translate(.,'¤','')"/>
                            <xsl:text> </xsl:text>
                        </xsl:for-each>
                          </Index:field>
                    <xsl:if test="contains(.,'¤')">
                       <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                           <xsl:for-each select=".">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </xsl:for-each>
                    </Index:field>
                    </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='745' or @tag='H45' or @tag='945' or @tag='J45']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="translate(.,'¤','')"/>
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
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='æ']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='ø']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                   <xsl:if test="contains(mc:subfield[@code='a'],'¤')">

                 <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="substring-after(.,'¤')"/>
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
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='æ']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='ø']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                    </Index:field>
                    </xsl:if>

                </xsl:for-each>

                <xsl:for-each select="mc:datafield[@tag='241' or @tag='C41']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="5">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="translate(.,'¤','')"/>
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
                 <xsl:if test="contains(mc:subfield[@code='a'],'¤')">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="5">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="substring-after(.,'¤')"/>
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
                     </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='795' or @tag='H95']">
                     <xsl:for-each select="mc:subfield[@code='a']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="6">

                            <xsl:value-of select="translate(.,'¤','')"/>
                             
                    </Index:field>
                         <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="6">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                           </xsl:for-each>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='795' or @tag='H95']/mc:subfield[@code='p' or @code='u' or @code='v'] ">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="6">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="ltitel" Index:type="keyword" Index:boostFactor="6">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='534' or @tag='F34']/mc:subfield[@code='t' or @code='x']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="6">
                        <xsl:value-of select="."/>
                        <xsl:text> </xsl:text>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="6">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='247' or @tag='C47' or @tag='248' or @tag='C48']/mc:subfield[@code='p']">
                    <Index:field Index:repeat="false" Index:name="lpa" Index:navn="lpa" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>

                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="ltil" Index:type="keyword" Index:boostFactor="4">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='247' or @tag='C47' or @tag='248' or @tag='C48']/mc:subfield[@code='x']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='526' or @tag='F26']/mc:subfield[@code='t' or @code='x']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='530' or @tag='F30']/mc:subfield[@code='t' or @code='x']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="."/>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='700' or @tag='H00']/mc:subfield[@code='t']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='710' or @tag='H10']/mc:subfield[@code='t']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                     <xsl:if test="contains(.,'¤')">
                                  <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">

                            <xsl:value-of select="substring-after(.,'¤')"/>

                    </Index:field>
                         </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='210' or @tag='C10']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="translate(.,'¤','')"/>
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
                 <xsl:if test="contains(mc:subfield[@code='a'],'¤')">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="substring-after(.,'¤')"/>
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
                     </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='222' or @tag='C22']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="8">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='512' or @tag='F12']/mc:subfield[@code='t' or @code='x']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                    <xsl:if test="contains(.,'¤')">
                        <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </Index:field>

                    </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='520' or @tag='F20']/mc:subfield[@code='t' or @code='x']">
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="."/>
                    </Index:field>
                    <xsl:if test="contains(.,'¤')">
                        <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="4">
                            <xsl:value-of select="substring-after(.,'¤')"/>
                        </Index:field>

                    </xsl:if>
                </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='440' or @tag='840' or @tag='E40' or @tag='I40']">
                <Index:field Index:repeat="false" Index:name="lse" Index:navn="lse" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'¤','')"/>
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
                </Index:field>

                <xsl:if test="contains(mc:subfield[@code='a'],'¤')">
                          <Index:field Index:repeat="false" Index:name="lse" Index:navn="lse" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="substring-after(.,'¤')"/>
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
                </Index:field>

                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='440' or @tag='840' or @tag='E40' or @tag='I40']">
                <Index:field Index:repeat="false" Index:name="lso" Index:navn="lso" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'¤','')"/>
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

                </Index:field>
                <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="translate(.,'¤','')"/>
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

                </Index:field>

                <xsl:if test="contains(mc:subfield[@code='a'],'¤')">
                          <Index:field Index:repeat="false" Index:name="lso" Index:navn="lso" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="substring-after(.,'¤')"/>
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
                          
                </Index:field>
                    <Index:field Index:repeat="false" Index:name="lti" Index:navn="lti" Index:type="keyword" Index:boostFactor="10">
              <xsl:for-each select="mc:subfield[@code='a']">
                  <xsl:value-of select="substring-after(.,'¤')"/>
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

          </Index:field>

                </xsl:if>
            </xsl:for-each>

        

    </xsl:template>

</xsl:stylesheet>
