<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">


    <xsl:output method="html" encoding="UTF-8"/>

    <!-- ID -->
    <xsl:template match="/" name="do-inputformat_oai">
        <div class="recordMain" id="record">
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="*">
        <em>
            <xsl:value-of select="name()"/>
            <xsl:if test="@*">
                <xsl:text>&#32;(</xsl:text>
                <xsl:value-of select="@*"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
            <xsl:text>:&#32;</xsl:text>
        </em>
        <xsl:value-of select="text()"/>

        <br/>
        <xsl:apply-templates select="*"/>
    </xsl:template>

</xsl:stylesheet>

