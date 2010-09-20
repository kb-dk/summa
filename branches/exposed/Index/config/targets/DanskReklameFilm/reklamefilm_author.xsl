<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="author">
        <Index:group Index:name="au" Index:navn="fo" Index:suggest="false">
				<xsl:for-each select="contributor">
				<xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
          								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="."/>
								
								</Index:field>

       </xsl:if>
			 </xsl:for-each>
			 
			 				<xsl:for-each select="creator">
				<xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
            								<Index:field Index:name="au_other" Index:navn="fo_andet" Index:repeat="true" Index:type="token" Index:boostFactor="6">
									<xsl:value-of select="."/>
								
								</Index:field>

       </xsl:if>
			 </xsl:for-each>

        </Index:group>
				<xsl:for-each select="contributor">
					<xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="2">
									<xsl:value-of select="."/>
								
						</Index:field>
						</xsl:if>
						</xsl:for-each>
				<xsl:for-each select="creator">
					<xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
						<Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="2">
									<xsl:value-of select="."/>
								
						</Index:field>
						</xsl:if>
						</xsl:for-each>
		
    </xsl:template>

</xsl:stylesheet>