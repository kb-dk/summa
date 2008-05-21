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
	<xsl:template name="subject">
				<Index:group Index:name="su" Index:navn="em">
									<xsl:for-each select="emneord">
										<Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="2">


					<xsl:value-of select="."/>
				    </Index:field>
                </xsl:for-each>
             </Index:group>
         <Index:group Index:suggest="true" Index:navn="lem" Index:name="lsubj">
             <xsl:for-each select="emneord">
         <Index:field Index:boostFactor="10" Index:type="keyword" Index:navn="lsu_oai" Index:name="lsu_oai" Index:repeat="false">
					<xsl:value-of select="."/>
				    </Index:field>
             </xsl:for-each>
        </Index:group>
    </xsl:template>
   </xsl:stylesheet>

