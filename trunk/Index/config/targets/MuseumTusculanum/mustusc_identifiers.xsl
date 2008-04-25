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
	<xsl:template name="identifiers">
				<Index:group Index:name="numbers" Index:navn="nr">
				  <xsl:for-each select="mdc:issn">
								<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
								<Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
							<xsl:value-of select="."/>
							</Index:field>
							</xsl:for-each>
							<xsl:for-each select="mdc:part/mdc:issn">
								<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
								<Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
							<xsl:value-of select="."/>
							</Index:field>
							</xsl:for-each>
									<xsl:for-each select="mdc:identifier">
										<Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="8">
											<xsl:value-of select="."/>
										</Index:field>
										<xsl:choose>
											<xsl:when test="starts-with(.,'http://')">
												<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
													<xsl:value-of select="."/>
												</Index:field>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
										<xsl:for-each select="mdc:part/mdc:identifier">
										<Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="8">
											<xsl:value-of select="."/>
										</Index:field>
										<xsl:choose>
											<xsl:when test="starts-with(.,'http://')">
												<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
													<xsl:value-of select="."/>
												</Index:field>
											</xsl:when>
										</xsl:choose>
									</xsl:for-each>
                   <xsl:for-each select="mdc:isbn">
							<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="translate(.,'- ','')"/>
							</Index:field>
								<Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="translate(.,'- ','')"/>
							</Index:field>
							</xsl:for-each>
						     <xsl:for-each select="mdc:part/mdc:isbn">
							<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="translate(.,'- ','')"/>
							</Index:field>
									<Index:field Index:name="standard_number" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="translate(.,'- ','')"/>
							</Index:field>
									</xsl:for-each>
						       <xsl:for-each select="mdc:isbn">
						
								<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
							
								<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
						</xsl:for-each>
						     <xsl:for-each select="mdc:part/mdc:isbn">
					
								<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
							
								<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
						</xsl:for-each>
						 
								</Index:group>
								
	
	</xsl:template>
</xsl:stylesheet>
