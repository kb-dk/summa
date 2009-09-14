<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="lem">

            <xsl:for-each select="mc:datafield[@tag='600' or @tag='G00']">
                <Index:field Index:repeat="false" Index:name="lep" Index:navn="lep" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
                 <Index:field Index:repeat="false" Index:name="lke" Index:navn="lke" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>
                 <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="person_name_inverted"/>
                </Index:field>

            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='600' or @tag='G00']">
                <xsl:if test="mc:subfield[@code='t' or @code='x' or @code='y' or @code='z' or @code='u']">
                    <Index:field Index:repeat="false" Index:name="lep" Index:navn="lep" Index:type="keyword" Index:boostFactor="10">

                        <xsl:call-template name="person_name_inverted"/>
                        <xsl:for-each select="mc:subfield[@code='t']">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='x']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='y']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='z']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='u']">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                    <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="10">

                        <xsl:call-template name="person_name_inverted"/>
                        <xsl:for-each select="mc:subfield[@code='t']">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='x']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='y']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='z']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='u']">
                            <xsl:text> : </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>

                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='610' or @tag='G10']">
                <Index:field Index:repeat="false" Index:name="lek" Index:navn="lek" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corporate_name"/>
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:text> : </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='x']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='y']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='z']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="."/>
                        <xsl:text> </xsl:text>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> : </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lke" Index:navn="lke" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corporate_name"/>
                  </Index:field>
                <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corporate_name"/>
                  </Index:field>

               </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='620' or @tag='G20']/mc:subfield[@code='a']">
                <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='621' or @tag='G21']">
                <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="5">
                    <xsl:for-each select="mc:subfield[@code='a']">
                             <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>)</xsl:text>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='e']">
                            <xsl:text>. </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='f']">
                              <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>)</xsl:text>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='j']">
                            <xsl:text>: </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                         </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='631' or @tag='G31']/mc:subfield[@code='a' or @code='b' or @code='f' or @code='g' or @code='s' or @code='t']">
                <Index:field Index:repeat="false" Index:name="luk" Index:navn="luk" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>

            </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='631' or @tag='G31']/mc:subfield[@code='f' or @code='g']">
                <Index:field Index:repeat="false" Index:name="ldf" Index:navn="ldf" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>


            </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='631' or @tag='G31']/mc:subfield[@code='s' or @code='t']">
                <Index:field Index:repeat="false" Index:name="lds" Index:navn="lds" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>


            </xsl:for-each>
         <xsl:for-each select="mc:datafield[@tag='631' or @tag='G31']/mc:subfield[@code='a' or @code='b']">
            <xsl:choose>
             <xsl:when test="starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m'],'sk') or
            starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'82') or
            starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'83') or
             starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'84') or
             starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'85') or
             starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'86') or
              starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'87') or
               starts-with(/mc:record/mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='m' or @code='n' or @code='o'],'88')">
                  <Index:field Index:repeat="false" Index:name="lds" Index:navn="lds" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
               </xsl:when>
                <xsl:otherwise>
                      <Index:field Index:repeat="false" Index:name="ldf" Index:navn="ldf" Index:type="keyword" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                </xsl:otherwise>
             </xsl:choose>


            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='652' or @tag='654' or @tag='655' or @tag='G52' or @tag='G54' or @tag='G55']">
                <xsl:if test="mc:subfield[@code='a' or @code='b' or @code='t']">
                    <Index:field Index:repeat="false" Index:name="lau" Index:navn="lau" Index:type="keyword" Index:boostFactor="5">
                        <xsl:call-template name="person_name_inverted"/>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='t']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>
                    <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="5">
                        <xsl:call-template name="person_name_inverted"/>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='t']">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </Index:field>

                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66' or @tag='667' or @tag='G67']/mc:subfield[@code='e' or @code='f' or @code='l' or @code='m' or @code='n' or @code='o'  or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
                <Index:field Index:repeat="false" Index:name="lke" Index:navn="lke" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>

            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t']">
                <Index:field Index:repeat="false" Index:name="ldb" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='e' or @code='f' or @code='t']">
                <Index:field Index:repeat="false" Index:name="ldf" Index:navn="ldf" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
              <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='q' or @code='r' or @code='s']">
                <Index:field Index:repeat="false" Index:name="lds" Index:navn="lds" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='l' or @code='m' or @code='n']">
                <Index:field Index:repeat="false" Index:name="lme" Index:navn="lme" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='o']">
                <Index:field Index:repeat="false" Index:name="lfm" Index:navn="lfm" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='u']">
                <Index:field Index:repeat="false" Index:name="lnb" Index:navn="lnb" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='i' or @code='p']">
                <Index:field Index:repeat="false" Index:name="ldt" Index:navn="ldt" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='668' or @tag='G68']">
                <Index:field Index:repeat="false" Index:name="ldb" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lem" Index:navn="lem" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>

                <Index:field Index:repeat="false" Index:name="lke" Index:navn="lke" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lds" Index:navn="lds" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>

            


    </xsl:template>

</xsl:stylesheet>
