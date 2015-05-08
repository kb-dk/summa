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
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchiv">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>

    <!-- ARBEJDE I GANG, DERFOR ER DER PT FEJL!!!!!! i DENNE TEMPLATE -->
    <!-- Author nomalized formaterer ikke navnet korrekt i ordinaer RSS 2.0 formatet, hvis forfatterne har mellemnavne -->

    <!-- Author -->
    <xsl:template name="author">

        <Index:group Index:name="au" Index:navn="fo">
            <xsl:for-each select="author">
                <xsl:call-template name="person">
                    <xsl:with-param name="names" select="."/>
                </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="dc:creator/text()">
                <xsl:call-template name="person_complex">
                    <xsl:with-param name="names" select="."/>
                </xsl:call-template>
            </xsl:for-each>
        </Index:group>

        <xsl:for-each select="author">

            <xsl:call-template name="person_inverted">
                <xsl:with-param name="names_inverted" select="."/>
            </xsl:call-template>

        </xsl:for-each>
        <xsl:for-each select="dc:creator/text()">
            <xsl:call-template name="person_inverted_complex">
                <xsl:with-param name="names" select="."  />
            </xsl:call-template>
        </xsl:for-each>

    </xsl:template>

    <!-- Rekursiv template for at hive flere forfattere ud af samme tag (Ordinaer RSS format)-->
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
            <xsl:when test="contains($names,';')">
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space(substring-before($names,';'))"/>
                </Index:field>
                <xsl:choose>
                    <xsl:when test="contains(substring-after($names,';'),';')">
                        <xsl:call-template name="person">
                            <xsl:with-param name="names" select="substring-after($names,';')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="normalize-space(substring-after($names,';'))"/>
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
        <xsl:choose>
            <xsl:when test="contains($names_inverted,', ')">
                <xsl:variable name="firstAuthor" select="substring-before($names_inverted,', ')"/>
                <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="substring-after($firstAuthor,' ')"  />
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="substring-before($firstAuthor,' ')"  />
                </Index:field>
                <xsl:call-template name="person_inverted">
                    <xsl:with-param name="names_inverted" select="substring-after($names_inverted,', ')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="contains($names_inverted,'; ')">
                <xsl:variable name="firstAuthor" select="substring-before($names_inverted,'; ')"/>
                <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="substring-after($firstAuthor,' ')"  />
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="substring-before($firstAuthor,' ')"  />
                </Index:field>
                <xsl:call-template name="person_inverted">
                    <xsl:with-param name="names_inverted" select="substring-after($names_inverted,'; ')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <Index:field Index:repeat="false" Index:name="author_normalised" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="substring-after($names_inverted,' ')"/>
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="substring-before($names_inverted,' ')"/>
                </Index:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- Rekursiv template for at hive flere forfattere ud af samme tag (komplekst RSS format)-->
    <xsl:template name="person_complex">
        <xsl:param name="names"/>
        <xsl:choose>
            <xsl:when test="contains($names,'.,')">
                <xsl:variable name="firstAuthor">
                    <xsl:value-of select="substring-before($names,'.,')"/>
                    <xsl:text>.</xsl:text>
                </xsl:variable>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space(substring-after($firstAuthor,','))"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="normalize-space(substring-before($firstAuthor,','))"/>
                </Index:field>
                <xsl:call-template name="person_complex" >
                    <xsl:with-param  name="names" select="substring-after($names,'.,')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space(substring-after($names,' '))"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="normalize-space(substring-before($names,' '))"/>
                </Index:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


      <!-- Rekursiv template for at hive flere forfattere ud af samme tag (komplekst RSS format)-->
    <xsl:template name="person_complex2">
        <xsl:param name="names"/>
        <xsl:choose>
            <xsl:when test="contains($names,',')">
                <xsl:variable name="firstAuthor">
                    <xsl:value-of select="substring-before($names,',')"/>
                </xsl:variable>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space(substring-after($firstAuthor,' '))"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="normalize-space(substring-before($firstAuthor,' '))"/>
                </Index:field>
                <xsl:call-template name="person_complex" >
                    <xsl:with-param  name="names" select="substring-after($names,',')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <Index:field Index:repeat="true" Index:name="author_person" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space(substring-after($names,' '))"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="normalize-space(substring-before($names,' '))"/>
                </Index:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Rekursiv template for at hive flere forfattere ud af samme tag (komplekst RSS format)-->
    <xsl:template name="person_inverted_complex">
        <xsl:param name="names" />
        <xsl:choose>
            <xsl:when test="contains($names,'.,')">
                <xsl:variable name="firstAuthor">
                    <xsl:value-of select="substring-before($names,'.,')"/>
                    <xsl:text>.</xsl:text>
                </xsl:variable>
                <Index:field Index:repeat="true" Index:name="author_normalized" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space($firstAuthor)"/>
                </Index:field>
                <xsl:call-template name="person_inverted_complex">
                    <xsl:with-param  name="names" select="substring-after($names,'.,')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <Index:field Index:repeat="true" Index:name="author_normalized" Index:navn="lfo" Index:type="keyword" Index:boostFactor="10">
                    <xsl:value-of select="normalize-space($names)"/>
                </Index:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



</xsl:stylesheet>