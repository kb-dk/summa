<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="lvp">
             <xsl:for-each select="mc:datafield[@tag='557' or @tag='F57']">
                    <Index:field Index:repeat="false" Index:name="lvp" Index:navn="lvp" Index:type="keyword" Index:boostFactor="4">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='æ']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:text> </xsl:text>
                        <xsl:for-each select="mc:subfield[@code='ø']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='v']">
                            <xsl:text> ; </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                         <xsl:for-each select="mc:subfield[@code='j']">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                             <xsl:text>)</xsl:text>
                        </xsl:for-each>
                    </Index:field>
                  <Index:field Index:repeat="false" Index:name="lvx" Index:navn="lvx" Index:type="keyword" Index:boostFactor="4">
                        <xsl:for-each select="mc:subfield[@code='a']">
                            <xsl:value-of select="translate(.,'¤','')"/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='æ']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:text> </xsl:text>
                        <xsl:for-each select="mc:subfield[@code='ø']">
                            <xsl:text> / </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                        <xsl:for-each select="mc:subfield[@code='b']">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:for-each>

                    </Index:field>
               <xsl:if test="contains(mc:subfield[@code='a'],'¤')">
                   <Index:field Index:repeat="false" Index:name="lvp" Index:navn="lvp" Index:type="keyword" Index:boostFactor="4">
                       <xsl:for-each select="mc:subfield[@code='a']">
                           <xsl:value-of select="substring-after(.,'¤')"/>
                       </xsl:for-each>
                       <xsl:for-each select="mc:subfield[@code='æ']">
                           <xsl:text> / </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                       <xsl:text> </xsl:text>
                       <xsl:for-each select="mc:subfield[@code='ø']">
                           <xsl:text> / </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                       <xsl:for-each select="mc:subfield[@code='b']">
                           <xsl:text> </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                       <xsl:for-each select="mc:subfield[@code='v']">
                           <xsl:text> ; </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                       <xsl:for-each select="mc:subfield[@code='j']">
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="."/>
                             <xsl:text>)</xsl:text>
                        </xsl:for-each>
                   </Index:field>
                   <Index:field Index:repeat="false" Index:name="lvx" Index:navn="lvx" Index:type="keyword" Index:boostFactor="4">
                       <xsl:for-each select="mc:subfield[@code='a']">
                           <xsl:value-of select="substring-after(.,'¤')"/>
                       </xsl:for-each>
                       <xsl:for-each select="mc:subfield[@code='æ']">
                           <xsl:text> / </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                       <xsl:text> </xsl:text>
                       <xsl:for-each select="mc:subfield[@code='ø']">
                           <xsl:text> / </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                       <xsl:for-each select="mc:subfield[@code='b']">
                           <xsl:text> </xsl:text>
                           <xsl:value-of select="."/>
                       </xsl:for-each>

                   </Index:field>

               </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="mc:datafield[@tag='558' or @tag='F58']/mc:subfield[@code='a']">
                   <Index:field Index:repeat="false" Index:name="lvp" Index:navn="lvp" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                     <Index:field Index:repeat="false" Index:name="lvx" Index:navn="lvx" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="translate(.,'¤','')"/>
                    </Index:field>
                    <xsl:if test="contains(.,'¤')">
                           <Index:field Index:repeat="false" Index:name="lvp" Index:navn="lvp" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="substring-after(.,'¤')"/>
                    </Index:field>
                        <Index:field Index:repeat="false" Index:name="lvx" Index:navn="lvx" Index:type="keyword" Index:boostFactor="4">
                        <xsl:value-of select="substring-after(.,'¤')"/>
                    </Index:field>
                    </xsl:if>
                </xsl:for-each>
    </xsl:template>


    </xsl:stylesheet>
