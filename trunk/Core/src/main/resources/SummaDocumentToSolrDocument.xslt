<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://statsbiblioteket.dk/summa/2008/Document"
                exclude-result-prefixes="doc">
    <!-- Simple (and lossy) transformation from SummaDocument to XML ready for
         ingesting into Solr (remember to wrap i <add>...</add> -->
    <xsl:output method="xml" encoding="UTF-8" omit-xml-declaration="yes"/>
    <xsl:param name="recordBase" select="'Undefined'" />
    <xsl:param name="recordID" select="'Undefined'" />

    <xsl:template match="doc:SummaDocument">
        <doc>
            <xsl:text>
            </xsl:text>
            <xsl:element name="field">
                <xsl:attribute name="name">recordID</xsl:attribute>
                <!--    <xsl:value-of select="@id"/> -->
                <!-- There is always a recordId != '', guaranteed by Record -->
                <xsl:value-of select="$recordID"/>
            </xsl:element>
            <xsl:text>
            </xsl:text>
            <xsl:element name="field">
                <xsl:attribute name="name">recordBase</xsl:attribute>
                <xsl:value-of select="$recordBase" />
            </xsl:element>
            <!-- We need to prefix attributes in order for the XMLTransformer to work in production, but not for
                 unit-tests to run. Why is that different? XSLTProc also requires prefixed attributes. -->
            <xsl:for-each select="doc:fields/doc:field">
                <xsl:if test=". != '' and @doc:name != 'recordBase' and @doc:name != 'recordID'">
                    <xsl:text>
                    </xsl:text>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:value-of select="@doc:name"/>
                        </xsl:attribute>
                        <xsl:if test="@doc:boost != ''">
                            <xsl:attribute name="boost">
                                <xsl:value-of select="@doc:boost"/>
                            </xsl:attribute>
                        </xsl:if>
                        <!--                        <xsl:attribute name="boost">
                            <xsl:value-of select="@doc:boost"/>
                        </xsl:attribute>-->
                        <xsl:value-of select="."/>
                    </xsl:element>
                </xsl:if>
            </xsl:for-each>
            <xsl:text>
            </xsl:text>
        </doc>
    </xsl:template>
</xsl:stylesheet>