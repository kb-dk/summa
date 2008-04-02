<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
		exclude-result-prefixes="java xs xalan xsl mc"
		version="1.0">
     <xsl:template name="identificers">
         <Index:group Index:name="numbers" Index:navn="nr">
						<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='a' or @code='e']">
							<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='521']/mc:subfield[@code='x']">
							<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='247' or @tag='248']/mc:subfield[@code='z' or @code='r']">
							<Index:field Index:name="isbn" Index:navn="ib" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='a' or @code='e']">
							<Index:field Index:name="isbn" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='521']/mc:subfield[@code='x']">
							<Index:field Index:name="isbn" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='247' or @tag='248']/mc:subfield[@code='z' or @code='r']">
							<Index:field Index:name="isbn" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='z' or @code='r']">
							<Index:field Index:name="isbn_other" Index:navn="is" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='x' or @code='w']">
							<Index:field Index:name="isbn_other" Index:navn="ib" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='x' or @code='w']">
							<Index:field Index:name="isbn_other" Index:navn="is" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='022']/mc:subfield[@code='a' or @code='x' or @code='z']">
							<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='440' or @tag='490' or @tag='840']/mc:subfield[@code='z']">
							<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='8']">
							<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='022']/mc:subfield[@code='a' or @code='x' or @code='z']">
							<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='440' or @tag='490' or @tag='840']/mc:subfield[@code='z']">
							<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='8']">
							<Index:field Index:name="issn" Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='557']/mc:subfield[@code='z']">
							<Index:field Index:name="issn_other" Index:navn="is" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='860' or @tag='861' or @tag='863' or @tag='865' or @tag='866' or @tag='867' or @tag='868' or @tag='870' or @tag='871' or @tag='873' or @tag='874' or @tag='879']/mc:subfield[@code='z']">
							<Index:field Index:name="issn_other" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
				
				<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='a' or @code='e']">
							<Index:field Index:name="isbn" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='521']/mc:subfield[@code='x']">
							<Index:field Index:name="isbn" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='247' or @tag='248']/mc:subfield[@code='z' or @code='r']">
							<Index:field Index:name="isbn" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='558']/mc:subfield[@code='z' or @code='r']">
							<Index:field Index:name="isbn_other" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='021']/mc:subfield[@code='x' or @code='w']">
							<Index:field Index:name="isbn_other" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='022']/mc:subfield[@code='a' or @code='x' or @code='z']">
							<Index:field Index:name="issn"  Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='440' or @tag='490' or @tag='840']/mc:subfield[@code='z']">
							<Index:field Index:name="issn"  Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='534']/mc:subfield[@code='8']">
							<Index:field Index:name="issn"  Index:navn="in" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='557']/mc:subfield[@code='z']">
							<Index:field Index:name="issn_other" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='860' or @tag='861' or @tag='863' or @tag='865' or @tag='866' or @tag='867' or @tag='868' or @tag='870' or @tag='871' or @tag='873' or @tag='874' or @tag='879']/mc:subfield[@code='z']">
							<Index:field Index:name="issn_other" Index:type="number" Index:boostFactor="6">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='001']/mc:subfield[@code='a']">
							<Index:field Index:repeat="true" Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='024' or @tag='027' or @tag='028']/mc:subfield[@code='a' or @code='x']">
							<Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
							</Index:field>
						</xsl:for-each>

						<xsl:for-each select="mc:datafield[@tag='024' or @tag='027' or @tag='028']/mc:subfield[@code='a' or @code='x']">
							<Index:field Index:repeat="true" Index:name="number" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='g' or @code='s']">
							<Index:field Index:repeat="true" Index:name="number" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='f']">
							<Index:field Index:repeat="true" Index:name="number" Index:type="number" Index:boostFactor="10">

								<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
								<xsl:for-each select="../mc:subfield[@code='g']">
									<xsl:text>. </xsl:text>
									<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
								</xsl:for-each>
							</Index:field>
						</xsl:for-each>
						<xsl:for-each select="mc:datafield[@tag='538']/mc:subfield[@code='o']">
							<Index:field Index:repeat="true" Index:name="number" Index:type="number" Index:boostFactor="10">
								<xsl:value-of select="."/>
								<xsl:for-each select="../mc:subfield[@code='p']">
									<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
								</xsl:for-each>
								<xsl:for-each select="../mc:subfield[@code='q']">
									<xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
								</xsl:for-each>
							</Index:field> 
						</xsl:for-each>
					</Index:group>
     </xsl:template>

</xsl:stylesheet>
