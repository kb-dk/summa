<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:purl="http://purl.org/rss/1.0/"
        xmlns:prism="http://purl.org/rss/1.0/modules/prism/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>



<!-- Identifiers -->
<!-- Fra det simple format hentes et libnk som identifier?? -->
<xsl:template name="identifiers">
    <Index:group Index:name="numbers" Index:navn="nr">
        <xsl:for-each select="link">
            <Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
    </Index:group>
    <Index:group Index:name="numbers" Index:navn="nr">
        <xsl:for-each select="../../rdf:RDF/purl:channel/prism:issn">
            <Index:field Index:repeat="true" Index:name="issn" Index:navn="in" Index:type="number">
                <xsl:value-of select="."/>
            </Index:field>
        </xsl:for-each>
    </Index:group>
</xsl:template>

</xsl:stylesheet>
