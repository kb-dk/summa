<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl"
		version="1.0" 
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchive.org">


    <xsl:include href="etss_short_format.xsl" />
	<xsl:include href="etss_publisher.xsl" />
	<xsl:include href="etss_ma.xsl" />
	<xsl:include href="etss_identifiers.xsl" />
	    <xsl:include href="etss_su.xsl" />
    <xsl:include href="etss_date.xsl" />
    <xsl:include href="etss_title.xsl" />
    <xsl:include href="etss_notes.xsl" />
      <xsl:include href="etss_class.xsl" />



	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="http:www.statsbiblioteket.dk/etss">
			<xsl:attribute name="Index:id">
                <xsl:text>etss_</xsl:text>
                <xsl:value-of select="ejournal/id"/>
			</xsl:attribute>


			<xsl:for-each select="ejournal">
                <Index:fields>
                      <xsl:call-template name="shortformat" />
							<xsl:call-template name="publisher" />
									<xsl:call-template name="ma" />
									<xsl:call-template name="identifiers" />   
									<xsl:call-template name="su" />
							<xsl:call-template name="date" />
							<xsl:call-template name="title" />
									<xsl:call-template name="notes" />
									<xsl:call-template name="class" />

                    <xsl:for-each select="keywords[@type='svb']">
                    <Index:field Index:repeat="true" Index:name="location" Index:navn="lokation" Index:freetext="false" Index:type="token" Index:boostFactor="10">
                           <xsl:value-of select="."/>
                       </Index:field>
                        	</xsl:for-each>
                    
                </Index:fields>
                   
            </xsl:for-each>
		
		
		
		</Index:document>
	</xsl:template>
	
</xsl:stylesheet>
