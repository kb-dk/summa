<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0" 
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchive.org">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="publisher">

					             <xsl:for-each select="publisher">
            <Index:field Index:repeat="true" Index:name="pu" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
                                     </xsl:for-each>
        <xsl:for-each select="item/resource">
                                     <Index:field Index:repeat="true" Index:name="ip" Index:navn="ip" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
                                      <Index:field Index:repeat="true" Index:name="lip" Index:navn="lip" Index:type="keyword">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
	</xsl:template>
</xsl:stylesheet>

