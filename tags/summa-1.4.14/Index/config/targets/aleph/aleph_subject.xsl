<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="subject">
        <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
						<xsl:for-each select="mc:datafield[@tag='600']">
							 <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person"/>
                </Index:field>
                <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>

						</xsl:for-each>

						<xsl:for-each select="mc:datafield[@tag='600']">
						 <xsl:if test="mc:subfield[@code='t' or @code='x' or @code='y' or @code='z' or @code='u']">
                    <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                        <xsl:call-template name="person_subdiv"/>
                    </Index:field>
                    <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
                        <xsl:call-template name="person_inverted_subdiv"/>
                    </Index:field>
                </xsl:if>
						</xsl:for-each>

						<xsl:for-each select="mc:datafield[@tag='083']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="4">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='X05' or @tag='X08']/mc:subfield[@code='p']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="4">
								<xsl:for-each select=".">
									<xsl:value-of select="."/>
								</xsl:for-each>
					
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='610']">
						 <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
                    <xsl:call-template name="corp"/>
                </Index:field>
                <xsl:if test="mc:subfield[@code='t' or @code='x' or @code='y' or @code='z' or @code='u']">
                    <Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
                        <xsl:call-template name="corp_subdiv"/>
                    </Index:field>
                </xsl:if>
								
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='621']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="4">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='j']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='g']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='s']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='t']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='631']/mc:subfield[@code='a' or @code='b' or @code='f' or @code='g' or @code='s' or @code='t']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='633']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='634']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='634']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='634']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='c']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='d']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='640']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='o']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='p']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='q']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='r']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='s']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='x']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='y']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='z']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='645']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='645']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='645']">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='c']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='650']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='b'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='c'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='d'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='e'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='v'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='x'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='y'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='z'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='651']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='v'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='x'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='y'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='z'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='652']">
							<Index:field Index:repeat="true" Index:name="subject_dk5" Index:navn="au" Index:type="token" Index:boostFactor="10">
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
						<xsl:for-each select="mc:datafield[@tag='652']/mc:subfield[@code='b' or @code='t']">
							<Index:field Index:repeat="true" Index:name="subject_dk5" Index:navn="au" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='653' or @tag='654' or @tag='655']">
							<Index:field Index:repeat="true" Index:name="subject_dk5" Index:navn="au" Index:type="token" Index:boostFactor="4">
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
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='653' or @tag='654' or @tag='655']/mc:subfield[@code='b']">
							<Index:field Index:repeat="true" Index:name="subject_dk5" Index:navn="au" Index:type="token" Index:boostFactor="4">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='653'or @tag='654' or @tag='655']/mc:subfield[@code='t']">
							<Index:field Index:repeat="true" Index:name="subject_dk5" Index:navn="au" Index:type="token" Index:boostFactor="4"></Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='660']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:if test="mc:subfield[@code='a' or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
									<xsl:for-each select="mc:subfield">
										<xsl:choose>
											<xsl:when test="@code='a'">
												<xsl:value-of select="."/>
											</xsl:when>
											<xsl:when test="@code='x'">
												<xsl:text>. </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
											<xsl:when test="@code='y'">
												<xsl:text>. </xsl:text>
												<xsl:value-of select="." />
												<xsl:text>) </xsl:text>
											</xsl:when>
											<xsl:when test="@code='z'">
												<xsl:text>. </xsl:text>
												<xsl:value-of select="." />
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
								</xsl:if>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='661']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='662']/mc:subfield[@code='a' or @code='b' or @code='c']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='666']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='o' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='667']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='o' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='668']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
                                    <xsl:text> </xsl:text>
                                    <xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='c']">
                                    <xsl:text> </xsl:text>
                                    <xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='670']">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='x'">
											<xsl:text>  - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='y'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='z'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>

						<xsl:for-each select="mc:datafield[@tag='690']/mc:subfield[@code='z']/..">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='b'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='c'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='d'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='e'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='f'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='g'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='h'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
                                        <xsl:when test="@code='i'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
                                        <xsl:when test="@code='u'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='v'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='w'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='x'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='690'][count(mc:subfield[@code='z']) = 0]">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='b'">
											<xsl:text>&#32;</xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='c'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='d'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
					</Index:group>

						 <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
            <xsl:for-each select="mc:datafield[@tag='600' or @tag='601' or @tag='602']">
                <Index:field Index:repeat="false" Index:name="su_pe" Index:navn="lep" Index:type="keyword" Index:boostFactor="10">

                    <xsl:call-template name="person_inverted"/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">

                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='600']">
                <xsl:if test="mc:subfield[@code='t' or @code='x' or @code='y' or @code='z' or @code='u']">
                    <Index:field Index:repeat="false" Index:name="su_lc" Index:navn="llcm" Index:type="keyword" Index:boostFactor="10">
                        <xsl:call-template name="person_inverted_subdiv"/>
                    </Index:field>
                    <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                        <xsl:call-template name="person_inverted_subdiv"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='601' or @tag='602']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="person_inverted"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='610']">
                <Index:field Index:repeat="false" Index:name="su_corp" Index:navn="lek" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp"/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp"/>
                </Index:field>

            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='610']">
                <Index:field Index:repeat="false" Index:name="su_lc" Index:navn="llcm" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp_subdiv"/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:call-template name="corp_subdiv"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='650']">
                <Index:field Index:repeat="false" Index:name="su_lc" Index:navn="llcm" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield">
                        <xsl:choose>
                            <xsl:when test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='b'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='c'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='d'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='e'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='v'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='x'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='y'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='z'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield">
                        <xsl:choose>
                            <xsl:when test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='b'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='c'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='d'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='e'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='v'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='x'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='y'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='z'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='651']">
                <Index:field Index:repeat="false" Index:name="su_lc" Index:navn="llcm" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield">
                        <xsl:choose>
                            <xsl:when test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='v'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='x'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='y'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='z'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield">
                        <xsl:choose>
                            <xsl:when test="@code='a'">
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='v'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='x'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='y'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                            <xsl:when test="@code='z'">
                                <xsl:text> - </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </Index:field>

            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='660']">
                <Index:field Index:repeat="false" Index:name="mesh" Index:navn="lms" Index:type="keyword" Index:boostFactor="10">
                    <xsl:if test="mc:subfield[@code='a' or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="@code='a'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@code='x'">
                                    <xsl:text>. </xsl:text>
                                    <xsl:value-of select="." />
                                </xsl:when>
                                <xsl:when test="@code='y'">
                                    <xsl:text>. </xsl:text>
                                    <xsl:value-of select="." />
                                    <xsl:text>) </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='z'">
                                    <xsl:text>. </xsl:text>
                                    <xsl:value-of select="." />
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:if>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:if test="mc:subfield[@code='a' or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
                        <xsl:for-each select="mc:subfield">
                            <xsl:choose>
                                <xsl:when test="@code='a'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:when test="@code='x'">
                                    <xsl:text>. </xsl:text>
                                    <xsl:value-of select="." />
                                </xsl:when>
                                <xsl:when test="@code='y'">
                                    <xsl:text>. </xsl:text>
                                    <xsl:value-of select="." />
                                    <xsl:text>) </xsl:text>
                                </xsl:when>
                                <xsl:when test="@code='z'">
                                    <xsl:text>. </xsl:text>
                                    <xsl:value-of select="." />
                                </xsl:when>
                            </xsl:choose>
                        </xsl:for-each>
                    </xsl:if>
                </Index:field>

            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='o' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
                <Index:field Index:repeat="false" Index:name="su_dk" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='668']">
                <Index:field Index:repeat="false" Index:name="su_dk" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>

            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='083']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='588']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="10">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='621']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='e']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='f']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='j']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
              <xsl:for-each select="mc:datafield[@tag='630']">
                    <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='630']">
                  <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='630']">
                  <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='f']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='630']">
                 <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='g']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='630']">
                 <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='s']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='630']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='t']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='631']/mc:subfield[@code='a' or @code='b' or @code='f' or @code='g' or @code='s' or @code='t']">
                  <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='633']">
                 <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='634']">
                 <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='634']">
                  <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='634']">
                  <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='d']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='640']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='o']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='p']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='q']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='r']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='s']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='x']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='y']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='z']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='645']">
                 <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='645']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='645']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='u']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
                        <xsl:for-each select="mc:datafield[@tag='652']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
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
               <xsl:for-each select="mc:datafield[@tag='652']/mc:subfield[@code='b' or @code='t']">
               <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='653' or @tag='654' or @tag='655']">
                 <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">

                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                     <xsl:for-each select="mc:subfield[@code='h']">

                        <xsl:text>,&#32;</xsl:text>
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
            <xsl:for-each select="mc:datafield[@tag='653' or @tag='654' or @tag='655']/mc:subfield[@code='b']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">

                     <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='653'or @tag='654' or @tag='655']/mc:subfield[@code='t']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">
                  <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='661']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">
                                             <xsl:value-of select="."/>
                          </Index:field>
                      </xsl:for-each>
                      <xsl:for-each select="mc:datafield[@tag='662']/mc:subfield[@code='a' or @code='b' or @code='c']">
                          <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">
                                             <xsl:value-of select="."/>
                          </Index:field>
                      </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='667']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='o' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
                <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">
                                                 <xsl:value-of select="."/>
                          </Index:field>
                      </xsl:for-each>
                      <xsl:for-each select="mc:datafield[@tag='668']">
                          <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">
                                              <xsl:for-each select="mc:subfield[@code='a']">
                                  <xsl:value-of select="."/>
                              </xsl:for-each>
                              <xsl:for-each select="mc:subfield[@code='b']">
                                  <xsl:text> </xsl:text>
                                  <xsl:value-of select="."/>
                              </xsl:for-each>
                              <xsl:for-each select="mc:subfield[@code='c']">
                                  <xsl:text> </xsl:text>
                                  <xsl:value-of select="."/>
                              </xsl:for-each>
                          </Index:field>
                      </xsl:for-each>
                      <xsl:for-each select="mc:datafield[@tag='670']">
                          <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">
                                               <xsl:for-each select="mc:subfield">
                                  <xsl:choose>
                                      <xsl:when test="@code='a'">
                                          <xsl:value-of select="."/>
                                      </xsl:when>
                                      <xsl:when test="@code='x'">
                                          <xsl:text>  - </xsl:text>
                                          <xsl:value-of select="."/>
                                      </xsl:when>
                                      <xsl:when test="@code='y'">
                                          <xsl:text> - </xsl:text>
                                          <xsl:value-of select="."/>
                                      </xsl:when>
                                      <xsl:when test="@code='z'">
                                          <xsl:text> - </xsl:text>
                                          <xsl:value-of select="."/>
                                      </xsl:when>
                                  </xsl:choose>
                              </xsl:for-each>
                          </Index:field>
                      </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='X05' or @tag='X08']/mc:subfield[@code='p']">
							  <Index:field Index:repeat="false" Index:name="lsubject" Index:navn="lsubject" Index:type="keyword" Index:boostFactor="4">

									<xsl:value-of select="."/>
								

							</Index:field>
						</xsl:for-each>
        </Index:group>


    </xsl:template>
     <xsl:template name="person_inverted">
       <xsl:for-each select="mc:subfield[@code='a']">
										 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
									</xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='h']">
            <xsl:text>, </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='e']">
            <xsl:text>&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='f']">
            <xsl:text>&#32;(</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)&#32;</xsl:text>
        </xsl:for-each>
        <xsl:if test="mc:subfield[@code='c']">
            <xsl:choose>
                <xsl:when test="contains (.,'f. ')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:when test="contains (.,'f.')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='c']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    <xsl:template name="person">
        <xsl:for-each select="mc:subfield[@code='h']">
            <xsl:value-of select="."/>
            <xsl:text> </xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='a']">
            <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='e']">
            <xsl:text>&#32;</xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='f']">
            <xsl:text>&#32;(</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>)&#32;</xsl:text>
        </xsl:for-each>
        <xsl:if test="mc:subfield[@code='c']">
            <xsl:choose>
                <xsl:when test="contains (.,'f. ')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f. ')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:when test="contains (.,'f.')">
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="substring-after(mc:subfield[@code='c'],'f.')"/>
                    <xsl:text>-</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#32;</xsl:text>
                    <xsl:value-of select="mc:subfield[@code='c']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
     <xsl:template name="person_subdiv">
        <xsl:call-template name="person"/>
        <xsl:for-each select="mc:subfield[@code='t']">
            <xsl:text> : </xsl:text>
           	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='x']">
            <xsl:text>. </xsl:text>
            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='y']">
            <xsl:text>. </xsl:text>
            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='z']">
            <xsl:text>. </xsl:text>
           	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='u']">
            <xsl:text> : </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="person_inverted_subdiv">
        <xsl:call-template name="person_inverted"/>
        <xsl:for-each select="mc:subfield[@code='t']">
            <xsl:text> : </xsl:text>
           	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='x']">
            <xsl:text>. </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='y']">
            <xsl:text>. </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='z']">
            <xsl:text>. </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='u']">
            <xsl:text> : </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="corp">
        <xsl:for-each select="mc:subfield[@code='a'or @code='s' or @code='e' or @code='c' or @code='i' or @code='k' or @code='j']">
            <xsl:choose>
                <xsl:when test="position()=1">
                   	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="@code='a'">
                            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@code='s'">
                            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@code='e'">
                            <xsl:text>&#32;(</xsl:text>
                            <xsl:value-of select="." />
                            <xsl:text>)</xsl:text>
                        </xsl:when>
                        <xsl:when test="@code='c'">
                            <xsl:if test="position()&gt;1">
                                <xsl:text>.&#32;</xsl:text>
                            </xsl:if>
                            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
                        </xsl:when>
                        <xsl:when test="@code='i'">
                            <xsl:text>&#32;;&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:when test="@code='k'">
                            <xsl:text>,&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                        <xsl:when test="@code='j'">
                            <xsl:text>,&#32;</xsl:text>
                            <xsl:value-of select="." />
                        </xsl:when>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

    </xsl:template>
    <xsl:template name="corp_subdiv">
        <xsl:call-template name="corp"/>
        <xsl:for-each select="mc:subfield[@code='t']">
            <xsl:text> : </xsl:text>
            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='x']">
            <xsl:text>. </xsl:text>
            	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='y']">
            <xsl:text>. </xsl:text>
           	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='z']">
            <xsl:text>. </xsl:text>
           	 <xsl:choose>
                        <xsl:when test="contains(.,'&lt;&lt;')">
                            <xsl:choose>
                            <xsl:when test="contains(.,'&gt;&gt;')">
                             <xsl:choose>
                                 <xsl:when test="contains(.,'=')">
                                   <xsl:value-of select="concat(substring-after(substring-before(.,'='),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                                 </xsl:when>
                                 <xsl:otherwise>
                                 <xsl:value-of select="concat(substring-after(substring-before(.,'&gt;&gt;'),'&lt;&lt;'),' ',substring-after(.,'&gt;&gt;'))"/>
                               </xsl:otherwise>

                               </xsl:choose>
                                </xsl:when>

                                 <xsl:otherwise>
                        <xsl:value-of select="substring-after(.,'&lt;&lt;')"/>
                        </xsl:otherwise>
                            </xsl:choose>
                            </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="."/>

                        </xsl:otherwise>
                        </xsl:choose>
            <xsl:text> </xsl:text>
        </xsl:for-each>
        <xsl:for-each select="mc:subfield[@code='u']">
            <xsl:text> : </xsl:text>
            <xsl:value-of select="."/>
        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>
