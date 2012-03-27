<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="rt">
                  <xsl:for-each select="mc:datafield[@tag='860' or @tag='I60']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Fortsættelse af: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='1'">
									<xsl:text>Delvis fortsættelse af: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='3'">
									<xsl:text>Udskilt fra: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='4'">
									<xsl:text>Sammenlagt af: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='6'">
									<xsl:text>Heri indgået: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='7'">
									<xsl:text>Heri delvis indgået: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Fortsættelse af: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='i'">
										<xsl:value-of select="."/>
									</xsl:when>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='861' or @tag='I61']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Fortsættes som: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='1'">
									<xsl:text>Fortsættes delvis som: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='2'">
									<xsl:text>Fortsættes under tidligere titel: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='3'">
									<xsl:text>Herfra udskilt: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:choose>
													<xsl:when test="position()='1'">
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:text>, og: </xsl:text>
														<xsl:value-of select="."/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='4'">
									<xsl:text>Sammenlagt med: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:choose>
													<xsl:when test="position()='1'">
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:text>, til: </xsl:text>
														<xsl:value-of select="."/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='5'">
									<xsl:text>Opdelt i: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:choose>
													<xsl:when test="position()='1'">
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:text>, og: </xsl:text>
														<xsl:value-of select="."/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='6'">
									<xsl:text>Indgået i: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:when test="@ind2='7'">
									<xsl:text>Delvis indgået i: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Fortsættes som: </xsl:text>
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='i'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='t'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:text> : </xsl:text>
												<xsl:value-of select="."/>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:otherwise>
							</xsl:choose>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='863' or @tag='I63']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Udgivet sammen med: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='1'">
									<xsl:text>Hermed udgivet: </xsl:text>
								</xsl:when>
								<xsl:when test="@ind2='2'">
									<xsl:text>Udgivet som del af: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Udgivet sammen med: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='865' or @tag='I65']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Udgave i andet medium: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Udgave i andet medium: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='866' or @tag='I66']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Oversættelse af: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Oversættelse af: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='867' or @tag='I67']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Anden udgave af: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Anden udgave af: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='868' or @tag='I68']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Også i anden udgave: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Også i anden udgave: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='870' or @tag='I70']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Supplement til: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Supplement til: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='871' or @tag='I71']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Har supplement: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Har supplement: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='873' or @tag='I73']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Er underserie af: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Er underserie af: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='874' or @tag='I74']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Har underserie: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Har underserie: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='879' or @tag='I79']">
						<Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token" Index:boostFactor="2">
							<xsl:choose>
								<xsl:when test="@ind2='0'">
									<xsl:text>Er knyttet til: </xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:text>Er knyttet til: </xsl:text>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="@code='t'">
										<xsl:choose>
											<xsl:when test="position()='1'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:text>, og: </xsl:text>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="@code='c'">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
    </xsl:template>


    </xsl:stylesheet>
