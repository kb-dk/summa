<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">

    <xsl:output method="html" encoding="UTF-8"/>

    <xsl:include href="targets/experts/experts_inputformat.xsl"/>
    <xsl:include href="targets/oai/oai_inputformat.xsl"/>


    <xsl:param name="locale"/>
    <xsl:param name="bundle_global"/>
    <xsl:param name="bundle_materials"/>
    <xsl:param name="bundle_description"/>
    <xsl:param name="bundle_labels"/>
    <xsl:param name="record_id"/>
    <xsl:param name="mattype"/>
    <xsl:param name="titel_kort"/>
    <xsl:param name="extra"/>


    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="substring($record_id, 1, 3) = 'oai'">
                <xsl:call-template name="do-inputformat_oai"/>
            </xsl:when>
            <xsl:when test="substring-after($record_id, '@') = 'statsbiblioteket.dk'">
                <xsl:call-template name="do-inputformat_experts"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="do-inputformat_oai"/>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
</xsl:stylesheet>