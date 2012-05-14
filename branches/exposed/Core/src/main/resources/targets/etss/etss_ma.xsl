<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchive.org">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="ma">

                       <xsl:for-each select=".">
								
								<Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
									<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>xe</xsl:text>
									</Index:field>
				<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>af</xsl:text>
										</Index:field>
										<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
											<xsl:text>te</xsl:text>
										</Index:field>

                   							<Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
										<xsl:text>pe</xsl:text>
									</Index:field>

									  
                                </Index:group>
								</xsl:for-each>
					
		<xsl:for-each select=".">
            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
													<xsl:text>e peri</xsl:text>
												</Index:field>
            <Index:field Index:repeat="true" Index:name="ma_long" Index:navn="ma_lang" Index:type="token">
										<xsl:text>peri</xsl:text>
									</Index:field>

                                    <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">

										    <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>eperi</xsl:text>
                      </Index:field>
												<Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="token">
										<xsl:text>peri</xsl:text>
									</Index:field>

                                        <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="token">
													<xsl:text>e_peri</xsl:text>
												</Index:field>
                                       
									</Index:group>
								</xsl:for-each>
								
	
	</xsl:template>
</xsl:stylesheet>
