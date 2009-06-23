<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0" xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>


    <!-- Title -->
    <xsl:template name="title">

        <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
            <xsl:for-each select="title">
                <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>

        </Index:group>
        <Index:field Index:name="sort_title" Index:sortLocale="da" Index:navn="sort_titel" Index:type="keyword" Index:boostFactor="6">

            <xsl:for-each select="title [position()=1]">
                <xsl:choose>
                    <xsl:when test="starts-with(.,'The ') and (starts-with(../../language,'en'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'The ') and (starts-with(../../language,'En'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'A ') and (starts-with(../../language,'en'))">
                        <xsl:value-of select="substring(.,3)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'A ') and (starts-with(../../language,'En'))">
                        <xsl:value-of select="substring(.,3)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'An ') and (starts-with(../../language,'en'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'An ') and (starts-with(../../language,'En'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'La ') and (starts-with(../../language,'fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'La ') and (starts-with(../../language,'Fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Le ') and (starts-with(../../language,'fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Le ') and (starts-with(../../language,'Fr'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Les ') and (starts-with(../../language,'fr'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Les ') and (starts-with(../../language,'Fr'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Der ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Der ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Der ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Die ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Die ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Die ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Das ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Das ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Das ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Ein ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Ein ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Ein ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Eine ') and (contains(../../language,'de'))">
                        <xsl:value-of select="substring(.,6)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Eine ') and (starts-with(../../language,'ger'))">
                        <xsl:value-of select="substring(.,6)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Eine ') and (starts-with(../../language,'Ger'))">
                        <xsl:value-of select="substring(.,6)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Las ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Las ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Las ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Los ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Los ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Los ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Un ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Un ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Un ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,4)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Una ') and (contains(../../language,'es'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Una ') and (starts-with(../../language,'spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:when test="starts-with(.,'Una ') and (starts-with(../../language,'Spa'))">
                        <xsl:value-of select="substring(.,5)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>

        </Index:field>

    </xsl:template>

</xsl:stylesheet>