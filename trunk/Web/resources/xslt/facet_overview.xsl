<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:output method="html" encoding="UTF-8"/>

    <xsl:param name="query"/>

    <xsl:template match="/facetmodel">
        <xsl:if test="count(.//facet) &gt; 0">
            <div class="clusterRight">
                <h2>Limit your search</h2>
                <xsl:apply-templates />
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template match="facet">
        <div id="facet_{@name}">
            <xsl:value-of select="@name"/>
        </div>
        <div id="{@name}_{position()}" class="clusterOverview">
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="tag">
        <xsl:variable name="search_query">
            <xsl:value-of select="query" />
        </xsl:variable>
        <div id="tag_{position()}" class="clusterOverviewItem">
            <!-- <a href="?query=%28{$query}%29%20{util:urlEncode($search_query)}"> -->
            <a href="?query=%28{$query}%29%20{$search_query}">
                <xsl:value-of select="@name" />
            </a>
            (<xsl:value-of select="@addedobjects" />)
        </div>
    </xsl:template>

</xsl:stylesheet>