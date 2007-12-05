<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0">

        <xsl:template name="author">
             <Index:group Index:name="au" Index:navn="fo" >
						<xsl:choose>
							<xsl:when test="mc:datafield[@tag='100']">
								<xsl:for-each select="mc:datafield[@tag='100']">
									<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
										<xsl:for-each select="mc:subfield[@code='h']">
											<xsl:value-of select="."/>
											<xsl:text> </xsl:text>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='a']">
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='e']">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='f']">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:for-each>
										<xsl:if test="mc:subfield[@code='c']">
											<xsl:choose>
												<xsl:when test="contains (.,'f. ')">
													<xsl:text> </xsl:text>
													<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
													<xsl:text>-</xsl:text>
												</xsl:when>
												<xsl:when test="contains (.,'f.')">
													<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
													<xsl:text>-</xsl:text>
												</xsl:when>
												<xsl:otherwise>
													<xsl:text> </xsl:text>
													<xsl:value-of select="mc:subfield[@code='c']" />
												</xsl:otherwise>
											</xsl:choose>
										</xsl:if>
									</Index:field>

                                    <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
										<xsl:for-each select="mc:subfield[@code='a']">
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='h']">
											<xsl:text>, </xsl:text>
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='e']">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='f']">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:for-each>
										<xsl:if test="mc:subfield[@code='c']">
											<xsl:choose>
												<xsl:when test="contains (.,'f. ')">
													<xsl:text> </xsl:text>
													<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
													<xsl:text>-</xsl:text>
												</xsl:when>
												<xsl:when test="contains (.,'f.')">
													<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
													<xsl:text>-</xsl:text>
												</xsl:when>
												<xsl:otherwise>
													<xsl:text> </xsl:text>
													<xsl:value-of select="mc:subfield[@code='c']" />
												</xsl:otherwise>
											</xsl:choose>
										</xsl:if>
									</Index:field>
								</xsl:for-each>
								<xsl:for-each select="mc:datafield[@tag='700']">
									<xsl:choose>
										<xsl:when test="position ()&lt;3">
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:value-of select="."/>
													<xsl:text> </xsl:text>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:text>, </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:value-of select="."/>
													<xsl:text> </xsl:text>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:text>, </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:for-each>
							</xsl:when>

							<xsl:when test="mc:datafield[@tag='110']">
								<xsl:for-each select="mc:datafield[@tag='110']">
									<Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
										<xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
											<xsl:for-each select="mc:subfield">
												<xsl:choose>
													<xsl:when test="@code='a'">
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:when test="@code='s'">
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='e'">
														<xsl:text> (</xsl:text>
														<xsl:value-of select="." />
														<xsl:text>) </xsl:text>
													</xsl:when>

													<xsl:when test="@code='c'">
														<xsl:if test="position&gt;1">
															<xsl:text>. </xsl:text>
														</xsl:if>
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:when test="@code='i'">
														<xsl:text> ; </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='k'">
														<xsl:text>, </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='j'">
														<xsl:text>, </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
												</xsl:choose>
											</xsl:for-each>
										</xsl:if>
									</Index:field>
								</xsl:for-each>
								<xsl:for-each select="mc:datafield[@tag='700']">
									<xsl:choose>
										<xsl:when test="position ()&lt;3">
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:value-of select="."/>
													<xsl:text> </xsl:text>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>) </xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:text>, </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>) </xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:value-of select="."/>
													<xsl:text> </xsl:text>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:text>, </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<xsl:for-each select="mc:datafield[@tag='700']">
									<xsl:choose>
										<xsl:when test="position ()&lt;4">
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:value-of select="."/>
													<xsl:text> </xsl:text>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:text>, </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:value-of select="."/>
													<xsl:text> </xsl:text>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
											<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="8">
												<xsl:for-each select="mc:subfield[@code='a']">
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='h']">
													<xsl:text>, </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='e']">
													<xsl:text> </xsl:text>
													<xsl:value-of select="."/>
												</xsl:for-each>
												<xsl:for-each select="mc:subfield[@code='f']">
													<xsl:text> (</xsl:text>
													<xsl:value-of select="."/>
													<xsl:text>)</xsl:text>
												</xsl:for-each>
												<xsl:if test="mc:subfield[@code='c']">
													<xsl:choose>
														<xsl:when test="contains (.,'f. ')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:when test="contains (.,'f.')">
															<xsl:text> </xsl:text>
															<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
															<xsl:text>-</xsl:text>
														</xsl:when>
														<xsl:otherwise>
															<xsl:text> </xsl:text>
															<xsl:value-of select="mc:subfield[@code='c']" />
														</xsl:otherwise>
													</xsl:choose>
												</xsl:if>
											</Index:field>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:for-each>
							</xsl:otherwise>
						</xsl:choose>
						<xsl:for-each select="mc:datafield[@tag='710']">
							<xsl:choose>
								<xsl:when test="position ()=1">
									<Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
										<xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
											<xsl:for-each select="mc:subfield">
												<xsl:choose>
													<xsl:when test="@code='a'">
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:when test="@code='s'">
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='e'">
														<xsl:text> (</xsl:text>
														<xsl:value-of select="." />
														<xsl:text>)</xsl:text>
													</xsl:when>
													<xsl:when test="@code='c'">
														<xsl:if test="position()&gt;1">
															<xsl:text>. </xsl:text>
														</xsl:if>
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:when test="@code='i'">
														<xsl:text> ; </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='k'">
														<xsl:text>, </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='j'">
														<xsl:text>, </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
												</xsl:choose>
											</xsl:for-each>
										</xsl:if>
									</Index:field>
								</xsl:when>
								<xsl:otherwise>
									<Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="8">
										<xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
											<xsl:for-each select="mc:subfield">
												<xsl:choose>
													<xsl:when test="@code='a'">
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:when test="@code='s'">
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='e'">
														<xsl:text> (</xsl:text>
														<xsl:value-of select="." />
														<xsl:text>)</xsl:text>
													</xsl:when>
													<xsl:when test="@code='c'">
														<xsl:if test="position()&gt;1">
															<xsl:text>. </xsl:text>
														</xsl:if>
														<xsl:value-of select="."/>
													</xsl:when>
													<xsl:when test="@code='i'">
														<xsl:text> ; </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='k'">
														<xsl:text>, </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
													<xsl:when test="@code='j'">
														<xsl:text>, </xsl:text>
														<xsl:value-of select="." />
													</xsl:when>
												</xsl:choose>
											</xsl:for-each>
										</xsl:if>
									</Index:field>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='239']">
							<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
							<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:text>, </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='770']">
							<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
							<Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:text>, </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='780']">
							<Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="6">
								<xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='a'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='s'">
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='e'">
												<xsl:text> (</xsl:text>
												<xsl:value-of select="." />
												<xsl:text>) </xsl:text>
											</xsl:when>
											<xsl:when test="@code='c'">
												<xsl:if test="position&gt;1">
													<xsl:text>. </xsl:text>
												</xsl:if>
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='i'">
												<xsl:text> ; </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='k'">
												<xsl:text>, </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='j'">
												<xsl:text>, </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='100']">
							<Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
							<Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:text>, </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='110']">
							<Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
								<xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='a'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='s'">
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='e'">
												<xsl:text> (</xsl:text>
												<xsl:value-of select="." />
												<xsl:text>) </xsl:text>
											</xsl:when>

											<xsl:when test="@code='c'">
												<xsl:if test="position&gt;1">
													<xsl:text>. </xsl:text>
												</xsl:if>
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='i'">
												<xsl:text> ; </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='k'">
												<xsl:text>, </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='j'">
												<xsl:text>, </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='239']">
							<Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
							<Index:field Index:repeat="true" Index:name="author_main" Index:navn="po" Index:type="token">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='h']">
									<xsl:text>, </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="."/>
									<xsl:text>)</xsl:text>
								</xsl:for-each>
								<xsl:if test="mc:subfield[@code='c']">
									<xsl:choose>
										<xsl:when test="contains (.,'f. ')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:when test="contains (.,'f.')">
											<xsl:text> </xsl:text>
											<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
											<xsl:text>-</xsl:text>
										</xsl:when>
										<xsl:otherwise>
											<xsl:text> </xsl:text>
											<xsl:value-of select="mc:subfield[@code='c']" />
										</xsl:otherwise>
									</xsl:choose>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
					</Index:group>


                    <xsl:for-each select="mc:datafield[@tag='245']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='j' or @code='k' or @code='t' or @code='3']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='247']/mc:subfield[@code='e' or @code='f' or @code='t']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='e' or @code='f' or @code='t']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='250']/mc:subfield[@code='c' or @code='d' or @code='t']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='440' or @tag='840']/mc:subfield[@code='e' or @code='t' or @code='3']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='512']/mc:subfield[@code='d' or @code='e']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='513']/mc:subfield[@code='a' or @code='e' or @code='f' or @code='i' or @code='j']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='520']/mc:subfield[@code='d' or @code='e']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='526']/mc:subfield[@code='d' or @code='e']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='530']/mc:subfield[@code='d' or @code='e']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='d' or @code='e']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='540']/mc:subfield[@code='a']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='557']/mc:subfield[@code='3']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='e']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='571']/mc:subfield[@code='a' or @code='x']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='572']/mc:subfield[@code='a']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='745']/mc:subfield[@code='3']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='795']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='j' or @code='k' or @code='t' or @code='3']">
						<Index:field Index:name="author_descr" Index:navn="fb" Index:type="token" Index:repeat="true">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='100']">
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
							<xsl:for-each select="mc:subfield[@code='a']">
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='h']">
								<xsl:text>, </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='e']">
								<xsl:text> </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='f']">
								<xsl:text> (</xsl:text>
								<xsl:value-of select="."/>
								<xsl:text>)</xsl:text>
							</xsl:for-each>
							<xsl:if test="mc:subfield[@code='c']">
								<xsl:choose>
									<xsl:when test="contains (.,'f. ')">
										<xsl:text> </xsl:text>
										<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
										<xsl:text>-</xsl:text>
									</xsl:when>
									<xsl:when test="contains (.,'f.')">
										<xsl:text> </xsl:text>
										<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
										<xsl:text>-</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:text> </xsl:text>
										<xsl:value-of select="mc:subfield[@code='c']" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:if>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='700']">
							<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
							<xsl:for-each select="mc:subfield[@code='a']">
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='h']">
								<xsl:text>, </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='e']">
								<xsl:text> </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='f']">
								<xsl:text> (</xsl:text>
								<xsl:value-of select="."/>
								<xsl:text>)</xsl:text>
							</xsl:for-each>
							<xsl:if test="mc:subfield[@code='c']">
								<xsl:choose>
									<xsl:when test="contains (.,'f. ')">
										<xsl:text> </xsl:text>
										<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
										<xsl:text>-</xsl:text>
									</xsl:when>
									<xsl:when test="contains (.,'f.')">
										<xsl:text> </xsl:text>
										<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
										<xsl:text>-</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:text> </xsl:text>
										<xsl:value-of select="mc:subfield[@code='c']" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:if>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='770']">
												<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
							<xsl:for-each select="mc:subfield[@code='a']">
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='h']">
								<xsl:text>, </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='e']">
								<xsl:text> </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='f']">
								<xsl:text> (</xsl:text>
								<xsl:value-of select="."/>
								<xsl:text>)</xsl:text>
							</xsl:for-each>
							<xsl:if test="mc:subfield[@code='c']">
								<xsl:choose>
									<xsl:when test="contains (.,'f. ')">
										<xsl:text> </xsl:text>
										<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')" />
										<xsl:text>-</xsl:text>
									</xsl:when>
									<xsl:when test="contains (.,'f.')">
										<xsl:text> </xsl:text>
										<xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')" />
										<xsl:text>-</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:text> </xsl:text>
										<xsl:value-of select="mc:subfield[@code='c']" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:if>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='110' or @tag='710'or @tag='780']">
						<Index:field Index:repeat="true" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
							<xsl:if test="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='s'">
											<xsl:value-of select="." />
										</xsl:when>
										<xsl:when test="@code='e'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="." />
											<xsl:text>)</xsl:text>
										</xsl:when>
										<xsl:when test="@code='c'">
											<xsl:if test="position()&gt;1">
												<xsl:text>. </xsl:text>
											</xsl:if>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='i'">
											<xsl:text> ; </xsl:text>
											<xsl:value-of select="." />
										</xsl:when>
										<xsl:when test="@code='k'">
											<xsl:text>, </xsl:text>
											<xsl:value-of select="." />
										</xsl:when>
										<xsl:when test="@code='j'">
											<xsl:text>, </xsl:text>
											<xsl:value-of select="." />
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</xsl:if>
						</Index:field>
					</xsl:for-each>

        </xsl:template>
</xsl:stylesheet>