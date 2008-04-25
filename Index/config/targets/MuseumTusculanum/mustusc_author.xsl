<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:Index="http://statsbiblioteket.dk/2004/Index"
				xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
				xmlns:xalan="http://xml.apache.org/xalan"
				xmlns:java="http://xml.apache.org/xalan/java"
				exclude-result-prefixes="java xs xalan xsl"
				version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:mdc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/"
				xmlns:mtp_dc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/">
		<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
		<xsl:template name="author">
				
				<Index:group Index:name="au" Index:navn="fo">
						
						<xsl:for-each select="mdc:creator">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						</xsl:for-each>
						<xsl:for-each select="mdc:editor">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						</xsl:for-each>
						
						<xsl:for-each select="mdc:contributor">
								
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						
						</xsl:for-each>
						
						<xsl:for-each select="mdc:author">
						
						
								
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						
						</xsl:for-each>
									<xsl:for-each select="mdc:part/mdc:creator">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						</xsl:for-each>
						<xsl:for-each select="mdc:part/mdc:editor">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						</xsl:for-each>
						
						<xsl:for-each select="mdc:part/mdc:contributor">
								
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						
						</xsl:for-each>
						
						<xsl:for-each select="mdc:part/mdc:author">
						
						
								
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
										<xsl:value-of select="."/>
								
								</Index:field>
								<xsl:choose>
										<xsl:when test="contains(.,' ')">
							<Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="6">
														<xsl:variable name="lastSpace">
																<xsl:call-template name="lastCharPosition">
																		<xsl:with-param name="original" select="." />
																		<xsl:with-param name="character" select="' '" />
																</xsl:call-template>
														</xsl:variable>
															<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

												</Index:field>
										</xsl:when>
								</xsl:choose>
						
						</xsl:for-each>
								
				
				</Index:group>
									
						
							
								
				
				<xsl:for-each select="mdc:contributor">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				
				</xsl:for-each>
						
				
			
				<xsl:for-each select="mdc:author">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				</xsl:for-each>
	
						<xsl:for-each select="mdc:editor">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				</xsl:for-each>
								<xsl:for-each select="mdc:creator">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				</xsl:for-each>
					<xsl:for-each select="mdc:part/mdc:contributor">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				
				</xsl:for-each>
						
				
			
				<xsl:for-each select="mdc:part/mdc:author">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				</xsl:for-each>
	
						<xsl:for-each select="mdc:part/mdc:editor">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				</xsl:for-each>
								<xsl:for-each select="mdc:part/mdc:creator">
						
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
								<xsl:variable name="lastSpace">
										<xsl:call-template name="lastCharPosition">
												<xsl:with-param name="original" select="." />
												<xsl:with-param name="character" select="' '" />
										</xsl:call-template>
								</xsl:variable>
									<xsl:value-of select="substring(., $lastSpace + 1)" />, <xsl:value-of select="substring(., 1, $lastSpace - 1)" />

						</Index:field>
				</xsl:for-each>
		</xsl:template>
			
		<xsl:template name="lastCharPosition">
				<xsl:param name="original"/>
				<xsl:param name="character"/>
				<xsl:param name="string_length"/>
				<xsl:variable name="len">
						<xsl:choose>
								<xsl:when test="$string_length">
										<xsl:value-of select="$string_length"/>
								</xsl:when>
								<xsl:otherwise>
										<xsl:value-of select="'0'"/>
								</xsl:otherwise>
						</xsl:choose>
				</xsl:variable>
				<xsl:variable name="char_len">
						<xsl:value-of select="string-length($character)"/>
				</xsl:variable>
				<xsl:choose>
						<xsl:when test="contains($original,$character)">
								<xsl:choose>
										<xsl:when test="contains(substring-after($original,$character),$character)">
												<xsl:call-template name="lastCharPosition">
														<xsl:with-param name="original" select="substring-after($original,$character)"/>
														<xsl:with-param name="character" select="$character"/>
														<xsl:with-param name="string_length" select="string-length(concat(substring-before($original,$character),''))+$len+$char_len"/>
												</xsl:call-template>
										</xsl:when>
										<xsl:otherwise>
												<xsl:value-of select="string-length(substring-before($original,$character))+$char_len+$len"/>
										</xsl:otherwise>
								</xsl:choose>
						</xsl:when>
						<xsl:otherwise>
								<xsl:value-of select="string-length($original)"/>
						</xsl:otherwise>
				</xsl:choose>
		</xsl:template>

</xsl:stylesheet>
