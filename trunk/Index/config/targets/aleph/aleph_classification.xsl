<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="classification">
       <Index:group Index:name="cl" Index:navn="cl" Index:suggest="true">
						<xsl:for-each select="mc:datafield[@tag='050']">
							<Index:field Index:repeat="true" Index:name="lcc_kw" Index:navn="lcc_kw" Index:type="token" Index:boostFactor="5">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='060']">
							<Index:field Index:repeat="true" Index:name="nlm_kw" Index:navn="nlm_kw" Index:type="token" Index:boostFactor="8">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='070']">
							<Index:field Index:repeat="true" Index:name="class_other" Index:navn="klassif" Index:type="token" Index:boostFactor="5">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='080']">
							<Index:field Index:repeat="true" Index:name="class_other" Index:navn="klassif" Index:type="token" Index:boostFactor="5">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='082' or @tag='089']">
							<Index:field Index:repeat="true" Index:name="ddc_kw" Index:navn="ddc_kw" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='087']">
							<Index:field Index:repeat="true" Index:name="kl" Index:navn="kl" Index:type="token" Index:boostFactor="5">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='088']">
							<Index:field Index:repeat="true" Index:name="kl" Index:navn="kl" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='p']">
							<Index:field Index:repeat="true" Index:name="class_other" Index:navn="klassif" Index:type="token" Index:boostFactor="5">
								<xsl:for-each select="mc:subfield[@code='p']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='589']">
							<Index:field Index:repeat="true" Index:name="inst" Index:navn="inst" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='652' or @tag='653' or @tag='654' or @tag='655']">
							<Index:field Index:repeat="true" Index:name="dk" Index:navn="dk" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:if test="@code='m'">
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='p'">
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='v'">
										<xsl:text>:</xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='z'">
										<xsl:text>-</xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='a'">
										<xsl:text> </xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='h'">
										<xsl:text>, </xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='c'">
										<xsl:text>, </xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='e'">
										<xsl:text> </xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='f'">
										<xsl:text> (</xsl:text>
										<xsl:value-of select="." />
										<xsl:text>)</xsl:text>
									</xsl:if>
									<xsl:if test="@code='t'">
										<xsl:text>: </xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
									<xsl:if test="@code='b'">
										<xsl:text> </xsl:text>
										<xsl:value-of select="." />
									</xsl:if>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
					</Index:group> 
    </xsl:template>

</xsl:stylesheet>
