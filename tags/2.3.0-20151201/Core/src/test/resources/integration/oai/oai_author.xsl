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
	<xsl:template name="author">
		
					             <Index:group Index:name="au" Index:navn="fo">

							<xsl:for-each select="dc:creator">
								
								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="."/>
								
								</Index:field>
								
								<xsl:choose>
									<xsl:when test="contains(.,',')">
								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
										
										</Index:field>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
                                     <xsl:for-each select="oai_dc:creator">

								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="."/>

								</Index:field>

								<xsl:choose>
									<xsl:when test="contains(.,',')">
								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>

										</Index:field>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
                            <xsl:for-each select="dc:contributor">
								
								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6" >
									<xsl:value-of select="."/>
								
								</Index:field>
								<xsl:choose>
									<xsl:when test="contains(.,',')">
								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
										
										</Index:field>
									</xsl:when>
								</xsl:choose>
							
							</xsl:for-each>
                                     <xsl:for-each select="oai_dc:contributor">

                                         <Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6" >
                                             <xsl:value-of select="."/>

                                         </Index:field>
                                         <xsl:choose>
                                             <xsl:when test="contains(.,',')">
                                         <Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
                                                     <xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>

                                                 </Index:field>
                                             </xsl:when>
                                         </xsl:choose>

                                     </xsl:for-each>

								<xsl:for-each select="dc:publisher">
									<Index:field Index:boostFactor="1" Index:type="token" Index:repeat="true" Index:navn="fo_andet" Index:name="au_other">
										<xsl:value-of select="."/>
									</Index:field>
								</xsl:for-each>
                                     <xsl:for-each select="oai_dc:publisher">
                                         <Index:field Index:boostFactor="1" Index:type="token" Index:repeat="true" Index:navn="fo_andet" Index:name="au_other">
                                             <xsl:value-of select="."/>
                                         </Index:field>
                                     </xsl:for-each>

                                <xsl:for-each select="dc:publisher">
									<Index:field  Index:boostFactor="1" Index:type="token" Index:repeat="true" Index:navn="ko" Index:name="author_corporation">
										<xsl:value-of select="."/>
									</Index:field>
								</xsl:for-each>
                                     <xsl:for-each select="oai_dc:publisher">
                                         <Index:field  Index:boostFactor="1" Index:type="token" Index:repeat="true" Index:navn="ko" Index:name="author_corporation">
                                             <xsl:value-of select="."/>
                                         </Index:field>
                                     </xsl:for-each>

								</Index:group>
								
							
									
							<!--		<xsl:choose>
										<xsl:when test="contains(.,',')">
											<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:suggest="true" Index:type="keyword" Index:boostFactor="10">
												<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
											
											</Index:field>
										</xsl:when>
									</xsl:choose> -->
							
								<xsl:for-each select="dc:contributor">
									
									<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
										<xsl:value-of select="."/>
									
									</Index:field>
							<!--		<xsl:choose>
										<xsl:when test="contains(.,',')">
											<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:suggest="true" Index:type="keyword" Index:boostFactor="10">
												<xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>
											
											</Index:field>
										</xsl:when>
									</xsl:choose> -->
								
								</xsl:for-each>
        <xsl:for-each select="oai_dc:contributor">

            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:value-of select="."/>

            </Index:field>
    <!--		<xsl:choose>
                <xsl:when test="contains(.,',')">
                    <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:suggest="true" Index:type="keyword" Index:boostFactor="10">
                        <xsl:value-of select="normalize-space(concat(substring-after(.,','),' ',substring-before(.,',')))"/>

                    </Index:field>
                </xsl:when>
            </xsl:choose> -->

        </xsl:for-each>

						<xsl:for-each select="dc:creator">
								
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
						
			<xsl:value-of select="."/>
									</Index:field>
									</xsl:for-each>
        <xsl:for-each select="oai_dc:creator">

        <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">

<xsl:value-of select="."/>
                    </Index:field>
                    </xsl:for-each>

	</xsl:template>
</xsl:stylesheet>
