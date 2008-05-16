<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>

        <xsl:template name="title">
        <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
                               <xsl:for-each select="title">
                                   <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                                       <xsl:value-of select="mainTitle"/>
                                   </Index:field>
                                   <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                                       <xsl:value-of select="alternativeTitle" />
                                   </Index:field>
                               </xsl:for-each>
                           </Index:group>


    </xsl:template>

</xsl:stylesheet>