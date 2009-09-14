<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl oai_dc dc"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="author">
		
					             <Index:group Index:name="au" Index:navn="fo">

							<xsl:for-each select="auths">
								<xsl:for-each select="au">
								<xsl:choose>
								<xsl:when test="contains(.,'[')">
								<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
									<xsl:value-of select="substring-before(.,'[')"/>
								</Index:field>
								</xsl:when>
                                    <xsl:when test="contains(.,'(Review of')">
                                <Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
									<xsl:value-of select="normalize-space(substring-before(.,'(Review of'))"/>
								</Index:field>
                                    </xsl:when>
                                <xsl:otherwise>
								<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
									<xsl:value-of select="."/>
								</Index:field>
								</xsl:otherwise>
								</xsl:choose> 
								<xsl:choose>
									<xsl:when test="contains(.,',')">
								<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
								<xsl:choose>
								<xsl:when test="contains(.,'(Review of')">
								<xsl:value-of select="normalize-space(concat(substring-after(substring-before(.,'(Review of'),','),' ',substring-before(.,',')))"/>
										</xsl:when>
                                    <xsl:when test="contains(.,'[')">
								<xsl:value-of select="normalize-space(concat(substring-after(substring-before(.,'['),','),' ',substring-before(.,',')))"/>
										</xsl:when>
                                        <xsl:otherwise>
										<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
										</xsl:otherwise>
										</xsl:choose>
										</Index:field>
									</xsl:when>
								</xsl:choose>
                                      </xsl:for-each>
                                     </xsl:for-each>
																		 
						<xsl:for-each select="auths">
								<xsl:for-each select="ed">
								<xsl:choose>
								<xsl:when test="contains(.,'[')">
								<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="substring-before(.,'[')"/>
								</Index:field>
								</xsl:when>
                                    <xsl:when test="contains(.,'(Review of')">
                                <Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="normalize-space(substring-before(.,'(Review of'))"/>
								</Index:field>
                                    </xsl:when>
                                <xsl:otherwise>
								<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="."/>
								</Index:field>
								</xsl:otherwise>
								</xsl:choose> 
								<xsl:choose>
									<xsl:when test="contains(.,',')">
								<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
								<xsl:choose>
								<xsl:when test="contains(.,'(Review of')">
								<xsl:value-of select="normalize-space(concat(substring-after(substring-before(.,'(Review of'),','),' ',substring-before(.,',')))"/>
										</xsl:when>
                                    <xsl:when test="contains(.,'[')">
								<xsl:value-of select="normalize-space(concat(substring-after(substring-before(.,'['),','),' ',substring-before(.,',')))"/>
										</xsl:when>
                                        <xsl:otherwise>
										<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
										</xsl:otherwise>
										</xsl:choose>
										</Index:field>
									</xsl:when>
								</xsl:choose>
                                      </xsl:for-each>
                                     </xsl:for-each>		
																		 
												<xsl:for-each select="oc">
																<Index:field Index:name="author_person" Index:navn="pe" Index:repeat="true" Index:type="token" Index:boostFactor="4">
									<xsl:value-of select="."/>
									</Index:field>
									<xsl:choose>
									<xsl:when test="contains(.,',')">
								<Index:field Index:name="author_person" Index:navn="pe" Index:repeat="true" Index:type="token" Index:boostFactor="4">
											<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
															</Index:field>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
				    </Index:group>
																			
       <xsl:for-each select="auths">
								<xsl:for-each select="au">
        <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
        <xsl:choose>
								<xsl:when test="contains(.,'[')">
									<xsl:value-of select="substring-before(.,'[')"/>
								</xsl:when>
                                    <xsl:when test="contains(.,'(Review of')">
									<xsl:value-of select="normalize-space(substring-before(.,'(Review of'))"/>
                                    </xsl:when>
                                <xsl:otherwise>
									<xsl:value-of select="."/>
								</xsl:otherwise>
								</xsl:choose>
        </Index:field>
        </xsl:for-each>
				</xsl:for-each>
				
				      <xsl:for-each select="auths">
								<xsl:for-each select="ed">
        <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="6">
        <xsl:choose>
								<xsl:when test="contains(.,'[')">
									<xsl:value-of select="substring-before(.,'[')"/>
								</xsl:when>
                                    <xsl:when test="contains(.,'(Review of')">
									<xsl:value-of select="normalize-space(substring-before(.,'(Review of'))"/>
                                    </xsl:when>
                                <xsl:otherwise>
									<xsl:value-of select="."/>
							</xsl:otherwise>
								</xsl:choose>
        </Index:field>
        </xsl:for-each>
        </xsl:for-each>
												

			                                           <xsl:for-each select="oc">

<xsl:choose>
                                <xsl:when test="contains(., ';')">
                                    <xsl:call-template name="loc">
                                        <xsl:with-param name="locs" select="." />
                                        <xsl:with-param name="lsep" select="';'" />
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="contains(., ',')">
                                    <xsl:call-template name="loc">
                                        <xsl:with-param name="locs" select="." />
                                        <xsl:with-param name="lsep" select="','" />
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="contains(., '.')">
                                    <xsl:call-template name="loc">
                                        <xsl:with-param name="locs" select="." />
                                        <xsl:with-param name="lsep" select="'.'" />
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:otherwise>
                                        <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="4">
																				<xsl:value-of select="."/>
									</Index:field>
                                    </xsl:otherwise>
                            </xsl:choose>
</xsl:for-each>
              
	</xsl:template>
	<xsl:template name="oc">
		<xsl:param name="ocs" />
		<xsl:param name="sep" />
		<xsl:choose>
			<xsl:when test="contains($ocs, $sep)">
					<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="4">
														<xsl:value-of select="normalize-space(substring-before($ocs, $sep))"/>
				</Index:field>
			</xsl:when>
			<xsl:otherwise>
	<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="4">
													<xsl:value-of select="normalize-space($ocs)"/>
				</Index:field>
			</xsl:otherwise>
		</xsl:choose>

		<xsl:if test="contains($ocs, $sep)">
			<xsl:call-template name="oc">
				<xsl:with-param name="ocs" select="substring-after($ocs, $sep)" />
				<xsl:with-param name="sep" select="$sep" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<xsl:template name="loc">
		<xsl:param name="locs" />
		<xsl:param name="lsep" />
		<xsl:choose>
			<xsl:when test="contains($locs, $lsep)">
	<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="4">
									<xsl:value-of select="normalize-space(substring-before($locs, $lsep))"/>
				</Index:field>
			</xsl:when>
			<xsl:otherwise>
		<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="4">
					<xsl:value-of select="normalize-space($locs)"/>
				</Index:field>
			</xsl:otherwise>
		</xsl:choose>

		<xsl:if test="contains($locs, $lsep)">
			<xsl:call-template name="loc">
				<xsl:with-param name="locs" select="substring-after($locs, $lsep)" />
				<xsl:with-param name="lsep" select="$lsep" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
