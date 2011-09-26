<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">

    <xsl:template name="em">
        <Index:group Index:name="em" Index:navn="em" Index:suggest="true">
						<xsl:for-each select="mc:datafield[@tag='600' or @tag='G00']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
						<xsl:call-template name="person_name"/>
							</Index:field>
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
									<xsl:call-template name="person_name_inverted"/>
							</Index:field>
						</xsl:for-each>

						<xsl:for-each select="mc:datafield[@tag='600' or @tag='G00']">
							<xsl:if test="mc:subfield[@code='t']">
								<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
										<xsl:call-template name="person_name"/>
									<xsl:for-each select="mc:subfield[@code='t']">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='x']">
										<xsl:text>. </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='y']">
										<xsl:text>. </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='z']">
										<xsl:text>. </xsl:text>
										<xsl:value-of select="."/>
										<xsl:text> </xsl:text>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='u']">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
								</Index:field>
								<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
										<xsl:call-template name="person_name_inverted"/>
									<xsl:for-each select="mc:subfield[@code='t']">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='x']">
										<xsl:text>. </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='y']">
										<xsl:text>. </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='z']">
										<xsl:text>. </xsl:text>
										<xsl:value-of select="."/>
										<xsl:text> </xsl:text>
									</xsl:for-each>
									<xsl:for-each select="mc:subfield[@code='u']">
										<xsl:text> : </xsl:text>
										<xsl:value-of select="."/>
									</xsl:for-each>
								</Index:field>
							</xsl:if>
						</xsl:for-each>

						
						<xsl:for-each select="mc:datafield[@tag='610' or @tag='G10']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="6">
									<xsl:call-template name="corporate_name"/>
										<xsl:for-each select="mc:subfield[@code='x']">
											<xsl:text>. </xsl:text>
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='y']">
											<xsl:text>. </xsl:text>
											<xsl:value-of select="."/>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='z']">
											<xsl:text>. </xsl:text>
											<xsl:value-of select="."/>
											<xsl:text> </xsl:text>
										</xsl:for-each>
										<xsl:for-each select="mc:subfield[@code='u']">
											<xsl:text> : </xsl:text>
											<xsl:value-of select="."/>
										</xsl:for-each>
							
											
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='621' or @tag='G21']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="4">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='e']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='j']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630' or @tag='G30']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630' or @tag='G30']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630' or @tag='G30']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='f']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630' or @tag='G30']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='g']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630' or @tag='G30']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='s']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='630' or @tag='G30']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='t']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='631' or @tag='G31']/mc:subfield[@code='a' or @code='b' or @code='f' or @code='g' or @code='s' or @code='t']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='633' or @tag='G33']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='634' or @tag='G34']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='634' or @tag='G34']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='634' or @tag='G34']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='c']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='d']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='645' or @tag='G45']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='645' or @tag='G45']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='645' or @tag='G45']">
							<Index:field Index:repeat="true" Index:name="uk" Index:navn="uk" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='c']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='u']">
									<xsl:text> </xsl:text>
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='650' or @tag='G50']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='b'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='c'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='d'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='e'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='v'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='x'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='y'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='z'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='651' or @tag='G51']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:for-each select="mc:subfield">
									<xsl:choose>
										<xsl:when test="@code='a'">
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='v'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='x'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='y'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
										<xsl:when test="@code='z'">
											<xsl:text> - </xsl:text>
											<xsl:value-of select="."/>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='652' or @tag='G52']">
						<xsl:if test="mc:subfield[@code='a']">
							<Index:field Index:repeat="true" Index:name="au" Index:navn="au" Index:type="token" Index:boostFactor="10">
								<xsl:call-template name="person_name_inverted"/>
							</Index:field>
							</xsl:if>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='652' or @tag='G52']/mc:subfield[@code='b' or @code='t']">
							<Index:field Index:repeat="true" Index:name="au" Index:navn="au" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='654' or @tag='655' or @tag='G54' or @tag='G55']">
							<xsl:if test="mc:subfield[@code='a']">
							<Index:field Index:repeat="true" Index:name="au" Index:navn="au" Index:type="token" Index:boostFactor="10">
								<xsl:call-template name="person_name_inverted"/>
							</Index:field>
							</xsl:if>
            </xsl:for-each>
                        <xsl:for-each select="mc:datafield[@tag='654' or @tag='655' or @tag='G54' or @tag='G55']/mc:subfield[@code='b']">
							<Index:field Index:repeat="true" Index:name="au" Index:navn="au" Index:type="token" Index:boostFactor="4">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='654' or @tag='655' or @tag='G54' or @tag='G55']/mc:subfield[@code='t']">
							<Index:field Index:repeat="true" Index:name="au" Index:navn="au" Index:type="token" Index:boostFactor="4"></Index:field>
						</xsl:for-each>
						
						<xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='o' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='667' or @tag='G67']/mc:subfield[@code='e' or @code='f' or @code='i' or @code='l' or @code='m' or @code='n' or @code='o' or @code='p' or @code='q' or @code='r' or @code='s' or @code='t' or @code='u']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='668' or @tag='G68']">
							<Index:field Index:repeat="true" Index:name="ke" Index:navn="ke" Index:type="token" Index:boostFactor="6">
								<xsl:for-each select="mc:subfield[@code='a']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='b']">
									<xsl:value-of select="."/>
								</xsl:for-each>
								<xsl:for-each select="mc:subfield[@code='c']">
									<xsl:value-of select="."/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
				
					</Index:group>
        <Index:group Index:name="db" Index:navn="db" Index:suggest="true">

						<xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='e' or @code='f' or @code='t']">
							<Index:field Index:repeat="true" Index:name="df" Index:navn="df" Index:type="token" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='s' or @code='r' or @code='q']">
							<Index:field Index:repeat="true" Index:name="ds" Index:navn="ds" Index:type="token" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='668' or @tag='G68']">
                <Index:field Index:repeat="true" Index:name="ds" Index:navn="ds" Index:type="token" Index:boostFactor="6">
                    <xsl:for-each select="mc:subfield[@code='a']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='b']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                    <xsl:for-each select="mc:subfield[@code='c']">
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='o']">
							<Index:field Index:repeat="true" Index:name="fm" Index:navn="fm" Index:type="token" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='m' or @code='n' or @code='p' or @code='l']">
                <Index:field Index:repeat="true" Index:name="me" Index:navn="me" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='u']">
                <Index:field Index:repeat="true" Index:name="nb" Index:navn="nb" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='666' or @tag='G66']/mc:subfield[@code='i']">
                <Index:field Index:repeat="true" Index:name="dt" Index:navn="dt" Index:type="token" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
            </xsl:for-each>


                    </Index:group>

    </xsl:template>

</xsl:stylesheet>
