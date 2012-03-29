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
	<xsl:template name="notes">
									
							<xsl:for-each select="ab">
								<Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token"  Index:boostFactor="2">
									<xsl:value-of select="."/>
								</Index:field>
							</xsl:for-each>
							
												<xsl:for-each select="nt">
								<Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token"  Index:boostFactor="2">
									<xsl:value-of select="."/>
								</Index:field>
							</xsl:for-each>
							
						<xsl:if test="@type='Journal Article (aja)' or @type='Dissertation (dis)' or @type='Book (bka)'or @type='Book Review (brv)' or @type='Book Chapter (bca)' or @type='Conferece Paper (acp)'"> 
												<xsl:for-each select="so">
									<Index:field Index:repeat="true" Index:name="so" Index:navn="vp" Index:type="token"  Index:boostFactor="2">
										<xsl:value-of select="."/>
																			
									</Index:field>
								</xsl:for-each>
								</xsl:if>
								<xsl:if test="jn">
								<xsl:for-each select="jn">
									<Index:field Index:repeat="false" Index:name="lso" Index:navn="lvp" Index:type="keyword">
										<xsl:value-of select="."/>
									</Index:field>
								</xsl:for-each>
								</xsl:if>
								
								
	</xsl:template>
</xsl:stylesheet>

