<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">
    <xsl:template name="material">
        <xsl:choose>
            <xsl:when test="mc:field[@type='001_00']/mc:subfield[@type='f']='new'">
                <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
                    <xsl:for-each select="mc:field[@type='009_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                            <xsl:if test="contains(.,'a')">
                                <xsl:text>te</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'m')">
                                <xsl:text>fi</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'r')">
                                <xsl:text>ly</xsl:text>
                            </xsl:if>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='009_00']/mc:subfield[@type='b']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="2">
                            <xsl:if test="contains(.,'a')">
                                <xsl:text>te</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'m')">
                                <xsl:text>fi</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'r')">
                                <xsl:text>ly</xsl:text>
                            </xsl:if>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='009_00']/mc:subfield[@type='g']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='009_00']/mc:subfield[@type='h']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="2">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='008_00']/mc:subfield[@type='d']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                            <xsl:if test="contains(.,'a')">
                                <xsl:text>bl</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'f')">
                                <xsl:text>ec</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'m')">
                                <xsl:text>dp</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'p')">
                                <xsl:text>tr</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'q')">
                                <xsl:text>ex</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'s')">
                                <xsl:text>am</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'w')">
                                <xsl:text>rw</xsl:text>
                            </xsl:if>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='008_00']/mc:subfield[@type='f']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                            <xsl:if test="contains(.,'1')">
                                <xsl:text>kf</xsl:text>
                            </xsl:if>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='008_00']/mc:subfield[@type='r']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>

                    <xsl:for-each select="mc:field[@type='008_00']/mc:subfield[@type='t']">
                        <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                            <xsl:if test="contains(.,'m')">
                                <xsl:text>mo</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'s')">
                                <xsl:text>sæ</xsl:text>
                            </xsl:if>
                            <xsl:if test="contains(.,'a')">
                                <xsl:text>an</xsl:text>
                            </xsl:if>
                        </Index:field>
                    </xsl:for-each>
                </Index:group>

                <xsl:for-each select="mc:field[@type='009_00']/mc:subfield[@type='a' or @type='b']">
                    <xsl:if test="contains(.,'a')">
                        <xsl:choose>
                            <xsl:when test="contains(/mc:record/mc:field[@type='008_00']/mc:subfield[@type='t'],'m') or contains(/mc:record/mc:field[@type='008']/mc:subfield[@type='t'],'s')">
                                <xsl:for-each select="/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g' or @type='h']">
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
                                    </xsl:choose>
                                </xsl:for-each>
                            </xsl:when>

                            <xsl:when test="contains(/mc:record/mc:field[@type='008_00']/mc:subfield[@type='t'],'a')">

                                <xsl:choose>
                                    <xsl:when test="/mc:record/mc:field[@type='557_00']">
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>artikel</xsl:text>
                                        </Index:field>
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>tss art</xsl:text>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="/mc:record/mc:field[@type='558_00']">
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>artikel</xsl:text>
                                        </Index:field>
                                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                                            <xsl:text>bog art</xsl:text>
                                        </Index:field>
                                    </xsl:when>
                                    <xsl:when test="contains(/mc:record/mc:field[@type='009_00']/mc:subfield[@type='g' or @type='h'],'xe')">
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
                        </xsl:choose>
                    </xsl:if>

                    <xsl:if test="contains(.,'m')">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>film</xsl:text>
                        </Index:field>
                    </xsl:if>

                    <xsl:if test="contains(.,'r')">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>lyd musik</xsl:text>
                        </Index:field>
                    </xsl:if>
                </xsl:for-each>
            </xsl:when>

            <!-- det gamle format -->

            <xsl:otherwise>
                <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
                    <xsl:choose>
                        <xsl:when test="/mc:record/mc:field[@type='140_00']/mc:subfield[@type='a'] or /mc:record/mc:field[@type='150_00']/mc:subfield[@type='a']">
                            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                <xsl:text>an</xsl:text>
                            </Index:field>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                <xsl:text>te</xsl:text>
                            </Index:field>
                            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                <xsl:text>mo</xsl:text>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
                </Index:group>

                <xsl:choose>
                    <xsl:when test="/mc:record/mc:field[@type='140_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>artikel</xsl:text>
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>bog art</xsl:text>
                        </Index:field>
                    </xsl:when>
                    <xsl:when test="/mc:record/mc:field[@type='150_00']/mc:subfield[@type='a']">
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>artikel</xsl:text>
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>tss art</xsl:text>
                        </Index:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
                            <xsl:text>bog</xsl:text>
                        </Index:field>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
</xsl:stylesheet>