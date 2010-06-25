<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                version="1.0"
                exclude-result-prefixes="xs xsl">

    <!-- Creates a dummy XML document with the contents of the source.
    -->
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <xsl:for-each select="//text()">
            <xsl:value-of select="."/>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
