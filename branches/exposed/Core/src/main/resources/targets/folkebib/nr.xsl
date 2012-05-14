<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:Index="http://statsbiblioteket.dk/2004/Index"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java" xmlns:mc="http://www.loc.gov/MARC21/slim"
                exclude-result-prefixes="java xs xalan xsl mc"
                version="1.0">
    <xsl:template name="nr">
        <Index:group Index:name="nr" Index:navn="nr">
            <xsl:for-each select="mc:datafield[@tag='001']/mc:subfield[@code='a']">
                <Index:field Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='248']/mc:subfield[@code='9']">
                <Index:field Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="id" Index:navn="id" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='002']/mc:subfield[@code='a' or @code='c' or @code='d' or @code='x']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='010' or @tag='017' or @tag='018']/mc:subfield[@code='a']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='020']/mc:subfield[@code='b' or @code='x']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='021' or @tag='A21']/mc:subfield[@code='a' or @code='e']">
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='021' or @tag='A21']/mc:subfield[@code='x' or @code='w']">
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='021' or @tag='A21']/mc:subfield[@code='n']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='022' or @tag='A22']/mc:subfield[@code='a' or @code='x' or @code='z']">
                <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='023' or @tag='A23']/mc:subfield[@code='a']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='024' or @tag='A24']/mc:subfield[@code='a' or @code='x']">
                 <Index:field Index:repeat="true" Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='027' or @tag='A27']/mc:subfield[@code='a' or @code='x']">
                <Index:field Index:repeat="true" Index:name="ir" Index:navn="ir" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ir" Index:navn="ir" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                  <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='028' or @tag='A28']/mc:subfield[@code='a' or @code='x']">
                <Index:field Index:name="im" Index:navn="im" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="im" Index:navn="im" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='028' or @tag='A28']/mc:subfield[@code='n']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='030' or @tag='A30']/mc:subfield[@code='a' or @code='x']">
                <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,' ')">
                    <Index:field Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="5">
                        <xsl:value-of select="translate(.,' ','')"/>
                    </Index:field>
                </xsl:if>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='245' or @tag='C45']/mc:subfield[@code='z']">
                 <Index:field Index:repeat="true" Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>

            <xsl:for-each select="mc:datafield[@tag='247' or @tag='248' or @tag='C47' or @tag='C48']/mc:subfield[@code='z' or @code='r']">
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='440' or @tag='840' or @tag='E40' or @tag='I40']/mc:subfield[@code='z']">
                <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='521' or @tag='F21']/mc:subfield[@code='x']">
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='523' or @tag='F23']/mc:subfield[@code='z']">
               <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='529' or @tag='F29']/mc:subfield[@code='z']">
                <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='530' or @tag='F30']/mc:subfield[@code='z']">
                 <Index:field Index:repeat="true" Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
             <xsl:for-each select="mc:datafield[@tag='538' or @tag='F38']/mc:subfield[@code='a' or @code='b' or @code='c' or @code='d' or @code='g' or @code='h' or  @code='p' or  @code='q' or @code='s']">
                <Index:field Index:repeat="true" Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='538' or @tag='F38']/mc:subfield[@code='f']">
                <Index:field Index:repeat="true"  Index:name="nummer" Index:navn="nummer" Index:type="number" Index:boostFactor="10">

                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                    <xsl:for-each select="../mc:subfield[@code='g']">
                        <xsl:text>. </xsl:text>
                        <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                    </xsl:for-each>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='538' or @tag='F38']/mc:subfield[@code='o']">
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
            <xsl:for-each select="mc:datafield[@tag='557' or @tag='F57']/mc:subfield[@code='z']">
                <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="in" Index:navn="in" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:nann="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='558' or @tag='F58']/mc:subfield[@code='z' or @code='r']">
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="6">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="ib" Index:navn="ib" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='795' or @tag='H95']/mc:subfield[@code='z']">
                 <Index:field Index:repeat="true" Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="ic" Index:navn="ic" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:repeat="true" Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="10">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>
            <xsl:for-each select="mc:datafield[@tag='860'  or @tag='I60' or @tag='861' or @tag='I61' or
					@tag='863' or @tag='I63' or @tag='865' or @tag='I65' or @tag='866' or @tag='I66' or @tag='867' or @tag='I67' or 
					@tag='868' or @tag='I68' or @tag='870' or @tag='I70' or @tag='871' or @tag='I71' or @tag=873 or @tag='I73' or 
					@tag='874' or @tag='I74' or @tag='879' or @tag='I79']/mc:subfield[@code='z']">
                <Index:field Index:name="is"  Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="."/>
                </Index:field>
                <xsl:if test="contains(.,'-')">
                    <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="6">
                        <xsl:value-of select="translate(.,'-','')"/>
                    </Index:field>
                </xsl:if>
                <Index:field Index:name="is" Index:navn="is" Index:type="number" Index:boostFactor="6">
                    <xsl:value-of select="java:dk.statsbiblioteket.summa.plugins.Normalize.normalize(.)"/>
                </Index:field>
            </xsl:for-each>


        </Index:group>
    </xsl:template>

</xsl:stylesheet>
