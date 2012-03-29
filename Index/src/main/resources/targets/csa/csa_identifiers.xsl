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
		<xsl:template name="identifiers">
				<Index:group Index:name="numbers" Index:navn="nr">
						<xsl:for-each select="an">
								<Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
										<xsl:value-of select="."/>
								</Index:field>
									
						</xsl:for-each>
							<xsl:for-each select="ui">
								<Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
										<xsl:value-of select="."/>
								</Index:field>
																				</xsl:for-each>
						<xsl:for-each select="sn">
								<Index:field Index:name="id" Index:navn="id" Index:type="token" Index:boostFactor="10">
										<xsl:value-of select="translate(.,' -','')"/>
								</Index:field>
									
						</xsl:for-each>
						<xsl:for-each select="ib">
								<Index:field Index:name="isbn" Index:navn="ib" Index:type="token" Index:boostFactor="10">
										<xsl:value-of select="translate(.,' -','')"/>
								</Index:field>
								
    <!--                         <Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.sbandex.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>--> 
                        </xsl:for-each>
						<xsl:for-each select="is">
								<Index:field Index:name="issn" Index:navn="is" Index:type="token" Index:boostFactor="10">
										<xsl:value-of select="."/>
								</Index:field>
						</xsl:for-each>
						
												<xsl:for-each select="ei">
								<Index:field Index:name="issn" Index:navn="is" Index:type="token" Index:boostFactor="10">
										<xsl:value-of select="."/>
								</Index:field>
						</xsl:for-each>
								
				</Index:group>
	
	
		</xsl:template>
</xsl:stylesheet>
