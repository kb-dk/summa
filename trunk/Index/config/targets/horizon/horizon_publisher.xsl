<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl"
                version="1.0">

    <xsl:template name="publication_data">

        <xsl:for-each select="mc:datafield[@tag='247' or @tag='248' or @tag='557' or @tag='558']/mc:subfield[@code='i']">
            <Index:field Index:repeat="true" Index:name="pu" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='260']/mc:subfield[@code='b' or @code='g' or @code='p']">
            <Index:field Index:repeat="true" Index:name="pu" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='f']">
            <Index:field Index:repeat="true" Index:name="pu" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='260']/mc:subfield[@code='c']">
						<Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="4">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z']">
						<Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="4">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='247' or @tag='248' or @tag='557' or @tag='558']/mc:subfield[@code='j']">
						<Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="4">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='521']/mc:subfield[@code='c']">
						<Index:field Index:repeat="true" Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="4">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>


					<xsl:for-each select="mc:datafield[@tag='260']/mc:subfield[@code='a' or @code='f']">
						<Index:field Index:repeat="true" Index:name="place" Index:navn="pu" Index:type="token">
							<xsl:for-each select="mc:subfield">
								<xsl:value-of select="."/>
								<xsl:text> </xsl:text>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='247' or @tag='248' or @tag='557' or @tag='558']/mc:subfield[@code='h']">
						<Index:field Index:repeat="true" Index:name="place" Index:navn="pu" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>

    </xsl:template>
</xsl:stylesheet>