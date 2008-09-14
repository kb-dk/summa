<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:marc="http://www.loc.gov/MARC21/slim" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        exclude-result-prefixes="xsl marc">

        <xsl:output method="xml" encoding="UTF-8"/>

        <xsl:template match="/">

                <xsl:for-each select="marc:record">

                        <xsl:call-template name="lav_felter_hovedpost"/>
                </xsl:for-each>
        </xsl:template>
        <xsl:template name="lav_felter_hovedpost">
                <datafield tag="24x" ind1="0" ind2="0">
                        <xsl:for-each select="marc:datafield[@tag='245']">
                                <xsl:for-each select="marc:subfield">
                                        <xsl:if test="@code='g'">
                                                <subfield code="g">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='a'">
                                                <subfield code="a">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='m'">
                                                <subfield code="m">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='3' or @code='4'">
                                                <subfield code="c">
                                                        <xsl:text>[</xsl:text>
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>]</xsl:text>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='b' or @code='c' or @code='n' or @code='o' or @code='u' or @code='y'">
                                                <subfield code="c">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='l'">
                                                <subfield code="k">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='e'">
                                                <subfield code="e">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='f' or @code='i' or @code='j' or @code='k' ">
                                                <subfield code="e">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='p' ">
                                                <subfield code="p">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='t' ">
                                                <subfield code="t">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='x' ">
                                                <subfield code="x">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                        <xsl:if test="@code='w' ">
                                                <subfield code="w">
                                                        <xsl:value-of select="."/>
                                                </subfield>
                                        </xsl:if>
                                </xsl:for-each>

                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='250']/marc:subfield[@code='a']">
                                <subfield code="w">
                                        <xsl:value-of select="."/>
                                </subfield>

                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='260']/marc:subfield[@code='a' or @code='f']">
                                <subfield code="h">
                                        <xsl:value-of select="."/>
                                </subfield>

                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='260']/marc:subfield[@code='b' or @code='g']">
                                <subfield code="i">
                                        <xsl:value-of select="."/>
                                </subfield>

                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='260']/marc:subfield[@code='c']">
                                <subfield code="j">
                                        <xsl:value-of select="."/>
                                </subfield>

                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='300']">
                                <subfield code="i">
                                        <xsl:for-each select="marc:subfield[@code='a']">
                                                <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="marc:subfield[@code='b']">
                                                <xsl:text> : </xsl:text>
                                                <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="marc:subfield[@code='c']">
                                                <xsl:text> ; </xsl:text>
                                                <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="marc:subfield[@code='d']">
                                                <xsl:text> + </xsl:text>
                                                <xsl:value-of select="."/>
                                        </xsl:for-each>
                                        <xsl:for-each select="marc:subfield[@code='n']">
                                                <xsl:value-of select="."/>
                                        </xsl:for-each>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='440']/marc:subfield[@code='a']">
                                <subfield code="s">
                                        <xsl:value-of select="."/>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='440']/marc:subfield[@code='n']">
                                <subfield code="s">
                                        <xsl:value-of select="."/>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='440']/marc:subfield[@code='o']">
                                <subfield code="o">
                                        <xsl:value-of select="."/>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='440']/marc:subfield[@code='v']">
                                <subfield code="v">
                                        <xsl:value-of select="."/>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='501']">
                                <subfield code="l">
                                        <xsl:choose>
                                                <xsl:when test="marc:subfield[@code='i']">"
<xsl:for-each select="marc:subfield">
  <xsl:if test="@code='i'">
  <xsl:text>Systemkrav: </xsl:text>
                                                                        <xsl:value-of select="."/>
                                                                        <xsl:text>: </xsl:text>
                                                                </xsl:if>
                                                                <xsl:if test="@code='a'">
                                                                        <xsl:value-of select="."/>
                                                                </xsl:if>
                                                                <xsl:if test="@code='b'">
                                                                        <xsl:value-of select="."/>
                                                                </xsl:if>
                                                        </xsl:for-each>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                        <xsl:if test="@code='a'">
                                                                <xsl:text>Systemkrav: </xsl:text>
                                                                <xsl:value-of select="."/>
                                                        </xsl:if>
                                                        <xsl:if test="@code='b'">
                                                                <xsl:text>Adgangsmåde: </xsl:text>
                                                                <xsl:value-of select="."/>
                                                        </xsl:if>

                                                </xsl:otherwise>
                                        </xsl:choose>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='502' or @tag='504' or @tag='505' or @tag='506' or @tag='507' or @tag='508' or @tag='509' or @tag='518' or @tag='532' or @tag='540' or @tag='555' or @tag='559']/marc:subfield[@code='a']">
                                <subfield code="l">
                                        <xsl:value-of select="."/>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='512']">
                                <subfield code="l">
                                        <xsl:for-each select="marc:subfield">
                                                <xsl:if test="@code='a'">
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='i'">
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>: </xsl:text>
                                                </xsl:if>
                                                <xsl:if test="@code='t'">
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='e'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='d'">
                                                        <xsl:value-of select="."/>
                                                        <xsl:text>: </xsl:text>
                                                </xsl:if>
                                                <xsl:if test="@code='x'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='b'">
                                                        <xsl:text>. </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                                                </xsl:for-each>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='513']">
                                <subfield code="l">
                                        <xsl:for-each select="marc:subfield">
                                                <xsl:if test="@code='a'">
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='e'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='f'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='i'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                                <xsl:if test="@code='j'">
                                                        <xsl:text> / </xsl:text>
                                                        <xsl:value-of select="."/>
                                                </xsl:if>
                                        </xsl:for-each>
                                </subfield>
                        </xsl:for-each>
                                <xsl:for-each select="marc:datafield[@tag='517']">
                                <subfield code="l">
                                <xsl:for-each select="marc:subfield">
     <xsl:value-of select="."/>
    <xsl:text> </xsl:text>
   </xsl:for-each>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='520']">
                                <subfield code="l">
                        <xsl:for-each select="marc:subfield">
     <xsl:if test="@code='a'">
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='i'">
     <xsl:value-of select="."/>
     <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:if test="@code='t'">
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='e'">
     <xsl:text> / </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='d'">
     <xsl:value-of select="."/>
     <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:if test="@code='x'">
     <xsl:text>. </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='b'">
     <xsl:text>. </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>

   </xsl:for-each>
                                </subfield>
                        </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='523']">
                                <subfield code="l">
                        <xsl:for-each select="marc:subfield">
     <xsl:if test="@code='a'">
     <xsl:value-of select="."/>
    </xsl:if>
                <xsl:if test="@code='z'">
                <xsl:text>ISBN </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
                </xsl:for-each>
                </subfield>
                </xsl:for-each>
                        <xsl:for-each select="marc:datafield[@tag='526']">
                                <subfield code="l">
                        <xsl:for-each select="marc:subfield">
     <xsl:if test="@code='a'">
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='i'">
     <xsl:value-of select="."/>
     <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:if test="@code='t'">
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='e'">
     <xsl:text> / </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='d'">
     <xsl:value-of select="."/>
     <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:if test="@code='x'">
     <xsl:text>. </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='b'">
     <xsl:text>. </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
      </xsl:for-each>
                </subfield>
                </xsl:for-each>
