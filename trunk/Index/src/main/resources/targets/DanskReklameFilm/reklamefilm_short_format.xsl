<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">

	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="short_format">
        <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
            <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
            <shortrecord>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <rdf:Description rdf:about="http://www.ilrt.bristol.ac.uk/people/cmdjb/">
                            <dc:title>
                                                                    <xsl:choose>
                                                                        <xsl:when test="substring(title/mainTitle,0)!=''">
                                                                    <xsl:value-of select="title/mainTitle"/>
                                                                    <xsl:if test="substring(title/alternativeTitle,0)!=''">
                                                                    <xsl:text>&#32;:&#32;</xsl:text>
                                                                    <xsl:value-of select="title/alternativeTitle"/>
                                                                    </xsl:if>
                                                                    </xsl:when>
                                                                    <xsl:when test="substring(subject/productName,0)!=''">
                                                                    <xsl:value-of select="subject/productName"/>
                                                                    <xsl:if test="substring(title/alternativeTitle,0)!=''">
                                                                    <xsl:text>:&#32;</xsl:text>
                                                                    <xsl:value-of select="title/alternativeTitle"/>
                                                                    </xsl:if>
                                                                    </xsl:when>

                                                                    <xsl:when test="substring(title/alternativeTitle,0)!=''">
                                                                    <xsl:value-of select="title/alternativeTitle"/>
                                                                    </xsl:when>
                                                                    <xsl:otherwise>
                                                                    <xml:text xml:lang="da">Uden titel</xml:text>
                                                                    <xml:text xml:lang="en">No title</xml:text>
                                                                    </xsl:otherwise>

                                                                    </xsl:choose>
                                                                    </dc:title>
                            <dc:creator><xsl:value-of select="contributor[@role='Producent']"/></dc:creator>
                            <dc:date>
                                <xsl:choose>
                        <xsl:when test="substring(premiereDate,0)!=''">
                           <xsl:value-of select="substring(premiereDate,1,4)" />
                           </xsl:when>
                            <xsl:when test="substring(censor/date,0)!=''">
                           <xsl:value-of select="substring(censor/date,1,4)" />
                           </xsl:when>
                                </xsl:choose>
                            </dc:date>
                            <dc:type xml:lang="da">reklamefilm</dc:type>
                            <dc:type xml:lang="en">commercial</dc:type>
                            <dc:identifier><xsl:text>https://sedna.statsbiblioteket.dk:8280/urn/</xsl:text><xsl:value-of select="id" /></dc:identifier>
                             <dc:format>todo</dc:format>

                    </rdf:Description>
                </rdf:RDF>
            </shortrecord>
            <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
  </Index:field>

    </xsl:template>

</xsl:stylesheet>