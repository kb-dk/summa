<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="title">

        <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
            <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                <xsl:value-of select="titel"/>
            </Index:field>
            <Index:field Index:repeat="true" Index:name="multi_title" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                <xsl:value-of select="titel"/>
            </Index:field>
        </Index:group>
        <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword">
            <xsl:value-of select="titel" />
        </Index:field>
        <Index:field Index:name="sort_verify" Index:navn="sort_verify" Index:type="keyword">
            <xsl:value-of select="sort_verify" />
        </Index:field>

        <Index:group Index:name="foo" Index:navn="ti" Index:suggest="true">
            <Index:field Index:repeat="true" Index:name="stilling" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                <xsl:value-of select="stilling"/>
            </Index:field>
        </Index:group>

	</xsl:template>
</xsl:stylesheet>

