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
	<xsl:template name="ma">
			
							<xsl:for-each select=".">
										<Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
											<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>csa </xsl:text><xsl:value-of select="@jdf"/>
									</Index:field>
										
										<xsl:choose>
														<xsl:when test="contains(@type,'Journal Article ') or contains(pt,'Journal Article )')">
						
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>an</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
								
									</Index:field>
													</xsl:when>
								<xsl:when test="contains(@type,'Book Review ') or contains(pt,'Book Review ')">
								<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>an</xsl:text>
									</Index:field>
                                    <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>am</xsl:text>
									</Index:field>
                                    <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
								</xsl:when>
													<xsl:when test="contains(@type,'Book ') or contains(pt,'Book ')">
								<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>mo</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
								</xsl:when>
								<xsl:when test="contains(@type,'Book Chapter ') or contains(pt,'Book Chapter ')">
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>an</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
									</xsl:when>
								<xsl:when test="contains(@type,'Dissertation ') or contains(pt,'Dissertation ')">
								<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>mo</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
								</xsl:when>
								<xsl:when test="contains(@type,'Conference Paper ') or contains(pt,'Conference Paper ')">
								<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>mo</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
								</xsl:when>
										<xsl:when test="contains(@type,'Film Review ') or contains(pt,'Film Review ')">
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>an</xsl:text>
									</Index:field>
									 <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>am</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
									</xsl:when>
											<xsl:when test="contains(@type,'Software Review ') or contains(pt,'Software Review ')">
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>an</xsl:text>
									</Index:field>
								 <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>am</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xy</xsl:text>
									</Index:field>
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>te</xsl:text>
									</Index:field>
									</xsl:when>
								</xsl:choose>
								</Index:group>
								</xsl:for-each>
					
		<xsl:for-each select=".">
									<Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
                                         <xsl:for-each select=".">
                <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>CSA bibliografier</xsl:text>
                      </Index:field>
               </xsl:for-each>
                                        <Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
										<xsl:choose>
										<xsl:when test="contains(@type,'Journal Article ') or contains(pt,'Journal Article ')">tidsskriftartikel</xsl:when>
										<xsl:when test="contains(@type,'Book Review ') or contains(pt,'Book Review ')">boganmeldelse</xsl:when>
                                        <xsl:when test="contains(@type,'Book ') or contains(pt,'Book ')">bog</xsl:when>
                                        <xsl:when test="contains(@type,'Book Chapter ') or contains(pt,'Book Chapter ')">artikel i bog</xsl:when>
                                        <xsl:when test="contains(@type,'Dissertation ') or contains(pt,'Dissertation ')">tidsskriftsartikel</xsl:when>
                                        <xsl:when test="contains(@type,'Conference Paper ') or contains(pt,'Conference Paper ')">konference papirer</xsl:when>
                                        <xsl:when test="contains(@type,'Film Review ') or contains(pt,'Film Review ')">filmanmeldelse</xsl:when>
										<xsl:when test="contains(@type,'Software Review ') or contains(pt,'Software Review ')">software anmeldelse</xsl:when>
                                        <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
														</xsl:choose>
										</Index:field>
										<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
											<xsl:text>CSA bibliografier</xsl:text>
										</Index:field>
									<xsl:for-each select="@jdf">
										<Index:field Index:repeat="false" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
										<xsl:value-of select="."/>
										</Index:field>
										</xsl:for-each>
										</Index:group>
										</xsl:for-each>
	
	</xsl:template>
</xsl:stylesheet>
