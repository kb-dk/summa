<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">
    <xsl:template name="material">
        <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
            <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='b']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="2">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='g']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:choose>
                        <xsl:when test="contains(.,'xf')">
                            <xsl:text>te</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="."/>
                        </xsl:otherwise>
                    </xsl:choose>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='h']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="2">
                    <xsl:choose>
                        <xsl:when test="contains(.,'xf')">
                            <xsl:text>te</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="."/>
                        </xsl:otherwise>
                    </xsl:choose>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='z']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='d']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:if test="contains(.,'a')">
                        <xsl:text>bl</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'b')">
                        <xsl:text>ka</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'c')">
                        <xsl:text>rg</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'d')">
                        <xsl:text>rf</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'e')">
                        <xsl:text>ob</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'f')">
                        <xsl:text>ec</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'g')">
                        <xsl:text>vv</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'h')">
                        <xsl:text>bj</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'i')">
                        <xsl:text>sa</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'j')">
                        <xsl:text>pg</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'k')">
                        <xsl:text>pt</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'l')">
                        <xsl:text>sd</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'m')">
                        <xsl:text>dp</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'n')">
                        <xsl:text>lo</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'o')">
                        <xsl:text>ta</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'p')">
                        <xsl:text>tr</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'q')">
                        <xsl:text>ex</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'r')">
                        <xsl:text>tt</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'s')">
                        <xsl:text>am</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'t')">
                        <xsl:text>tn</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'u')">
                        <xsl:text>ug</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'w')">
                        <xsl:text>rw</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'z')">
                        <xsl:text>bv</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'å')">
                        <xsl:text>så</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'x')">
                        <xsl:text>sk</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'y')">
                        <xsl:text>fa</xsl:text>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='f']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:if test="contains(.,'1')">
                        <xsl:text>kf</xsl:text>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='g']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:if test="contains(.,'1')">
                        <xsl:text>fe</xsl:text>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='h']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='j']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='m']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:if test="contains(.,'1')">
                        <xsl:text>ss</xsl:text>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='n']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='o']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:if test="contains(.,'b')">
                        <xsl:text>bø</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'s')">
                        <xsl:text>bs</xsl:text>
                    </xsl:if>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='q']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='r']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='t']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='h']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='i']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='j']">
                <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
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
            </xsl:for-each>
        </Index:group>

        <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='a' or @code='b']">
            <xsl:if test="contains(.,'a')">
                <xsl:choose>
                    <xsl:when test="contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'m') or contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'s')">
                        <xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
                            <xsl:choose>
                                <xsl:when test="contains(.,'xx')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>bog</xsl:text>
                                    </Index:field>
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>trykt bog</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(.,'xe')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>bog</xsl:text>
                                    </Index:field>
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>e bog</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(.,'xa')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>bog</xsl:text>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:when>

                    <xsl:when test="contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                        <xsl:choose>
                            <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                <xsl:choose>
                                    <xsl:when test="contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>artikel</xsl:text>
                                        </Index:field>
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>tss art</xsl:text>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>artikel</xsl:text>
                                        </Index:field>
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>avis art</xsl:text>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>artikel</xsl:text>
                                        </Index:field>
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>tss art</xsl:text>
                                        </Index:field>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>artikel</xsl:text>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>bog art</xsl:text>
                                </Index:field>
                            </xsl:when>
                            <xsl:when test="contains(/record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h'],'xe')">
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>artikel</xsl:text>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>e art</xsl:text>
                                </Index:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>bog</xsl:text>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>tss specif</xsl:text>
                                </Index:field>
                                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                    <xsl:text>trykt bog</xsl:text>
                                </Index:field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when test="contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>peri</xsl:text>
                        </Index:field>
                        <xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
                            <xsl:choose>
                                <xsl:when test="contains(.,'xx')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>trykt peri</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(.,'xe')">

                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>e peri</xsl:text>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="contains(.,'c') or contains(.,'d')">
                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                    <xsl:text>node</xsl:text>
                </Index:field>
                <xsl:choose>
                    <xsl:when test="contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'m') or contains(/record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'s')">
                        <xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
                            <xsl:choose>
                                <xsl:when test="contains(.,'xe')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>e node</xsl:text>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="contains(.,'g')">
                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                    <xsl:text>billede</xsl:text>
                </Index:field>
            </xsl:if>
            <xsl:if test="contains(.,'m')">
                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                    <xsl:text>film</xsl:text>
                </Index:field>
                <xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">

                    <xsl:choose>
                        <xsl:when test="contains(.,'th')">

                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>dvd film</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'xd')">

                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>dvd film</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'np')">

                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>dvd film</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'nh')">

                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>vhs film</xsl:text>
                            </Index:field>
                        </xsl:when>

                    </xsl:choose>
                </xsl:for-each>
            </xsl:if>
            <xsl:if test="contains(.,'r') or contains(.,'s')">
                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                    <xsl:text>lyd musik</xsl:text>
                </Index:field>
                <xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">

                    <xsl:choose>
                        <xsl:when test="contains(.,'xc')">
                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>cd lyd</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'xd') or contains(.,'th')">
                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>dvd lyd</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'nh')">
                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>vhs lyd</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'xh')">
                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>kas lyd</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="contains(.,'xk')">
                            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                <xsl:text>gram lyd</xsl:text>
                            </Index:field>
                            <xsl:choose>
                                <xsl:when test="contains(/record/mc:datafield[@tag='998']/mc:subfield[@code='a'],'lak')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>lak lyd</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/record/mc:datafield[@tag='300']/mc:subfield[@code='b'],'78')">
                                    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                        <xsl:text>lak lyd</xsl:text>
                                    </Index:field>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:when>

                    </xsl:choose>
                </xsl:for-each>
            </xsl:if>
            <xsl:if test="contains(.,'t')">
                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                    <xsl:text>elektr</xsl:text>
                </Index:field>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
