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
                                         <xsl:choose>
                                                                        <xsl:when test="substring(mainTitle,0)!=''">
                                                                    <xsl:value-of select="mainTitle"/>
                                                                       </xsl:when>
                                                                    <xsl:when test="substring(/mdcs/subject/productName,0)!=''">
                                                                    <xsl:value-of select="/mdcs/subject/productName"/>
                                                                                   </xsl:when>
                                                             <xsl:when test="substring(alternativeTitle,0)!=''">
                                                                    <xsl:value-of select="alternativeTitle"/>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                    <xml:text>Uden titel</xml:text>
                                                                     </xsl:otherwise>

                                                                    </xsl:choose>
                                   </Index:field>
																	 <xsl:for-each select="alternativeTitle">
																	 <xsl:if test="substring(.,0)!=''">
                                   <Index:field Index:repeat="true" Index:name="title" Index:navn="title" Index:type="token" Index:boostFactor="8">
                                       <xsl:value-of select="." />
                                   </Index:field>
</xsl:if>
																	 </xsl:for-each>
                               </xsl:for-each>
                           </Index:group>


    </xsl:template>

</xsl:stylesheet>