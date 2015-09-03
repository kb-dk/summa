<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0">

         <xsl:template name="author">
             <Index:group Index:name="au" Index:navn="fo" Index:suggest="true">

                    <Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                        <xsl:value-of select="navn" />
                    </Index:field>
                 <Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                        <xsl:value-of select="navn_sort" />
                    </Index:field>
              </Index:group>
                     <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo"  Index:type="keyword" Index:boostFactor="10">
                <xsl:value-of select="navn_sort"/>

            </Index:field>
        </xsl:template>
</xsl:stylesheet>