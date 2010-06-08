<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:IndexDescriptor="http://statsbiblioteket.dk/summa/2008/IndexDescriptor"
        exclude-result-prefixes="IndexDescriptor">
    <xsl:template match="/">
        <schema name="SummaToSolrExample" version="1.3">
            <xsl:text>
            </xsl:text>
            <fields>
                <xsl:text>
                </xsl:text>
                <xsl:for-each select="IndexDescriptor:IndexDescriptor/IndexDescriptor:fields/IndexDescriptor:field">
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:attribute>
                        <xsl:attribute name="type">
                            <xsl:value-of select="@parent"/>
                        </xsl:attribute>
                        <xsl:attribute name="indexed">
                            <xsl:value-of select="@indexed"/>
                        </xsl:attribute>
                        <xsl:attribute name="stored">
                            <xsl:value-of select="@stored"/>
                        </xsl:attribute>
                        <xsl:attribute name="required">
                            <xsl:value-of select="@required"/>
                        </xsl:attribute>
                    </xsl:element>
                    <xsl:text>
                    </xsl:text>                        
                </xsl:for-each>
            </fields>
            <xsl:text>
            </xsl:text>
        </schema>
    </xsl:template>
</xsl:stylesheet>