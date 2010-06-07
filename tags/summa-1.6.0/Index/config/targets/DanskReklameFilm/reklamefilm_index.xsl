<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">
           <xsl:include href="reklamefilm_author.xsl" />
     <xsl:include href="reklamefilm_classification.xsl" />
            <xsl:include href="reklamefilm_date.xsl" />
     <xsl:include href="reklamefilm_id.xsl" />
       <xsl:include href="reklamefilm_ma.xsl" />
     <xsl:include href="reklamefilm_notes.xsl" />
             <xsl:include href="reklamefilm_publisher.xsl" />
     <xsl:include href="reklamefilm_short_format.xsl"/>
           <xsl:include href="reklamefilm_subject.xsl" />
       <xsl:include href="reklamefilm_title.xsl" />



	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
		<Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="https://sedna.statsbiblioteket.dk:8280/urn/" >
			<xsl:attribute name="Index:id">
				<xsl:value-of select="mdcs/id"/>
			</xsl:attribute>
			<xsl:for-each select="mdcs">
                <Index:fields>
                    <xsl:call-template name="short_format"/>
                   <xsl:call-template name="title"/>
                   <xsl:call-template name="author"/>
                     <xsl:call-template name="materials"/>
                    <xsl:call-template name="id"/>
                     <xsl:call-template name="subject"/>
                    <xsl:call-template name="classification"/>
                       <xsl:call-template name="date"/>
                     <xsl:call-template name="notes"/>
                   <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                       <xsl:text>dan</xsl:text>
                   </Index:field>
      
                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                        <xsl:choose>
                        <xsl:when test="substring(premiereDate,0)!=''">
                           <xsl:value-of select="substring(premiereDate,1,4)" />
                           </xsl:when>
                            <xsl:when test="substring(censor/date,0)!=''">
                           <xsl:value-of select="substring(censor/date,1,4)" />
                           </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>0000</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
					</Index:field>

                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                        <xsl:choose>
                        <xsl:when test="substring(premiereDate,0)!=''">
                           <xsl:value-of select="substring(premiereDate,1,4)" />
                           </xsl:when>
                            <xsl:when test="substring(censor/date,0)!=''">
                           <xsl:value-of select="substring(censor/date,1,4)" />
                           </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>9999</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                      
					</Index:field>
                     <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="6">

                                                                    <xsl:choose>
                                                                        <xsl:when test="substring(title/mainTitle,0)!=''">
                                                                    <xsl:value-of select="title/mainTitle"/>
                                                                    </xsl:when>
                                                                    <xsl:when test="substring(subject/productName,0)!=''">
                                                                    <xsl:value-of select="subject/productName"/>
                                                                     </xsl:when>

                                                                    <xsl:when test="substring(title/alternativeTitle,0)!=''">
                                                                    <xsl:value-of select="title/alternativeTitle"/>
                                                                    </xsl:when>
                                                                    
                                                                    </xsl:choose>
                         </Index:field>
                </Index:fields>






            </xsl:for-each>
        </Index:document>

    </xsl:template>

</xsl:stylesheet>