<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://statsbiblioteket.dk/summa/2008/Document"
                exclude-result-prefixes="doc">
    <!-- Simple (and lossy) transformation from SummaDocument to XML ready for
         ingesting into Solr -->
    <xsl:output method="xml" encoding="UTF-8"/>
    <xsl:template match="doc:SummaDocument">
        <add>
            <doc>
            <xsl:text>
            </xsl:text>
                <xsl:element name="field">
                    <xsl:attribute name="name">id</xsl:attribute>
                    <xsl:value-of select="@doc:id"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>
                <xsl:element name="field">
                    <xsl:attribute name="name">recordId</xsl:attribute>
                    <xsl:value-of select="@doc:id"/>
                </xsl:element>
                <xsl:text>
                </xsl:text>

                <xsl:for-each select="doc:fields/doc:field">
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:value-of select="@doc:name"/>
                        </xsl:attribute>
<!--                        <xsl:attribute name="boost">
                            <xsl:value-of select="@doc:boost"/>
                        </xsl:attribute>-->
                        <xsl:value-of select="."/>
                    </xsl:element>
                    <xsl:text>
                    </xsl:text>
                </xsl:for-each>
            <xsl:text>
            </xsl:text>
            </doc>
        </add>
    </xsl:template>
</xsl:stylesheet>