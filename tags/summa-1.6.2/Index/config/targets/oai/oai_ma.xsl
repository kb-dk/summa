<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="ma">
			<xsl:for-each select="dc:format">
								<Index:field Index:repeat="true" Index:name="format" Index:navn="format" Index:type="token">
									<xsl:value-of select="."/>
								</Index:field>
							
							</xsl:for-each>
        <xsl:for-each select="oai_dc:format">
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
										<xsl:text>oai</xsl:text>
									</Index:field>
                                    <xsl:if test="contains(dc:type,'collection') or contains(dc:type,'Collection') or contains(oai_dc:type,'collection') or contains(oai_dc:type,'Collection')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>sæ</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>sm</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'dataset') or contains(dc:type,'Dataset') or contains(oai_dc:type,'dataset') or contains(oai_dc:type,'Dataset')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>aa</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>el</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'image') or contains(dc:type,'Image') or contains(dc:type,'stillimage') or contains(dc:type,'StillImage')
									or contains(oai_dc:type,'image') or contains(oai_dc:type,'Image') or contains(oai_dc:type,'stillimage') or contains(oai_dc:type,'StillImage')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>ab</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>bi</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'interactiveresource') or contains(dc:type,'InteractiveResource')
									or contains(oai_dc:type,'interactiveresource') or contains(oai_dc:type,'InteractiveResource')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>cb</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>el</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'service') or contains(dc:type,'Service') or contains(oai_dc:type,'service') or contains(oai_dc:type,'Service')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>cc</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>el</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'service') or contains(dc:type,'Service') or contains(oai_dc:type,'service') or contains(oai_dc:type,'Service')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>cc</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>el</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'software') or contains(dc:type,'Software') or contains(oai_dc:type,'software') or contains(oai_dc:type,'Software')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>ba</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>el</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'sound') or contains(dc:type,'Sound') or contains(oai_dc:type,'sound') or contains(oai_dc:type,'Sound')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>ad</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>ly</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'text') or contains(dc:type,'Text') or contains(dc:type,'TEXT')
									 or contains(oai_dc:type,'text') or contains(oai_dc:type,'Text') or contains(oai_dc:type,'TEXT')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>af</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>te</xsl:text>
										</Index:field>
									</xsl:if>
									<xsl:if test="contains(dc:type,'movingimage') or contains(dc:type,'MovingImage') or contains(oai_dc:type,'movingimage') or contains(oai_dc:type,'MovingImage')">
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>mo</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>ab</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>fi</xsl:text>
										</Index:field>
									</xsl:if>
                                    <xsl:if test="contains(dc:type,'peer-reviewed article') or contains(dc:type,'Peer-Revied Article') or contains(dc:type,'article') or contains(dc:type,'Article')
                                    or contains(oai_dc:type,'peer-reviewed article') or contains(oai_dc:type,'Peer-Revied Article') or contains(oai_dc:type,'article') or contains(oai_dc:type,'Article')
                                    or contains(dc:type,'journal') or contains(dc:type,'Journal') or contains(oai_dc:type,'journal') or contains(oai_dc:type,'Journal')">
											<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>an</xsl:text>
									</Index:field>

									   <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>te</xsl:text>
										</Index:field>
									</xsl:if>

                                      <xsl:if test="contains(dc:type,'thesis') or contains(dc:type,'Thesis') or contains(oai_dc:type,'thesis') or contains(oai_dc:type,'Thesis')">
											<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>mo</xsl:text>
									</Index:field>

									   <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>te</xsl:text>
										</Index:field>
                                          <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>dp</xsl:text>
										</Index:field>
                                    </xsl:if>
                                </Index:group>
								</xsl:for-each>
					
		<xsl:for-each select=".">
									<Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
                                       <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>oai</xsl:text>
                      </Index:field>
                                        <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
											<xsl:text>netdokument</xsl:text>
										</Index:field>
										
										<xsl:choose>
											<xsl:when test="contains(dc:type,'collection') or contains(oai_dc:type,'collection')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>samling</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Collection') or contains(oai_dc:type,'Collection')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>samling</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'dataset') or contains(oai_dc:type,'dataset')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>datasæt</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Dataset') or contains(oai_dc:type,'Dataset')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>datasæt</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'event') or contains(oai_dc:type,'event')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>begivenhed</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Event') or contains(oai_dc:type,'Event')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>begivenhed</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'image') or contains(oai_dc:type,'image')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>billede</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Image') or contains(oai_dc:type,'Image')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>billede</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'interactiveresource') or contains(oai_dc:type,'interactiveresource')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>interaktiv_ressource</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'InteractiveResource') or contains(oai_dc:type,'InteractiveResource')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>interaktiv_ressource</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'service') or contains(oai_dc:type,'service')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>service</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Service') or contains(oai_dc:type,'Service')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>service</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'software') or contains(oai_dc:type,'software')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>software</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Software') or contains(oai_dc:type,'Software')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>software</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'sound') or contains(oai_dc:type,'sound')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>lyd_musik</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Sound') or contains(oai_dc:type,'Sound')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>lyd_musik</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'text') or contains(oai_dc:type,'text')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>tekst</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'Text') or contains(oai_dc:type,'Text')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>tekst</xsl:text>
												</Index:field>
											</xsl:when>
                                            <xsl:when test="contains(dc:type,'TEXT') or contains(oai_dc:type,'TEXT')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>tekst</xsl:text>
												</Index:field>
											</xsl:when>
                                            <xsl:when test="contains(dc:type,'stillimage') or contains(oai_dc:type,'stillimage')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>billede</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'StillImage') or contains(oai_dc:type,'StillImage')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>billede</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'movingimage') or contains(oai_dc:type,'movingimage')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>film</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(dc:type,'MovingImage') or contains(oai_dc:type,'MovingImage')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>film</xsl:text>
												</Index:field>
											</xsl:when>
                                            <xsl:when test="contains(dc:type,'article') or contains(dc:type,'Article') or contains(oai_dc:type,'article') or contains(oai_dc:type,'Article')
                                            or contains(dc:type,'journal') or contains(dc:type,'Journal') or contains(oai_dc:type,'journal') or contains(oai_dc:type,'Journal')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>artikel</xsl:text>
												</Index:field>
											</xsl:when>

                                            <xsl:otherwise>
												<xsl:if test="dc:type or oai_dc:type">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:value-of select="dc:type"/>
													</Index:field>
												</xsl:if>
											</xsl:otherwise>
										</xsl:choose>
									</Index:group>
								</xsl:for-each>
								
	
	</xsl:template>
</xsl:stylesheet>
