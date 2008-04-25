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
	<xsl:template name="su">
			<Index:group Index:name="su" Index:navn="em">
									<xsl:for-each select="mdc:subject">
										<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="2">
											<xsl:value-of select="."/>
										</Index:field>
											</xsl:for-each>
    									</Index:group>
								        <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
														<xsl:for-each select="mdc:subject">
			<Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">

															<xsl:value-of select="."/>
															</Index:field>
									
							</xsl:for-each>
                                            
												</Index:group>

	</xsl:template>

</xsl:stylesheet>

