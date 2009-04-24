<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:Index="http://statsbiblioteket.dk/2004/Index"
        xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xalan="http://xml.apache.org/xalan"
        xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl dc"
        version="1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchive.org">


    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
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
                        <xsl:for-each select="item">
                            <xsl:variable name="pos">
                                <xsl:value-of select="position ()"/>
                            </xsl:variable>

                            <dc:creator summapos="{$pos}">
                                <xsl:value-of select="resource"/>
                            </dc:creator>
                            <xsl:if test="substring(startdate,1)!=''">
                                <dc:date summapos="{$pos}">
                                    <xsl:for-each select="startdate">

                                        <xsl:value-of select="."/>
                                        <xsl:text>-</xsl:text>
                                    </xsl:for-each>
                                    <xsl:if test="substring(enddate,1)!=''">
                                        <xsl:for-each select="enddate">

                                            <xsl:value-of select="."/>
                                        </xsl:for-each>
                                    </xsl:if>
                                </dc:date>
                                <xsl:variable name="pw">
                                    <xsl:value-of select="requires_password"/>
                                </xsl:variable>
                                <xsl:variable name="id">
                                    <xsl:value-of select="//id"/>
                                </xsl:variable>
                                <xsl:variable name="res_id">
                                    <xsl:value-of select="resource"/>
                                </xsl:variable>
                                <xsl:choose>
                                <xsl:when test="$pw!=''">
                                     <dc:identifier summapos="{$pos}" requires_passord="{$pw}" id="{$id}" resource_id="{$res_id}">


                                <xsl:value-of select="url"/>
                            </dc:identifier>
                            </xsl:when>
                                    <xsl:otherwise>
                                          <dc:identifier summapos="{$pos}">

                                <xsl:value-of select="url"/>
                            </dc:identifier>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>




                        </xsl:for-each>
                        <xsl:for-each select=".">
                            <dc:type xml:lang="da">

                                <xsl:text>e-tidsskrift</xsl:text>
                            </dc:type>
                            <dc:type xml:lang="en">
                                <xsl:text>e-journal</xsl:text>
                            </dc:type>
                        </xsl:for-each>
                        <xsl:for-each select="id">

                            <dc:identifier>
                                <xsl:text>ISSN </xsl:text>
                                <xsl:value-of select="."/>
                            </dc:identifier>
                                 <xsl:for-each select=".">
												<dc:format>todo</dc:format>
											</xsl:for-each>
                        </xsl:for-each>




                    </rdf:Description>
                </rdf:RDF>

            </shortrecord>
            <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
        </Index:field>

    </xsl:template>
</xsl:stylesheet>
