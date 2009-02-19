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
	<xsl:template name="date">
        <xsl:param name="string"/>


                                <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
										<xsl:text>0</xsl:text>
								</Index:field>
								<Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
																	<xsl:text>9999</xsl:text>

								</Index:field>
	</xsl:template>



</xsl:stylesheet>

