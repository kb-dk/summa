<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:template name="lnm">
        <xsl:choose>
            <xsl:when test="/mc:record/mc:datafield[@tag='A05']">
                <xsl:for-each select="mc:datafield[@tag='A05']/mc:subfield[@code='k']">

                    <xsl:call-template name="musikopst_lnm"/>

                </xsl:for-each>

            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="mc:datafield[@tag='005']/mc:subfield[@code='k']">

                    <xsl:call-template name="musikopst_lnm"/>


                </xsl:for-each>

            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="musikopst_lnm">

        <xsl:if test="contains(.,'a')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>nd</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>noder</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'b')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>bc</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>becifring</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'c')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>so</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>solmisation</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'d')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>bc</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>tekst med becifring</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'e')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>tl</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>tal</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'f')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>bg</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>bogstaver</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'g')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>ak</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>akkorddiagrammer (strengeinstrumenter)</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'h')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>ak</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>akkorddiagrammer (klaviaturinstrumenter)</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'i')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>sd</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>spillediagrammer</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'j')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>tu</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>tabulatur</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'k')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>ne</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>neumer</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'l')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>me</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>mensuralnotation</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'m')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>gr</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>grafisk/optisk notation</xsl:text>
            </Index:field>
        </xsl:if>
        <xsl:if test="contains(.,'z')">
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>nx</xsl:text>
            </Index:field>
            <Index:field Index:repeat="false" Index:name="lnm" Index:navn="lnm" Index:type="keyword">
                <xsl:text>andre notationsformer</xsl:text>
            </Index:field>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
