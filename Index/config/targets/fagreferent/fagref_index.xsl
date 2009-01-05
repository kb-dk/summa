<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0">

    <xsl:include href="fagref_short_format.xsl" />
    <xsl:include href="fagref_author.xsl" />
    <xsl:include href="fagref_title.xsl" />
    <xsl:include href="fagref_subject.xsl" />
    <xsl:include href="fagref_notes.xsl" />


    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
		<Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="fagref">
			<xsl:attribute name="Index:id">
				<xsl:value-of select="fagref/email" />
			</xsl:attribute>

            <xsl:for-each select="fagref">
				<Index:fields>
                    <xsl:call-template name="shortformat" />
                    <xsl:call-template name="author" />
                    <xsl:call-template name="title" />
                    <xsl:call-template name="subject" />
                    <xsl:call-template name="notes" />

                <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">person</Index:field>
								    <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">fagspecialist</Index:field>

                <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
                    <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>fagspecialist</xsl:text>
                      </Index:field>
                   <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">fagspecialist</Index:field>
                </Index:group>
                   </Index:fields>
            </xsl:for-each>
		</Index:document>
	</xsl:template>
</xsl:stylesheet>