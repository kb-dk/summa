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


    <xsl:include href="mustusc_short_format.xsl" />
	<xsl:include href="mustusc_author.xsl" />
	<xsl:include href="mustusc_ma.xsl" />
	<xsl:include href="mustusc_identifiers.xsl" />
	    <xsl:include href="mustusc_su.xsl" />
    <xsl:include href="mustusc_date.xsl" />
    <xsl:include href="mustusc_title.xsl" />
    <xsl:include href="mustusc_notes.xsl" />
    <xsl:include href="mustusc_publisher.xsl" />



	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="http:www.statsbiblioteket.dk/mdc">
			<xsl:attribute name="Index:id">
				<xsl:value-of select="record/header/identifier"/>
			</xsl:attribute>

            <xsl:attribute name="Index:resolver">
				<xsl:value-of select="'mdc'"/>
			</xsl:attribute>
			<xsl:for-each select="record">
                <Index:fields>
                    <xsl:call-template name="shortformat" />
					<xsl:for-each select="metadata">
						<xsl:for-each select="mtp_dc:mdc">
								<xsl:call-template name="author" />
									<xsl:call-template name="ma" />  
									
																	<xsl:call-template name="su" />
							<xsl:call-template name="date" />
							<xsl:call-template name="title" />
									<xsl:call-template name="notes" />
										<xsl:call-template name="publisher" />
										<xsl:call-template name="identifiers" />   
							</xsl:for-each>
								
								</xsl:for-each>

				</Index:fields>
			</xsl:for-each>
		
		
		
		</Index:document>
	</xsl:template>
	
</xsl:stylesheet>
