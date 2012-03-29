<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0">

    <xsl:template name="other">
         <xsl:for-each select="mc:datafield[@tag='250']/mc:subfield[@code='a' or @code='b' or @code='p' or @code='x']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='255']/mc:subfield[@code='a']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='256']/mc:subfield[@code='a'or @code='b' or @code='c' or @code='d' or @code='f']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='257']/mc:subfield[@code='a'or @code='p']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='260']/mc:subfield[@code='k']">
                <Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='259']/mc:subfield[@code='a'or @code='b']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>

					<xsl:for-each select="mc:datafield[@tag='300']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:for-each select="mc:subfield">
								<xsl:choose>
									<xsl:when test="position()=1">
										<xsl:value-of select="."/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:if test="@code='n'">
											<xsl:text> + </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='a'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:if>
										<xsl:if test="@code='b'">
											<xsl:text> : </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='c'">
											<xsl:text> ; </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='d'">
											<xsl:text> + </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='e'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:if>
										<xsl:if test="@code='l'">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>)</xsl:text>
										</xsl:if>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='539']/mc:subfield[@code='a']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='555']/mc:subfield[@code='a']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='247']/mc:subfield[@code='l']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='l']">
						<Index:field Index:repeat="true" Index:name="other" Index:navn="andet" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
