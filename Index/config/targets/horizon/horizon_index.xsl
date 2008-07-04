<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">

    <xsl:include href="horizon_short_format.xsl" />
    <xsl:include href="horizon_author.xsl" />
    <xsl:include href="horizon_title.xsl" />
    <xsl:include href="horizon_subject.xsl" />
    <xsl:include href="horizon_publisher.xsl" />
    <xsl:include href="horizon_other.xsl" />
    <xsl:include href="horizon_notes.xsl" />
    <xsl:include href="horizon_relations.xsl" />
    <xsl:include href="horizon_classification.xsl" />
    <xsl:include href="horizon_identificers.xsl" />
    <xsl:include href="horizon_material.xsl" />
    <xsl:include href="horizon_lcl.xsl" />
    <xsl:include href="horizon_lma.xsl" />


    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <Index:document Index:defaultBoost="1" Index:defaultType="token" Index:defaultFreetext="true" Index:defaultSuggest="false"
                        Index:defaultGroup="false" Index:langAutogroup="true" Index:resolver="horizon">
            <xsl:attribute name="Index:id">
                <xsl:value-of select="mc:record/mc:datafield[@tag='994']/mc:subfield[@code='z']" />
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
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='z']">
                        <Index:field Index:repeat="true" Index:name="location" Index:navn="lokation" Index:freetext="false" Index:type="token" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='b']">
                        <Index:field Index:repeat="true" Index:name="collection" Index:navn="samling" Index:type="token" Index:freetext="false" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='j']">
                        <Index:field Index:repeat="false" Index:name="barcode" Index:type="token">
                            <xsl:value-of select="."/>
                            <xsl:text> </xsl:text>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='532' or @tag='534' or @tag='559' or @tag='565' or @tag='856' or @tag='860' or @tag='861' or @tag='863' or @tag='865' or @tag='866' or @tag='867' or @tag='868' or @tag='870' or @tag='871' or @tag='873' or @tag='874' or @tag='879']/mc:subfield[@code='u']">
                        <Index:field Index:repeat="true" Index:name="ww" Index:navn="ww" Index:type="token">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>




                    <xsl:for-each select="mc:datafield[@tag='440' or @tag='840']">
                        <Index:field Index:repeat="false" Index:name="series_normalised" Index:navn="lse" Index:type="keyword" Index:boostFactor="10">
                            <xsl:for-each select="mc:subfield[@code='a']">
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='3']">
                                <xsl:text> / </xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:for-each>
                            <xsl:for-each select="mc:subfield[@code='4']">
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
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='z']">
                        <Index:field Index:repeat="false" Index:name="location_normalised" Index:navn="l_lokation" Index:type="keyword" Index:boostFactor="10">
                            <xsl:choose>
                                <xsl:when test="substring(.,1)='Aestet'">
                                    <xsl:text>Æstetikbiblioteket, AU	</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='AlmM'">
                                    <xsl:text>Institut for Almen Medicin,  AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='AlmP'">
                                    <xsl:text>Institut for Almen Patologi,  AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='AlmPhum'">
                                    <xsl:text>Institut for Human Genetik,  AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='AlmPmed'">
                                    <xsl:text>Institut for Medicinsk Mikrobiologi og Immunologi,  AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtca'">
                                    <xsl:text>Århus Sygehus, Medicinsk Kardiologisk Afdeling A</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtcl'">
                                    <xsl:text>Århus Sygehus, Klinisk Biokemisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtend'">
                                    <xsl:text>Århus Sygehus, Medicinsk Endokrinologisk Afdeling C</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtg'">
                                    <xsl:text>Århus Sygehus, Geriatrisk afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amthae'">
                                    <xsl:text>Århus Sygehus, Hæmatologisk Afdeling B</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtk'">
                                    <xsl:text>Århus Sygehus, Kirurgisk Gastroentologisk Afdeling L</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtor'">
                                    <xsl:text>Århus Sygehus, Ortopædkirurgisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtp'">
                                    <xsl:text>Århus Sygehus, Patologisk Institut</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Amtroe'">
                                    <xsl:text>Århus Sygehus, Røntgenafdelingen</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Ark'">
                                    <xsl:text>Arkitektskolen i Aarhus</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='B'">
                                    <xsl:text>Institut for Medicinsk Biokemi, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='biobib'">
                                    <xsl:text>Biobiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Biof'">
                                    <xsl:text>Biofysisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Biostat'">
                                    <xsl:text>Institut for Biostatistik, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Bot'">
                                    <xsl:text>Biobiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Canfau'">
                                    <xsl:text>Center for Rusmiddelforskning, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Cekvina'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='DJFA'">
                                    <xsl:text>Det Jordbrugsvidenskabelige Fakultet, Årslev, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='DJFF'">
                                    <xsl:text>Det Jordbrugsvidenskabelige Fakultet, Foulum, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='DJFL'">
                                    <xsl:text>Det Jordbrugsvidenskabelige Fakultet, Flakkebjerg, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='DJM'">
                                    <xsl:text>Det Jyske Musikkonservatorium</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='DsHAA'">
                                    <xsl:text>Den Sociale Højskole i Århus</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='DSy'">
                                    <xsl:text>JCVU Vennelystparken</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Dsy'">
                                    <xsl:text>JCVU Vennelystparken</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='eksNa'">
                                    <xsl:text>Steno Biblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Engelsk'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Europa'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='F'">
                                    <xsl:text>Institut for Fysik og Astronomi, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Farm'">
                                    <xsl:text>Farmakologisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Filo'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Forh'">
                                    <xsl:text>Moesgårdbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='ForhArk'">
                                    <xsl:text>Moesgårdbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='ForhEtn'">
                                    <xsl:text>Moesgårdbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='FraDok'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Fys'">
                                    <xsl:text>Fysiologisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='GenOek'">
                                    <xsl:text>Biobiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Geol'">
                                    <xsl:text>Geologisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='HANC'">
                                    <xsl:text>Århus Sygehus, Hammel Neurocenter</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Hist'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Idraet'">
                                    <xsl:text>Idrætsbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='IHist'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Ind'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Info'">
                                    <xsl:text>IT-Biblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Int-fri'">
                                    <xsl:text>Internetpublikation uden adgangsbegrænsning</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='J'">
                                    <xsl:text>Peter Skautrup Centret (Jysk), AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Jur'">
                                    <xsl:text>Juridisk Bibliotek, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='K' or substring(.,1)='Kemi'">
                                    <xsl:text>Kemisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHan'">
                                    <xsl:text>Århus Sygehus, Anæstesiologisk Afdeling N</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHapt'">
                                    <xsl:text>Århus Sygehus,  Apoteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHcl'">
                                    <xsl:text>Århus Sygehus,  Klinisk Biokemisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHf'">
                                    <xsl:text>Århus Sygehus, Fysioterapien og Ergoterapien</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHgen'">
                                    <xsl:text>Århus Sygehus, Klinisk Genetisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHk'">
                                    <xsl:text>Århus Sygehus, Gastrokirurgisk Afdeling L</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHm'">
                                    <xsl:text>Århus Sygehus, Medicinsk Afdeling M</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHmv'">
                                    <xsl:text>Århus Sygehus, Medicinsk Afdeling V</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHn'">
                                    <xsl:text>Århus Sygehus, Neurologisk Afdeling F</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHnf'">
                                    <xsl:text>Århus Sygehus, Neurofysiologisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHnk'">
                                    <xsl:text>Århus Sygehus, Neurokirurgisk Afdeling GS</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHnp'">
                                    <xsl:text>Århus Sygehus, Neuropatologisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHnr'">
                                    <xsl:text>Århus Sygehus, Neuroradiologisk Afdeling P</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHnuk'">
                                    <xsl:text>Århus Sygehus, Nuklearmedicinsk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHoe'">
                                    <xsl:text>Århus Sygehus, Øre-, Næse-, Halsafdeling H</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHoej'">
                                    <xsl:text>Århus Sygehus, Øjenafdeling J</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHor'">
                                    <xsl:text>Århus Sygehus, Ortopædkirurgisk Afdeling E</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHp'">
                                    <xsl:text>Århus Sygehus, Patologisk-Anatomisk Institut</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHpl'">
                                    <xsl:text>Århus Sygehus, Plastikkirurgisk Afdeling Z</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHr'">
                                    <xsl:text>Århus Sygehus, Onkologisk Afdeling D</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHrhf'">
                                    <xsl:text>Århus Sygehus, Reumatologisk Afdeling U</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHt'">
                                    <xsl:text>Århus Sygehus, Lungemedicinsk Afdeling B</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KHta'">
                                    <xsl:text>Århus Sygehus, Tand-, Mund- og Kæbekirurgisk Afdeling O</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='KlaArk'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Ling'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='LingCNS'">
                                    <xsl:text>Moesgårdbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MA'">
                                    <xsl:text>Skejby Sygehus, Infektionsmedicinsk afdeling Q</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Marin'">
                                    <xsl:text>Biobiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Mat'">
                                    <xsl:text>Bibliotek for Matematiske Fag, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MB'">
                                    <xsl:text>Århus Sygehus, Dermato-Venerologisk afdeling S</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MedHi'">
                                    <xsl:text>Steno Museet, Medicinhistorie</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MedUdd'">
                                    <xsl:text>Enhed for Medicinsk Uddannelse, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MidArk'">
                                    <xsl:text>Moesgårdbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Miljo'">
                                    <xsl:text>Miljølære, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MoBbio'">
                                    <xsl:text>Institut for Molekylær og Strukturel Biologi, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MoBfor'">
                                    <xsl:text>Institut for Molekylær og Strukturel Biologi/FP, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Moesg'">
                                    <xsl:text>Moesgårdbiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MPH'">
                                    <xsl:text>Master of Public Health, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Mu-node'">
                                    <xsl:text>Afdelingen for Musikvidenskab - Noder, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='MVS'">
                                    <xsl:text>Den Sundhedsfaglige Kandidat-uddannelse, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='N'">
                                    <xsl:text>Naturhistorisk Museum</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='No'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Nobel'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='OE'">
                                    <xsl:text>Institut for Økonomi, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Oest'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='OM'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='PlFys'">
                                    <xsl:text>Institut for Plantefysiologi, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Psyk'">
                                    <xsl:text>Psykologisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='RetsMed'">
                                    <xsl:text>Retsmedicinsk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Ringg'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Romansk'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-afly'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-alm'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-avis'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-avls'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-brab'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-dvd'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-e'">
                                    <xsl:text>Internetværker (adgang fra Statsbiblioteket)</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-fag'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-fon'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-hast'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-kort'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-KT'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-laes'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-laes'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-LK'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-lyd'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-pa'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-per'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-Post'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-PT'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-ratv'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-rekl'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-retr'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-rot'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-Sem'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-ureg'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-vaer'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SB-vid'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SBAU-e'">
                                    <xsl:text>Internetværker (begrænset adgang)</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SBDB'">
                                    <xsl:text>Statsbiblioteket</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SBFjern'">
                                    <xsl:text>Lån fra andre biblioteker (via Statsbiblioteket)</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SBIB'">
                                    <xsl:text>Statsbiblioteket - BiblioteksCenter for Integration</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Semi'">
                                    <xsl:text>Teologisk Fakultet. Biblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKan'">
                                    <xsl:text>Skejby Sygehus, Anæstesiafdelingen</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKb'">
                                    <xsl:text>Skejby Sygehus, Børneafdeling A</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKca'">
                                    <xsl:text>Skejby Sygehus, Hjertemedicinsk Afdeling B</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKcl'">
                                    <xsl:text>Skejby Sygehus, Klinisk Biokemisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKgyn'">
                                    <xsl:text>Skejby Sygehus, Gynækologisk-Obstetrisk Afdeling Y</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKki'">
                                    <xsl:text>Skejby Sygehus, Klinisk Immunologisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKmob'">
                                    <xsl:text>Skejby Sygehus, Molekylær Medicinsk Forskningsenhed</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKmt'">
                                    <xsl:text>Skejby Sygehus, Medico-Teknisk Afdeling</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKnyc'">
                                    <xsl:text>Skejby Sygehus, Nyremedicinsk Afdeling C</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKroe'">
                                    <xsl:text>Skejby Sygehus, Røntgenafdelingen</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKth'">
                                    <xsl:text>Skejby Sygehus, Hjerte-Lunge-Karkirurgisk Afdeling T</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SKur'">
                                    <xsl:text>Skejby Sygehus, Urinvejskirurgisk Afdeling K</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Slavisk'">
                                    <xsl:text>Ringgadebiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Socialm'">
                                    <xsl:text>Institut for Epidemiologi og Socialmedicin, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Sprog'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Stat'">
                                    <xsl:text>Institut for Statskundskab, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='SVB'">
                                    <xsl:text>Statsbiblioteket - Det Sundhedsvidenskabelige Bibliotek</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='T'">
                                    <xsl:text>Teologisk Fakultet. Biblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Ta'">
                                    <xsl:text>Odontologisk Institut, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Tysk'">
                                    <xsl:text>Biblioteket for Sprog, Litteratur og Kultur, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='ZooFys'">
                                    <xsl:text>Biobiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:when test="substring(.,1)='Zool'">
                                    <xsl:text>Biobiblioteket, AU</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='b']">
                        <Index:field Index:repeat="false" Index:name="collection_normalised" Index:navn="l_samling" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='j']">
                        <Index:field Index:repeat="false" Index:name="barcode_normalised" Index:navn="l_stregkode" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                    <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="l_call" Index:navn="lop" Index:type="keyword" Index:boostFactor="10">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                      <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='a']">
                        <Index:field Index:repeat="false" Index:name="call" Index:navn="opst" Index:type="token" Index:boostFactor="4">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>
                     <xsl:for-each select="mc:datafield[@tag='096' or @tag='x96']/mc:subfield[@code='i']">
                        <Index:field Index:repeat="false" Index:name="itype" Index:navn="matkat" Index:type="token" Index:boostFactor="4">
                            <xsl:value-of select="."/>
                        </Index:field>
                    </xsl:for-each>


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
                            <xsl:for-each select="mc:subfield[@code='A' or @code='a' or @code='b'  or @code='n' or @code='o' or @code='c' or @code='u' or @code='x' or @code='y' or @code='G' or @code='g']">
                                <xsl:choose>
                                    <xsl:when test="position()=1">
                                        <xsl:value-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:if test="@code='g'">
                                            <xsl:value-of select="."/>
                                            <xsl:text>: </xsl:text>
                                        </xsl:if>
                                        <xsl:if test="@code='A'">
                                            <xsl:text> : </xsl:text>
                                            <xsl:value-of select="."/>
                                        </xsl:if>
                                        <xsl:if test="@code='a'">
                                            <xsl:if test="not(preceding-sibling::mc:subfield[@code='A'])">
                                                <xsl:text> : </xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:if>
                                            <xsl:if test="not(preceding-sibling::mc:subfield[@code='a'])">
                                                <xsl:text> : </xsl:text>
                                                <xsl:value-of select="."/>
                                            </xsl:if>
                                            <xsl:if test="(preceding-sibling::mc:subfield[@code='a'])">
                                                <xsl:text>;</xsl:text>
                                                <xsl:value-of select="."/>
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
                      <xsl:for-each select="mc:datafield[@tag='001']/mc:subfield[@code='d']">
            <Index:field Index:repeat="true" Index:name="op" Index:navn="op" Index:type="token" Index:boostFactor="2" Index:freetext="false">

                   <xsl:value-of select="."/>

            </Index:field>

                    </xsl:for-each>
                     <xsl:call-template name="identifiers" />
                </Index:fields>
            </xsl:for-each>
        </Index:document>
    </xsl:template>
</xsl:stylesheet>
