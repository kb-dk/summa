<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dym="http://statsbiblioteket.dk/summa/2009/DidYouMeanResponse">
    <xsl:output method="html" encoding="UTF-8"/>

    <xsl:template match="dym:DidYouMeanResponse">
        <xsl:choose>
            <xsl:when test="count(.//dym:didyoumean) &gt; 0">
                Did You Mean: 
                <xsl:apply-templates />
            </xsl:when>
            <xsl:otherwise>
                No did you mean suggestions
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dym:didyoumean">
        <xsl:value-of select="." />(<xsl:value-of select="@score" />)        
    </xsl:template>

</xsl:stylesheet>
