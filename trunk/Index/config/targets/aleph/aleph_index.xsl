<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:include href="aleph_short_format.xsl" />
    <xsl:include href="aleph_author.xsl" />
    <xsl:include href="aleph_title.xsl" />
    <xsl:include href="aleph_subject.xsl" />
    <xsl:include href="aleph_publisher.xsl" />
    <xsl:include href="aleph_other.xsl" />
    <xsl:include href="aleph_notes.xsl" />
    <xsl:include href="aleph_relations.xsl" />
  <xsl:include href="aleph_classification.xsl" />
    <xsl:include href="aleph_identifiers.xsl" />
    <xsl:include href="aleph_material.xsl" />
    <xsl:include href="aleph_lcl.xsl" />
    <xsl:include href="aleph_lma.xsl" />


    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
		<Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
				Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="aleph">
			<xsl:attribute name="Index:id">
				<xsl:value-of select="mc:record/mc:datafield[@tag='001']/mc:subfield[@code='a']" />
			</xsl:attribute>

            <xsl:for-each select="mc:record">
				<Index:fields>

    <xsl:call-template name="shortformat" />
                    <xsl:call-template name="author" />
                    <xsl:call-template name="title" />
                    <xsl:call-template name="subject" />
               <xsl:call-template name="publication_data" /> 
                    <xsl:call-template name="other" />
                    <xsl:call-template name="notes" />
                    <xsl:call-template name="relations" />
                 <xsl:call-template name="classification" /> 
                   <xsl:call-template name="identificers" /> 
                    <xsl:call-template name="material" />
                    <xsl:call-template name="lcl" /> 
                    <xsl:call-template name="lma" />

                    
					
					<xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='l']">
						<Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='041']/mc:subfield[@code='a' or @code='p' or @code='u' or @code='e' or @code='d']">
						<Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='041']/mc:subfield[@code='c' or @code='b']">
						<Index:field Index:repeat="true" Index:name="original_language" Index:navn="ou" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
	<!--  			<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='z']">
						<Index:field Index:repeat="true" Index:name="location" Index:navn="lokation" Index:freetext="false" Index:type="token" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='b']">
						<Index:field Index:repeat="true" Index:name="collection" Index:navn="samling" Index:type="token" Index:freetext="false" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='j']">
						<Index:field Index:repeat="true" Index:name="barcode" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>-->	
					<xsl:choose>
					 <xsl:when test="contains(mc:datafield[@tag='096']/mc:subfield[@code='e'],'The IT Library') or contains(mc:datafield[@tag='096']/mc:subfield[@code='e'],'The IT-Library')">
           <xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='e']">
					 <Index:field Index:repeat="true" Index:name="location" Index:navn="lokation" Index:freetext="false" Index:type="token" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
						</xsl:for-each>
						 <xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='e']">
						<Index:field Index:repeat="false" Index:name="location_normalised" Index:navn="l_lokation" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
						</xsl:for-each>
							<xsl:for-each select="mc:datafield[@tag='096' ]/mc:subfield[@code='f' or @code ='l']">
						<Index:field Index:repeat="false" Index:name="collection" Index:navn="samling" Index:type="token" Index:freetext="false" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='096']">
						
						<Index:field Index:repeat="false" Index:name="collection_normalised" Index:navn="l_samling" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="normalize-space(concat(mc:subfield[@code='f'],' ',mc:subfield[@code='l']))"/>
						</Index:field>
					
					</xsl:for-each>
					<xsl:if test="mc:datafield[@tag='096']/mc:subfield[@code='a' or @code='b' or @code='c']">
						<Index:field Index:repeat="false" Index:name="l_call" Index:navn="lop" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="normalize-space(concat(mc:datafield[@tag='096']/mc:subfield[@code='a'],' ',mc:datafield[@tag='096']/mc:subfield[@code='b'],' ',mc:datafield[@tag='096']/mc:subfield[@code='c']))"/>
						</Index:field>
					</xsl:if>
						</xsl:when>
						<xsl:otherwise>
					<xsl:for-each select="mc:datafield[@tag='U03' or @tag='U04' or @tag='U05' or @tag='U07' or @tag='U08' or @tag='U10']/mc:subfield[@code='k']">
						<Index:field Index:repeat="true" Index:name="location" Index:navn="lokation" Index:freetext="false" Index:type="token" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
						</xsl:for-each>
							<xsl:for-each select="mc:datafield[@tag='U03' or @tag='U04' or @tag='U05' or @tag='U07' or @tag='U08' or @tag='U10']/mc:subfield[@code='k']">
						<Index:field Index:repeat="false" Index:name="location_normalised" Index:navn="l_lokation" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
						</xsl:for-each>
							<xsl:for-each select="mc:datafield[@tag='U03' or @tag='U04' or @tag='U05' or @tag='U07' or @tag='U08' or @tag='U10']/mc:subfield[@code='c' or @code ='p']">
						<Index:field Index:repeat="false" Index:name="collection" Index:navn="samling" Index:type="token" Index:freetext="false" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='U03' or @tag='U04' or @tag='U05' or @tag='U07' or @tag='U08' or @tag='U10']">
						
						<Index:field Index:repeat="false" Index:name="collection_normalised" Index:navn="l_samling" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="normalize-space(concat(mc:subfield[@code='c'],' ',mc:subfield[@code='p']))"/>
						</Index:field>
					
					</xsl:for-each>
					
					<xsl:for-each select="mc:datafield[@tag='U03' or @tag='U04' or @tag='U05' or @tag='U07' or @tag='U08' or @tag='U10']/mc:subfield[@code='r']">
						<Index:field Index:repeat="false" Index:name="l_call" Index:navn="lop" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
						</xsl:otherwise>
						</xsl:choose>
					
					
				
					
					
					
					<xsl:for-each select="mc:datafield[@tag='532' or @tag='534' or @tag='559' or @tag='565' or @tag='856' or @tag='860' or @tag='861' or @tag='863' or @tag='865' or @tag='866' or @tag='867' or @tag='868' or @tag='870' or @tag='871' or @tag='873' or @tag='874' or @tag='879']/mc:subfield[@code='u']">
						<Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>



                    <xsl:for-each select="mc:datafield[@tag='440' or @tag='840']">
						<Index:field Index:repeat="false" Index:name="series_normalised" Index:navn="lse" Index:type="keyword" Index:boostFactor="10">
							<xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="translate(.,'&lt;&gt;','')"/>
					
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='æ']">
								<xsl:text> / </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='ø']">
								<xsl:text> / </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='n']">
								<xsl:text>. </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
							<xsl:for-each select="mc:subfield[@code='o']">
								<xsl:text>. </xsl:text>
								<xsl:value-of select="."/>
							</xsl:for-each>
						</Index:field>
					</xsl:for-each>


              

                    <xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='l']">
						<Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='041']/mc:subfield[@code='a' or @code='p' or @code='u']">
						<Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
			<!-- 	<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='z']">
						<Index:field Index:repeat="true" Index:name="location_normalised" Index:navn="l_lokation" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='b']">
						<Index:field Index:repeat="true" Index:name="collection_normalised" Index:navn="l_samling" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='j']">
						<Index:field Index:repeat="true" Index:name="barcode_normalised" Index:navn="l_stregkode" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each>
					<xsl:for-each select="mc:datafield[@tag='096']/mc:subfield[@code='a']">
						<Index:field Index:repeat="true" Index:name="l_call" Index:navn="lop" Index:type="keyword" Index:boostFactor="10">
							<xsl:value-of select="."/>
						</Index:field>
					</xsl:for-each> -->	
					
				
					

					<xsl:for-each select="mc:datafield[@tag='008']">
						<xsl:choose>
							<xsl:when test="contains(mc:subfield[@code='u'],'?')">
								<Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
									<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.YearRange.makeRange(mc:subfield[@code='a'], mc:subfield[@code='z'])"/>
								</Index:field>
							</xsl:when>
							<xsl:when test="contains(mc:subfield[@code='u'],'o')">
								<Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
									<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.YearRange.makeRange(mc:subfield[@code='a'],'2030')"/>
								</Index:field>
							</xsl:when>
							<xsl:otherwise>
								<xsl:for-each select="mc:subfield[@code='a' or @code='z']">
									<xsl:choose>
										<xsl:when test="contains(.,'?')">
											<Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
												<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.YearRange.makeRange(.)"/>
											</Index:field>
										</xsl:when>
										<xsl:otherwise>
											<Index:field Index:name="py" Index:navn="år" Index:type="token" Index:boostFactor="10">
												<xsl:value-of select="."/>
											</Index:field>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:for-each>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>   
					<xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z']">
						<xsl:choose>
							<xsl:when test="@code='z'">
								<Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
									<xsl:value-of select="translate(.,'0123456789?','01234567890')"/>
								</Index:field>
							</xsl:when>
							<xsl:when test="@code='a' and not(../mc:subfield[@code='z']) ">
								<Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
									<xsl:value-of select="translate(.,'0123456789?','01234567890')"/>
								</Index:field>
							</xsl:when>
						</xsl:choose>
					</xsl:for-each>
					<xsl:if test="not(mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z'])">
						<Index:field Index:name="sort_year_desc" Index:navn="sort_år_desc" Index:type="keyword" Index:boostFactor="10">
							<xsl:text>0</xsl:text>
						</Index:field>
					</xsl:if>
					<xsl:for-each select="mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z']">
						<xsl:choose>
							<xsl:when test="@code='z'">
								<Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
									<xsl:value-of select="translate(.,'0123456789?','01234567899')"/>
								</Index:field>
							</xsl:when>
							<xsl:when test="@code='a' and not(../mc:subfield[@code='z']) ">
								<Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword" Index:boostFactor="10">
									<xsl:value-of select="translate(.,'0123456789?','01234567899')"/>
								</Index:field>
							</xsl:when>
						</xsl:choose>
					</xsl:for-each>
					<xsl:if test="not(mc:datafield[@tag='008']/mc:subfield[@code='a' or @code='z'])">
						<Index:field Index:name="sort_year_asc" Index:navn="sort_år_asc" Index:type="keyword">
							<xsl:text>9999</xsl:text>
						</Index:field>
					</xsl:if>
					<Index:field Index:name="sort_title" Index:navn="sort_titel" Index:sortLocale="da" Index:type="keyword" Index:boostFactor="100">
						<xsl:for-each select="mc:datafield[@tag='245']">
							
							<xsl:for-each select="mc:subfield[@code ='A' or @code='a' or @code='b' or  @code='n' or @code='o' or  @code='x' or @code='y' or @code='g']">
								<xsl:choose>
									
									<xsl:when test="position()=1">
									<xsl:choose>
									<xsl:when test="contains(.,'&gt;&gt;')">	
									<xsl:value-of select="substring-after(.,'&gt;&gt;')"/>
									
									</xsl:when>
									<xsl:otherwise>
									<xsl:value-of select="."/>
									</xsl:otherwise>
									</xsl:choose>
									</xsl:when>

                                    <xsl:otherwise>
             <xsl:if test="@code='g'">
											<xsl:choose>
									<xsl:when test="contains(.,'&gt;&gt;')">	
									<xsl:value-of select="substring-after(.,'&gt;&gt;')"/>
									
									</xsl:when>
									<xsl:otherwise>
									<xsl:value-of select="."/>
									</xsl:otherwise>
									</xsl:choose>
											<xsl:text>: </xsl:text>
										</xsl:if>
										<xsl:if test="@code='a'">
											<xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
												<xsl:text> : </xsl:text>
												<xsl:choose>
									<xsl:when test="contains(.,'&gt;&gt;')">	
									<xsl:value-of select="substring-after(.,'&gt;&gt;')"/>
									
									</xsl:when>
									<xsl:otherwise>
									<xsl:value-of select="."/>
									</xsl:otherwise>
									</xsl:choose>
											</xsl:if>
											<xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
												<xsl:text>;</xsl:text>
											<xsl:choose>
									<xsl:when test="contains(.,'&gt;&gt;')">	
									<xsl:value-of select="substring-after(.,'&gt;&gt;')"/>
									
									</xsl:when>
									<xsl:otherwise>
									<xsl:value-of select="."/>
									</xsl:otherwise>
									</xsl:choose>
											</xsl:if>
										</xsl:if>
										<xsl:if test="@code='b'">
											<xsl:text> </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
					
										<xsl:if test="@code='n'">
											<xsl:text>. </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='o'">
											<xsl:text>. </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
								
										<xsl:if test="@code='x'">
											<xsl:text>. </xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
										<xsl:if test="@code='y'">
											
											<xsl:text>- -</xsl:text>
											<xsl:value-of select="."/>
										</xsl:if>
									
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						
						</xsl:for-each>
					</Index:field>

				
				</Index:fields>
			</xsl:for-each>
		</Index:document>
	</xsl:template>
</xsl:stylesheet>
