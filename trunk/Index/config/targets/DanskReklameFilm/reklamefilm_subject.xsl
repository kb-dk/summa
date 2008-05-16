<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:template name="subject">
    <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                           <Index:field Index:repeat="false" Index:name="su_dk" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                                    <xsl:text>reklamefilm</xsl:text>
                                </Index:field>
                           <xsl:for-each select="subject/keyword">
                                <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                                    <xsl:value-of select="."/>
                                </Index:field>
                            </xsl:for-each>
                            <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                                <xsl:value-of select="subject/productName"/>
                            </Index:field>


                                <xsl:for-each select="subject/restrictedTerm">
                                    <Index:field Index:repeat="true" Index:type="keyword" Index:name="commercials_subject" Index:navn="reklamefilm_subj" Index:boostFactor="6">
                                        <xsl:value-of select="./text()" />
                                   </Index:field>
                                  <!--  <xsl:for-each select="childTerm">
                                        <Index:field Index:repeat="true" Index:type="keyword" Index:name="commercials_subject" Index:navn="reklamefilm_subj" Index:boostFactor="6">
                                            <xsl:value-of select="../text()"/><xsl:text> : </xsl:text><xsl:value-of select="./text()" />
                                        </Index:field>
                                    </xsl:for-each>  -->
                                </xsl:for-each>

                        </Index:group>

    </xsl:template>

</xsl:stylesheet>