<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">
    

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="materials">
       <Index:group Index:freetext="false" Index:navn="lma" Index:name="lma">
      <Index:field Index:repeat="false" Index:name="ltarget" Index:navn="datakilde" Index:type="keyword" Index:freetext="false">
                      <xsl:text>reklamefilm</xsl:text>
                      </Index:field>
        <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                 <xsl:text>film</xsl:text>
        </Index:field>
        <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                 <xsl:text>e_film</xsl:text>
         </Index:field>
        </Index:group>
        <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
                 <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                      <xsl:text>fi</xsl:text>
                 </Index:field>
                 <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                      <xsl:text>xe</xsl:text>
                 </Index:field>
        </Index:group>
		
    </xsl:template>

</xsl:stylesheet>