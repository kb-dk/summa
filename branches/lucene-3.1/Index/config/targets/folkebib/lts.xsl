<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="lts">


          <xsl:choose>
              <xsl:when test="mc:datafield[@tag='D00']">
                  <xsl:for-each select="mc:subfield[@code='e']">
                      <Index:field Index:repeat="false" Index:name="lts" Index:navn="lts" Index:type="keyword" Index:boostFactor="2">

                             <xsl:value-of select="."/>

                      </Index:field>

                  </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                     <xsl:for-each select="mc:datafield[@tag='300']/mc:subfield[@code='e']">
                      <Index:field Index:repeat="false" Index:name="lts" Index:navn="lts" Index:type="keyword" Index:boostFactor="2">

                             <xsl:value-of select="."/>

                      </Index:field>

                  </xsl:for-each>
              </xsl:otherwise>
          </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
