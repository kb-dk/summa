<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
             xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:dc="http://purl.org/dc/elements/1.1/"
        exclude-result-prefixes="java xs xalan xsl oai_dc dc"
		version="1.0">


    <xsl:include href="csa_short_format.xsl" />
	<xsl:include href="csa_author.xsl" />
	<xsl:include href="csa_ma.xsl" />
	<xsl:include href="csa_identifiers.xsl" />
	<xsl:include href="csa_language.xsl" />
    <xsl:include href="csa_su.xsl" />
    <xsl:include href="csa_date.xsl" />
    <xsl:include href="csa_title.xsl" />
    <xsl:include href="csa_notes.xsl" />
    <xsl:include href="csa_publisher.xsl" />
    <xsl:include href="csa_classification.xsl" />



	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="http:www.statsbiblioteket.dk/csa">
			<xsl:attribute name="Index:id">llba_<xsl:value-of select="rec/an"/></xsl:attribute>

            <xsl:attribute name="Index:resolver">
				<xsl:value-of select="csa"/>
			</xsl:attribute>
			<xsl:for-each select="rec">
                <Index:fields>
                    <xsl:call-template name="shortformat" />
													<xsl:call-template name="author" />
									<xsl:call-template name="ma" />  
									<xsl:call-template name="identifiers" />   
									<xsl:call-template name="language" />
									<xsl:call-template name="su" />
							<xsl:call-template name="date" />
							<xsl:call-template name="title" />
									<xsl:call-template name="notes" />
										<xsl:call-template name="publisher" />
										<xsl:call-template name="classification" />
										</Index:fields>
			</xsl:for-each>
		
		
		
		</Index:document>
	</xsl:template>
	
</xsl:stylesheet>
