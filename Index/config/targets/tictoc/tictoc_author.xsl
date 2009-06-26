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

    <!-- ARBEJDE I GANG, DERFOR ER DER PT FEJL!!!!!! i DENNE TEMPLATE -->

    <!-- Author -->
    <xsl:template name="author">

        <Index:group Index:name="au" Index:navn="fo">
            <xsl:for-each select="author">
                <xsl:call-template name="person">
                     <xsl:with-param name="names" select="."/>
                </xsl:call-template>
            </xsl:for-each>
        </Index:group>

        <xsl:for-each select="author">
            <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                <xsl:call-template name="person_inverted">
                    <xsl:with-param name="names_inverted" select="."/>
                </xsl:call-template>
            </Index:field>
        </xsl:for-each>

    </xsl:template>

    <!-- Rekursiv template for at hive forfattere ud af samme tag-->
    <xsl:template name="person">
        <xsl:param name="names"/>
        <xsl:choose>
            <xsl:when test="contains($names,',')">
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space(substring-before($names,','))"/>
                </Index:field>
                    <xsl:choose>
                        <xsl:when test="contains(substring-after($names,','),',')">
                            <xsl:call-template name="person">
                                <xsl:with-param name="names" select="substring-after($names,',')"/>
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                            <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="normalize-space(substring-after($names,','))"/>
                            </Index:field>
                        </xsl:otherwise>
                    </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="person_inverted">
        <xsl:param name="names_inverted" />
  <!--       <xsl:param name="count" select="1" />
        <xsl:variable name="name1" />
        <xsl:choose>
            <xsl:when test="contains(substring-after($names_inverted,' '),' ')">
                <xsl:text>Indeholder 3 eller flere navne</xsl:text>
                <xsl:variable name="name1" select="substring-before($names_inverted,' ')"/>
                <xsl:call-template name="person_inverted">
                    <xsl:with-param name="names_inverted" select="substring-after($names_inverted,' ')"/>
                    <xsl:with-param name="count" select="$count+1"/>
               </xsl:call-template>
       -->        <!-- <xsl:value-of select="substring-before($names_inverted,' ')"/> -->
     <!--       </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="substring-after($names_inverted,' ')"/>
                <xsl:text>, </xsl:text>
                <xsl:value-of select="$name1"/>
                <xsl:value-of select="substring-before($names_inverted,' ')"/>
            </xsl:otherwise>
        </xsl:choose>
 -->   </xsl:template>    

</xsl:stylesheet>