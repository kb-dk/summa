<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:dobundle="http://doms.statsbiblioteket.dk/types/digitalobjectbundle/default/0/1/#"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                xmlns:pbcore="http://www.pbcore.org/PBCore/PBCoreNamespace.html"
                xmlns:oai="http://www.openarchives.org/OAI/2.0/"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:v3="http://www.loc.gov/mods/v3"
                xmlns:java="http://xml.apache.org/xalan/java"
                xmlns:fedora_model="info:fedora/fedora-system:def/model#"
                xmlns:tns="http://doms.statsbiblioteket.dk/types/access/0/1/#"
                xmlns:rc="http://statsbiblioteket.dk/summa/2009/Record"
                version="1.0" exclude-result-prefixes="xs xsl dobundle dc mods rdf foxml oai oai_dc fedora_model pbcore java tns v3 tns rc">
    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml" omit-xml-declaration="yes"/>
    <xsl:param name="recordID" />
    <xsl:param name="recordBase" />

    <xsl:template match="/">
        <xsl:for-each select="rc:record">

        <xsl:variable name="id"><xsl:value-of select="rc:content/dobundle:digitalObjectBundle/foxml:digitalObject/@PID"/></xsl:variable>
        <xsl:variable name="pageObject" select="rc:content/dobundle:digitalObjectBundle/foxml:digitalObject[foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/fedora_model:hasModel[@rdf:resource='info:fedora/doms:ContentModel_Page']]"/>
        <xsl:variable name="editionObject" select="rc:content/dobundle:digitalObjectBundle/foxml:digitalObject[foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/fedora_model:hasModel[@rdf:resource='info:fedora/doms:ContentModel_Edition']]"/>
        <xsl:variable name="titleObject" select="rc:parents/rc:record/rc:content/dobundle:digitalObjectBundle/foxml:digitalObject[foxml:datastream[@ID='RELS-EXT']/foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/rdf:Description/fedora_model:hasModel[@rdf:resource='info:fedora/doms:ContentModel_Newspaper']]"/>


        <!-- TODO: If pageObject is empty, there is no corresponding image.
             Here and now, we should discard the document (setting modsExists=no is a quick hack to do that) -->

        <!-- For precise designation, refer to altosegment#segmentid -->
        <doc>
            <!-- Copied to authID by the schema -->
            <field name="pageUUID"><xsl:text>doms_aviser_page:</xsl:text><xsl:value-of select="$pageObject/@PID"/></field>
            <!-- UUID for edition, copied to authID by the schema -->
            <field name="editionUUID"><xsl:text>doms_aviser_edition:</xsl:text><xsl:value-of select="$editionObject/@PID"/></field>
            <!-- UUID for newpaper title -->
            <field name="titleUUID"><xsl:text>doms_aviser_title:</xsl:text><xsl:value-of select="$titleObject/@PID"/></field>


            <!--is there a mods record for the newspaper title present? -->

            <xsl:if test="$titleObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/v3:mods/v3:titleInfo[@type='uniform']/v3:title!=''">
                <field name="titleObjectExists"><xsl:text>yes</xsl:text></field>
            </xsl:if>
          <!-- is there a page present? -->
            <xsl:if test="$pageObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:part/mods:extent[@unit='pages']/mods:start!=''">
                <field name="pageExists"><xsl:text>yes</xsl:text></field>
            </xsl:if>



                <field name="recordID"><xsl:value-of select="$recordID"/></field>
                <field name="recordBase"><xsl:value-of select="$recordBase"/></field>
                <field name="lma_long"><xsl:text>avis</xsl:text></field>

                <!-- TODO: If recordID does not contain '-segment', this gives an empty ID. Fix so that the full ID is assigned instead -->
                <xsl:variable name="sansSegment" select="substring-before($recordID, '-segment')"/>
                <xsl:variable name="realRecordID" select="substring-before($sansSegment, '-_nogroup_')"/>
                <xsl:variable name="segment" select="substring-after($recordID, '-segment-')"/>
                <field name="shortformat">
                    <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                    <shortrecord>
                        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
                            <rdf:Description>
                                <dc:title>
                                    <xsl:value-of select="$pageObject/foxml:datastream[@ID='ALTO']/foxml:datastreamVersion/foxml:xmlContent/rc:altosegment/rc:headline"/>
                                </dc:title>
                                <dc:creator>

                                </dc:creator>
                                <dc:date>
                                    <xsl:value-of select="substring($editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:originInfo/mods:dateIssued,1,4)"/>
                                </dc:date>
                                <newspaperTitle>
                                    <xsl:value-of select="$titleObject[position()=1]/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/v3:mods/v3:titleInfo[@type='uniform']/v3:title"/>
                                </newspaperTitle>
                                <dateTime>
                                    <xsl:value-of select="$editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:originInfo/mods:dateIssued"/>
                                </dateTime>
                                <dateTimeIso>
                                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Datetime.solrDateTime($editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:originInfo/mods:dateIssued)"/>
                                </dateTimeIso>
                                <newspaperEdition>
                                    <xsl:value-of select="$editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:relatedItem[@type='host']/mods:part/mods:detail[@type='edition']/mods:number"/>
                                </newspaperEdition>
                                <newspaperSection>
                                    <xsl:value-of select="$pageObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:part/mods:detail[@type='sectionLabel']/mods:number"/>
                                </newspaperSection>
                                <newspaperPage>
                                    <xsl:value-of select="$pageObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:part/mods:extent[@unit='pages']/mods:start" />
                                </newspaperPage>
                                <dc:identifier>
                                    <xsl:value-of select="$sansSegment"/>
                                </dc:identifier>
                                <dc:identifier>         <xsl:value-of select="$id"/>
                                </dc:identifier>
                                <dc:type xml:lang="da">avisartikel</dc:type>
                                <dc:type xml:lang="en">newspaper article</dc:type>
                                <xsl:for-each select="$pageObject/foxml:datastream[@ID='DC']/foxml:datastreamVersion/foxml:xmlContent/oai_dc:dc/dc:identifier" >
                                    <xsl:if test="starts-with(.,'hdl')">
                                        <PID><xsl:value-of select="."/></PID>
                                    </xsl:if>
                                </xsl:for-each>

                            </rdf:Description>
                        </rdf:RDF>
                    </shortrecord>
                    <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
                </field>
                          <xsl:variable name="edition_no">
                              <xsl:for-each select="$editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:relatedItem[@type='host']/mods:part/mods:detail[@type='edition']/mods:number">
                                  <xsl:choose>
                                      <xsl:when test="string-length(.)=1">
                                          <xsl:value-of select="concat('00',.)"/>

                                      </xsl:when>
                                      <xsl:when test="string-length(.)=2">
                                          <xsl:value-of select="concat('0',.)"/>

                                      </xsl:when>
                                      <xsl:when test="string-length(.)&gt;2">
                                          <xsl:value-of select="."/>

                                      </xsl:when>
                                  </xsl:choose>
                              </xsl:for-each>
                          </xsl:variable>
                <field name="editionId">
                    <xsl:value-of select="concat($pageObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:relatedItem[@type='host']/mods:titleInfo[@type='uniform']/mods:title,' ',$editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:originInfo/mods:dateIssued,' ',$edition_no)"/>
                </field>
                <field name="familyId">
                    <xsl:value-of select="$pageObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:relatedItem[@type='host']/mods:titleInfo[@type='uniform']/mods:title"/>
                </field>
                <!-- Source uniform title+date -->
                <field name="lso">
                    <xsl:value-of select="concat($titleObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent/v3:mods/v3:titleInfo[@type='uniform']/v3:title,' ',$editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent/mods:mods/mods:originInfo/mods:dateIssued)"/>
                </field>

                <!-- Title data for newspaper title -->
                <xsl:for-each select="$titleObject[position()=1]/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent">
                    <xsl:for-each select="v3:mods/v3:titleInfo[@type='uniform']">

                        <field name="lvx"><xsl:value-of select="v3:title"/></field>
                    </xsl:for-each>
                        <!--Geographic coverage for newspaper -->

                    <xsl:for-each select="v3:mods/v3:subject/v3:hierarchicalGeographic">
                        <xsl:choose>
                            <xsl:when test="v3:country='Danmark'">
                                <field name="llocal_coverage"><xsl:value-of select="v3:area"/></field>
                                <field name="llocal_coverage"><xsl:value-of select="concat(v3:area,' ||| ',v3:city)"/></field>
                            </xsl:when>
                            <xsl:otherwise>
                                <field name="llocal_coverage"><xsl:value-of select="concat(v3:country,' - ',v3:area)"/></field>
                                <field name="llocal_coverage"><xsl:value-of select="concat(v3:country,' - ',v3:area,' ||| ',v3:city)"/></field>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                    <xsl:for-each select="v3:mods/v3:originInfo/v3:place/v3:placeTerm">
                               <field name="lplace"><xsl:value-of select="."/></field>

                    </xsl:for-each>
                </xsl:for-each>
                <!-- Title data for other newspaper titles -->
                <xsl:for-each select="$titleObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent">
                    <xsl:for-each select="v3:mods/v3:titleInfo[@type='uniform']">
                        <field name="vp_org"><xsl:value-of select="v3:title"/></field>


                    </xsl:for-each>

                </xsl:for-each>

                <xsl:for-each select="$pageObject/foxml:datastream[@ID='MODS']/foxml:datastreamVersion/foxml:xmlContent">

                    <xsl:for-each select="mods:mods/mods:part">
                        <!-- section title -->
                        <xsl:if test="mods:detail[@type='sectionLabel']/mods:number!=''">
                            <field name="newspaper_section">
                                <xsl:value-of select="mods:detail[@type='sectionLabel']/mods:number"/>
                            </field>
                        </xsl:if>
                        <!-- edition page no -->
                        <xsl:variable name="page" select="mods:extent[@unit='pages']/mods:start"/>
                        <field name="newspaper_page">
                            <xsl:value-of select="$page"/>
                        </field>
                        <!-- edition page no for sorting -->
                        <xsl:variable name="prefixed_page" select="concat('00000000', $page)"/>
                        <field name="newspaper_page_sort">
                            <xsl:value-of select="substring($prefixed_page, string-length($prefixed_page) - 6, string-length($prefixed_page))"/>
                        </field>
                    </xsl:for-each>

                </xsl:for-each>

                <!-- title and content of article -->
                <xsl:for-each select="$pageObject/foxml:datastream[@ID='ALTO']/foxml:datastreamVersion/foxml:xmlContent">

                    <xsl:for-each select="rc:altosegment">
                      <field name="segment_index"><xsl:value-of select="@segmentIndex"/></field>

                        <xsl:if test="rc:headline!=''">
                            <field name="main_title_org">
                                <xsl:value-of select="rc:headline"/>
                            </field>
                            <field name="lti">
                                <xsl:value-of select="rc:headline"/>
                            </field>
                        </xsl:if>
                        <xsl:for-each select="rc:pageWidth"><field name="page_width"><xsl:value-of select="."/></field></xsl:for-each>
                        <xsl:for-each select="rc:pageHeight"><field name="page_height"><xsl:value-of select="."/></field></xsl:for-each>
                        <xsl:for-each select="rc:pagePixels"><field name="page_pixels"><xsl:value-of select="."/></field></xsl:for-each>
                        
                        <xsl:for-each select="rc:predictedWordAccuracy">
                            <field name="pwa">
                                <xsl:value-of select="."/>
                            </field>
                        </xsl:for-each>
                        <xsl:for-each select="rc:predictedWordAccuracy_sort">
                            <field name="pwa_sort">
                                <xsl:value-of select="."/>
                            </field>
                        </xsl:for-each>
                        <xsl:for-each select="rc:characterErrorRatio">
                            <field name="cer">
                                <xsl:value-of select="."/>
                            </field>
                        </xsl:for-each>
                        <xsl:for-each select="rc:characterErrorRatio_sort">
                            <field name="cer_sort">
                                <xsl:value-of select="."/>
                            </field>
                        </xsl:for-each>

                        <xsl:for-each select="rc:content/rc:textblock">
                            <xsl:if test=".!=''">
                                <field name="fulltext_org">
                                    <xsl:value-of select="."/>
                                </field>
                                <field name="alto_box">x=<xsl:value-of select="@x"/>,y=<xsl:value-of select="@y"/>,w=<xsl:value-of select="@width"/>,h=<xsl:value-of select="@height"/></field>
                            </xsl:if>
                        </xsl:for-each>

                        <xsl:for-each select="rc:content/rc:statistics">
                            <field name="statBlocks"><xsl:value-of select="@blocks"/></field>
                            <field name="statWords"><xsl:value-of select="@words"/></field>
                            <field name="statChars"><xsl:value-of select="@chars"/></field>
                            <field name="statLowercase"><xsl:value-of select="@lowercase"/></field>
                            <field name="statUppercase"><xsl:value-of select="@uppercase"/></field>
                            <field name="statLetters"><xsl:value-of select="@letters"/></field>
                            <field name="statDigits"><xsl:value-of select="@digits"/></field>
                            <field name="statStopchars"><xsl:value-of select="@stopchars"/></field>
                            <field name="statCommas"><xsl:value-of select="@commas"/></field>
                        </xsl:for-each>

                        <xsl:for-each select="rc:illustrations/rc:illustration">
                          <field name="illustration">id=<xsl:value-of select="@id"/>,x=<xsl:value-of select="@x"/>,y=<xsl:value-of select="@y"/>,w=<xsl:value-of select="@width"/>,h=<xsl:value-of select="@height"/></field>
                        </xsl:for-each>

                        <!-- segment no for page -->
                        <field name="page_segment">
                            <xsl:value-of select="@segmentid"/>
                        </field>


                    </xsl:for-each>

                </xsl:for-each>
                <xsl:for-each select="$editionObject/foxml:datastream[@ID='EDITION']/foxml:datastreamVersion/foxml:xmlContent">
                    <xsl:for-each
                            select="mods:mods/mods:relatedItem[@type='host']/mods:part/mods:detail[@type='edition']">
                        <field name="newspaper_edition">
                            <xsl:value-of select="mods:number"/>
                        </field>
                    </xsl:for-each>
                    <xsl:for-each select="mods:mods/mods:originInfo/mods:dateIssued">
                      <field name="iso_dateTime">
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Datetime.solrDateTime(.)"/>
                      </field>
                    </xsl:for-each>
                    <xsl:for-each select="mods:mods/mods:originInfo">
                        <field name="sort_year_asc">
                            <xsl:value-of select="mods:dateIssued"/>
                        </field>
                        <field name="sort_year_desc">
                            <xsl:value-of select="mods:dateIssued"/>
                        </field>
                    </xsl:for-each>

                    <field name="place">
                        <xsl:value-of select="mods:mods/mods:originInfo/mods:place/mods:placeTerm"/>
                    </field>
                    <xsl:for-each select="mods:mods/mods:originInfo">
                        <field name="iso_date">
                            <xsl:value-of select="mods:dateIssued"/>
                        </field>
                        <field name="py">
                            <xsl:value-of select="substring(mods:dateIssued,1,4)"/>
                        </field>

                        <!-- muliggør søgning af datoer i forskellige udformninger som f.eks. 10. januar 2001  og 10/1-2001-->
                        <field name="date">
                            <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Datetime.dateExpand(mods:dateIssued, 'da')"/>
                        </field>