<xsl:for-each select="marc:datafield[@tag='534']">
                                <subfield code="l">
                        <xsl:for-each select="marc:subfield">
     <xsl:if test="@code='a'">
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='i'">
     <xsl:value-of select="."/>
     <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:if test="@code='t'">
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='e'">
     <xsl:text> / </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='d'">
     <xsl:value-of select="."/>
     <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:if test="@code='x'">
     <xsl:text>. </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
    <xsl:if test="@code='b'">
     <xsl:text>. </xsl:text>
     <xsl:value-of select="."/>
    </xsl:if>
       </xsl:for-each>
                </subfield>
                </xsl:for-each>
<xsl:for-each select="marc:datafield[@tag='021']/marc:subfield[@code='a']">
                                <subfield code="z">

        <xsl:value-of select="."/>

                </subfield>
                </xsl:for-each>
                <xsl:for-each select="marc:datafield[@tag='021']/marc:subfield[@code='e']">
                                <subfield code="r">

        <xsl:value-of select="."/>

                </subfield>
                </xsl:for-each>
                <xsl:for-each select="marc:datafield[@tag='856' or @tag='512' or @tag='520' or @tag='523' or @tag='526' or @tag='529' or @tag='530' or @tag='532' or @tag='534' or @tag='559']/marc:subfield[@code='u']">
                                <subfield code="u">
                        <xsl:value-of select="."/>
                        </subfield>
                </xsl:for-each>
                <xsl:for-each select="marc:datafield[@tag='256']/marc:subfield[@code='a']">
                                <subfield code="b">
                        <xsl:value-of select="."/>
                        </subfield>
                </xsl:for-each>
                <subfield code="9">
        <xsl:choose>
        <xsl:when test="marc:datafield[@tag='994']/marc:subfield[@code='z']">
        <xsl:value-of select="marc:datafield[@tag='994']/marc:subfield[@code='z']"/>
        </xsl:when>
        <xsl:when test="marc:datafield[@tag='994']/marc:subfield[@code='a']">
                <xsl:value-of select="marc:datafield[@tag='994']/marc:subfield[@code='a']"/>
</xsl:when>
<xsl:when test="marc:datafield[@tag='001']/marc:subfield[@code='a']">
                <xsl:value-of select="marc:datafield[@tag='001']/marc:subfield[@code='a']"/>
</xsl:when>
</xsl:choose>
                </subfield>
                        </datafield>
                        <xsl:for-each select="marc:datafield[@tag='096']">
                                <datafield tag="x96" ind1="0" ind2="0">
                                <xsl:copy-of select="marc:subfield"/>
                        </datafield>
                        </xsl:for-each>

            <xsl:for-each select="marc:datafield[not(@tag='096' or @tag='245' or @tag='250' or @tag='260' or @tag='300' or @tag='440' or @tag='501' or @tag='502' or @tag='504' or @tag='505' or @tag='506' or @tag='507' or @tag='508' or @tag='509' or @tag='512' or @tag='513' or @tag='517' or @tag='518' or @tag='520' or @tag='523' or @tag='526' or @tag='530' or @tag='532' or @tag='534' or @tag='540' or @tag='555' or @tag='559' or @tag='021' or @tag='856' or @tag='256' or @tag='013' or @tag='014' or @tag='015' or @tag='001' or @tag='004' or @tag='994')]">
                        <xsl:copy-of select="."/>
            </xsl:for-each>


        </xsl:template>


</xsl:stylesheet>
