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
		<xsl:template name="shortformat">
				
				<Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
						<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
						<shortrecord>
								<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
										<rdf:Description rdf:about="http://www.ilrt.bristol.ac.uk/people/cmdjb/">
												
												<xsl:for-each select=".">
														<xsl:for-each select="ti">
																<dc:title>
																		<xsl:value-of select="."/>
																</dc:title>
														</xsl:for-each>
														<xsl:for-each select="auths">
																<xsl:for-each select="au">
																	<dc:creator>	
																		<xsl:choose>
																				
																				<xsl:when test="contains(.,',')">
																						<xsl:choose>
																								<xsl:when test="contains(.,'(Review')">
	
										<xsl:value-of select="normalize-space(concat(substring-after(substring-before(.,'(Review of'),','),' ',substring-before(.,',')))"/>
											</xsl:when>
																								<xsl:when test="contains(.,'[Ed')">
																									<xsl:value-of select="normalize-space(concat(substring-after(substring-before(.,'[Ed'),','),' ',substring-before(.,',')))"/>
									
												</xsl:when>
																								<xsl:otherwise>											
												<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
												</xsl:otherwise>
																						</xsl:choose>
																							</xsl:when>
																						<xsl:otherwise>
																								<xsl:value-of select="."/>
																						</xsl:otherwise>
																			
																		</xsl:choose>													
											
				                                                    </dc:creator>
																</xsl:for-each>
														</xsl:for-each>
														<xsl:for-each select="py">
																<dc:date>
																		<xsl:value-of select="."/>
																</dc:date>
														</xsl:for-each>
														<xsl:for-each select=".">
																<dc:type xml:lang="da">
																				
																		<xsl:choose>
																				<xsl:when test="contains(@type,'Journal Article (aja)') or contains(pt,'Journal Article (aja)')">tidsskriftsartikel</xsl:when>
																				<xsl:when test="contains(@type,'Journal Article (aja)') or contains(pt,'Journal Article')">tidsskriftsartikel</xsl:when>
																				<xsl:when test="contains(@type,'Book Review (brv)') or contains(pt,'Book Review (brv)')">boganmeldelse</xsl:when>
																					<xsl:when test="contains(@type,'Film Review (frv)') or contains(pt,'Film Review (frv)')">filmanmeldelse</xsl:when>
																					<xsl:when test="contains(@type,'Software Review (swr)') or contains(pt,'Software Review (swr')">software anmeldelse</xsl:when>	
                                                                                    <xsl:when test="contains(@type,'Book (bka)') or contains(pt,'Book (bka)')">bog</xsl:when>
																				<xsl:when test="contains(@type,'Book Chapter (bca)') or contains(pt,'Book Chapter (bca)')">artikel i bog</xsl:when>
																				<xsl:when test="contains(@type,'Dissertation (dis)') or contains(pt,'Dissertation (dis)')">tidsskriftsartikel</xsl:when>
																				<xsl:when test="contains(@type,'Conference Paper (acp)') or contains(pt,'Conference Paper (acp)')">bog</xsl:when>
																				<xsl:otherwise>
																						<xsl:value-of select="@type"/>
																				</xsl:otherwise>
																				
																		</xsl:choose>
																</dc:type>
														</xsl:for-each>
														<xsl:for-each select=".">
																<dc:type xml:lang="en">
																		<xsl:choose>
																				<xsl:when test="contains(@type,'Journal Article (aja)') or contains(pt,'Journal Article (aja)')">journal article</xsl:when>
																				<xsl:when test="contains(@type,'Journal Article (aja)') or contains(pt,'Journal Article')">journal article</xsl:when>																				
																				<xsl:when test="contains(@type,'Book Review (brv)') or contains(pt,'Book Review (brv)')">book review</xsl:when>
																						<xsl:when test="contains(@type,'Film Review (frv)') or contains(pt,'Film Review (frv)')">film review</xsl:when>
																							<xsl:when test="contains(@type,'Software Review (swr)') or contains(pt,'Software Review (swr)')">software review</xsl:when>
																				<xsl:when test="contains(@type,'Book (bka)') or contains(pt,'Book (bka)')">book</xsl:when>
																				<xsl:when test="contains(@type,'Book Chapter (bca)') or contains(pt,'Book Chapter (bca)')">book article</xsl:when>
																				<xsl:when test="contains(@type,'Dissertation (dis)') or contains(pt,'Dissertation (dis)')">journal article</xsl:when>
																				<xsl:when test="contains(@type,'Conference Paper (acp') or contains(pt,'Conference Paper (acp')">book</xsl:when>
																				<xsl:otherwise>
																						<xsl:value-of select="@type"/>
																				</xsl:otherwise>
																				
																		</xsl:choose>
																</dc:type>
														</xsl:for-each>
	                                             <xsl:for-each select=".">
												<dc:format>todo</dc:format>
											</xsl:for-each>
										</xsl:for-each>
										</rdf:Description>
								</rdf:RDF>
								
						</shortrecord>
						<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
				</Index:field>
				
		</xsl:template>
</xsl:stylesheet>
