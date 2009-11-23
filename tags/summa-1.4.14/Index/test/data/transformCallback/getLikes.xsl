<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:marc="http://www.loc.gov/MARC21/slim"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:message="http://xml.apache.org/xalan/java/dk.statsbiblioteket.summa.index.TransformCallbackTest"
                exclude-result-prefixes="message marc">

    <xsl:output method="html" encoding="UTF-8"/>
    <xsl:param name="bundle_global"/>
    <xsl:param name="locale"/>
    <xsl:param name="record_id"/>


    <xsl:template name="getLikes" match="/searchresult">

        <xsl:if test=". != ''">

        <div class="addon">
            <div class="addon header">
                <xsl:value-of select="message:getMessage($bundle_global, $locale, 'label.similar_items')"/>
            </div>

            <!--<div class="contentsContainer" id="contentsContainer_getlikes">-->
            <div id="contentsContainer_getlikes">
                <xsl:for-each select="record/shortrecord/RDF/Description">
                    <xsl:variable name="id" select="../../../@recordID"/>
                    <div class="addon content">
                        <div>
                            <!-- author  -->
                            <xsl:value-of select="normalize-space(creator)"/>
                        </div>
                        <div>
                            <!-- title variable -->
                            <xsl:if test="title">
                                <a onclick="log(event)" href="showrecord.jsp?record_id={$id}">
                                    <xsl:choose>
                                        <xsl:when test="contains(title,' : ')">
                                            <xsl:value-of select="substring-before(title,' : ')"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="title"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </a>
                            </xsl:if>
                        </div>

                        <!--<div style="float:left">-->
                        <div>
                            <!-- year  -->
                            <xsl:if test="date">
                                <xsl:value-of select="date"/>
                            </xsl:if>
                            <!-- type -->
                            <xsl:if test="date">
                                <xsl:text>&#160;-&#160;</xsl:text>
                            </xsl:if>
                            <xsl:value-of select="type[@lang=$locale]"/>
                        </div>
                    </div>
                </xsl:for-each>
            </div>
        </div>
   </xsl:if>
    </xsl:template>


    <xsl:template name="getLikes_call">
             <div id="getlikes" style="display:none; clear:left">&#160;</div>
            <script type="text/javascript" charset="UTF-8">
                getlikes_arr[getlikes_arr.length] = new Array('<xsl:value-of select="$record_id"/>', '3', '1');
            </script>
       
    </xsl:template>

</xsl:stylesheet>