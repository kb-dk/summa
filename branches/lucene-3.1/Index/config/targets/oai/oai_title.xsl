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
	<xsl:template name="title">
			
								<Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
									<xsl:for-each select="dc:title">
										<Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="6">
											
											<xsl:value-of select="."/>
										
										</Index:field>
									</xsl:for-each>
                                    <xsl:for-each select="oai_dc:title">
                                        <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="6">

                                            <xsl:value-of select="."/>

                                        </Index:field>
                                    </xsl:for-each>
								</Index:group>
									<Index:field Index:name="sort_title" Index:sortLocale="da" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="6">
									
									<xsl:for-each select="dc:title [position()=1]">
										<xsl:choose>
											<xsl:when test="starts-with(.,'The ') and (starts-with(../dc:language,'en') or starts-with(../oai_dc:language,'en'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'The ') and (starts-with(../dc:language,'En') or starts-with(../oai_dc:language,'En'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'A ') and (starts-with(../dc:language,'en') or starts-with(../oai_dc:language,'en'))">
												<xsl:value-of select="substring(.,3)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'A ') and (starts-with(../dc:language,'En') or starts-with(../oai_dc:language,'En'))">
												<xsl:value-of select="substring(.,3)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'An ') and (starts-with(../dc:language,'en')  or starts-with(../oai_dc:language,'en'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'An ') and (starts-with(../dc:language,'En') or starts-with(../oai_dc:language,'En'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'La ') and (starts-with(../dc:language,'fr') or starts-with(../oai_dc:language,'fr'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'La ') and (starts-with(../dc:language,'Fr') or starts-with(../oai_dc:language,'Fr'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Le ') and (starts-with(../dc:language,'fr') or starts-with(../oai_dc:language,'fr'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Le ') and (starts-with(../dc:language,'Fr')  or starts-with(../oai_dc:language,'Fr'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Les ') and (starts-with(../dc:language,'fr') or starts-with(../oai_dc:language,'fr'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Les ') and (starts-with(../dc:language,'Fr') or starts-with(../oai_dc:language,'Fr'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<!--  <xsl:when test="starts-with(.,'L&apos;') and starts-with(../dc:language,'fr')">
												<xsl:value-of select="substring(.,3)"/>
											</xsl:when>
										<xsl:when test="starts-with(.,'L&apos;') and starts-with(../dc:language,'Fr')">
												<xsl:value-of select="substring(.,3)"/>
											</xsl:when>-->
											<xsl:when test="starts-with(.,'Der ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Der ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Der ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Die ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Die ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Die ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Das ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Das ') and (starts-with(../dc:language,'ger')  or starts-with(../oai_dc:language,'ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Das ') and (starts-with(../dc:language,'Ger')  or starts-with(../oai_dc:language,'Ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Ein ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Ein ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Ein ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Eine ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
												<xsl:value-of select="substring(.,6)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Eine ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
												<xsl:value-of select="substring(.,6)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Eine ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
												<xsl:value-of select="substring(.,6)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Las ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Las ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Las ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Los ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Los ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Los ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Un ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Un ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Un ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
												<xsl:value-of select="substring(.,4)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Una ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Una ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:when test="starts-with(.,'Una ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
												<xsl:value-of select="substring(.,5)"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="."/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:for-each>
                                        <xsl:for-each select="oai_dc:title [position()=1]">
                                            <xsl:choose>
                                                <xsl:when test="starts-with(.,'The ') and (starts-with(../dc:language,'en') or starts-with(../oai_dc:language,'en'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'The ') and (starts-with(../dc:language,'En') or starts-with(../oai_dc:language,'En'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'A ') and (starts-with(../dc:language,'en') or starts-with(../oai_dc:language,'en'))">
                                                    <xsl:value-of select="substring(.,3)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'A ') and (starts-with(../dc:language,'En') or starts-with(../oai_dc:language,'En'))">
                                                    <xsl:value-of select="substring(.,3)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'An ') and (starts-with(../dc:language,'en')  or starts-with(../oai_dc:language,'en'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'An ') and (starts-with(../dc:language,'En') or starts-with(../oai_dc:language,'En'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'La ') and (starts-with(../dc:language,'fr') or starts-with(../oai_dc:language,'fr'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'La ') and (starts-with(../dc:language,'Fr') or starts-with(../oai_dc:language,'Fr'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Le ') and (starts-with(../dc:language,'fr') or starts-with(../oai_dc:language,'fr'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Le ') and (starts-with(../dc:language,'Fr')  or starts-with(../oai_dc:language,'Fr'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Les ') and (starts-with(../dc:language,'fr') or starts-with(../oai_dc:language,'fr'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Les ') and (starts-with(../dc:language,'Fr') or starts-with(../oai_dc:language,'Fr'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <!--  <xsl:when test="starts-with(.,'L&apos;') and starts-with(../dc:language,'fr')">
                                                    <xsl:value-of select="substring(.,3)"/>
                                                </xsl:when>
                                            <xsl:when test="starts-with(.,'L&apos;') and starts-with(../dc:language,'Fr')">
                                                    <xsl:value-of select="substring(.,3)"/>
                                                </xsl:when>-->
                                                <xsl:when test="starts-with(.,'Der ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Der ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Der ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Die ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Die ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Die ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Das ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Das ') and (starts-with(../dc:language,'ger')  or starts-with(../oai_dc:language,'ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Das ') and (starts-with(../dc:language,'Ger')  or starts-with(../oai_dc:language,'Ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Ein ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Ein ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Ein ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Eine ') and (contains(../dc:language,'de') or contains(../oai_dc:language,'de'))">
                                                    <xsl:value-of select="substring(.,6)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Eine ') and (starts-with(../dc:language,'ger') or starts-with(../oai_dc:language,'ger'))">
                                                    <xsl:value-of select="substring(.,6)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Eine ') and (starts-with(../dc:language,'Ger') or starts-with(../oai_dc:language,'Ger'))">
                                                    <xsl:value-of select="substring(.,6)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Las ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Las ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Las ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Los ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Los ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Los ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Un ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Un ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Un ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
                                                    <xsl:value-of select="substring(.,4)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Una ') and (contains(../dc:language,'es') or contains(../oai_dc:language,'es'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Una ') and (starts-with(../dc:language,'spa') or starts-with(../oai_dc:language,'spa'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:when test="starts-with(.,'Una ') and (starts-with(../dc:language,'Spa') or starts-with(../oai_dc:language,'Spa'))">
                                                    <xsl:value-of select="substring(.,5)"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="."/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:for-each>

                                    </Index:field>
						
	</xsl:template>
</xsl:stylesheet>

