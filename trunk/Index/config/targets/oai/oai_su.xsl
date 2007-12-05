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
	<xsl:template name="su">
			<Index:group Index:name="su" Index:navn="em">
									<xsl:for-each select="dc:subject">
										<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="2">
											<xsl:value-of select="."/>
										</Index:field>
										<xsl:choose>
											<xsl:when test="contains(., ';')">
												<xsl:call-template name="subject">
													<xsl:with-param name="subjects" select="." />
													<xsl:with-param name="sep" select="';'" />
												</xsl:call-template>
											</xsl:when>
											<xsl:when test="contains(., ',')">
												<xsl:call-template name="subject">
													<xsl:with-param name="subjects" select="." />
													<xsl:with-param name="sep" select="','" />
												</xsl:call-template>
											</xsl:when>
											<xsl:when test="contains(., '.')">
												<xsl:call-template name="subject">
													<xsl:with-param name="subjects" select="." />
													<xsl:with-param name="sep" select="'.'" />
												</xsl:call-template>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>

                <xsl:for-each select="oai_dc:subject">
                    <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="2">
                        <xsl:value-of select="."/>
                    </Index:field>
                    <xsl:choose>
                        <xsl:when test="contains(., ';')">
                            <xsl:call-template name="subject">
                                <xsl:with-param name="subjects" select="." />
                                <xsl:with-param name="sep" select="';'" />
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:when test="contains(., ',')">
                            <xsl:call-template name="subject">
                                <xsl:with-param name="subjects" select="." />
                                <xsl:with-param name="sep" select="','" />
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:when test="contains(., '.')">
                            <xsl:call-template name="subject">
                                <xsl:with-param name="subjects" select="." />
                                <xsl:with-param name="sep" select="'.'" />
                            </xsl:call-template>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
									</Index:group>
								        <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
														<xsl:for-each select="dc:subject">

<xsl:choose>
											<xsl:when test="contains(., ';')">
												<xsl:call-template name="lsubject">
													<xsl:with-param name="lsubjects" select="." />
													<xsl:with-param name="lsep" select="';'" />
												</xsl:call-template>
											</xsl:when>
											<xsl:when test="contains(., ',')">
												<xsl:call-template name="lsubject">
													<xsl:with-param name="lsubjects" select="." />
													<xsl:with-param name="lsep" select="','" />
												</xsl:call-template>
											</xsl:when>
											<xsl:when test="contains(., '.')">
												<xsl:call-template name="lsubject">
													<xsl:with-param name="lsubjects" select="." />
													<xsl:with-param name="lsep" select="'.'" />
												</xsl:call-template>
											</xsl:when>
											<xsl:otherwise>
															<Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">

															<xsl:value-of select="."/>
															</Index:field>
												</xsl:otherwise>
										</xsl:choose>
															
							</xsl:for-each>
                                            <xsl:for-each select="oai_dc:subject">					

<xsl:choose>
                                <xsl:when test="contains(., ';')">
                                    <xsl:call-template name="lsubject">
                                        <xsl:with-param name="lsubjects" select="." />
                                        <xsl:with-param name="lsep" select="';'" />
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="contains(., ',')">
                                    <xsl:call-template name="lsubject">
                                        <xsl:with-param name="lsubjects" select="." />
                                        <xsl:with-param name="lsep" select="','" />
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="contains(., '.')">
                                    <xsl:call-template name="lsubject">
                                        <xsl:with-param name="lsubjects" select="." />
                                        <xsl:with-param name="lsep" select="'.'" />
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:otherwise>
                                                <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">

                                                <xsl:value-of select="."/>
                                                </Index:field>
                                    </xsl:otherwise>
                            </xsl:choose>

                </xsl:for-each>
												</Index:group>

	</xsl:template>
	<xsl:template name="subject">
		<xsl:param name="subjects" />
		<xsl:param name="sep" />
		<xsl:choose>
			<xsl:when test="contains($subjects, $sep)">
				<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="2">
					<xsl:value-of select="normalize-space(substring-before($subjects, $sep))"/>
				</Index:field>
			</xsl:when>
			<xsl:otherwise>
				<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="2">
					<xsl:value-of select="normalize-space($subjects)"/>
				</Index:field>
			</xsl:otherwise>
		</xsl:choose>
		
		<xsl:if test="contains($subjects, $sep)">
			<xsl:call-template name="subject">
				<xsl:with-param name="subjects" select="substring-after($subjects, $sep)" />
				<xsl:with-param name="sep" select="$sep" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template name="lsubject">
		<xsl:param name="lsubjects" />
		<xsl:param name="lsep" />
		<xsl:choose>
			<xsl:when test="contains($lsubjects, $lsep)">
			<Index:field Index:boostFactor="4" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">
					<xsl:value-of select="normalize-space(substring-before($lsubjects, $lsep))"/>
				</Index:field>
			</xsl:when>
			<xsl:otherwise>
			<Index:field Index:boostFactor="4" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">
					<xsl:value-of select="normalize-space($lsubjects)"/>
				</Index:field>
			</xsl:otherwise>
		</xsl:choose>
		
		<xsl:if test="contains($lsubjects, $lsep)">
			<xsl:call-template name="lsubject">
				<xsl:with-param name="lsubjects" select="substring-after($lsubjects, $lsep)" />
				<xsl:with-param name="lsep" select="$lsep" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>

