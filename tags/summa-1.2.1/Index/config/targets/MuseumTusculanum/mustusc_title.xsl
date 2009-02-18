<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns="http://www.openarchives.org/OAI/2.0/"
        xmlns:oai="http://www.openarchives.org/OAI/2.0/"
        xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
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
	<xsl:template name="title">
			
								<Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
									<xsl:for-each select="title">
										<Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="6">
											
											<xsl:value-of select="."/>
										
										</Index:field>
									</xsl:for-each>
										
										<xsl:for-each select="subtitle">
							    <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="5"  Index:suggest="true">
											
											<xsl:value-of select="."/>
										
										</Index:field>
									</xsl:for-each>
									
									<xsl:for-each select="series_title">
                               <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="2">   
															 <xsl:value-of select="."/>
															 <xsl:if test="../year_of_publ!= '' ">
															 	 <xsl:text>&#32;(</xsl:text>
															 <xsl:value-of select="../year_of_publ"/>
															 <xsl:text>)</xsl:text>
															 </xsl:if>
															 <xsl:text>;&#32;</xsl:text>
															 <xsl:if test="../volume!= '' ">
																	 <xsl:value-of select="../volume"/>
														
															 </xsl:if>
															 
															 <xsl:if test="../issue!= '' ">
															 	 <xsl:text>&#32;(</xsl:text>
															 <xsl:value-of select="../issue"/>
															 <xsl:text>)</xsl:text>
															 </xsl:if>
															 </Index:field>
															 </xsl:for-each>
								</Index:group>
									<Index:field Index:name="sort_title" Index:sortLocale="da" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="6">
									
									<xsl:for-each select="title [position()=1]">
										<xsl:choose>
											<xsl:when test="starts-with(.,'The ') ">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'A ')">
												<xsl:value-of select="substring(.,3)"/>
											</xsl:when>
												<xsl:when test="starts-with(.,'An ') ">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'En ') ">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Et ') ">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:for-each>
									<xsl:if test="volume!= '' ">
									<xsl:text>&#32;</xsl:text>
																	 <xsl:value-of select="volume"/>
														
															 </xsl:if>
															 
															 <xsl:if test="issue!= '' ">
															 	 <xsl:text>&#32;(</xsl:text>
															 <xsl:value-of select="issue"/>
															 <xsl:text>)</xsl:text>
															 </xsl:if>
                     </Index:field>
										 		<xsl:for-each select="series_title">
							<Index:field Index:repeat="false" Index:name="lso" Index:navn="lvp" Index:type="keyword">
						
															 <xsl:value-of select="."/>
															
															 </Index:field>
														</xsl:for-each>
															
	</xsl:template>
</xsl:stylesheet>

