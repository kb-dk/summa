<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">
    <xsl:template name="lcl">
        <Index:group Index:suggest="true" Index:navn="lcl" Index:name="lcl">

						<xsl:for-each select="mc:datafield[@tag='050']">
							<Index:field Index:repeat="false" Index:name="llcc" Index:navn="llcc" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='060']">
							<Index:field Index:repeat="false" Index:name="lnlm" Index:navn="lnlm" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='070']">
							<Index:field Index:repeat="false" Index:name="ldbk" Index:navn="ldbk" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='080']">
							<Index:field Index:repeat="false" Index:name="ludk" Index:navn="ludk" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='082' or @tag='089']">
							<Index:field Index:repeat="false" Index:name="ldcc" Index:navn="lddc" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='087']">
							<Index:field Index:repeat="false" Index:name="lkl" Index:navn="lkl" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='088']">
							<Index:field Index:repeat="false" Index:name="lkl" Index:navn="lkl" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='p']">
							<Index:field Index:repeat="false" Index:name="lfn" Index:navn="lfn" Index:type="keyword" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						 <xsl:for-each select="mc:datafield[@tag='V03']/mc:subfield[@code='a']">
														
	<Index:field Index:repeat="true" Index:name="linst" Index:navn="linst" Index:type="keyword" Index:boostFactor="10">
											<xsl:text>Mat </xsl:text>
											<xsl:value-of select="."/>
							
								
							</Index:field>
							
						</xsl:for-each>
						
						
						<xsl:for-each select="mc:datafield[@tag='V05']">
	
								<xsl:for-each select="mc:subfield">
														<Index:field Index:repeat="false" Index:name="linst" Index:navn="linst" Index:type="keyword" Index:boostFactor="10">
								<xsl:text>F </xsl:text>
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
										</Index:field>
								</xsl:for-each>
						
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='V07']">
							<xsl:for-each select="mc:subfield">
						<Index:field Index:repeat="false" Index:name="linst" Index:navn="linst" Index:type="keyword" Index:boostFactor="10">
						
									<xsl:text>K </xsl:text>
									<xsl:value-of select="."/>
									<xsl:text> </xsl:text>
						
							</Index:field>
									</xsl:for-each>
						</xsl:for-each>
						
						<xsl:for-each select="mc:datafield[@tag='652' or @tag='653' or @tag='654' or @tag='655']">
							<Index:field Index:repeat="false" Index:name="ldk5" Index:navn="ldk5" Index:type="keyword" Index:boostFactor="10">
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
