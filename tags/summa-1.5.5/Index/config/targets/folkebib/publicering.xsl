<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="fl">

        <xsl:for-each select="mc:datafield[@tag='247' or @tag='C27' or @tag='248' or @tag='C48' or @tag='557' or @tag='F57' or @tag='558'or @tag='F58']/mc:subfield[@code='i']">
            <Index:field Index:repeat="true" Index:name="fl" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='260' or @tag='C60']/mc:subfield[@code='b' or @code='g' or @code='p']">
            <Index:field Index:repeat="true" Index:name="fl" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='538' or @tag='F38']/mc:subfield[@code='f']">
            <Index:field Index:repeat="true" Index:name="fl" Index:navn="fl" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>



    </xsl:template>
    <xsl:template name="pu">


					<xsl:for-each select="mc:datafield[@tag='260' or @tag='C60']/mc:subfield[@code='a' or @code='f']">
						<Index:field Index:repeat="true" Index:name="pu" Index:navn="pu" Index:type="token">
							<xsl:for-each select="mc:subfield">
								<xsl:value-of select="."/>
								<xsl:text> </xsl:text>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='247' or @tag='C27' or @tag='248' or @tag='C48' or @tag='557' or @tag='F57' or @tag='558' or @tag='F58']/mc:subfield[@code='h']">
						<Index:field Index:repeat="true" Index:name="pu" Index:navn="pu" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>

    </xsl:template>
    <xsl:template name="ul">
                           <xsl:for-each select="mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='b']">
            <Index:field Index:repeat="true" Index:name="ul" Index:navn="ul" Index:type="token" Index:boostFactor="4">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>

    </xsl:template>
    <xsl:template name="ww">

        <xsl:for-each select="mc:datafield[@tag='532' or @tag='F32' or @tag='534' or @tag='F34'
        or @tag='559' or @tag='F59' or @tag='856' or @tag='I56'
        or @tag='860' or @tag='I60' or @tag='861' or @tag='I61' or @tag='863' or @tag='I63'
        or @tag='865' or @tag='I65' or @tag='866' or @tag='I66' or @tag='867' or @tag='I67'
        or @tag='868' or @tag='I68' or @tag='870' or @tag='I70' or @tag='871' or @tag='I71'
        or @tag='873' or @tag='I73' or @tag='874' or @tag='I74' or @tag='879' or @tag='I79' ]/mc:subfield[@code='u']">
						<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token" Index:boostFactor="4">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
                   <xsl:for-each select="mc:datafield[@tag='247' or @tag='C47' or @tag='248' or @tag='C48'
        or @tag='512' or @tag='F12' or @tag='520' or @tag='F20'
        or @tag='523' or @tag='F23' or @tag='526' or @tag='F26'  or @tag='520' or @tag='F29'
        or @tag='530' or @tag='F30']/mc:subfield[@code='u']">
						<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='856']/mc:subfield[@code='a' or @code='b']">
						<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
    </xsl:template>
    <xsl:template name="aar">



        <xsl:for-each select="mc:datafield[@tag='260' or @tag='C60']/mc:subfield[@code='c']">
						<Index:field Index:repeat="true" Index:name="år" Index:navn="år" Index:type="token" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='a' or @code='z']">
						<Index:field Index:repeat="true" Index:name="år" Index:navn="år" Index:type="token" Index:boostFactor="4">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='247' or @tag='C47' or @tag='248' or @tag='C48' or @tag='557' or @tag='F57' or @tag='558' or @tag='F58']/mc:subfield[@code='j']">
						<Index:field Index:repeat="true" Index:name="år" Index:navn="år" Index:type="token" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='521' or @tag='F21']/mc:subfield[@code='c']">
						<Index:field Index:repeat="true" Index:name="år" Index:navn="år" Index:type="token" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>



    </xsl:template>


</xsl:stylesheet>
