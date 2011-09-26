<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>


    <xsl:template name="shortformat">

                <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
                    <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                    <shortrecord>
                        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                                 xmlns:dc="http://purl.org/dc/elements/1.1/">
                            <rdf:Description>
                                <dc:title>
                                    <xsl:for-each select="mc:field[@type='110_00']/mc:subfield[@type='a']">
                                        <xsl:value-of select="."/>
                                    </xsl:for-each>
                                </dc:title>

                                <xsl:for-each select="mc:field[@type='100_00']/mc:subfield[@type='a']">
                                    <dc:creator>
                                        <xsl:call-template name="author_forward"/>    <!-- Author template ligger i nordicom_author.xsl-->
                                    </dc:creator>
                                </xsl:for-each>

                                <dc:type xml:lang="da">
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="/marc/mc:record/mc:field[@type='150_00']">
                                                <xsl:text>tidsskriftartikel</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="/marc/mc:record/mc:field[@type='140_00']">
                                                <xsl:text>artikel i bog</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>bog</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:type>

                                <dc:type xml:lang="en">
                                    <xsl:for-each select=".">
                                        <xsl:choose>
                                            <xsl:when test="/marc/mc:record/mc:field[@type='150_00']">
                                                <xsl:text>journal article</xsl:text>
                                            </xsl:when>
                                            <xsl:when test="/marc/mc:record/mc:field[@type='140_00']">
                                                <xsl:text>book article</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text>book</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </dc:type>

                                <xsl:for-each select="mc:field[@type='090_00']/mc:subfield[@type='a']">
                                    <dc:date>
                                        <xsl:value-of select="."/>
                                    </dc:date>
                                </xsl:for-each>

                                <dc:format>
                                    <xsl:for-each select=".">
                                        <xsl:text>todo</xsl:text>
                                    </xsl:for-each>
                                </dc:format>

                            </rdf:Description>
                        </rdf:RDF>
                    </shortrecord>
                    <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                </Index:field>

    </xsl:template>

</xsl:stylesheet>