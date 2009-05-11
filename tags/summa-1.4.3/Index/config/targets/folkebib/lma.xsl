<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="lma">


        <xsl:choose>
            <xsl:when test="/mc:record/mc:datafield[@tag='A09'] and
                    not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'td'))
                    and not(contains(/mc:record/mc:datafield[@tag='A09']/mc:subfield[@code='h'],'te'))">
                <!-- Klartekster for *g og *h  så skal næste linie erstatte den aktive-->
               <!-- <xsl:for-each select="mc:datafield[@tag='A09']/mc:subfield[@code='g' or @code='h']">-->
                <xsl:for-each select="mc:datafield[@tag='A09']/mc:subfield[@code='g']">

                    <xsl:call-template name="klartekster"/>

                </xsl:for-each>

            </xsl:when>
            <xsl:otherwise>
                <!--Klartekster for både *g og *h, så skal næste linie erstatte den aktive -->
                <!--<xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">-->

                <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='g']">
                    <xsl:call-template name="klartekster"/>


                </xsl:for-each>

            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='D00']">
                <xsl:for-each select="mc:datafield[@tag='D00']/mc:subfield[@code='e']">
                    <xsl:call-template name="felt300-tekster"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='300']/mc:subfield[@code='e']">
                    <xsl:call-template name="felt300-tekster"/>
                </xsl:for-each>

            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='D05']">
                <xsl:for-each select="mc:datafield[@tag='D05']/mc:subfield[@code='z']">
                    <xsl:if test="contains(.,'p') or contains(.,'q')">
                        <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                            <xsl:text>lydbog</xsl:text>
                        </Index:field>
                    </xsl:if>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='z']">
                    <xsl:if test="contains(.,'p') or contains(.,'q')">
                        <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                            <xsl:text>lydbog</xsl:text>
                        </Index:field>
                    </xsl:if>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
    <xsl:template name="klartekster">

        <xsl:if test="substring(.,1)='ga'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>akvarel</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gb'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>billedtæppe</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gg'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>grafisk blad</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gk'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>originalkunst</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gm'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>maleri</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gp'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>plakat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gr'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>kunstreproduktion</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='gt'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>tegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ha'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>arkitekturtegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hb'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>billedkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hd'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>ordkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hg'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>flonellograf</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hf'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>foto</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hl'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>flipover</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ho'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>postkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hp'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>planche</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hr'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>fotoreproduktion</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ht'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>teknisk tegning</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='hy'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>symbolkort</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ia'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ic'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='if'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ih'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ik'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ip'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='is'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='it'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='kb'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>billedbånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='kt'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>transparent</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='mj'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>filmspole</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='nh'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>video</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tb'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>cd-rom</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tg'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>cd-i</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='th'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>dvd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ti'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>foto-cd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='tk'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>diskette</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='to'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>dvd-rom</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ua'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>måleapparat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ub'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>laborativt materiale</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ue'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>emnekasse</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ui'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>materiale til indlæringsapparat</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ul'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ut'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>legetøj</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='uu'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>puslespil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='ud'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>teaterdukke</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='us'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>udstillingsmontage</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='uv'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>øvelsesmodel</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='wt'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>teateropførelse</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='wu'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>udstilling</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xa'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mikroform</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xb'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>dias</xsl:text>
            </Index:field>
        </xsl:if>

        <xsl:if test="substring(.,1)='xc'">
            <xsl:choose>
                <xsl:when test="/mc:record/mc:datafield[@tag='D00']">
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='D00']/mc:subfield[@code='e'],'mp3')">
                            <xsl:choose>
                                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
                     substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>lydbog (mp3)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>mp3</xsl:text>
                                    </Index:field>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
              substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>lydbog (cd)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>cd</xsl:text>
                                    </Index:field>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='e'],'mp3')">
                            <xsl:choose>
                                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
                     substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>lydbog (mp3)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>mp3</xsl:text>
                                    </Index:field>
                                </xsl:otherwise>
                            </xsl:choose>


                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
                     substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>lydbog (cd)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>cd</xsl:text>
                                    </Index:field>
                                </xsl:otherwise>
                            </xsl:choose>

                            
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:if>
        <xsl:if test="substring(.,1)='xd'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>dvd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xe'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
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
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>netdokument (avis)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'z')">
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>netdokument (årbog)</xsl:text>
                                            </Index:field>
                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'p')">
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>netdokument (tidsskrift)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='A08']/mc:subfield[@code='h'],'m')">
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>netdokument (serie)</xsl:text>
                                            </Index:field>

                                        </xsl:when>

                                        <xsl:otherwise>
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
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
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (avis)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (årbog)</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (tidsskrift)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (serie)</xsl:text>
                                    </Index:field>

                                </xsl:when>

                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (tidsskrift)</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (sang)</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>netdokument (artikel)</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                <xsl:text>netdokument (bog)</xsl:text>
                            </Index:field>

                        </xsl:otherwise>
                    </xsl:choose>

                </xsl:if>
                <xsl:if test="substring(.,1)='c'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (node)</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='e'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (kort)</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='g'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (billede)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='m'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (film)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='r'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (lyd)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='s'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (musik)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='t'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>netdokument (elektronisk materiale)</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='v'">

                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>sammensat materiale</xsl:text>
                    </Index:field>

                </xsl:if>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="substring(.,1)='xg'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mini disc</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xh'">

            <xsl:choose>
                <xsl:when test="substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='p' or
                     substring(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='z'],1)='q'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>lydbog (bånd)</xsl:text>
                    </Index:field>
                </xsl:when>
                <xsl:otherwise>
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>kassettelydbånd</xsl:text>
                    </Index:field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="substring(.,1)='xi'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>dcc-bånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xj'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>spolelydbånd</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xk'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>grammofonplade</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xl'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>fastplade</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="substring(.,1)='xn'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
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
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>avis</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'z')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>årbog</xsl:text>
                                    </Index:field>
                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'p')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>tidsskrift</xsl:text>
                                    </Index:field>

                                </xsl:when>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='h'],'m')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>serie</xsl:text>
                                    </Index:field>

                                </xsl:when>

                                <xsl:otherwise>
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                        <xsl:text>periodicum</xsl:text>
                                    </Index:field>

                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='032']/mc:subfield[@code='x'],'SA')">
                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
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
                                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                        <xsl:text>anmeldelse (tidsskrift)</xsl:text>
                                                    </Index:field>

                                                </xsl:when>

                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='014']/mc:subfield[@code='x'],'ANM')
                                            and contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">

                                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                        <xsl:text>anmeldelse (avis)</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">

                                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:when
                                                        test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
                                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                        <xsl:text>avisartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                        <xsl:text>tidsskriftartikel</xsl:text>
                                                    </Index:field>

                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:when>
                                        <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>artikel i bog</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g'],'xe')">
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>netdokument (artikel)</xsl:text>
                                            </Index:field>

                                        </xsl:when>
                                        <xsl:otherwise>
                                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                                <xsl:text>artikel</xsl:text>
                                            </Index:field>

                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                <xsl:text>bog</xsl:text>
                            </Index:field>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='m'],'1') or
(contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'agna')
and contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'rint')) or
(contains(/mc:record/mc:datafield[@tag='440']/mc:subfield[@code='a'],'agna')
and contains(/mc:record/mc:datafield[@tag='440']/mc:subfield[@code='a'],'rint'))">
                                <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                    <xsl:text>stor skrift</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'bi') or
           contains(/mc:record/mc:datafield[@tag='504']/mc:subfield[@code='a'],'illedbog') or
                             contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'illedbog') or
                             contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'illedbog')">
                                <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                    <xsl:text>billedbog</xsl:text>
                                </Index:field>
                            </xsl:if>
                            <xsl:if test="contains(/mc:record/mc:datafield[@tag='038']/mc:subfield[@code='a'],'te') or
           contains(/mc:record/mc:datafield[@tag='250']/mc:subfield[@code='a'],'egneserie') or
                             contains(/mc:record/mc:datafield[@tag='505']/mc:subfield[@code='a'],'egneserie') or
                             contains(/mc:record/mc:datafield[@tag='631']/mc:subfield[@code='a'],'egneserie')">
                                <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                    <xsl:text>tegneserie</xsl:text>
                                </Index:field>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:if test="substring(.,1)='c'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>node</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='e'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>kort</xsl:text>
                    </Index:field>

                </xsl:if>
                <xsl:if test="substring(.,1)='p'">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>punktskrift</xsl:text>
                    </Index:field>
                </xsl:if>
                <xsl:if test="substring(.,1)='v'">

                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>sammensat materiale</xsl:text>
                    </Index:field>

                </xsl:if>

            </xsl:for-each>
        </xsl:if>
        <xsl:if test="substring(.,1)='xy'">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>uspecificeret medie</xsl:text>
            </Index:field>
        </xsl:if>

    </xsl:template>
    <xsl:template name="felt300-tekster">
        <xsl:if test="contains(.,'lu ray') or contains(.,'lu-ray')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>blu ray disc</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'game boy') or contains(.,'ame Boy') or contains(.,'ameBoy')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>gameboy-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'mp3')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>mp3</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'playstation 2')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>playstation2-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'playstation')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
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
                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                <xsl:text>PSP-film</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                                <xsl:text>PSP-spil</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='a'],'m')">
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>PSP-film</xsl:text>
                    </Index:field>
                </xsl:when>
                <xsl:otherwise>
                    <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                        <xsl:text>PSP-spil</xsl:text>
                    </Index:field>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:if test="contains(.,'xbox')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>xbox-spil</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'wii')">
            <Index:field Index:repeat="false" Index:name="lma" Index:navn="lma" Index:type="keyword">
                <xsl:text>wii-spil</xsl:text>
            </Index:field>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
