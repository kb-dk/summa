<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:purl="http://purl.org/rss/1.0/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>



    <!-- Material type -->
    <xsl:template name="ma">
        <Index:field Index:repeat="true" Index:name="format" Index:navn="format" Index:type="token">
            <xsl:value-of select="'format??'"/> <!-- TODO check format -->
        </Index:field>
        <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:text>xe</xsl:text>
            </Index:field>
            <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                <xsl:text>tictoc</xsl:text>
            </Index:field>
        </Index:group>
        <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
            <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                <xsl:text>tictoc</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                <xsl:text>netdokument</xsl:text>
            </Index:field>
        </Index:group>
    </xsl:template>

</xsl:stylesheet>