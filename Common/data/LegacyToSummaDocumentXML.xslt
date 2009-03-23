<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:sd="http://statsbiblioteket.dk/summa/2008/Document"
                exclude-result-prefixes="xsl xs Index"
                version="1.0">
    <!-- Converts XML in the old style Index format to SummaDocumentXML.
         This is a fairly simple conversion, as the primary task is to
         remove information.
         Users of this transformer should create an IndexDescriptor with
         information on groups and similar.
          -->
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="Index:document">
        <sd:SummaDocument version="1.0" xmlns:sd="http://statsbiblioteket.dk/summa/2008/Document">
            <xsl:attribute name="id">
                <xsl:value-of select="@Index:id" />
            </xsl:attribute>
            <xsl:for-each select="@Index:defaultBoost">
                <xsl:attribute name="boost">
                    <xsl:value-of select="." />
                </xsl:attribute>
            </xsl:for-each>
            <sd:fields>
                <xsl:for-each select="Index:fields">
                    <xsl:call-template name="fields" />
                </xsl:for-each>
                <xsl:for-each select="Index:fields/Index:group">
                    <xsl:call-template name="fields" />
                </xsl:for-each>
                <xsl:for-each select="Index:groups/Index:group">
                    <xsl:call-template name="fields" />
                </xsl:for-each>
            </sd:fields>
        </sd:SummaDocument>
    </xsl:template>
    

    <xsl:template name="fields">
        <xsl:for-each select="Index:field">
            <sd:field>
                <xsl:attribute name="name">
                    <xsl:value-of select="@Index:name" />
                </xsl:attribute>
                <xsl:for-each select="@Index:boostFactor">
                    <xsl:attribute name="boost">
                        <xsl:value-of select="." />
                    </xsl:attribute>
                </xsl:for-each>
                <xsl:choose>
                    <xsl:when test="@Index:name='shortformat'">
                        <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                        <xsl:value-of disable-output-escaping="yes" select="node()" />
                        <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="node()" />
                    </xsl:otherwise>
                </xsl:choose>
            </sd:field>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>