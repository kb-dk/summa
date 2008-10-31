<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="id">
        <Index:group Index:name="numbers" Index:navn="nr">
                     <Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
                             <xsl:value-of select="id"/>
                     </Index:field>
                             <Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
                                  <xsl:text>https://sedna.statsbiblioteket.dk:8280/urn/</xsl:text><xsl:value-of select="id" />
                     </Index:field>
                </Index:group>

    </xsl:template>

</xsl:stylesheet>