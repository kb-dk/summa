<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
             xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:Index="http://statsbiblioteket.dk/2004/Index"
		xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:xalan="http://xml.apache.org/xalan"
		xmlns:java="http://xml.apache.org/xalan/java"
        exclude-result-prefixes="java xs xalan xsl oai_dc dc"
		version="1.0">
	<xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template name="language">
			<xsl:for-each select="la">
									<Index:field Index:repeat="true" Index:name="lang" Index:navn="sp" Index:type="token">
										
										<xsl:choose>
											<xsl:when test="string-length(.)=3">
												<xsl:value-of select="."/>
											
											</xsl:when>
											<xsl:when test="string-length(.)=2">
												
												<xsl:if test="contains(.,'ab')">
													<xsl:text>abk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'aa')">
													<xsl:text>aar</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'af')">
													<xsl:text>afr</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ak')">
													<xsl:text>aka</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sq')">
													<xsl:text>alb</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'am')">
													<xsl:text>amh</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ar')">
													<xsl:text>ara</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'an')">
													<xsl:text>arg</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hy')">
													<xsl:text>arm</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'as')">
													<xsl:text>asm</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'av')">
													<xsl:text>ava</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ae')">
													<xsl:text>ave</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ay')">
													<xsl:text>aym</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'az')">
													<xsl:text>aze</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bm')">
													<xsl:text>bam</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ba')">
													<xsl:text>bak</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'eu')">
													<xsl:text>baq</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'be')">
													<xsl:text>bel</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bn')">
													<xsl:text>ben</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bh')">
													<xsl:text>bih</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bi')">
													<xsl:text>bis</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'bs')">
													<xsl:text>bos</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'br')">
													<xsl:text>bre</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bg')">
													<xsl:text>bul</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'my')">
													<xsl:text>bur</xsl:text>
												</xsl:if>
                                               	<xsl:if test="contains(.,'ca')">
													<xsl:text>cat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ch')">
													<xsl:text>cha</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ce')">
													<xsl:text>che</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'cs')">
													<xsl:text>cze</xsl:text>
												</xsl:if>
   												<xsl:if test="contains(.,'zh')">
													<xsl:text>chi</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cu')">
													<xsl:text>chu</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cv')">
													<xsl:text>chv</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kw')">
													<xsl:text>cor</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'co')">
													<xsl:text>cos</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cr')">
													<xsl:text>cre</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'da')">
													<xsl:text>dan</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'dv')">
													<xsl:text>div</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nl')">
													<xsl:text>dut</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'dz')">
													<xsl:text>dzo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'en')">
													<xsl:text>eng</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'eo')">
													<xsl:text>epo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'et')">
													<xsl:text>est</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ee')">
													<xsl:text>ewe</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'fo')">
													<xsl:text>fao</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'fj')">
													<xsl:text>fij</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'fi')">
													<xsl:text>fin</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'fr')">
													<xsl:text>fre</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'fy')">
													<xsl:text>fry</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ff')">
													<xsl:text>ful</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gd')">
													<xsl:text>gla</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gl')">
													<xsl:text>glg</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'ka')">
													<xsl:text>geo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'de')">
													<xsl:text>ger</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'el')">
													<xsl:text>gre</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ga')">
													<xsl:text>gle</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gv')">
													<xsl:text>glv</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gn')">
													<xsl:text>grn</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'gu')">
													<xsl:text>guj</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ht')">
													<xsl:text>hat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ha')">
													<xsl:text>hau</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'he')">
													<xsl:text>heb</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hz')">
													<xsl:text>her</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hi')">
													<xsl:text>hin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ho')">
													<xsl:text>hmo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hu')">
													<xsl:text>hun</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'is')">
													<xsl:text>ice</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'io')">
													<xsl:text>ido</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ig')">
													<xsl:text>ibo</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ii')">
													<xsl:text>iii</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'id')">
													<xsl:text>ind</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ia')">
													<xsl:text>ina</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ie')">
													<xsl:text>ile</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'iu')">
													<xsl:text>iku</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ik')">
													<xsl:text>ipk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'it')">
													<xsl:text>ita</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ja')">
													<xsl:text>jpn</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'jv')">
													<xsl:text>jav</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kl')">
													<xsl:text>kal</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'kn')">
													<xsl:text>kan</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kr')">
													<xsl:text>kau</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ks')">
													<xsl:text>kas</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kk')">
													<xsl:text>kaz</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'km')">
													<xsl:text>khm</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ki')">
													<xsl:text>kik</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'rw')">
													<xsl:text>kin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ky')">
													<xsl:text>kir</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kv')">
													<xsl:text>kom</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kg')">
													<xsl:text>kon</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ko')">
													<xsl:text>kor</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kj')">
													<xsl:text>kua</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ku')">
													<xsl:text>kur</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lo')">
													<xsl:text>lao</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'la')">
													<xsl:text>lat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lv')">
													<xsl:text>lav</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lb')">
													<xsl:text>ltz</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'li')">
													<xsl:text>lim</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ln')">
													<xsl:text>lin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lt')">
													<xsl:text>lit</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lu')">
													<xsl:text>lub</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'lg')">
													<xsl:text>lug</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'mk')">
													<xsl:text>mac</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mg')">
													<xsl:text>mlg</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ms')">
													<xsl:text>may</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ml')">
													<xsl:text>mal</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mt')">
													<xsl:text>mlt</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mi')">
													<xsl:text>mao</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mr')">
													<xsl:text>mar</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mh')">
													<xsl:text>mah</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mo')">
													<xsl:text>mol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mn')">
													<xsl:text>mon</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'na')">
													<xsl:text>nau</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nv')">
													<xsl:text>nav</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nd')">
													<xsl:text>nde</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'nr')">
													<xsl:text>nbl</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ng')">
													<xsl:text>ndo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ne')">
													<xsl:text>nep</xsl:text>
												</xsl:if>
	                                            <xsl:if test="contains(.,'no')">
													<xsl:text>nor</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'nb')">
													<xsl:text>nob</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nn')">
													<xsl:text>nno</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ny')">
													<xsl:text>nya</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'oc')">
													<xsl:text>oci</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'oj')">
													<xsl:text>oji</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'or')">
													<xsl:text>ori</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'om')">
													<xsl:text>orm</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'os')">
													<xsl:text>oss</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pi')">
													<xsl:text>pli</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pa')">
													<xsl:text>pan</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'fa')">
													<xsl:text>per</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pl')">
													<xsl:text>pol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pt')">
													<xsl:text>por</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ps')">
													<xsl:text>pus</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'qu')">
													<xsl:text>que</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'rm')">
													<xsl:text>roh</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ro')">
													<xsl:text>rum</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'rn')">
													<xsl:text>run</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ru')">
													<xsl:text>rus</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sm')">
													<xsl:text>smo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sg')">
													<xsl:text>sag</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sa')">
													<xsl:text>san</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sc')">
													<xsl:text>srd</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sr')">
													<xsl:text>scc</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'hr')">
													<xsl:text>scr</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'sn')">
													<xsl:text>sna</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sd')">
													<xsl:text>snd</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'si')">
													<xsl:text>sin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sk')">
													<xsl:text>slo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sl')">
													<xsl:text>slv</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'se')">
													<xsl:text>sme</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'so')">
													<xsl:text>som</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'st')">
													<xsl:text>sot</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'es')">
													<xsl:text>spa</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'su')">
													<xsl:text>sun</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sw')">
													<xsl:text>swa</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ss')">
													<xsl:text>ssw</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sv')">
													<xsl:text>swe</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tl')">
													<xsl:text>tgl</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ty')">
													<xsl:text>tah</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tg')">
													<xsl:text>tgk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ta')">
													<xsl:text>tam</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tt')">
													<xsl:text>tat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'te')">
													<xsl:text>tel</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'th')">
													<xsl:text>tha</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bo')">
													<xsl:text>tib</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ti')">
													<xsl:text>tir</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'to')">
													<xsl:text>ton</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ts')">
													<xsl:text>tso</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tn')">
													<xsl:text>tsn</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tr')">
													<xsl:text>tur</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tk')">
													<xsl:text>tuk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tw')">
													<xsl:text>twi</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ug')">
													<xsl:text>uig</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'uk')">
													<xsl:text>ukr</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ur')">
													<xsl:text>urd</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'uz')">
													<xsl:text>uzb</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ve')">
													<xsl:text>ven</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'vi')">
													<xsl:text>vie</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'vo')">
													<xsl:text>vol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'wa')">
													<xsl:text>wln</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cy')">
													<xsl:text>wel</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'wo')">
													<xsl:text>wol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'xh')">
													<xsl:text>xho</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'yi')">
													<xsl:text>yid</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'yo')">
													<xsl:text>yor</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'za')">
													<xsl:text>zha</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'zu')">
													<xsl:text>zul</xsl:text>
												</xsl:if>
											
											</xsl:when>
											<xsl:otherwise>
												<xsl:choose>

                                                    <xsl:when test="contains(.,'Afar')">
														<xsl:text>aar</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'afar')">
														<xsl:text>aar</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Abkhazian')">
														<xsl:text>abk</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'abkhazian')">
														<xsl:text>abk</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Achinese')">
														<xsl:text>ace</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'achinese')">
														<xsl:text>ace</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Acoli')">
														<xsl:text>ach</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'acoli')">
														<xsl:text>ach</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Adangme')">
														<xsl:text>ada</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'adangme')">
														<xsl:text>ada</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Adyghe')">
														<xsl:text>ady</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'adyghe')">
														<xsl:text>ady</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Afro-Asiatic (Other)')">
														<xsl:text>afa</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'afro-asiatic (other)')">
														<xsl:text>afa</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Afrihilie')">
														<xsl:text>afh</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'afrihili')">
														<xsl:text>afh</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Afrikaans')">
														<xsl:text>afr</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'afrikaans')">
														<xsl:text>afr</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="contains(.,'Ainu')">
														<xsl:text>ain</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'ainu')">
														<xsl:text>ain</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Akan')">
														<xsl:text>aka</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'akan')">
														<xsl:text>aka</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Akkadian')">
														<xsl:text>akk</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'akkadian')">
														<xsl:text>akk</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Albanian')">
														<xsl:text>alb</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'albanian')">
														<xsl:text>alb</xsl:text>
													</xsl:when>
													    <xsl:when test="contains(.,'Aleut')">
														<xsl:text>ale</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'aleut')">
														<xsl:text>ale</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Algonquian languages')">
														<xsl:text>art</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'algonquian languages')">
														<xsl:text>art</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Southern Altai')">
														<xsl:text>art</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'southern altai')">
														<xsl:text>art</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Amharic')">
														<xsl:text>amh</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'amharic')">
														<xsl:text>amh</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'English, Old')">
														<xsl:text>ang</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'english, old')">
														<xsl:text>ang</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Angika')">
														<xsl:text>anp</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'angika')">
														<xsl:text>anp</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Apache languages')">
														<xsl:text>apa</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'Apache languages')">
														<xsl:text>apa</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Arabic')">
														<xsl:text>arr</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'arabic')">
														<xsl:text>arr</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Aramaic')">
														<xsl:text>arc</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'aramaic')">
														<xsl:text>arc</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Aragonese')">
														<xsl:text>arg</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'aragonese')">
														<xsl:text>arg</xsl:text>
													</xsl:when>
													    <xsl:when test="contains(.,'Armenian')">
														<xsl:text>arm</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'armenian')">
														<xsl:text>arm</xsl:text>
													</xsl:when>

                                                        <xsl:when test="contains(.,'Mapudungun')">
														<xsl:text>arn</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'mapudungun')">
														<xsl:text>arn</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Arapaho')">
														<xsl:text>arp</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'arapaho')">
														<xsl:text>arp</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Artificial (Other)')">
														<xsl:text>art</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'artificial (other)')">
														<xsl:text>art</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'Arawak')">
														<xsl:text>arw</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'arawak')">
														<xsl:text>arw</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Assamese')">
														<xsl:text>asm</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'assamese')">
														<xsl:text>asm</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Asturian')">
														<xsl:text>ast</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'asturian')">
														<xsl:text>ast</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Athapascan languages')">
														<xsl:text>ath</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'athapascan languages')">
														<xsl:text>ath</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Australian languages')">
														<xsl:text>aus</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'australian languages')">
														<xsl:text>aus</xsl:text>
													</xsl:when>
                                                		<xsl:when test="contains(.,'Avaric')">
														<xsl:text>ava</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'avaric')">
														<xsl:text>ava</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Avestan')">
														<xsl:text>ave</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'avestan')">
														<xsl:text>ave</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Awadhi')">
														<xsl:text>awa</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'awadhi')">
														<xsl:text>awa</xsl:text>
													</xsl:when>
                                                    	<xsl:when test="contains(.,'Aymara')">
														<xsl:text>aym</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'aymara')">
														<xsl:text>aym</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Azerbaijani')">
														<xsl:text>aze</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'azerbaijani')">
														<xsl:text>aze</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Basa')">
														<xsl:text>bas</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'basa')">
														<xsl:text>bas</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Basque')">
														<xsl:text>baq</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	basque')">
														<xsl:text>baq</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Bulgarian')">
														<xsl:text>bul</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	bulgarian')">
														<xsl:text>bul</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Byelorussian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	byelorussian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Belarusian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	belarusian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Breton')">
														<xsl:text>bre</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	breton')">
														<xsl:text>bre</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	Catalan')">
														<xsl:text>cat</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	catalan')">
														<xsl:text>cat</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Chinese')">
														<xsl:text>chi</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	chinese')">
														<xsl:text>chi</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'croatian')">
														<xsl:text>scr</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Croatian')">
														<xsl:text>scr</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'	Czech')">
														<xsl:text>cze</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	czech')">
														<xsl:text>cze</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Danish')">
														<xsl:text>dan</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'danish')">
														<xsl:text>dan</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Dutch')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	dutch')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'english')">
														<xsl:text>eng</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'English')">
														<xsl:text>eng</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'esperanto')">
														<xsl:text>epo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Esperanto')">
														<xsl:text>epo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'estonian')">
														<xsl:text>est</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Estonian')">
														<xsl:text>est</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'faroese')">
														<xsl:text>fao</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Faroese')">
														<xsl:text>fao</xsl:text>
													</xsl:when>
					  								<xsl:when test="contains(.,'filipino')">
														<xsl:text>fil</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Filipino')">
														<xsl:text>fil</xsl:text>
													</xsl:when>
												<xsl:when test="contains(.,'finnish')">
														<xsl:text>fin</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Finnish')">
														<xsl:text>fin</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'finno-ugrian')">
														<xsl:text>fiu</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Finno-ugrian')">
														<xsl:text>fiu</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'flemish')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Flemish')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'french')">
														<xsl:text>fre</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'French')">
														<xsl:text>fre</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'frisian')">
														<xsl:text>fry</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Frisian')">
														<xsl:text>fry</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'gaelic')">
														<xsl:text>gla</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Gaelic')">
														<xsl:text>gla</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'galician')">
														<xsl:text>glg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Galician')">
														<xsl:text>glg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'galla')">
														<xsl:text>orm</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Galla')">
														<xsl:text>orm</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'georgian')">
														<xsl:text>geo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Georgian')">
														<xsl:text>geo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'german')">
														<xsl:text>ger</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'German')">
														<xsl:text>ger</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'greek ancient')">
														<xsl:text>grc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Greek Ancient')">
														<xsl:text>grc</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'greek classical')">
														<xsl:text>grc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Greek classical')">
														<xsl:text>grc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'greek')">
														<xsl:text>gre</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Greek')">
														<xsl:text>gre</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'hawaian')">
														<xsl:text>haw</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hawaian')">
														<xsl:text>haw</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'hebrew')">
														<xsl:text>heb</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hebrew')">
														<xsl:text>heb</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'hindi')">
														<xsl:text>hin</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hindi')">
														<xsl:text>hin</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'hungarian')">
														<xsl:text>hun</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hungarian')">
														<xsl:text>hun</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'icelandic')">
														<xsl:text>ice</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Icelandic')">
														<xsl:text>ice</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'indonesian')">
														<xsl:text>ind</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Indonesian')">
														<xsl:text>ind</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Interlingua')">
														<xsl:text>ina</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'interlingua')">
														<xsl:text>ina</xsl:text>
													</xsl:when>

													<xsl:when test="contains(.,'irish')">
														<xsl:text>gle</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Irish')">
														<xsl:text>gle</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'italian')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Italian')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'japanese')">
														<xsl:text>jpn</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Japanese')">
														<xsl:text>jpn</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'korean')">
														<xsl:text>kor</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Korean')">
														<xsl:text>kor</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'latin')">
														<xsl:text>lat</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Latin')">
														<xsl:text>lat</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'latvian')">
														<xsl:text>lav</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Latvian')">
														<xsl:text>lav</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'lithuanian')">
														<xsl:text>lit</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Lithuanian')">
														<xsl:text>lit</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	malagasy')">
														<xsl:text>mlg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Malagasy')">
														<xsl:text>mlg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	macedonian')">
														<xsl:text>mac</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Macedonian')">
														<xsl:text>mac</xsl:text>
													</xsl:when>
                                                    			<xsl:when test="contains(.,'	malay')">
														<xsl:text>may</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Malay')">
														<xsl:text>may</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'	malayo-polynesian')">
														<xsl:text>may</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Malayo-Polynesian')">
														<xsl:text>may</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	moldavian')">
														<xsl:text>mol</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Moldavian')">
														<xsl:text>mol</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'	multiple languages')">
														<xsl:text>mul</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Multiple languages')">
														<xsl:text>mul</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'	multilingual')">
														<xsl:text>mul</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Multilingual')">
														<xsl:text>mul</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	northern sotho')">
														<xsl:text>nso</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Northern Sotho')">
														<xsl:text>nso</xsl:text>
													</xsl:when>
											<xsl:when test="contains(.,'	norwegian')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Norwegian')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'	nynorsk')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Nynorsk')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	occitan')">
														<xsl:text>oci</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Occitan')">
														<xsl:text>oci</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'oromo')">
														<xsl:text>orm</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Oromo')">
														<xsl:text>orm</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'	persian')">
														<xsl:text>per</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Persian')">
														<xsl:text>per</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	piedmontese')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Piedmontese')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	polish')">
														<xsl:text>pol</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Polish')">
														<xsl:text>pol</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Portuguese')">
														<xsl:text>por</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'portuguese')">
														<xsl:text>por</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	romanian')">
														<xsl:text>rum</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Romanian')">
														<xsl:text>rum</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	romansh')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Romansh')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	raeto-romance')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Raeto-Romance')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													
														<xsl:when test="contains(.,'Russian')">
														<xsl:text>rus</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'russian')">
														<xsl:text>rus</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'sardinian')">
														<xsl:text>srd</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Sardinian')">
														<xsl:text>srd</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'serbo-croatian with cyrillic alphabet/serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Serbo-croatian with cyrillic alphabet/serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'serbo-croatian with roman alphabet/croatian')">
														<xsl:text>scr</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Serbo-croatian with roman alphabet/croatian')">
														<xsl:text>scr</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'serrano')">
                                                    <xsl:text>ser</xsl:text>
                                                </xsl:when>
                                                        <xsl:when test="contains(.,'Serrano')">
														<xsl:text>ser</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'slavic')">
														<xsl:text>sla</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slavic')">
														<xsl:text>sla</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'slovak')">
														<xsl:text>slo</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slovak')">
														<xsl:text>slo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'slovene')">
														<xsl:text>slv</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slovene')">
														<xsl:text>slv</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'slovenian')">
														<xsl:text>slv</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slovenian')">
														<xsl:text>slv</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Sorbian languages')">
														<xsl:text>wen</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Sorbian languages')">
														<xsl:text>wen</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'Spanish')">
														<xsl:text>spa</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'spanish')">
														<xsl:text>spa</xsl:text>
													</xsl:when>
												<xsl:when test="contains(.,'susu')">
														<xsl:text>sus</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Susu')">
														<xsl:text>sus</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'swahili')">
														<xsl:text>swa</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Swahili')">
														<xsl:text>swa</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'swedish')">
														<xsl:text>swe</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Swedish')">
														<xsl:text>swe</xsl:text>
													</xsl:when>
                                                    <xsl:when test="contains(.,'tagalog')">
														<xsl:text>tgl</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Tagalog')">
														<xsl:text>tgl</xsl:text>
													</xsl:when>
                                                        <xsl:when test="contains(.,'tswana')">
														<xsl:text>tsn</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Tswana')">
														<xsl:text>tsn</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'turkish')">
														<xsl:text>tur</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Turkish')">
														<xsl:text>tur</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'ukrainian')">
														<xsl:text>ukr</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Ukrainian')">
														<xsl:text>ukr</xsl:text>
													</xsl:when>
												
														<xsl:when test="contains(.,'vietnamese')">
														<xsl:text>vie</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Vietnamese')">
														<xsl:text>vie</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'welsh')">
														<xsl:text>wel</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Welsh')">
														<xsl:text>wel</xsl:text>
													</xsl:when>
													
													<xsl:when test="contains(.,'wendic')">
														<xsl:text>wen</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Wendic')">
														<xsl:text>wen</xsl:text>
													</xsl:when>
													
													<xsl:otherwise>
														<xsl:value-of select="."/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:otherwise>
										</xsl:choose>
									</Index:field>
								</xsl:for-each>
								<xsl:for-each select="la">
													<Index:field Index:repeat="false" Index:name="llang" Index:navn="lsp" Index:type="keyword" Index:boostFactor="10">
			
									<xsl:choose>
											<xsl:when test="string-length(.)=3">
												<xsl:value-of select="."/>
											
											</xsl:when>
											<xsl:when test="string-length(.)=2">
												
												<xsl:if test="contains(.,'ab')">
													<xsl:text>abk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'aa')">
													<xsl:text>aar</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'af')">
													<xsl:text>afr</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ak')">
													<xsl:text>aka</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sq')">
													<xsl:text>alb</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'am')">
													<xsl:text>amh</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ar')">
													<xsl:text>ara</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'an')">
													<xsl:text>arg</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hy')">
													<xsl:text>arm</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'as')">
													<xsl:text>asm</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'av')">
													<xsl:text>ava</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ae')">
													<xsl:text>ave</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ay')">
													<xsl:text>aym</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'az')">
													<xsl:text>aze</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bm')">
													<xsl:text>bam</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ba')">
													<xsl:text>bak</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'eu')">
													<xsl:text>baq</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'be')">
													<xsl:text>bel</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bn')">
													<xsl:text>ben</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bh')">
													<xsl:text>bih</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bi')">
													<xsl:text>bis</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'bs')">
													<xsl:text>bos</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'br')">
													<xsl:text>bre</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bg')">
													<xsl:text>bul</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'my')">
													<xsl:text>bur</xsl:text>
												</xsl:if>
                                               	<xsl:if test="contains(.,'ca')">
													<xsl:text>cat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ch')">
													<xsl:text>cha</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ce')">
													<xsl:text>che</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'cs')">
													<xsl:text>cze</xsl:text>
												</xsl:if>
   												<xsl:if test="contains(.,'zh')">
													<xsl:text>chi</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cu')">
													<xsl:text>chu</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cv')">
													<xsl:text>chv</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kw')">
													<xsl:text>cor</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'co')">
													<xsl:text>cos</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cr')">
													<xsl:text>cre</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'da')">
													<xsl:text>dan</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'dv')">
													<xsl:text>div</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nl')">
													<xsl:text>dut</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'dz')">
													<xsl:text>dzo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'en')">
													<xsl:text>eng</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'eo')">
													<xsl:text>epo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'et')">
													<xsl:text>est</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ee')">
													<xsl:text>ewe</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'fo')">
													<xsl:text>fao</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'fj')">
													<xsl:text>fij</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'fi')">
													<xsl:text>fin</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'fr')">
													<xsl:text>fre</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'fy')">
													<xsl:text>fry</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ff')">
													<xsl:text>ful</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gd')">
													<xsl:text>gla</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gl')">
													<xsl:text>glg</xsl:text>
												</xsl:if>

												<xsl:if test="contains(.,'ka')">
													<xsl:text>geo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'de')">
													<xsl:text>ger</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'el')">
													<xsl:text>gre</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ga')">
													<xsl:text>gle</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gv')">
													<xsl:text>glv</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'gn')">
													<xsl:text>grn</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'gu')">
													<xsl:text>guj</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ht')">
													<xsl:text>hat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ha')">
													<xsl:text>hau</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'he')">
													<xsl:text>heb</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hz')">
													<xsl:text>her</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hi')">
													<xsl:text>hin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ho')">
													<xsl:text>hmo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'hu')">
													<xsl:text>hun</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'is')">
													<xsl:text>ice</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'io')">
													<xsl:text>ido</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ig')">
													<xsl:text>ibo</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ii')">
													<xsl:text>iii</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'id')">
													<xsl:text>ind</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ia')">
													<xsl:text>ina</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ie')">
													<xsl:text>ile</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'iu')">
													<xsl:text>iku</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ik')">
													<xsl:text>ipk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'it')">
													<xsl:text>ita</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ja')">
													<xsl:text>jpn</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'jv')">
													<xsl:text>jav</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kl')">
													<xsl:text>kal</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'kn')">
													<xsl:text>kan</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kr')">
													<xsl:text>kau</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ks')">
													<xsl:text>kas</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kk')">
													<xsl:text>kaz</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'km')">
													<xsl:text>khm</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ki')">
													<xsl:text>kik</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'rw')">
													<xsl:text>kin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ky')">
													<xsl:text>kir</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kv')">
													<xsl:text>kom</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kg')">
													<xsl:text>kon</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ko')">
													<xsl:text>kor</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'kj')">
													<xsl:text>kua</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ku')">
													<xsl:text>kur</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lo')">
													<xsl:text>lao</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'la')">
													<xsl:text>lat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lv')">
													<xsl:text>lav</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lb')">
													<xsl:text>ltz</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'li')">
													<xsl:text>lim</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ln')">
													<xsl:text>lin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lt')">
													<xsl:text>lit</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'lu')">
													<xsl:text>lub</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'lg')">
													<xsl:text>lug</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'mk')">
													<xsl:text>mac</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mg')">
													<xsl:text>mlg</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ms')">
													<xsl:text>may</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ml')">
													<xsl:text>mal</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mt')">
													<xsl:text>mlt</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mi')">
													<xsl:text>mao</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mr')">
													<xsl:text>mar</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mh')">
													<xsl:text>mah</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mo')">
													<xsl:text>mol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'mn')">
													<xsl:text>mon</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'na')">
													<xsl:text>nau</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nv')">
													<xsl:text>nav</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nd')">
													<xsl:text>nde</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'nr')">
													<xsl:text>nbl</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ng')">
													<xsl:text>ndo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ne')">
													<xsl:text>nep</xsl:text>
												</xsl:if>
	                                            <xsl:if test="contains(.,'no')">
													<xsl:text>nor</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'nb')">
													<xsl:text>nob</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'nn')">
													<xsl:text>nno</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'ny')">
													<xsl:text>nya</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'oc')">
													<xsl:text>oci</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'oj')">
													<xsl:text>oji</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'or')">
													<xsl:text>ori</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'om')">
													<xsl:text>orm</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'os')">
													<xsl:text>oss</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pi')">
													<xsl:text>pli</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pa')">
													<xsl:text>pan</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'fa')">
													<xsl:text>per</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pl')">
													<xsl:text>pol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'pt')">
													<xsl:text>por</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ps')">
													<xsl:text>pus</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'qu')">
													<xsl:text>que</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'rm')">
													<xsl:text>roh</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ro')">
													<xsl:text>rum</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'rn')">
													<xsl:text>run</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ru')">
													<xsl:text>rus</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sm')">
													<xsl:text>smo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sg')">
													<xsl:text>sag</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sa')">
													<xsl:text>san</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sc')">
													<xsl:text>srd</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sr')">
													<xsl:text>scc</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'hr')">
													<xsl:text>scr</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'sn')">
													<xsl:text>sna</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sd')">
													<xsl:text>snd</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'si')">
													<xsl:text>sin</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sk')">
													<xsl:text>slo</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sl')">
													<xsl:text>slv</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'se')">
													<xsl:text>sme</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'so')">
													<xsl:text>som</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'st')">
													<xsl:text>sot</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'es')">
													<xsl:text>spa</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'su')">
													<xsl:text>sun</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sw')">
													<xsl:text>swa</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ss')">
													<xsl:text>ssw</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'sv')">
													<xsl:text>swe</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tl')">
													<xsl:text>tgl</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ty')">
													<xsl:text>tah</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tg')">
													<xsl:text>tgk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ta')">
													<xsl:text>tam</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tt')">
													<xsl:text>tat</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'te')">
													<xsl:text>tel</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'th')">
													<xsl:text>tha</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'bo')">
													<xsl:text>tib</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ti')">
													<xsl:text>tir</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'to')">
													<xsl:text>ton</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ts')">
													<xsl:text>tso</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tn')">
													<xsl:text>tsn</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tr')">
													<xsl:text>tur</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tk')">
													<xsl:text>tuk</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'tw')">
													<xsl:text>twi</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ug')">
													<xsl:text>uig</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'uk')">
													<xsl:text>ukr</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ur')">
													<xsl:text>urd</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'uz')">
													<xsl:text>uzb</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'ve')">
													<xsl:text>ven</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'vi')">
													<xsl:text>vie</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'vo')">
													<xsl:text>vol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'wa')">
													<xsl:text>wln</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'cy')">
													<xsl:text>wel</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'wo')">
													<xsl:text>wol</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'xh')">
													<xsl:text>xho</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'yi')">
													<xsl:text>yid</xsl:text>
												</xsl:if>
												<xsl:if test="contains(.,'yo')">
													<xsl:text>yor</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'za')">
													<xsl:text>zha</xsl:text>
												</xsl:if>
                                                <xsl:if test="contains(.,'zu')">
													<xsl:text>zul</xsl:text>
												</xsl:if>
											
											</xsl:when>
											<xsl:otherwise>
												<xsl:choose>
													
													<xsl:when test="contains(.,'Afrikaans')">
														<xsl:text>afr</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'afrikaans')">
														<xsl:text>afr</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'Albanian')">
														<xsl:text>alb</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'albanian')">
														<xsl:text>alb</xsl:text>
													</xsl:when>
																			
													
													
														<xsl:when test="contains(.,'Arabic')">
														<xsl:text>afr</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'arabic')">
														<xsl:text>afr</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Aragonese')">
														<xsl:text>arg</xsl:text>
													</xsl:when>
																<xsl:when test="contains(.,'aragonese')">
														<xsl:text>arg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Armenian')">
														<xsl:text>arm</xsl:text>
													</xsl:when>
																<xsl:when test="contains(.,'armenian')">
														<xsl:text>arm</xsl:text>
													</xsl:when>
													
													<xsl:when test="contains(.,'Azerbaijani')">
														<xsl:text>aze</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'azerbaijani')">
														<xsl:text>aze</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Basa')">
														<xsl:text>bas</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'basa')">
														<xsl:text>bas</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Basque')">
														<xsl:text>baq</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	basque')">
														<xsl:text>baq</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Bulgarian')">
														<xsl:text>bul</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	bulgarian')">
														<xsl:text>bul</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Byelorussian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	byelorussian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Belorussian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	belorussian')">
														<xsl:text>bel</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Breton')">
														<xsl:text>bre</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	breton')">
														<xsl:text>bre</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	Catalan')">
														<xsl:text>cat</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	catalan')">
														<xsl:text>cat</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Chinese')">
														<xsl:text>chi</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	chinese')">
														<xsl:text>chi</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	Czech')">
														<xsl:text>cze</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	czech')">
														<xsl:text>cze</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Danish')">
														<xsl:text>dan</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	danish')">
														<xsl:text>dan</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Dutch')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	dutch')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'english')">
														<xsl:text>eng</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'English')">
														<xsl:text>eng</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'esperanto')">
														<xsl:text>epo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Esperanto')">
														<xsl:text>epo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'estonian')">
														<xsl:text>est</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Estonian')">
														<xsl:text>est</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'faroese')">
														<xsl:text>fao</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Faroese')">
														<xsl:text>fao</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'filipino')">
														<xsl:text>tgl</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Filipino')">
														<xsl:text>tgl</xsl:text>
													</xsl:when>
													
												<xsl:when test="contains(.,'finnish')">
														<xsl:text>fin</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Finnish')">
														<xsl:text>fin</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'finno-ugrian')">
														<xsl:text>fiu</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Finno-ugrian')">
														<xsl:text>fiu</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'flemish')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Flemish')">
														<xsl:text>dut</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'french')">
														<xsl:text>fre</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'French')">
														<xsl:text>fre</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'frisian')">
														<xsl:text>fry</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Frisian')">
														<xsl:text>fry</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'gaelic')">
														<xsl:text>gla</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Gaelic')">
														<xsl:text>gla</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'galician')">
														<xsl:text>glg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Galician')">
														<xsl:text>glg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'galla')">
														<xsl:text>orm</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Galla')">
														<xsl:text>orm</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'georgian')">
														<xsl:text>geo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Georgian')">
														<xsl:text>geo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'german')">
														<xsl:text>ger</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'German')">
														<xsl:text>ger</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'greek classical')">
														<xsl:text>grc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Greek classical')">
														<xsl:text>grc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'greek')">
														<xsl:text>gre</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Greek')">
														<xsl:text>gre</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'hawaian')">
														<xsl:text>haw</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hawaian')">
														<xsl:text>haw</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'hebrew')">
														<xsl:text>heb</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hebrew')">
														<xsl:text>heb</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'hindi')">
														<xsl:text>hin</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hindi')">
														<xsl:text>hin</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'hungarian')">
														<xsl:text>hun</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Hungarian')">
														<xsl:text>hun</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'icelandic')">
														<xsl:text>ice</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Icelandic')">
														<xsl:text>ice</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'indonesian')">
														<xsl:text>ind</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Interlingua')">
														<xsl:text>ina</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'interlingua')">
														<xsl:text>ina</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Indonesian')">
														<xsl:text>ind</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'irish')">
														<xsl:text>gle</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Irish')">
														<xsl:text>gle</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'italian')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Italian')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'japanese')">
														<xsl:text>jpn</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Japanese')">
														<xsl:text>jpn</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'korean')">
														<xsl:text>kor</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Korean')">
														<xsl:text>kor</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'latin')">
														<xsl:text>lat</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Latin')">
														<xsl:text>lat</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'latvian')">
														<xsl:text>lav</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Latvian')">
														<xsl:text>lav</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'lithuanian')">
														<xsl:text>lit</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Lithuanian')">
														<xsl:text>lit</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	malagasy')">
														<xsl:text>mlg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Malagasy')">
														<xsl:text>mlg</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	macedonian')">
														<xsl:text>mac</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Macedonian')">
														<xsl:text>mac</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	malayo-polynesian')">
														<xsl:text>may</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Malayo-Polynesian')">
														<xsl:text>may</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	moldavian')">
														<xsl:text>mol</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Moldavian')">
														<xsl:text>mol</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	multilingual')">
														<xsl:text>mul</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Multilingual')">
														<xsl:text>mul</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	northern sotho')">
														<xsl:text>nso</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Northern Sotho')">
														<xsl:text>nso</xsl:text>
													</xsl:when>
											<xsl:when test="contains(.,'	norwegian')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Norwegian')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
															<xsl:when test="contains(.,'	nynorsk')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Nynorsk')">
														<xsl:text>nor</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	occitan')">
														<xsl:text>oci</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Occitan')">
														<xsl:text>oci</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	persian')">
														<xsl:text>per</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Persian')">
														<xsl:text>per</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	piedmontese')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Piedmontese')">
														<xsl:text>ita</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	polish')">
														<xsl:text>pol</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Polish')">
														<xsl:text>pol</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Portuguese')">
														<xsl:text>por</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'portuguese')">
														<xsl:text>por</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	romanian')">
														<xsl:text>rum</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Romanian')">
														<xsl:text>rum</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'	romansh')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Romansh')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	raeto-romance')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'	Raeto-Romance')">
														<xsl:text>roh</xsl:text>
													</xsl:when>
													
														<xsl:when test="contains(.,'Russian')">
														<xsl:text>rus</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'russian')">
														<xsl:text>rus</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'sardinian')">
														<xsl:text>srd</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Sardinian')">
														<xsl:text>srd</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'serbo-croatian with cyrillic alphabet/serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Serbo-croatian with cyrillic alphabet/serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'serbo-croatian with roman alphabet/croatian')">
														<xsl:text>scr</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Serbo-croatian with roman alphabet/croatian')">
														<xsl:text>scr</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Serbian')">
														<xsl:text>scc</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'slavic')">
														<xsl:text>sla</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slavic')">
														<xsl:text>sla</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'slovak')">
														<xsl:text>slo</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slovak')">
														<xsl:text>slo</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'slovene')">
														<xsl:text>slv</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Slovene')">
														<xsl:text>slv</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'Spanish')">
														<xsl:text>spa</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'spanish')">
														<xsl:text>spa</xsl:text>
													</xsl:when>
												<xsl:when test="contains(.,'susu')">
														<xsl:text>sus</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Susu')">
														<xsl:text>sus</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'swahili')">
														<xsl:text>swa</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Swahili')">
														<xsl:text>swa</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'swedish')">
														<xsl:text>swe</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Swedish')">
														<xsl:text>swe</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'tswana')">
														<xsl:text>tsn</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Tswana')">
														<xsl:text>tsn</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'turkish')">
														<xsl:text>tur</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Turkish')">
														<xsl:text>tur</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'ukrainian')">
														<xsl:text>ukr</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Ukrainian')">
														<xsl:text>ukr</xsl:text>
													</xsl:when>
												
														<xsl:when test="contains(.,'vietnamese')">
														<xsl:text>vie</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Vietnamese')">
														<xsl:text>vie</xsl:text>
													</xsl:when>
													<xsl:when test="contains(.,'welsh')">
														<xsl:text>wel</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Welsh')">
														<xsl:text>wel</xsl:text>
													</xsl:when>
													
													<xsl:when test="contains(.,'wendic')">
														<xsl:text>wen</xsl:text>
													</xsl:when>
														<xsl:when test="contains(.,'Wendic')">
														<xsl:text>wen</xsl:text>
													</xsl:when>
													
													<xsl:otherwise>
														<xsl:value-of select="."/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:otherwise>
										</xsl:choose>
									</Index:field>
								</xsl:for-each>
	
	</xsl:template>
</xsl:stylesheet>
