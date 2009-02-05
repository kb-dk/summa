<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="lma">
        <xsl:choose>
            <xsl:when test="mc:datafield[@tag='001']/mc:subfield[@code='f']='new'">
                <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
                    <xsl:for-each select=".">
                        <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                            <xsl:text>Nordicom</xsl:text>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='a' or @code='b']">
                        <xsl:if test="contains(.,'a')">
                            <xsl:choose>
                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'m') or contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'s')">
                                    <xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
                                        <xsl:choose>
                                            <xsl:when test="contains(.,'xx')">
                                                <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                    <xsl:text>bog</xsl:text>
                                                </Index:field>
                                                <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                    <xsl:text>trykt_bog</xsl:text>
                                                </Index:field>
                                            </xsl:when>
                                            <xsl:when test="contains(.,'xe')">
                                                <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                    <xsl:text>bog</xsl:text>
                                                </Index:field>
                                                <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                    <xsl:text>e_bog</xsl:text>
                                                </Index:field>
                                            </xsl:when>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </xsl:when>

                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

                                    <xsl:choose>
                                        <xsl:when test="/mc:record/mc:datafield[@tag='557']">
                                            <xsl:choose>
                                                <xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
                                                    <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                        <xsl:text>artikel</xsl:text>
                                                    </Index:field>
                                                    <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                        <xsl:text>tss_art</xsl:text>
                                                    </Index:field>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                        <xsl:text>artikel</xsl:text>
                                                    </Index:field>
                                                    <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                        <xsl:text>tss_art</xsl:text>
                                                    </Index:field>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:when>

                                        <xsl:when test="/mc:record/mc:datafield[@tag='558']">
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>artikel</xsl:text>
                                            </Index:field>
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>bog_art</xsl:text>
                                            </Index:field>
                                        </xsl:when>

                                        <xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h'],'xe')">
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>artikel</xsl:text>
                                            </Index:field>
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>e_art</xsl:text>
                                            </Index:field>
                                        </xsl:when>

                                        <xsl:otherwise>
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>bog</xsl:text>
                                            </Index:field>
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>tss_specif</xsl:text>
                                            </Index:field>
                                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                <xsl:text>trykt_bog</xsl:text>
                                            </Index:field>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:if>

                        <xsl:if test="contains(.,'m')">
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>film</xsl:text>
                            </Index:field>
                        </xsl:if>

                        <xsl:if test="contains(.,'r')">
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>lyd_musik</xsl:text>
                            </Index:field>
                        </xsl:if>
                    </xsl:for-each>

                </Index:group>
            </xsl:when>

            <!-- Det gamle format -->

            <xsl:otherwise>
                <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
                    <xsl:for-each select=".">
                        <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                            <xsl:text>Nordicom</xsl:text>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:choose>
                        <xsl:when test="/mc:record/mc:datafield[@tag='140']/mc:subfield[@code='a']">
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>artikel</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>bog_art</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:when test="/mc:record/mc:datafield[@tag='150']/mc:subfield[@code='a']">
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>artikel</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>tss_art</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>bog</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                <xsl:text>trykt_bog</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </Index:group>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>