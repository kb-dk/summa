<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl dc oai_dc oai"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
					xmlns:mdc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/"
				xmlns:mtp_dc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	
	<xsl:template match="br">
	<xsl:text> </xsl:text>
	</xsl:template>
	<xsl:template name="notes">

			<xsl:for-each select="contents">
				<xsl:if test=".!=' '">
								<Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
									<xsl:apply-templates select="."/>
								</Index:field>
								</xsl:if>
							</xsl:for-each>
      

							<xsl:for-each select="presswrote">
								<xsl:if test=".!=' '">
								<Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
									<xsl:apply-templates select="."/>
								</Index:field>
								</xsl:if>
							</xsl:for-each>
       

							<xsl:for-each select="description">
								<xsl:if test=".!=' '">
									<Index:field Index:repeat="true" Index:name="no" Index:navn="no" Index:type="token" Index:boostFactor="1">
										<xsl:apply-templates select="."/>
												</Index:field>
												</xsl:if>
								</xsl:for-each>
       <xsl:for-each select="part/partnr">
			 	<xsl:if test=".!=' '">
   		<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
			<xsl:value-of select="."/>
			</Index:field>
			</xsl:if>
			</xsl:for-each>
			 <xsl:for-each select="pages">
			 	<xsl:if test=".!=' '">
   		<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
			<xsl:value-of select="."/>
			</Index:field>
			</xsl:if>
			</xsl:for-each>
			   <xsl:for-each select="part/startpage">
				 	<xsl:if test=".!=' '">
			<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
			<xsl:value-of select="."/><xsl:text>-</xsl:text><xsl:value-of select="following-sibling::endpage"/>
			</Index:field>
				</xsl:if>
			</xsl:for-each>
			
	</xsl:template>
</xsl:stylesheet>

