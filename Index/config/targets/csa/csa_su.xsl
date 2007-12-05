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
	<xsl:template name="su">
			<Index:group Index:name="su" Index:navn="em">
									<xsl:for-each select="de">
									<xsl:for-each select="term">
									<xsl:choose>
										<xsl:when test="@type='label'">
										</xsl:when>
									<xsl:when test="starts-with(.,'*')">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
											<xsl:value-of select="substring-after(substring-before(.,' ('),'*')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
											<xsl:value-of select="substring-after(.,'*')"/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
											<xsl:when test="not(starts-with(.,'*'))">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
							  	<xsl:value-of select="substring-before(.,' (')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="subject_controlled" Index:navn="ke" Index:type="token" Index:boostFactor="10">
											<xsl:value-of select="."/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
										</xsl:choose>
										</xsl:for-each>
										</xsl:for-each> 
										
									<xsl:for-each select="ak">
									<xsl:for-each select="term">
									<xsl:choose>
	
									<xsl:when test="starts-with(.,'*')">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="substring-after(substring-before(.,' ('),'*')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="substring-after(.,'*')"/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
											<xsl:when test="not(starts-with(.,'*'))">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
							  	<xsl:value-of select="substring-before(.,' (')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="."/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
									
		  						</xsl:choose>
										</xsl:for-each>
										</xsl:for-each>

																		<xsl:for-each select="id">
									<xsl:for-each select="term">
									<xsl:choose>
		
									<xsl:when test="starts-with(.,'*')">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="substring-after(substring-before(.,' ('),'*')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="substring-after(.,'*')"/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
											<xsl:when test="not(starts-with(.,'*'))">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
							  	<xsl:value-of select="substring-before(.,' (')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
											<xsl:value-of select="."/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
									
										</xsl:choose>
										</xsl:for-each>
										</xsl:for-each>
	
										</Index:group>
		
										<Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
									<xsl:for-each select="de">
									<xsl:for-each select="term">
									<xsl:choose>
										<xsl:when test="@type='label'">
										</xsl:when>
									<xsl:when test="starts-with(.,'*')">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
							<Index:field Index:repeat="true" Index:name="lcsa" Index:navn="lcsa" Index:type="keyword" Index:boostFactor="10" Index:suggest="true">
											<xsl:value-of select="substring-after(substring-before(.,' ('),'*')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:repeat="true" Index:name="lcsa" Index:navn="lcsa" Index:type="keyword" Index:boostFactor="10" Index:suggest="true">
											<xsl:value-of select="substring-after(.,'*')"/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
											<xsl:when test="not(starts-with(.,'*'))">
									<xsl:choose>
									<xsl:when test="contains(.,'(')">
								<Index:field Index:repeat="true" Index:name="lcsa" Index:navn="lcsa" Index:type="keyword" Index:boostFactor="10" Index:suggest="true">
											<xsl:value-of select="substring-before(.,' (')"/>
										</Index:field>
										</xsl:when>
										<xsl:otherwise>
												<Index:field Index:repeat="true" Index:name="lcsa" Index:navn="lcsa" Index:type="keyword" Index:boostFactor="10" Index:suggest="true">
											<xsl:value-of select="."/>
										</Index:field>
										</xsl:otherwise>
										</xsl:choose>
										</xsl:when>
									
										</xsl:choose>
										</xsl:for-each>
										</xsl:for-each>
										</Index:group>
				      
	</xsl:template>
</xsl:stylesheet>

