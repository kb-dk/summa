<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://statsbiblioteket.dk/summa/2008/Document"
                exclude-result-prefixes="doc">
    <!-- Simple (and lossy) transformation from SummaDocument to XML ready for
         ingesting into Solr (remember to wrap i <add>...</add> -->
    <xsl:output method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
    <xsl:param name="recordBase" select="'Undefined'" />

    <xsl:template match="doc:SummaDocument">
        <doc>
            <xsl:text>
            </xsl:text>
            <xsl:element name="field">
                <xsl:attribute name="name">recordId</xsl:attribute>
                <xsl:value-of select="@id"/>
            </xsl:element>
            <xsl:text>
            </xsl:text>
            <xsl:element name="field">
                <xsl:attribute name="name">recordBase</xsl:attribute>
                <xsl:value-of select="$recordBase" />
            </xsl:element>
                <xsl:text>
                </xsl:text>

            <xsl:for-each select="doc:fields/doc:field">
                <xsl:if test=". != '' and @name != 'recordBase' and @name != 'recordId'">
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:attribute>
                        <!--                        <xsl:attribute name="boost">
                            <xsl:value-of select="@doc:boost"/>
                        </xsl:attribute>-->
                        <xsl:value-of select="."/>
                    </xsl:element>
                    <xsl:text>
                    </xsl:text>
                </xsl:if>
            </xsl:for-each>
            <xsl:text>
            </xsl:text>
        </doc>
    </xsl:template>
</xsl:stylesheet>