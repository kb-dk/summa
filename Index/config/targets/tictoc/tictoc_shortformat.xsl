<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>


<!-- Shortformat -->
    <xsl:template name="shortformat">

        <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
            <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
            <shortrecord>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <rdf:Description>
                        <xsl:for-each select="title">
                            <dc:title>
                                <xsl:value-of select="."/>
                            </dc:title>
                        </xsl:for-each>

                        <xsl:for-each select="author">
                            <dc:creator>
                                <xsl:value-of select="."/>
                            </dc:creator>
                        </xsl:for-each>

                        <xsl:for-each select="pubDate">
                            <dc:date>
                                <xsl:value-of select="substring(.,string-length(.)-3)" />
                            </dc:date>
                        </xsl:for-each>

                        <dc:type xml:lang="da">
                            <xsl:text>netdokument</xsl:text>
                        </dc:type>
                        <dc:type xml:lang="da">
                            <xsl:text>net document</xsl:text>
                        </dc:type>

                        <xsl:for-each select="link">
                            <dc:identifier>
                                <xsl:value-of select="."/>
                            </dc:identifier>
                        </xsl:for-each>

                    </rdf:Description>
                </rdf:RDF>
            </shortrecord>
            <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </Index:field>

    </xsl:template>

</xsl:stylesheet>