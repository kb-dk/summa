<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:oai="http://www.openarchives.org/OAI/2.0/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl dc oai_dc oai"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">


    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="shortformat">
		
					<Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
						<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
						<shortrecord>							
							<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
								<rdf:Description>
									
									<xsl:for-each select="oai:metadata">
										<xsl:for-each select="oai_dc:dc">
											<xsl:for-each select="dc:title">
												<dc:title>
													<xsl:value-of select="."/>
												</dc:title>
											</xsl:for-each>
                                            <xsl:for-each select="oai_dc:title">
                                                <dc:title>
                                                    <xsl:value-of select="."/>
                                                </dc:title>
                                            </xsl:for-each>

                                            <xsl:for-each select="dc:creator">
												<dc:creator>
													<xsl:value-of select="."/>
												</dc:creator>
											</xsl:for-each>
                                            <xsl:for-each select="oai_dc:creator">
												<dc:creator>
													<xsl:value-of select="."/>
												</dc:creator>
											</xsl:for-each>
											<xsl:for-each select="dc:date">
												<dc:date>
													<xsl:value-of select="."/>
												</dc:date>
											</xsl:for-each>
                                            <xsl:for-each select="oai_dc:date">
                                                <dc:date>
                                                    <xsl:value-of select="."/>
                                                </dc:date>
                                            </xsl:for-each>

												<xsl:for-each select=".">
												<dc:type xml:lang="da">
													
													<xsl:choose>
														<xsl:when test="contains(dc:type,'collection') or contains(oai_dc:type,'collection')">netdokument (samling)</xsl:when>
														<xsl:when test="contains(dc:type,'Collection') or contains(oai_dc:type,'Collection')">netdokument (samling)</xsl:when>
														<xsl:when test="contains(dc:type,'dataset') or contains(oai_dc:type,'dataset')">netdokument (datasæt)</xsl:when>
														<xsl:when test="contains(dc:type,'Dataset') or contains(dc:type,'Dataset')">netdokument (datasæt)</xsl:when>
														<xsl:when test="contains(dc:type,'event') or contains(oai_dc:type,'event')">netdokument (begivenhed)</xsl:when>
														<xsl:when test="contains(dc:type,'Event') or contains(oai_dc:type,'Event')">netdokument (begivenhed)</xsl:when>
														<xsl:when test="contains(dc:type,'image') or contains(oai_dc:type,'image')">netdokument (billede(r))</xsl:when>
														<xsl:when test="contains(dc:type,'Image' or contains(oai_dc:type,'Image'))">netdokument (billede(r))</xsl:when>
														<xsl:when test="contains(dc:type,'interactiveresource') or contains(oai_dc:type,'interactiveresource')">netdokument (interaktiv ressource)</xsl:when>
														<xsl:when test="contains(dc:type,'InteractiveResource') or contains(oai_dc:type,'InteractiveResource')">netdokument (interaktiv ressource)</xsl:when>
														<xsl:when test="contains(dc:type,'service') or contains(oai_dc:type,'service')">netdokument (tjeneste)</xsl:when>
														<xsl:when test="contains(dc:type,'Service') or contains(oai_dc:type,'Service')">netdokument (tjeneste)</xsl:when>
														<xsl:when test="contains(dc:type,'software') or contains(oai_dc:type,'software')">netdokument (software)</xsl:when>
														<xsl:when test="contains(dc:type,'Software') or contains(oai_dc:type,'Software')">netdokument (software)</xsl:when>
														<xsl:when test="contains(dc:type,'sound') or contains(oai_dc:type,'sound')">netdokument (lyd)</xsl:when>
														<xsl:when test="contains(dc:type,'Sound') or contains(oai_dc:type,'Sound')">netdokument (lyd)</xsl:when>
														<xsl:when test="contains(dc:type,'text') or contains(oai_dc:type,'text')">netdokument (tekst)</xsl:when>
														<xsl:when test="contains(dc:type,'Text') or contains(oai_dc:type,'Text')">netdokument (tekst)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'TEXT') or contains(oai_dc:type,'TEXT')">netdokument (tekst)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'stillimage') or contains(oai_dc:type,'stillimage')">netdokument (billede)</xsl:when>
														<xsl:when test="contains(dc:type,'StillImage') or contains(oai_dc:type,'StillImage')">netdokument (billede)</xsl:when>
														<xsl:when test="contains(dc:type,'movingimage') or contains(oai_dc:type,'movingimage')">netdokument (film)</xsl:when>
														<xsl:when test="contains(dc:type,'MovingImage') or contains(oai_dc:type,'MovingImage')">netdokument (film)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'peer-reviewed article') or contains(oai_dc:type,'peer-reviewed article')">netdokument (artikel)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'Peer-Reviewed Article') or contains(oai_dc:type,'Peer-Reviewed Article')">netdokument (artikel)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'article') or contains(oai_dc:type,'article')">netdokument (artikel)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'Article') or contains(oai_dc:type,'Article')">netdokument (artikel)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'journal') or contains(oai_dc:type,'journal')">netdokument (artikel)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'Journal') or contains(oai_dc:type,'Journal')">netdokument (artikel)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'Thesis') or contains(oai_dc:type,'Thesis')">netdokument (disputats)</xsl:when>
                                                        <xsl:when test="contains(dc:type,'thesis') or contains(oai_dc:type,'thesis')">netdokument (disputats)</xsl:when>

                                                        <xsl:otherwise>netdokument<xsl:if test="dc:type or oai_dc:type"><xsl:text> (</xsl:text><xsl:value-of select="dc:type or oai_dc:type"/><xsl:text>)</xsl:text></xsl:if>
														</xsl:otherwise>
													
													
													</xsl:choose>
												</dc:type>
											</xsl:for-each>
											<xsl:for-each select=".">
												<dc:type xml:lang="en">net document<xsl:if test="dc:type"><xsl:text>&#32;(</xsl:text><xsl:value-of select="dc:type"/><xsl:text>)</xsl:text></xsl:if>
												</dc:type>
											</xsl:for-each>
											<xsl:for-each select="dc:identifier">
												<xsl:choose>
													<xsl:when test="starts-with(.,'http://')">
														<dc:identifier>
															<xsl:value-of select="."/>
														</dc:identifier>
													</xsl:when>
												</xsl:choose>
											</xsl:for-each>
                                            <xsl:for-each select="oai_dc:identifier">
                                                <xsl:choose>
                                                    <xsl:when test="starts-with(.,'http://')">
                                                        <dc:identifier>
                                                            <xsl:value-of select="."/>
                                                        </dc:identifier>
                                                    </xsl:when>
                                                </xsl:choose>
                                            </xsl:for-each>
                                           <xsl:for-each select=".">
												<dc:format>todo</dc:format>
											</xsl:for-each>
										</xsl:for-each>
									</xsl:for-each>
								
								
								</rdf:Description>
							</rdf:RDF>
						
						</shortrecord>
						<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
					</Index:field>
				
	</xsl:template>
</xsl:stylesheet>
