<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="lmo">
        <xsl:for-each select="mc:datafield[@tag='039']">
            <Index:field Index:repeat="false" Index:name="lmo" Index:navn="lmo" Index:type="keyword">
                <xsl:for-each select="mc:subfield[@code='a']">
                    <xsl:if test="contains(.,'sam')">
                        <xsl:text>SAMLINGER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'mir')">
                        <xsl:text>MIDDELALDER/RENÆSSANCE</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'mia')">
                        <xsl:text>MIDDELALDER/RENÆSSANCE. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'avg')">
                        <xsl:text>AVANTGARDE</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'ava')">
                        <xsl:text>AVANTGARDE. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'ork')">
                        <xsl:text>ORKESTERMUSIK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'ora')">
                        <xsl:text>ORKESTERMUSIK. AMTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'kam')">
                        <xsl:text>KAMMERMUSIK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'kaa')">
                        <xsl:text>KAMMERMUSIK. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'eti')">
                        <xsl:text>ET INSTRUMENT</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'eta')">
                        <xsl:text>ET INSTRUMENT. ANTOLOGIER</xsl:text>
                    </xsl:if>

                    <xsl:if test="contains(.,'vok')">
                        <xsl:text>VOKALMUSIK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'voa')">
                        <xsl:text>VOKALMUSIK. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'opr')">
                        <xsl:text>OPERAER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'opa')">
                        <xsl:text>OPERAER. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'otm')">
                        <xsl:text>OPERETTER/MUSICALS</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'ota')">
                        <xsl:text>OPERETTER/MUSICALS. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'new')">
                        <xsl:text>NEW AGE</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'fol')">
                        <xsl:text>FOLKEMUSIK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'blu')">
                        <xsl:text>BLUES</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'bla')">
                        <xsl:text>BLUES. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'jaz')">
                        <xsl:text>JAZZ</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'jaa')">
                        <xsl:text>JAZZ. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'bef')">
                        <xsl:text>ROCK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'bea')">
                        <xsl:text>ROCK. ANTOLOGIER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'und')">
                        <xsl:text>UNDERHOLDNING</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'dau')">
                        <xsl:text>DANSK UNDERHOLDNING</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'jul')">
                        <xsl:text>JULEMUSIK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'fil')">
                        <xsl:text>FILMMUSIK</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'tes')">
                        <xsl:text>TESTPLADER</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'mmo')">
                        <xsl:text>MUSIC MINUS ONE</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'hib')">
                        <xsl:text>HISTORIER. BØRN</xsl:text>
                    </xsl:if>
                    <xsl:if test="contains(.,'mub')">
                        <xsl:text>MUSIK. BØRN</xsl:text>
                    </xsl:if>

                </xsl:for-each>
            </Index:field>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
