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
	<xsl:template name="su">
			<Index:group Index:name="su" Index:navn="em">
									<xsl:for-each select="keywords[@type='serial solutions']">
                                        <xsl:for-each select="keyword">
                                        <Index:field Index:repeat="true" Index:name="subject_serial_solutions" Index:navn="uk_ser_sol" Index:type="token" Index:boostFactor="2">
											<xsl:value-of select="."/>
										</Index:field>
                                           </xsl:for-each>
									</xsl:for-each>
                	<xsl:for-each select="keywords[@type='ulrichs']">

                                        <Index:field Index:repeat="true" Index:name="subject_ulrichs" Index:navn="uk_ulrichs" Index:type="token" Index:boostFactor="2">
										 <xsl:for-each select="keyword">
                                            <xsl:value-of select="."/>
                                              <xsl:if test="position()!=last()">
                                                 <xsl:text> - </xsl:text>
                                             </xsl:if>
                                             </xsl:for-each>
                                        </Index:field>

									</xsl:for-each>

                                    </Index:group>
								        <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
														<xsl:for-each select="keywords[@type='serial solutions']">
                                                             <xsl:for-each select="keyword">
															<Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_ser_sol" Index:name="lsu_ser_sol" Index:repeat="false">

															<xsl:value-of select="."/>
															</Index:field>
                                                                 </xsl:for-each>
															
							</xsl:for-each>
                                            <xsl:for-each select="keywords[@type='ulrichs']">
                                                             <xsl:for-each select="keyword">
                                                                 <xsl:if test="position()=1">
                                                            <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_ulrichs_1" Index:name="lsu_ulrichs" Index:repeat="false">

															<xsl:value-of select="."/>

                                                                     </Index:field>
                                             </xsl:if>
                                                           <xsl:if test="position()>1">
                                                            <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_ulrichs_2" Index:name="lsu_ulrichs" Index:repeat="false">

															<xsl:value-of select="."/>

                                                                     </Index:field>
                                             </xsl:if>

                                            </xsl:for-each>

                                               <Index:field Index:boostFactor="2" Index:type="keyword" Index:navn="lsu_ulrichs_hier" Index:name="lsu_ulrichs_hier" Index:repeat="false">
                                                  <xsl:for-each select="keyword">
                                               <xsl:value-of select="."/>
                                                      <xsl:if test="position()!=last()">
                                    <xsl:text> - </xsl:text>
                                </xsl:if>
                                                   </xsl:for-each>
                                               </Index:field>



                                            </xsl:for-each>
                              </Index:group>

	</xsl:template>
</xsl:stylesheet>

