<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
                exclude-result-prefixes="xsl"
                version="1.0">

    <xsl:output version="1.0" encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">





        <html xml:lang="en">
            <div xml:lang="da">
                <p>Standard søgningen i summa, er en søgning ned i en række felter og feltgrupper, hvor alle søgeordene skal forekomme i et af felterne for at søgningen matcher.
                Denne standard søgning vil i lang de fleste tilfælde være fyldestgørende og præsis nok.</p>
                <p>Hvis du ønsker mere kontrol over din søgning kan du bruge summas søgesprog.</p>
                <p title="Operatorer">
                Summa understøtter følgende operatore: +,-,?,*,
                <p> + er AND operatoren</p>
                <p> - er NOT operatoren</p>
                <p> ? er enkelttegns wildcard operator</p>
                <p> * er trunkerings operator</p>

                <p>Eksempel: søgningen "-foo +fight*" vil matche alle poster hvor ordet "foo" ikke forekommer samtidigt med at der skal findes ord i posten der stater med "fight" </p>
                </p>
                <p title="frase">
                    Du kan også angive at dine søgeord skal opfattes som en frase med "" omkring. Frase søgninger matcher kun hvis ordene optræder i posten i nøjagtig den rækkefølge der er angivet i søgningen.
                </p>
                <p title="fields">
                <p>foruden operatore kan man specificere hvilke felter eller grupper man vil matche på. syntaksen for specifikation af felter og grupper er [feltNavn]:[søgeord eller frase] og flere søgeord kan grupperes med ()</p>
                <p>Eksempel søgningen: +au:(Hans Christian Andersen) vil matche alle poster hvori der i forfatter felterne findes ordene Hans, Christian og Andersen - rækkefølgen af ordene er underordet</p>
                <p>Eksempel: +au:"Hans Christian Andersen" vil matche poster hvor der i et af forfatterfelterne optræder ordene Hans Christian Andersen i denne rækkefølge</p>
                </p>

                <div class="gruppeopremsning">
                    <ul>
                        <xsl:for-each select="SummaQueryDescriptor/group">
                            <li><xsl:value-of select="@name" />
                            <ul>
                                <xsl:for-each select="fields/field">
                                    <li><xsl:value-of select="@name" /></li>
                                </xsl:for-each>
                            </ul>
                            </li>
                        </xsl:for-each>
                    </ul>
                </div>
                <div class="enkeltFelter">
                    EnkeltFelter
                    <ul>
                        <xsl:for-each select="SummaQueryDescriptor/singleFields/field">
                              <li><xsl:value-of select="@name" /></li>
                        </xsl:for-each>
                    </ul>
                </div>
            </div>
            </html>
    </xsl:template>
</xsl:stylesheet>