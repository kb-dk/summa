<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/summa/2008/Document"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                exclude-result-prefixes="java xs xalan xsl" version="1.0">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>

    <xsl:template match="/">
        <Index:SummaDocument version="1.0">
            <xsl:for-each select="fagref">
                <Index:fields>
                    <xsl:call-template name="author"/>
                </Index:fields>
            </xsl:for-each>
        </Index:SummaDocument>
    </xsl:template>

    <xsl:template name="author">
        <xsl:for-each select="author/name">
            <xsl:call-template name="author2">
                <xsl:with-param name="au">
                    <xsl:value-of select="."/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="author2">
        <xsl:param name="au"/>
        <xsl:choose>
            <xsl:when test="contains($au,',')">
                <Index:field Index:name="author">
                    <xsl:value-of select="substring-before($au,',')"/>
                </Index:field>
                <xsl:call-template name="author2">
                    <xsl:with-param name="au">
                        <xsl:value-of select="substring-after($au,',')"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
