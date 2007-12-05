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
	  <xsl:template name="class">
        <Index:group Index:suggest="true" Index:navn="lcl" Index:name="lcl">
					<xsl:for-each select="dewey">
							<Index:field Index:repeat="false" Index:name="lddc" Index:navn="lddc" Index:type="keyword" Index:boostFactor="10">
									<xsl:value-of select="."/>
								</Index:field>
						</xsl:for-each>
						</Index:group>
                         <Index:group Index:name="cl" Index:navn="cl" Index:suggest="true">

                             <xsl:for-each select="dewey">
							<Index:field Index:repeat="true" Index:name="DDC_kw" Index:navn="DDC_kw" Index:type="token" Index:boostFactor="10">
										<xsl:value-of select="."/>
							</Index:field>
                             </xsl:for-each>
                             </Index:group>
    </xsl:template>
</xsl:stylesheet>

