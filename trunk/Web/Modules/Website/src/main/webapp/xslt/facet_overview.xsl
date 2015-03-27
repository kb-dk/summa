<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="html" encoding="UTF-8"/>

    <xsl:param name="filter"/>
    <xsl:param name="query"/>

    <xsl:template match="*">
        <xsl:for-each select=".">
            <xsl:call-template name="facet_response" />
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="facet_response">
        <xsl:for-each select="response/facetmodel">
            <xsl:choose>
                <xsl:when test="count(.//facet) &gt; 0">
                    <h2>Limit your search</h2>
                    <xsl:for-each select="facet">
                        <xsl:call-template name="facet" />
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    No facets found
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="facet">
        <div id="facet_{@name}">
            <xsl:value-of select="@name"/>
        </div>
        <div id="{@name}_{position()}" class="clusterOverview">
            <xsl:for-each select="tag">
                <xsl:call-template name="tag" />
            </xsl:for-each>
        </div>
    </xsl:template>

    <xsl:template name="tag">
        <xsl:variable name="facet_query">
            <xsl:value-of select="query" />
        </xsl:variable>
        <div id="tag_{position()}" class="clusterOverviewItem">
            <!-- <a href="?query=%28{$query}%29%20{util:urlEncode($search_query)}"> -->
            <a href="?filter={$filter}{$facet_query}&amp;query={$query}">
                <xsl:value-of select="@name" />
            </a>
            (<xsl:value-of select="@addedobjects" />)

        </div>
    </xsl:template>

</xsl:stylesheet>