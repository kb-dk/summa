<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0">

        <xsl:template name="lma">
           <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
               <xsl:for-each select=".">
                <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>Horizon</xsl:text>
                      </Index:field>
               </xsl:for-each>
                        <xsl:for-each select="mc:datafield[@tag='009']/mc:subfield[@code='a' or @code='b']">
							<xsl:if test="contains(.,'a')">
								<xsl:choose>
									<xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'m') or contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'s')">
										<xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
											<xsl:choose>
												<xsl:when test="contains(.,'xx')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>bog</xsl:text>
													</Index:field>
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>trykt_bog</xsl:text>
													</Index:field>
												</xsl:when>
												<xsl:when test="contains(.,'xe')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>bog</xsl:text>
													</Index:field>
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>e_bog</xsl:text>
													</Index:field>
												</xsl:when>
												<xsl:when test="contains(.,'xa') or contains(.,'ia') or contains(.,'ic') or contains(.,'if') or contains(.,'ih') or contains(.,'ik') or contains(.,'ip') or contains (.,'is') or contains(.,'it')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>bog</xsl:text>
													</Index:field>
                                                    <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>bog_mikroform</xsl:text>
													</Index:field>
                                                </xsl:when>
											</xsl:choose>
										</xsl:for-each>
									</xsl:when>

									<xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'a')">

										<xsl:choose>
											<xsl:when test="/mc:record/mc:datafield[@tag='557']">
												<xsl:choose>
													<xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'ap')">
														<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
															<xsl:text>artikel</xsl:text>
														</Index:field>
														<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
															<xsl:text>tss_art</xsl:text>
														</Index:field>
													</xsl:when>
													<xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='r'],'an')">
														<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
															<xsl:text>artikel</xsl:text>
														</Index:field>
														<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
															<xsl:text>avis_art</xsl:text>
														</Index:field>
													</xsl:when>
													<xsl:otherwise>
														<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
															<xsl:text>artikel</xsl:text>
														</Index:field>
														<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
															<xsl:text>tss_art</xsl:text>
														</Index:field>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:when>
											<xsl:when test="/mc:record/mc:datafield[@tag='558']">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>artikel</xsl:text>
												</Index:field>
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>bog_art</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:when test="contains(/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h'],'xe')">
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>artikel</xsl:text>
												</Index:field>
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>e_art</xsl:text>
												</Index:field>
											</xsl:when>
											<xsl:otherwise>
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>bog</xsl:text>
												</Index:field>
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>tss_specif</xsl:text>
												</Index:field>
												<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
													<xsl:text>trykt_bog</xsl:text>
												</Index:field>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'p')">
                                        <xsl:choose>
                                            <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='v'],'g')">
                                                 <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                                                           <xsl:text>radio/tv</xsl:text>
                                                </Index:field>
                                                                                       </xsl:when>
                                             <xsl:when test="contains(/mc:record/mc:datafield[@tag='005']/mc:subfield[@code='v'],'v')">
                                                 <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                                                                                           <xsl:text>radio/tv</xsl:text>
                                                </Index:field>
                                                                                       </xsl:when>
                                            <xsl:otherwise>
                                        <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
											<xsl:text>peri</xsl:text>
										</Index:field>
										<xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
											<xsl:choose>
												<xsl:when test="contains(.,'xx')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>trykt_peri</xsl:text>
													</Index:field>
												</xsl:when>
												<xsl:when test="contains(.,'xe')">

													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>e_peri</xsl:text>
													</Index:field>
												</xsl:when>
											</xsl:choose>
										</xsl:for-each>
									    </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
								</xsl:choose>
							</xsl:if>
							<xsl:if test="contains(.,'c') or contains(.,'d')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>node</xsl:text>
								</Index:field>

												<xsl:choose>
									<xsl:when test="contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'m') or contains(/mc:record/mc:datafield[@tag='008']/mc:subfield[@code='t'],'s')">
										<xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">
											<xsl:choose>
												<xsl:when test="contains(.,'xe')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>e_node</xsl:text>
													</Index:field>
												</xsl:when>
											</xsl:choose>
										</xsl:for-each>
									</xsl:when>
								</xsl:choose>
							</xsl:if>


							<xsl:if test="contains(.,'e') or contains(.,'f')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>kort</xsl:text>
								</Index:field>
							</xsl:if>
							<xsl:if test="contains(.,'g')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>billede</xsl:text>
								</Index:field>
							</xsl:if>
							<xsl:if test="contains(.,'u')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>genstand</xsl:text>
								</Index:field>
							</xsl:if>
							<xsl:if test="contains(.,'v')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>sammensat</xsl:text>
								</Index:field>
							</xsl:if>
								<xsl:if test="contains(.,'p')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>punktskrift</xsl:text>
								</Index:field>
							</xsl:if>
							<xsl:if test="contains(.,'m')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>film</xsl:text>
								</Index:field>
								<xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">

									<xsl:choose>
										<xsl:when test="contains(.,'th')">

											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>dvd_film</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'xd')">

											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>dvd_film</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'np')">

											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>dvd_film</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'nh')">

											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>vhs_film</xsl:text>
											</Index:field>
										</xsl:when>

									</xsl:choose>
								</xsl:for-each>
							</xsl:if>
							<xsl:if test="contains(.,'r') or contains(.,'s')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>lyd_musik</xsl:text>
								</Index:field>
								<xsl:for-each select="/mc:record/mc:datafield[@tag='009']/mc:subfield[@code='g' or @code='h']">

									<xsl:choose>
										<xsl:when test="contains(.,'xc')">
											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>cd_lyd</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'xd') or contains(.,'th')">
											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>dvd_lyd</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'nh')">
											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>vhs_lyd</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'xh')">
											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>kas_lyd</xsl:text>
											</Index:field>
										</xsl:when>
										<xsl:when test="contains(.,'xk')">
											<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
												<xsl:text>gram_lyd</xsl:text>
											</Index:field>
											<xsl:choose>
												<xsl:when test="contains(/mc:record/mc:datafield[@tag='998']/mc:subfield[@code='a'],'lak')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>lak_lyd</xsl:text>
													</Index:field>
												</xsl:when>
												<xsl:when test="contains(/mc:record/mc:datafield[@tag='300']/mc:subfield[@code='b'],'78')">
													<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
														<xsl:text>lak_lyd</xsl:text>
													</Index:field>
												</xsl:when>
											</xsl:choose>
										</xsl:when>

									</xsl:choose>
								</xsl:for-each>
							</xsl:if>
							<xsl:if test="contains(.,'t')">
								<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
									<xsl:text>elektr</xsl:text>
								</Index:field>
							</xsl:if>
						</xsl:for-each>

					</Index:group>
        </xsl:template>
</xsl:stylesheet>