<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">
    <xsl:template name="ma">
        <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='A09'] and
                    not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'td'))
                    and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'te'))">
                    <xsl:for-each select="mc:datafield[@tag='A09']/mc:subfield">

                        <xsl:call-template name="klartekster009-2bogst"/>

                    </xsl:for-each>

                </xsl:when>
                <xsl:otherwise>
                    <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield">

                        <xsl:call-template name="klartekster009-2bogst"/>


                    </xsl:for-each>

                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='A05']">
                    <xsl:for-each select="mc:datafield[@tag='A05']/mc:subfield">

                        <xsl:call-template name="klartekster005-2bogst"/>

                    </xsl:for-each>

                </xsl:when>
                <xsl:otherwise>
                    <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield">

                        <xsl:call-template name="klartekster005-2bogst"/>


                    </xsl:for-each>

                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='A08']">
                    <xsl:for-each select="mc:datafield[@tag='A08']/mc:subfield">

                        <xsl:call-template name="klartekster008-2bogst"/>

                    </xsl:for-each>

                </xsl:when>
                <xsl:otherwise>
                    <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield">

                        <xsl:call-template name="klartekster008-2bogst"/>


                    </xsl:for-each>

                </xsl:otherwise>
            </xsl:choose>




        </Index:group>
        <xsl:choose>
            <xsl:when test="/mc:record/mc:datafield[@tag='A09'] and
                    not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'td'))
                    and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'te'))">
                <xsl:for-each select="mc:datafield[@tag='A09']/mc:subfield[@code='g']">
                    <xsl:call-template name="klartekster009-lange-g"/>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='A09']/mc:subfield[@code='h']">
                    <xsl:call-template name="klartekster009-lange-h"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='g']">

                    <xsl:call-template name="klartekster009-lange-g"/>


                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='h']">

                    <xsl:call-template name="klartekster009-lange-h"/>


                </xsl:for-each>

            </xsl:otherwise>
        </xsl:choose>



        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='D00']">
                <xsl:for-each select="mc:datafield[@tag='D00']/mc:subfield[@code='e']">
                    <xsl:call-template name="klartekster300-lange"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='300']/mc:subfield[@code='e']">
                    <xsl:call-template name="klartekster300-lange"/>
                </xsl:for-each>

            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='D05']">
                <xsl:for-each select="mc:datafield[@tag='D05']/mc:subfield[@code='z']">
                    <xsl:if test="contains(.,'p') or contains(.,'q')">
                        <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                            <xsl:text>lydbog</xsl:text>
                        </Index:field>
                    </xsl:if>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='z']">
                    <xsl:if test="contains(.,'p') or contains(.,'q')">
                        <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                            <xsl:text>lydbog</xsl:text>
                        </Index:field>
                    </xsl:if>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:for-each select="mc:datafield[@tag='014']/mc:subfield[@code='x']">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="klartekster009-2bogst">
        <xsl:if test="@code='a'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'a')">
                    <xsl:text>te</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'b')">
                    <xsl:text>hå</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'c')">
                    <xsl:text>mu</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'d')">
                    <xsl:text>mu</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'e')">
                    <xsl:text>km</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'f')">
                    <xsl:text>km</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'g')">
                    <xsl:text>bi</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'m')">
                    <xsl:text>fi</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'n')">
                    <xsl:text>fi</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'p')">
                    <xsl:text>br</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'r')">
                    <xsl:text>ly</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'s')">
                    <xsl:text>lm</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'t')">
                    <xsl:text>el</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'u')">
                    <xsl:text>tm</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'v')">
                    <xsl:text>sm</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='b'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="2">
                <xsl:if test="contains(.,'a')">
                    <xsl:text>te</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'b')">
                    <xsl:text>hå</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'c')">
                    <xsl:text>mu</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'d')">
                    <xsl:text>mu</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'e')">
                    <xsl:text>km</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'f')">
                    <xsl:text>km</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'g')">
                    <xsl:text>bi</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'m')">
                    <xsl:text>fi</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'n')">
                    <xsl:text>fi</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'p')">
                    <xsl:text>br</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'r')">
                    <xsl:text>ly</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'s')">
                    <xsl:text>lm</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'t')">
                    <xsl:text>el</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'u')">
                    <xsl:text>tm</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'v')">
                    <xsl:text>sm</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='g'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:choose>
                    <xsl:when test="contains(.,'xf')">
                        <xsl:text>te</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='h'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="2">
                <xsl:choose>
                    <xsl:when test="contains(.,'xf')">
                        <xsl:text>te</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </Index:field>
        </xsl:if>
    </xsl:template>
    <xsl:template name="klartekster005-2bogst">
        <xsl:if test="@code='h'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'a')">
                    <xsl:text>lv</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'b')">
                    <xsl:text>lw</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'c')">
                    <xsl:text>lu</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'d')">
                    <xsl:text>lh</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'e')">
                    <xsl:text>ll</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'f')">
                    <xsl:text>lt</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'g')">
                    <xsl:text>lr</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'h')">
                    <xsl:text>ls</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'i')">
                    <xsl:text>lø</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'j')">
                    <xsl:text>la</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'k')">
                    <xsl:text>ld</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'l')">
                    <xsl:text>lj</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'m')">
                    <xsl:text>li</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'n')">
                    <xsl:text>lp</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'o')">
                    <xsl:text>lq</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'p')">
                    <xsl:text>læ</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'q')">
                    <xsl:text>lå</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'r')">
                    <xsl:text>le</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'s')">
                    <xsl:text>lg</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'t')">
                    <xsl:text>lf</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'u')">
                    <xsl:text>lz</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'v')">
                    <xsl:text>lx</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'w')">
                    <xsl:text>lc</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'x')">
                    <xsl:text>ln</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'y')">
                    <xsl:text>mm</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='i'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'a')">
                    <xsl:text>pa</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'b')">
                    <xsl:text>ps</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'c')">
                    <xsl:text>pl</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'d')">
                    <xsl:text>pk</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'e')">
                    <xsl:text>pp</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'g')">
                    <xsl:text>px</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'h')">
                    <xsl:text>ph</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'i')">
                    <xsl:text>pd</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'j')">
                    <xsl:text>pc</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'k')">
                    <xsl:text>pr</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'l')">
                    <xsl:text>pu</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'o')">
                    <xsl:text>po</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='j'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'a')">
                    <xsl:text>st</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'b')">
                    <xsl:text>sv</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'c')">
                    <xsl:text>so</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='z'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'p')">
                    <xsl:text>lb</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'q')">
                    <xsl:text>lb</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'q')">
                    <xsl:text>lk</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>

    </xsl:template>
    <xsl:template name="klartekster008-2bogst">
        <xsl:if test="@code='d'">

            <xsl:if test="contains(.,'a')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>bl</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'b')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>ka</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'c')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>rg</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'d')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>rf</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'e')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>ob</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'f')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>ec</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'g')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>vv</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'h')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>bj</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'i')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>sa</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'j')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>pg</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'k')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>pt</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'l')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>sd</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'m')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>dp</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'n')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>lo</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'o')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>ta</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'p')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>tr</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'q')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>ex</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'r')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>tt</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'s')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>am</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'t')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>tn</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'u')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>ug</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'w')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>rw</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'z')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>bv</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'å')">
                <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:text>så</xsl:text>
                </Index:field>
            </xsl:if>
        </xsl:if>
        <xsl:if test="@code='f'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'1')">
                    <xsl:text>kf</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='g'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'1')">
                    <xsl:text>fe</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='h'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'m')">
                    <xsl:text>ms</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'n')">
                    <xsl:text>av</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'p')">
                    <xsl:text>ts</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'z')">
                    <xsl:text>åp</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'?')">
                    <xsl:text>up</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='j'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'d')">
                    <xsl:text>dr</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'e')">
                    <xsl:text>ea</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'f')">
                    <xsl:text>ro</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'i')">
                    <xsl:text>bx</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'j')">
                    <xsl:text>no</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'m')">
                    <xsl:text>ig</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'p')">
                    <xsl:text>di</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='m'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'1')">
                    <xsl:text>ss</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='n'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'a')">
                    <xsl:text>ou</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'b')">
                    <xsl:text>od</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'c')">
                    <xsl:text>oi</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='o'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'b')">
                    <xsl:text>bø</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'s')">
                    <xsl:text>bs</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='q'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='r'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:if>
        <xsl:if test="@code='t'">
            <Index:field Index:repeat="true" Index:name="ma_kort" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:if test="contains(.,'m')">
                    <xsl:text>mo</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'s')">
                    <xsl:text>sæ</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'p')">
                    <xsl:text>pe</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'a')">
                    <xsl:text>an</xsl:text>
                </xsl:if>
                <xsl:if test="contains(.,'h')">
                    <xsl:text>hj</xsl:text>
                </xsl:if>
            </Index:field>
        </xsl:if>

    </xsl:template>
    <xsl:template name="klartekster009-lange-g">

        <xsl:if test="substring(.,1)='ga'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>akvarel</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>billedtæppe</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>grafisk blad</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gk'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>originalkunst</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gm'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>maleri</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gp'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>plakat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gr'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>kunstreproduktion</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gt'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>tegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ha'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>arkitekturtegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>billedkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hd'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>ordkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>flonellograf</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hf'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>foto</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hl'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>flipover</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ho'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>postkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hp'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>planche</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hr'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>fotoreproduktion</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ht'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>teknisk tegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hy'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>symbolkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ia'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ic'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='if'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ih'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ik'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ip'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='is'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='it'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='kb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>billedbånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='kt'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>transparent</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='mj'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>filmspole</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='nh'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>video</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>cd-rom</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>cd-i</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='th'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>dvd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ti'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>foto-cd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tk'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>diskette</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='to'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>dvd-rom</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ua'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>måleapparat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ub'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>laborativt materiale</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ue'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>emnekasse</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ui'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>materiale til indlæringsapparat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ul'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ut'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>legetøj</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='uu'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>puslespil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ud'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>teaterdukke</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='us'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>udstillingsmontage</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='uv'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>øvelsesmodel</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='wt'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>teateropførelsem</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='wu'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>udstilling</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xa'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>dias</xsl:text>
            </Index:field>
        </xsl:if>

        <xsl:if test="substring(.,1)='xc'">
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='D00']">
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='D00']/mc:subfield[@code='e'],'mp3')">
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                <xsl:text>cd-mp3</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                <xsl:text>cd</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                <xsl:text>cd-mp3</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                <xsl:text>cd</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="substring(.,1)='xd'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>dvd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xe'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>netdokument</xsl:text>
            </Index:field>
            <xsl:for-each select="../mc:subfield[@code='a' or @code='b']">
                <xsl:if test="substring(.,1)='a'">
                    <xsl:choose>
                        <xsl:when test="/mc:record/mc:datafield[@tag='A08']">
                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'p')">
                                    <xsl:choose>

                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'n')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>netdokument (avis)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'z')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>netdokument (årbog)</xsl:text>
                                            </Index:field>
                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'p')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>netdokument (tidsskrift)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'m')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>netdokument (serie)</xsl:text>
                                            </Index:field>

                                        </xsl:when>

                                        <xsl:otherwise>
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>netdokument (tidsskrift)</xsl:text>
                                            </Index:field>

                                        </xsl:otherwise>
                                    </xsl:choose>

                                </xsl:when>
                            </xsl:choose>
                        </xsl:when>

                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                            <xsl:choose>

                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (avis)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (årbog)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (tidsskrift)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (serie)</xsl:text>
                                    </Index:field>

                                </xsl:when>

                                <xsl:otherwise>
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (tidsskrift)</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (sang)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>netdokument (artikel)</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                <xsl:text>netdokument (bog)</xsl:text>
                            </Index:field>

                        </xsl:otherwise>
                    </xsl:choose>

                </xsl:if>
                <xsl:if test="substring(.,1)='c'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (node)</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='e'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (kort)</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='g'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (billede)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='m'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (film)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='r'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (lyd)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='s'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (musik)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='t'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>netdokument (elektronisk materiale)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='v'">

                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>sammensat materiale</xsl:text>
                    </Index:field>

                </xsl:if>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="substring(.,1)='xg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>mini disc</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xh'">

            <xsl:choose>
                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
                     substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>lydbog (bånd)</xsl:text>
                    </Index:field>
                </xsl:when>
                <xsl:otherwise>
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>kassettelydbånd</xsl:text>
                    </Index:field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="substring(.,1)='xi'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>dcc-bånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xj'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>spolelydbånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xk'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>grammofonplade</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xl'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>fastplade</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xy'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>uspecificeret medie</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xn'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                <xsl:text>lydspor</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xx'">
            <xsl:for-each select="../mc:subfield[@code='a' or @code='b']">
                <xsl:if test="substring(.,1)='a'">
                    <xsl:choose>

                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                            <xsl:choose>

                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>avis</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>årbog</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>tidsskrift</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>serie</xsl:text>
                                    </Index:field>

                                </xsl:when>

                                <xsl:otherwise>
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>tidsskrift</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                        <xsl:text>sang</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:otherwise>


                                    <xsl:choose>

                                        <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                            <xsl:choose>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                            and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                        <xsl:text>anmeldelse (tidsskrift)</xsl:text>
                                                    </Index:field>

                                                </xsl:when>

                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                            and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">

                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                        <xsl:text>anmeldelse (avis)</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">

                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:when>
                                        <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>artikel i bog</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>netdokument (artikel)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:otherwise>
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                                <xsl:text>artikel</xsl:text>
                                            </Index:field>

                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                <xsl:text>bog</xsl:text>
                            </Index:field>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='m'],'1') or
(contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna')
and contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'rint')) or
(contains(/mc:record/mc:datafield[@tag='440']/mc:subfield[@code='a'],'agna')
and contains(/mc:record/mc:datafield[@tag='440']/mc:subfield[@code='a'],'rint'))">
                                <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                    <xsl:text>stor skrift</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'bi') or
           contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'illedbog') or
                             contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'illedbog') or
                             contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'illedbog')">
                                <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                    <xsl:text>billedbog</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'te') or
           contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'egneserie') or
                             contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'egneserie') or
                             contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'egneserie')">
                                <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                                    <xsl:text>tegneserie</xsl:text>
                                </Index:field>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:if test="substring(.,1)='c'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>node</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='e'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>kort</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='p'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>punktskrift</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='v'">

                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token" Index:boostFactor="4">
                        <xsl:text>sammensat materiale</xsl:text>
                    </Index:field>

                </xsl:if>

            </xsl:for-each>
        </xsl:if>

    </xsl:template>
    <xsl:template name="klartekster009-lange-h">

        <xsl:if test="substring(.,1)='ga'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>akvarel</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>billedtæppe</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>grafisk blad</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gk'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>originalkunst</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gm'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>maleri</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gp'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>plakat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gr'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>kunstreproduktion</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gt'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>tegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ha'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>arkitekturtegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>billedkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hd'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>ordkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>flonellograf</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hf'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>foto</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hl'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>flipover</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ho'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>postkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hp'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>planche</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hr'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>fotoreproduktion</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ht'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>teknisk tegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hy'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>symbolkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ia'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ic'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='if'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ih'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ik'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ip'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='is'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='it'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='kb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>billedbånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='kt'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>transparent</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='mj'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>filmspole</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='nh'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>video</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>cd-rom</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>cd-i</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='th'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>dvd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ti'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>foto-cd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tk'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>diskette</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='to'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>dvd-rom</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ua'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>måleapparat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ub'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>laborativt materiale</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ue'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>emnekasse</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ui'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>materiale til indlæringsapparat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ul'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ut'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>legetøj</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='uu'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>puslespil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ud'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>teaterdukke</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='us'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>udstillingsmontage</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='uv'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>øvelsesmodel</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='wt'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>teateropførelsem</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='wu'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>udstilling</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xa'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xb'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>dias</xsl:text>
            </Index:field>
        </xsl:if>

        <xsl:if test="substring(.,1)='xc'">
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='D00']">
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='D00']/mc:subfield[@code='e'],'mp3')">
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>cd-mp3</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>cd</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>cd-mp3</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>cd</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="substring(.,1)='xd'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>dvd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xe'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>netdokument</xsl:text>
            </Index:field>
            <xsl:for-each select="../mc:subfield[@code='a' or @code='b']">
                <xsl:if test="substring(.,1)='a'">
                    <xsl:choose>
                        <xsl:when test="/mc:record/mc:datafield[@tag='A08']">
                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='t'],'p')">
                                    <xsl:choose>

                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'n')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>netdokument (avis)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'z')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>netdokument (årbog)</xsl:text>
                                            </Index:field>
                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'p')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>netdokument (tidsskrift)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'m')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>netdokument (serie)</xsl:text>
                                            </Index:field>

                                        </xsl:when>

                                        <xsl:otherwise>
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>netdokument (tidsskrift)</xsl:text>
                                            </Index:field>

                                        </xsl:otherwise>
                                    </xsl:choose>

                                </xsl:when>
                            </xsl:choose>
                        </xsl:when>

                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                            <xsl:choose>

                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (avis)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (årbog)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (tidsskrift)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (serie)</xsl:text>
                                    </Index:field>

                                </xsl:when>

                                <xsl:otherwise>
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (tidsskrift)</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (sang)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>netdokument (artikel)</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>netdokument (bog)</xsl:text>
                            </Index:field>

                        </xsl:otherwise>
                    </xsl:choose>

                </xsl:if>
                <xsl:if test="substring(.,1)='c'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (node)</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='e'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (kort)</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='g'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (billede)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='m'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (film)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='r'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (lyd)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='s'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (musik)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='t'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>netdokument (elektronisk materiale)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='v'">

                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>sammensat materiale</xsl:text>
                    </Index:field>

                </xsl:if>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="substring(.,1)='xg'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mini disc</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xh'">

            <xsl:choose>
                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
                     substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>lydbog (bånd)</xsl:text>
                    </Index:field>
                </xsl:when>
                <xsl:otherwise>
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>kassettelydbånd</xsl:text>
                    </Index:field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="substring(.,1)='xi'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>dcc-bånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xj'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>spolelydbånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xk'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>grammofonplade</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xl'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>fastplade</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xy'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>uspecificeret medie</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xn'">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>lydspor</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xx'">
            <xsl:for-each select="../mc:subfield[@code='a' or @code='b']">
                <xsl:if test="substring(.,1)='a'">
                    <xsl:choose>

                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                            <xsl:choose>

                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'n')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>avis</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>årbog</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>tidsskrift</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>serie</xsl:text>
                                    </Index:field>

                                </xsl:when>

                                <xsl:otherwise>
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>tidsskrift</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>sang</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:otherwise>


                                    <xsl:choose>

                                        <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                            <xsl:choose>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                            and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                        <xsl:text>anmeldelse (tidsskrift)</xsl:text>
                                                    </Index:field>

                                                </xsl:when>

                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                            and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">

                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                        <xsl:text>anmeldelse (avis)</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">

                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:when>
                                        <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>artikel i bog</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>netdokument (artikel)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:otherwise>
                                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                                <xsl:text>artikel</xsl:text>
                                            </Index:field>

                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>bog</xsl:text>
                            </Index:field>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='m'],'1') or
(contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna')
and contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'rint')) or
(contains(/mc:record/mc:datafield[@tag='440']/mc:subfield[@code='a'],'agna')
and contains(/mc:record/mc:datafield[@tag='440']/mc:subfield[@code='a'],'rint'))">
                                <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>stor skrift</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'bi') or
           contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'illedbog') or
                             contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'illedbog') or
                             contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'illedbog')">
                                <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>billedbog</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'te') or
           contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'egneserie') or
                             contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'egneserie') or
                             contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'egneserie')">
                                <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>tegneserie</xsl:text>
                                </Index:field>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:if test="substring(.,1)='c'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>node</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='e'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>kort</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='p'">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>punktskrift</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='v'">

                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>sammensat materiale</xsl:text>
                    </Index:field>

                </xsl:if>

            </xsl:for-each>
        </xsl:if>

    </xsl:template>
    <xsl:template name="klartekster300-lange">
        <xsl:if test="contains(.,'lu ray') or contains(.,'lu-ray')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>blu ray disc</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'game boy') or contains(.,'ame Boy') or contains(.,'ameBoy')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>gameboy-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'mp3')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>mp3</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'playstation 2')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>playstation2-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'playstation')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>playstation-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'PSP')">
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='A09']
                         and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'td'))
                         and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'te'))">
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A09']/subfield[@code='a'],'m')">
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>PSP-film</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>PSP-spil</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'m')">
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>PSP-film</xsl:text>
                    </Index:field>
                </xsl:when>
                <xsl:otherwise>
                    <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                        <xsl:text>PSP-spil</xsl:text>
                    </Index:field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(.,'xbox')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>xbox-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'wii')">
            <Index:field Index:repeat="true" Index:name="ma_lang" Index:navn="ma_lang" Index:type="token">
                <xsl:text>wii-spil</xsl:text>
            </Index:field>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
