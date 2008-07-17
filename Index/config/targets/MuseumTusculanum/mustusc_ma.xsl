<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl dc oai_dc oai"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
					xmlns:mdc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/"
				xmlns:mtp_dc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="ma">
			<xsl:for-each select="format">
								<Index:field Index:repeat="true" Index:name="format" Index:navn="format" Index:type="token">
									<xsl:value-of select="."/>
								</Index:field>
							
							</xsl:for-each>
      
                      <xsl:for-each select=".">
								
								<Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xe</xsl:text>
									</Index:field>
                                    <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>mdc</xsl:text>
									</Index:field>
                        </Index:group>            
	
									<Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
									    <Index:field Index:repeat="false" Index:name="target" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>MuseumTusculanum</xsl:text>
                      </Index:field>
											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
											<xsl:text>netdokument</xsl:text>
										</Index:field>
										
									
									</Index:group>
								</xsl:for-each>
								
	
	</xsl:template>
</xsl:stylesheet>
