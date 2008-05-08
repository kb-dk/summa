<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl dc oai_dc oai"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
					xmlns:mdc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/"
				xmlns:mtp_dc="http://www.mtp.hum.ku.dk/library/uni/sta/oai2v1/">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="date">
			<xsl:for-each select="year_of_publ">
								<Index:field Index:repeat="true" Index:name="py" Index:navn="år"  Index:type="token" Index:boostFactor="2">
									<xsl:value-of select="."/>
								</Index:field>
						
							</xsl:for-each>
      

                                <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
									<xsl:for-each select="year_of_publ">
										<xsl:call-template name="getYear">
                                        <xsl:with-param name="str" select="." />
                                        <xsl:with-param name="split">
                                            <xsl:call-template name="getNumericSplit">
                                                <xsl:with-param name="org_str" select="." />
                                            </xsl:call-template>
                                        </xsl:with-param>
                                    </xsl:call-template>


									</xsl:for-each>
                                

                                    <xsl:if test="not(year_of_publ)">
										<xsl:text>0</xsl:text>
									</xsl:if>
								</Index:field>
								<Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
									<xsl:for-each select="year_of_publ">
										<xsl:call-template name="getYear">
                                        <xsl:with-param name="str" select="." />
                                        <xsl:with-param name="split">
                                            <xsl:call-template name="getNumericSplit">
                                                <xsl:with-param name="org_str" select="." />
                                            </xsl:call-template>
                                        </xsl:with-param>
                                    </xsl:call-template>

                                       
									</xsl:for-each>
                                  
									<xsl:if test="not(year_of_publ) ">
										<xsl:text>9999</xsl:text>
									</xsl:if>
								</Index:field>
	</xsl:template>

    <xsl:template name="getYear">
        <xsl:param name="str" />
        <xsl:param name="split" />
        <xsl:choose>
            <xsl:when test="contains($str, $split)">
                  <xsl:if test="string-length(substring-before($str,$split)) = 4">
                        <xsl:value-of select="substring-before($str,$split)" />
                  </xsl:if>

                  <xsl:call-template name="getYear">
                        <xsl:with-param name="str" select="substring-after($str,  $split)" />
                      <xsl:with-param name="split" select="$split" />
                   </xsl:call-template>

            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="string-length($str) = 4">
                    <xsl:value-of select="$str" />
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>


    </xsl:template>

    <xsl:template name="getNumericSplit">
        <xsl:param name="org_str" />
        <xsl:choose>
            <xsl:when test="contains($org_str, '/') or contains($org_str, '-')">
            <xsl:if test="contains($org_str, '/')">
              <xsl:text>/</xsl:text>
        </xsl:if>
        <xsl:if test="contains($org_str, '-')">
           <xsl:text>-</xsl:text>
        </xsl:if>
            </xsl:when>
            <xsl:otherwise>
               <xsl:text>*</xsl:text>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
</xsl:stylesheet>

