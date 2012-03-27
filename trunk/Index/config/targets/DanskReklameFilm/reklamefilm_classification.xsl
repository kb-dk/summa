<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="classification">
		

                            <xsl:for-each select="subject">
													<xsl:for-each select="restrictedTerm">
												          <xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
                                    <Index:field Index:repeat="true" Index:type="keyword" Index:name="commercials_subject" Index:navn="reklamefilm_subj" Index:boostFactor="6">
                                        <xsl:value-of select="./text()"/>
																				<xsl:for-each select="childTerm">
																				<xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
																						<xsl:text> - </xsl:text><xsl:value-of select="." />
																				</xsl:if>
																				</xsl:for-each>
                                    </Index:field>
																		</xsl:if>
                                </xsl:for-each> 
                         
</xsl:for-each>
    </xsl:template>

</xsl:stylesheet>