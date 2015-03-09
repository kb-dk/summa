<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="lfo">



        <xsl:for-each select="mc:datafield[@tag='100' or @tag='B00' or @tag='700' or @tag='H00' or @tag='770' or @tag='H70']">
            <Index:field Index:repeat="false" Index:name="lfo" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_name_inverted"/>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lpe" Index:navn="lpe" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_name_inverted"/>
            </Index:field>
        </xsl:for-each>

        <xsl:for-each select="mc:datafield[@tag='110' or @tag='710'or @tag='780' or @tag='B10' or @tag='H10' or @tag='H80']">
            <Index:field Index:repeat="false" Index:name="lfo" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="corporate_name"/>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lko" Index:navn="lko" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="corporate_name"/>
            </Index:field>
        </xsl:for-each>

            <xsl:choose>
                <xsl:when test="mc:datafield[@tag='B00']">
                    <xsl:for-each select="mc:datafield[@tag='B00']">
                          <xsl:if test="not(substring(mc:subfield[@code='a'],1)='-')">
                        <Index:field Index:repeat="false" Index:name="lpo" Index:navn="lpo" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="person_name_inverted"/>
                        </Index:field>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="mc:datafield[@tag='100']">
                    <xsl:for-each select="mc:datafield[@tag='100']">

                        <Index:field Index:repeat="false" Index:name="lpo" Index:navn="lpo" Index:type="keyword" Index:boostFactor="10">
                            <xsl:call-template name="person_name_inverted"/>
                        </Index:field>
                    </xsl:for-each>
                </xsl:when>
            </xsl:choose>
        <xsl:choose>
                        <xsl:when test="mc:datafield[@tag='B10']">
                            <xsl:for-each select="mc:datafield[@tag='B10']">
                              <xsl:if test="not(substring(mc:subfield[@code='a'],1)='-')">
                                <Index:field Index:repeat="false" Index:name="lpo" Index:navn="lpo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="corporate_name"/>
            </Index:field>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="mc:datafield[@tag='110']">
                            <xsl:for-each select="mc:datafield[@tag='110']">

                               <Index:field Index:repeat="false" Index:name="lpo" Index:navn="lpo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="corporate_name"/>
            </Index:field>
        </xsl:for-each>

                        </xsl:when>
                    </xsl:choose>

      
        <xsl:for-each select="mc:datafield[@tag='239' or @tag='C39' or @tag='739' or @tag='H39']">
            <Index:field Index:repeat="false" Index:name="lfo" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_name_inverted"/>
            </Index:field>
        </xsl:for-each>
        <xsl:for-each select="mc:datafield[@tag='239' or @tag='C39']">
            <Index:field Index:repeat="false" Index:name="lpo" Index:navn="lpo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_name_inverted"/>
            </Index:field>
        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>
