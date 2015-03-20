<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="ar">


      <xsl:choose>
          <xsl:when test="mc:datafield[@tag='A06']">
          <xsl:for-each select="mc:datafield[@tag='A06']">
            <xsl:for-each select="mc.subfield">
                        <Index:field Index:repeat="true" Index:freetext="false" Index:name="ar" Index:navn="ar" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
                    </xsl:for-each>
                     </xsl:when>
          <xsl:otherwise>
                     <xsl:for-each select="mc:datafield[@tag='006']">
            <xsl:for-each select="mc.subfield">
                        <Index:field Index:repeat="true" Index:freetext="false" Index:name="ar" Index:navn="ar" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
                    </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
     <xsl:template name="bs">


         <xsl:choose>
             <xsl:when test="mc:datafield[@tag='A38']">
                 <xsl:for-each select="mc:datafield[@tag='A38']/mc:subfield[@code='a']">
                       <xsl:call-template name="børn"/>
                 </xsl:for-each>
             </xsl:when>
             <xsl:otherwise>
                      <xsl:for-each select="mc:datafield[@tag='038']/mc:subfield[@code='a']">
                             <xsl:call-template name="børn"/>
                    </xsl:for-each>
             </xsl:otherwise>
         </xsl:choose>
    </xsl:template>
    <xsl:template name="børn">
    <xsl:for-each select=".">
                        <Index:field Index:repeat="true" Index:name="bs" Index:navn="bs" Index:type="token" Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                <xsl:if test="contains(.,'bi')">
                      <Index:field Index:repeat="true" Index:name="bs" Index:navn="bs" Index:type="token" Index:freetext="false">
							<xsl:text>billedbog</xsl:text>
						</Index:field>
                </xsl:if>
                 <xsl:if test="contains(.,'te')">
                      <Index:field Index:repeat="true" Index:name="bs" Index:navn="bs" Index:type="token" Index:freetext="false">
							<xsl:text>tegneserie</xsl:text>
						</Index:field>
                </xsl:if>
                 <xsl:if test="contains(.,'dr')">
                      <Index:field Index:repeat="true" Index:name="bs" Index:navn="bs" Index:type="token" Index:freetext="false">
							<xsl:text>dramatik</xsl:text>
						</Index:field>
                </xsl:if>
            </xsl:for-each>
    </xsl:template>
    <xsl:template name="fg">



        <xsl:for-each select="mc:datafield[@tag='044' or @tag='A44']/mc:subfield">
                       <Index:field Index:repeat="true" Index:name="fg"  Index:freetext="false" Index:navn="fg" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="ip">



        <xsl:for-each select="mc:datafield[@tag='545' or @tag='F45']/mc:subfield[@code='a' or @code='x' or @code='z']">
                       <Index:field Index:repeat="true" Index:name="ip"  Index:navn="ip" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="ix">



        <xsl:for-each select="mc:datafield[@tag='042' or @tag='A42']/mc:subfield[@code='a']">
                       <Index:field Index:repeat="true" Index:name="ix"  Index:navn="ix" Index:type="token" Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="ka">



        <xsl:for-each select="mc:datafield[@tag='990' or @tag='K90']/mc:subfield[@code='b']">
                       <Index:field Index:repeat="true" Index:name="ka"  Index:navn="ka" Index:type="token" Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="kg">



        <xsl:for-each select="mc:datafield[@tag='990' or @tag='K90']/mc:subfield[@code='c']">
                       <Index:field Index:repeat="true" Index:name="kg"  Index:navn="kg" Index:type="token" Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="kk">
           <xsl:for-each select="mc:datafield[@tag='032' or @tag='A32']/mc:subfield[@code='a' or @code='x']">
                       <Index:field Index:repeat="true" Index:name="kk"  Index:navn="kk" Index:type="token" Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>


        <xsl:for-each select="mc:datafield[@tag='990' or @tag='K90']/mc:subfield[@code='a']">
                       <Index:field Index:repeat="true" Index:name="kk"  Index:navn="kk" Index:type="token" Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>

    <xsl:template name="sf">



        <xsl:for-each select="mc:datafield[@tag='990' or @tag='K90']/mc:subfield[@code='b' or @code='c' or @code='o']">
                       <Index:field Index:repeat="true" Index:name="sf"  Index:navn="sf" Index:type="token"  Index:freetext="false">
							<xsl:value-of select="."/>
						</Index:field>
                </xsl:for-each>
    </xsl:template>
    <xsl:template name="sp">



                    <xsl:for-each select="mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='l']">
						<Index:field Index:repeat="true" Index:name="sp" Index:navn="sp" Index:type="token" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='041' or @tag='A41']/mc:subfield[@code='a' or @code='p' or @code='u' or @code='e' or @code='d']">
						<Index:field Index:repeat="true" Index:name="sp" Index:navn="sp" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>

                    <xsl:for-each select="mc:datafield[@tag='008' or @tag='A08']/mc:subfield[@code='l']">
						<Index:field Index:repeat="false" Index:name="lsp" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='041' or @tag='A41']/mc:subfield[@code='a' or @code='p' or @code='u']">
						<Index:field Index:repeat="false" Index:name="lsp" Index:navn="lsp" Index:type="keyword" Index:boostFactor="5">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='041' or @tag='A41']/mc:subfield[@code='d' or @code='e']">
						<Index:field Index:repeat="false" Index:name="lsp" Index:navn="lsp" Index:type="keyword" Index:boostFactor="2">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
    </xsl:template>
      <xsl:template name="ou">

        <xsl:for-each select="mc:datafield[@tag='041' or @tag='A41']/mc:subfield[@code='c' or @code='b']">
						<Index:field Index:repeat="true" Index:name="ou" Index:navn="ou" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
    </xsl:template>
    
</xsl:stylesheet>
