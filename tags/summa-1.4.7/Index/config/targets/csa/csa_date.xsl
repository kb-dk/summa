<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl oai_dc dc"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="date">
			<xsl:for-each select="py">
								<Index:field Index:repeat="true" Index:name="py" Index:navn="år"  Index:type="token" Index:boostFactor="2">
									<xsl:value-of select="."/>
								</Index:field>
                <Index:field Index:repeat="true" Index:name="year" Index:navn="year"  Index:type="number" Index:boostFactor="2">
									<xsl:value-of select="."/>
								</Index:field>

                            </xsl:for-each>
								<Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
									<xsl:for-each select="py">
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:if test="not(py)">
										<xsl:text>0</xsl:text>
									</xsl:if>
								</Index:field>
								<Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
									<xsl:for-each select="py">
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:if test="not(py)">
										<xsl:text>9999</xsl:text>
									</xsl:if>
								</Index:field>
	</xsl:template>

    
</xsl:stylesheet>

