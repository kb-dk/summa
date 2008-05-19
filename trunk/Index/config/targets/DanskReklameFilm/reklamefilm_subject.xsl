<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:template name="subject">
    <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                         
                           <xsl:for-each select="subject/keyword">
                                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
														<xsl:if test="substring(subject/productName,0)!=''">
                            <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                                <xsl:value-of select="subject/productName"/>
                            </Index:field>
</xsl:if>
<xsl:if test="substring(subject/restrictedTerm,0)!=''">
                                <xsl:for-each select="subject/restrictedTerm">
                                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                                        <xsl:value-of select="./text()" />
																				<xsl:for-each select="childTerm">
																				<xsl:if test="substring(.,0)!='' and substring(.,0)!='N/A'">
																						<xsl:text> - </xsl:text><xsl:value-of select="." />
																				</xsl:if>
																				</xsl:for-each>
                                   </Index:field>
                                
                                </xsl:for-each>
																</xsl:if>

                        </Index:group>
												  <Index:field Index:repeat="false" Index:name="su_dk" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:text>reklamefilm</xsl:text>
                                </Index:field>

    </xsl:template>

</xsl:stylesheet>