<!--                        <field name="date">
                            <xsl:value-of select="kbext:datetime-expand-date(mods:dateIssued, 'da')"/>
                        </field>-->
                        <!-- muliggør søgning af datoer i forskellige engelsk/amerikanske udformninger som f.eks. 10. january 2001  og January 10, 2001 -->

                        <field name="date">
                            <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Datetime.dateExpand(mods:dateIssued, 'en')"/>
                        </field>

                    </xsl:for-each>


                </xsl:for-each>
                <!-- Acess. klausuleret, individuelt_forbud -->

                <field name="individuelt_forbud">
                    <xsl:choose>
                        <xsl:when
                                test="$pageObject/foxml:datastream[@ID='ACCESS']/foxml:datastreamVersion[last()]/foxml:xmlContent/tns:access/tns:individuelt_forbud/text() = 'Ja'">
                            <xsl:value-of select="'Ja'"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="'Nej'"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </field>

                <field name="klausuleret">
                    <xsl:choose>
                        <xsl:when
                                test="$pageObject/foxml:datastream[@ID='ACCESS']/foxml:datastreamVersion[last()]/foxml:xmlContent/tns:access/tns:klausuleret/text() = 'Ja'">
                            <xsl:value-of select="'Ja'"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="'Nej'"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </field>

                <field name="individuelt_forbud_edition">
                    <xsl:choose>
                        <xsl:when
                                test="$editionObject/foxml:datastream[@ID='ACCESS']/foxml:datastreamVersion[last()]/foxml:xmlContent/tns:access/tns:individuelt_forbud/text() = 'Ja'">
                            <xsl:value-of select="'Ja'"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="'Nej'"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </field>

                <field name="klausuleret_edition">
                    <xsl:choose>
                        <xsl:when
                                test="$editionObject/foxml:datastream[@ID='ACCESS']/foxml:datastreamVersion[last()]/foxml:xmlContent/tns:access/tns:klausuleret/text() = 'Ja'">
                            <xsl:value-of select="'Ja'"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="'Nej'"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </field>


        </doc>
          </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>

