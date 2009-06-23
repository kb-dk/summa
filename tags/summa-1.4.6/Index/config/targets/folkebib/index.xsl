<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:include href="short_format.xsl" />
    <xsl:include href="lfo.xsl" />
     <xsl:include href="fo.xsl" />
     <xsl:include href="lti.xsl" />
    <xsl:include href="ti.xsl" />
     <xsl:include href="vp.xsl" />
      <xsl:include href="lem.xsl" />
    <xsl:include href="em.xsl" />
    <xsl:include href="publicering.xsl" />
     <xsl:include href="rt.xsl" />
        <xsl:include href="lrt.xsl" />
     <xsl:include href="lcl.xsl" />
     <xsl:include href="cl.xsl" />
   <xsl:include href="nr.xsl" />
     <xsl:include href="lma.xsl" />
    <xsl:include href="ma.xsl" />
    <xsl:include href="kodede.xsl" />
    <xsl:include href="andre.xsl" />


     <xsl:include href="lmo.xsl" />
      <xsl:include href="lnm.xsl" />

    <xsl:include href="lts.xsl" />
     <xsl:include href="lvp.xsl" />

    <xsl:include href="sort.xsl" />



    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
		<Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="silkeborg">
			<xsl:attribute name="Index:id">
				<xsl:value-of select="mc:record/mc:datafield[@tag='001']/mc:subfield[@code='a']" />
			</xsl:attribute>


            <xsl:for-each select="mc:record">
				<Index:fields>

    <xsl:call-template name="shortformat" />
                     <xsl:call-template name="em" />
                    <xsl:call-template name="fo" />
                      <xsl:call-template name="cl" />
                        <xsl:call-template name="ar" />
                     <xsl:call-template name="bs" />
                     <xsl:call-template name="fg" />
                    <xsl:call-template name="ip" />
                    <xsl:call-template name="ix" />
                       <xsl:call-template name="ka" />
                     <xsl:call-template name="kg" />
                    <xsl:call-template name="kk" />
                       <xsl:call-template name="ma" />
                     <xsl:call-template name="sf" />
                     <xsl:call-template name="sp" />

                          <xsl:call-template name="fl" />
                     <xsl:call-template name="ou" />
                     <xsl:call-template name="pu" />
                    <xsl:call-template name="ul" />
                    <xsl:call-template name="ww" />
                     <xsl:call-template name="aar" />
                        <xsl:call-template name="rt" />

                    <xsl:call-template name="ti" />
                         <xsl:call-template name="vp" />
                      <xsl:call-template name="aj" />
                     <xsl:call-template name="km" />
                    <xsl:call-template name="mo" />
                     <xsl:call-template name="nm" />
                        <xsl:call-template name="no" />
                        <xsl:call-template name="op" />
                       <xsl:call-template name="ts" />
                           <xsl:call-template name="lem" />

                      <xsl:call-template name="lfo" />
                     <xsl:call-template name="lcl" />
                      <xsl:call-template name="lma" />
                        <xsl:call-template name="lrt" />
                           <xsl:call-template name="lti" />
                               <xsl:call-template name="lvp" />
                    <xsl:call-template name="other" />
                         <xsl:call-template name="lmo" />
                     <xsl:call-template name="lnm" />

                       <xsl:call-template name="lts" />
                        <xsl:call-template name="sort" />
                          <xsl:call-template name="nr" />


    <xsl:for-each select=".">
        <Index:field Index:repeat="false" Index:name="base" Index:navn="base" Index:type="keyword"  Index:freetext="false">silkeborg</Index:field>
        
                      </xsl:for-each>


				</Index:fields>
			</xsl:for-each>
		</Index:document>
	</xsl:template>
</xsl:stylesheet>
