<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="notes">
	<xsl:if test="substring(description,0)!=''">
		  <Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token">
						<xsl:value-of select="description"/>
				    </Index:field>
</xsl:if>
    </xsl:template>

</xsl:stylesheet>