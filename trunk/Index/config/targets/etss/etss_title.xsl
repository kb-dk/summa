<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl"
		version="1.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchive.org">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="title">
			
								<Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
									<xsl:for-each select="title">
										<Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
											
											<xsl:value-of select="."/>
										
										</Index:field>
                                         <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                                                <xsl:value-of select="."/>

										</Index:field>

                                    </xsl:for-each>
                                    <xsl:for-each select="acronym_title">
                                       
                                         <Index:field Index:repeat="true" Index:name="title" Index:navn="titel" Index:type="token" Index:boostFactor="10"  Index:suggest="true">
                                                <xsl:value-of select="."/>

                                        </Index:field>

                                    </xsl:for-each>

								</Index:group>
									<Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="200">
                                      <xsl:variable name="title_without_differentiation">
                            <xsl:for-each select="title [position()=1]">
                                <xsl:choose>
                                    <xsl:when test="contains(.,')')">
                                        <xsl:value-of select="substring-before(.,'(')"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>
                        </xsl:variable>
                        <xsl:variable name="title_differentiation">
                             <xsl:for-each select="title [position()=1]">
                            <xsl:value-of select="substring-after(translate(.,')',''),'(')"/>
                            </xsl:for-each>
                        </xsl:variable>

                        <xsl:variable name="title">
                            <xsl:for-each select="title [position()=1]">
                                <xsl:choose>
                                    <xsl:when test="starts-with(.,'The ')">
                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>
                                    <xsl:when test="starts-with(.,'A ')">
                                             <xsl:value-of select="substring($title_without_differentiation,3)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'An ')">

                                        <xsl:value-of select="substring($title_without_differentiation,4)"/>
                                    </xsl:when>
                                    <xsl:when test="starts-with(.,'La ')">

                                        <xsl:value-of select="substring($title_without_differentiation,4)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Le ')">

                                        <xsl:value-of select="substring($title_without_differentiation,4)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Les ')">

                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Der ')">

                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Die ')">

                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Das ')">

                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>


                                    <xsl:when test="starts-with(.,'Las ')">

                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Los ')">

                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Un ')">

                                        <xsl:value-of select="substring($title_without_differentiation,4)"/>
                                    </xsl:when>

                                    <xsl:when test="starts-with(.,'Una ')">
                                        
                                        <xsl:value-of select="substring($title_without_differentiation,5)"/>
                                    </xsl:when>

                                    <xsl:otherwise>
                                        <xsl:value-of select="$title_without_differentiation"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>


                        </xsl:variable>
									<xsl:value-of select="normalize-space($title)"/>

                                    </Index:field>
						
	</xsl:template>
    <xsl:template name="title_prefix">
        <xsl:choose>
            <xsl:when test="starts-with(.,'The ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>
            <xsl:when test="starts-with(.,'A ')">
                <xsl:value-of select="substring(.,3)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'An ')">
                <xsl:value-of select="substring(.,4)"/>
            </xsl:when>
            <xsl:when test="starts-with(.,'La ')">
                <xsl:value-of select="substring(.,4)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Le ')">
                <xsl:value-of select="substring(.,4)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Les ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Der ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Die ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Das ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>


            <xsl:when test="starts-with(.,'Las ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Los ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Un ')">
                <xsl:value-of select="substring(.,4)"/>
            </xsl:when>

            <xsl:when test="starts-with(.,'Una ')">
                <xsl:value-of select="substring(.,5)"/>
            </xsl:when>

            <xsl:otherwise>
                <xsl:value-of select="."/>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
</xsl:stylesheet>

