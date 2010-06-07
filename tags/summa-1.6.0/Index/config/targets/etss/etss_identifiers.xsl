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
	<xsl:template name="identifiers">
				<Index:group Index:name="numbers" Index:navn="nr">
									<xsl:for-each select="id">
										<Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="8">
											<xsl:value-of select="."/>
										</Index:field>
										<xsl:choose>
											<xsl:when test="contains(@is_issn,'true')">
								                    <Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>

							<Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						
                                            </xsl:when>
										</xsl:choose>

                                    </xsl:for-each>
                   
								</Index:group>
						<xsl:for-each select="item/url">
						<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
	
	</xsl:template>
</xsl:stylesheet>
