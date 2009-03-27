<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
		exclude-result-prefixes="java xs xalan xsl" version="1.0">
  
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
		<Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="https://sedna.statsbiblioteket.dk:8280/urn/" >
			<xsl:attribute name="Index:id">
				<xsl:value-of select="mdcs/id"/>
			</xsl:attribute>
			<xsl:for-each select="mdcs">
                <Index:fields>
                    <Index:group Index:name="ti" Index:navn="ti" Index:suggest="true">
                        <xsl:for-each select="title">
                            <Index:field Index:repeat="true" Index:name="main_titel" Index:navn="ht" Index:type="token" Index:boostFactor="10">
                                <xsl:value-of select="mainTitle"/>
                            </Index:field>
                            <Index:field Index:repeat="true" Index:name="se" Index:navn="se" Index:type="token" Index:boostFactor="8">
                                <xsl:value-of select="alternativeTitle" />
                            </Index:field>
                        </xsl:for-each>
					</Index:group>

                    <Index:group Index:name="au" Index:navn="fo" Index:suggest="false">
                        <Index:field Index:name="author_person" Index:repeat="true" Index:navn="pe" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="contributor[@role='Tekniske arbejder']" />
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                             <xsl:value-of select="contributor[@role='Producent']" />
                        </Index:field>
                        <Index:field Index:repeat="true" Index:name="author_corporation" Index:navn="ko" Index:type="token" Index:boostFactor="10">
                             <xsl:value-of select="contributor[@role='Bureau']" />
                        </Index:field>
                    </Index:group>

                    <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                             <xsl:text>film</xsl:text>
                    </Index:field>
                    <Index:field Index:repeat="true" Index:name="lma_long" Index:navn="lma_lang" Index:type="keyword">
                             <xsl:text>e_film</xsl:text>
                     </Index:field>

                    <Index:group Index:name="ma" Index:navn="ma" Index:freetext="false">
                             <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                  <xsl:text>fi</xsl:text>
                             </Index:field>
                             <Index:field Index:repeat="true" Index:name="ma_short" Index:navn="ma_kort" Index:type="token" Index:boostFactor="4">
                                  <xsl:text>xe</xsl:text>
                             </Index:field>
                    </Index:group>


                    <Index:group Index:name="numbers" Index:navn="nr">
                        <Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
                                <xsl:value-of select="id"/>
                        </Index:field>
                                <Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
                                     <xsl:text>https://sedna.statsbiblioteket.dk:8280/urn/</xsl:text><xsl:value-of select="id" />
                        </Index:field>
				   </Index:group>

                   <Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
                       <xsl:text>dan</xsl:text>
                   </Index:field>

                   <Index:group Index:name="su" Index:navn="em" Index:suggest="true">
                       <Index:field Index:repeat="false" Index:name="su_dk" Index:navn="ldb" Index:type="keyword" Index:boostFactor="10">
                                <xsl:text>reklamefilm</xsl:text>
                            </Index:field>
                       <xsl:for-each select="subject/keyword">
                            <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                                <xsl:value-of select="."/>
                            </Index:field>
                        </xsl:for-each>
                        <Index:field Index:repeat="true" Index:name="subject_other" Index:navn="uk" Index:type="token" Index:boostFactor="6">
                            <xsl:value-of select="subject/productName"/>
                        </Index:field>


                            <xsl:for-each select="subject/restrictedTerm">
                                <Index:field Index:repeat="true" Index:type="keyword" Index:name="commercials_subject" Index:navn="reklamefilm_subj" Index:boostFactor="6">
                                    <xsl:value-of select="./text()" />
                               </Index:field>
                              <!--  <xsl:for-each select="childTerm">
                                    <Index:field Index:repeat="true" Index:type="keyword" Index:name="commercials_subject" Index:navn="reklamefilm_subj" Index:boostFactor="6">
                                        <xsl:value-of select="../text()"/><xsl:text> : </xsl:text><xsl:value-of select="./text()" />
                                    </Index:field>
                                </xsl:for-each>  -->
                            </xsl:for-each>
                       
                    </Index:group>

                    <Index:field Index:repeat="true" Index:name="py" Index:navn="år"  Index:type="token" Index:boostFactor="2">
						 <xsl:value-of select="censor"/>
					</Index:field>
                    <Index:field Index:repeat="true" Index:name="py" Index:navn="år"  Index:type="token" Index:boostFactor="2">
						 <xsl:value-of select="premiereDate"/>
					</Index:field>

                    <Index:field Index:repeat="true" Index:name="rt" Index:navn="rt" Index:type="token">
						<xsl:value-of select="description"/>
				    </Index:field>

                    <Index:field Index:name="shortformat" Index:type="stored" Index:freetext="false">
						<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
						<shortrecord>
							<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
								<rdf:Description rdf:about="http://www.ilrt.bristol.ac.uk/people/cmdjb/">
                                        <dc:title>
																				<xsl:choose>
																					<xsl:when test="substring(title/mainTitle,0)!=''">
																				<xsl:value-of select="title/mainTitle"/>
																				<xsl:if test="substring(title/alternativeTitle,0)!=''">
																				<xsl:text>&#32;:&#32;</xsl:text>
																				<xsl:value-of select="title/alternativeTitle"/>
																				</xsl:if>
																				</xsl:when>
																				<xsl:when test="substring(subject/productName,0)!=''">
																				<xsl:value-of select="subject/productName"/>
																				<xsl:if test="substring(title/alternativeTitle,0)!=''">
																				<xsl:text>:&#32;</xsl:text>
																				<xsl:value-of select="title/alternativeTitle"/>
																				</xsl:if>
																				</xsl:when>
																				
																				<xsl:when test="substring(title/alternativeTitle,0)!=''">
																				<xsl:value-of select="title/alternativeTitle"/>
																				</xsl:when>
																				<xsl:otherwise>
																				<xml:text xml:lang="da">Uden titel</xml:text>
																				<xml:text xml:lang="en">No title</xml:text>
																				</xsl:otherwise>
																		
																				</xsl:choose>
																				</dc:title>
                                        <dc:creator><xsl:value-of select="contributor[@role='Producent']"/></dc:creator>
                                        <dc:date><xsl:value-of select="premiereDate"/></dc:date>
										<dc:type xml:lang="da">reklamefilm</dc:type>
										<dc:type xml:lang="en">commercial</dc:type>
                                        <dc:identifier><xsl:text>https://sedna.statsbiblioteket.dk:8280/urn/</xsl:text><xsl:value-of select="id" /></dc:identifier>
                                     	<dc:format>todo</dc:format>

                                </rdf:Description>
							</rdf:RDF>
						</shortrecord>
						<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
		      </Index:field>

                    <Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="1">
                        <xsl:value-of select="translate(premiereDate,'0123456789?','01234567890')"/>
					</Index:field>

                    <Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="1">
                        <xsl:value-of select="translate(premiereDate,'0123456789?','01234567899')"/>
					</Index:field>
                     <Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="6">
                        <xsl:value-of select="title/mainTitle"/>
                         </Index:field>
                </Index:fields>






            </xsl:for-each>
        </Index:document>

    </xsl:template>

</xsl:stylesheet>