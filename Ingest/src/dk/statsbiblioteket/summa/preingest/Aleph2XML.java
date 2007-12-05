/* $Id: Aleph2XML.java,v 1.10 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.10 $
 * $Date: 2007/10/05 10:20:24 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.preingest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 *
 * FØR:
 *
 * 000000001 FMT   L ML
 * 000000001 LDR   L -----nam----------a-----
 * 000000001 00100 L $$ax100006565$$fa
 * 000000001 00400 L $$rc$$ae
 * 000000001 00800 L $$a1979$$bdk$$e1$$f0$$g0$$ldan$$tm
 * 000000001 00900 L $$aa
 * 000000001 09600 L $$aXI,5b S$$z840860
 * 000000001 24500 L $$aKemiske stoffer$$canvendelse og kontrol$$eudarb. af Byggesektorgruppen på den teknologisk-samfundsvidenskabelige planlæggeruddannelse på RUC
 * 000000001 26000 L $$a<Roskilde>$$b<s.n.>$$c1979
 * 000000001 30000 L $$a303 sp.$$bill.
 * 000000001 50600 L $$aSpecialeafh.
 * 000000001 71000 L $$aRoskilde Universitetscenter$$c<<Den >>teknologisk-samfundsvidenskabelige planlæggeruddannelse$$cByggesektorgruppen
 * 000000001 BAS   L 30
 * 000000001 CAT   L $$aBATCH$$b00$$c19971106$$lKEM01$$h1228
 * 000000001 CAT   L $$aBATCH$$b00$$c20040617$$lKEM01$$h0923
 * 000000001 CAT   L $$c20060322$$lKEM01$$h0946
 * 000000001 CAT   L $$c20060329$$lKEM01$$h1019
 * 000000001 C0100 L $$asbu
 * 000000001 U0700 L $$kThe Chemistry Library$$rXI,5b S
 * 000000001 V0700 L $$aXI,5b S
 *
 *
 *
 *
 * EFTER:
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <collection xmlns="http://www.loc.gov/MARC21/slim">
 *   <record>
 *       <datafield tag="FMT" ind1="" ind2="">ML</datafield>
 *       <datafield tag="LDR" ind1="" ind2="">-----nam----------a-----</datafield>
 *       <datafield tag="001" ind1="0" ind2="0">
 *           <subfield code="a">x100006565</subfield>
 *           <subfield code="f">a</subfield>
 *       </datafield>
 *       <datafield tag="004" ind1="0" ind2="0">
 *           <subfield code="r">c</subfield>
 *           <subfield code="a">e</subfield>
 *       </datafield>
 *       <datafield tag="008" ind1="0" ind2="0">
 *           <subfield code="a">1979</subfield>
 *           <subfield code="b">dk</subfield>
 *           <subfield code="e">1</subfield>
 *           <subfield code="f">0</subfield>
 *           <subfield code="g">0</subfield>
 *           <subfield code="l">dan</subfield>
 *           <subfield code="t">m</subfield>
 *       </datafield>
 *       <datafield tag="009" ind1="0" ind2="0">
 *           <subfield code="a">a</subfield>
 *       </datafield>
 *       <datafield tag="096" ind1="0" ind2="0">
 *           <subfield code="a">XI,5b S</subfield>
 *           <subfield code="z">840860</subfield>
 *       </datafield>
 *       <datafield tag="245" ind1="0" ind2="0">
 *           <subfield code="a">Kemiske stoffer</subfield>
 *           <subfield code="c">anvendelse og kontrol</subfield>
 *           <subfield code="e">udarb. af Byggesektorgruppen på den
 *               teknologisk-samfundsvidenskabelige planlæggeruddannelse på RUC
 *           </subfield>
 *       </datafield>
 *       <datafield tag="260" ind1="0" ind2="0">
 *           <subfield code="a">&lt;Roskilde&gt;</subfield>
 *           <subfield code="b">&lt;s.n.&gt;</subfield>
 *           <subfield code="c">1979</subfield>
 *       </datafield>
 *       <datafield tag="300" ind1="0" ind2="0">
 *           <subfield code="a">303 sp.</subfield>
 *           <subfield code="b">ill.</subfield>
 *       </datafield>
 *       <datafield tag="506" ind1="0" ind2="0">
 *           <subfield code="a">Specialeafh.</subfield>
 *       </datafield>
 *       <datafield tag="710" ind1="0" ind2="0">
 *           <subfield code="a">Roskilde Universitetscenter</subfield>
 *           <subfield code="c">&lt;&lt;Den &gt;&gt;teknologisk-samfundsvidenskabelige
 *               planlæggeruddannelse
 *           </subfield>
 *           <subfield code="c">Byggesektorgruppen</subfield>
 *       </datafield>
 *       <datafield tag="BAS" ind1="" ind2="">30</datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="a">BATCH</subfield>
 *           <subfield code="b">00</subfield>
 *           <subfield code="c">19971106</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">1228</subfield>
 *       </datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="a">BATCH</subfield>
 *           <subfield code="b">00</subfield>
 *           <subfield code="c">20040617</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">0923</subfield>
 *       </datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="c">20060322</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">0946</subfield>
 *       </datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="c">20060329</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">1019</subfield>
 *       </datafield>
 *       <datafield tag="C01" ind1="0" ind2="0">
 *           <subfield code="a">sbu</subfield>
 *       </datafield>
 *       <datafield tag="U07" ind1="0" ind2="0">
 *           <subfield code="k">The Chemistry Library</subfield>
 *           <subfield code="r">XI,5b S</subfield>
 *       </datafield>
 *       <datafield tag="V07" ind1="0" ind2="0">
 *           <subfield code="a">XI,5b S</subfield>
 *       </datafield>
 *   </record>
 * </collection>
 *
 *
 *
 * @deprecated  use Aleph2XML2
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class Aleph2XML implements IngestFilter{

    private static Log log = LogFactory.getLog(Aleph2XML.class);

    private static final String data_start = "<datafield tag=\"";
    private static final String data_end = "</datafield>\n";

    private static final String lead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n<record>";

    private static final String startRecord ="<record>\n";
    private static final String endRecord = "</record>\n";

    private static final String subfieldStart = "<subfield code=\"";
    private static final String subfieldEnd = "</subfield>\n";



    private String myTrim(String in) {
        in = in.trim();
        in = in.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;"); // standard xml escaping
        in = in.replaceAll("\\p{Cntrl}","");  // some records has this illegal char in the record - needs to be removed.
        return in;
    }

    public void applyFilter(File input, Extension ext, String encoding) {

        try{
            StringBuffer sb = new StringBuffer();
            StringBuffer record = new StringBuffer();
            boolean isOK = true;
            File output = new File(input.getAbsolutePath() + "." + ext);
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), encoding));
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(input),encoding));
            String str;
            long idx = 0;
            long huskidx = 1;
            sb.append(lead);

            while (((str = in.readLine()) != null) && (idx < 60000)){
                String[] strparts = str.split("[ ]{1,5}",4);
                if (strparts.length == 4) {
                    idx = new Long(strparts[0]);
                    if (idx > huskidx) {
                        record.append(data_start).append("SID\" ind1=\"0\" ind2=\"0\">\n").append(subfieldStart).append("a\">").append(huskidx).append(subfieldEnd).append("\n").append(data_end);
                        record.append(endRecord);
                        if (isOK){
                            sb.append(record);
                        } else {
                            log.error("Skipping record containing errors:\n" + record);
                            isOK = true;
                        }
                        record.setLength(0);
                        out.write(sb.toString().trim());
                        sb.setLength(0);
                        record.append(startRecord);
                        huskidx = idx;
                    }
                    String felt = strparts[1];
                    String ind1 = "";
                    String ind2 = "";
                    if (felt.length() >= 3) {

                        if (felt.length() > 3) {
                            try{
                            ind1 = felt.substring(3,4); 
                            ind2 = felt.substring(4,5);
                            } catch (StringIndexOutOfBoundsException e){
                                log.warn(e.getMessage()  + str + "buffer:\n" + sb  + "\nFile:\n" + input.getAbsolutePath());
                                isOK = false;
                            }
                        } else {

                        }
                        felt = felt.substring(0,3);
                    } else {
                        log.info("ALARM........................" + felt);
                    }
                    record.append(data_start).append(myTrim(felt)).append("\" ind1=\"").append(myTrim(ind1)).append("\" ind2=\"").append(myTrim(ind2)).append("\">");
                    if (strparts[3].indexOf("$$") >= 0) {
                        String[] subf = strparts[3].split("\\$\\$");
                        int i = 0;
                        int j = 0;
                        while (i < subf.length) {
                            if (!subf[i].equals("")) {
                                String code = subf[i].substring(0,1);
                                String subfield = subf[i].substring(1);
                                if (j == 0) {
                                    sb.append("\n");
                                }
                                record.append(subfieldStart).append(myTrim(code)).append("\">").append(myTrim(subfield)).append(subfieldEnd);
                                j++;
                            }
                            i++;
                        }
                    } else {
                        record.append(myTrim(strparts[3]));
                    }
                    record.append(data_end);
                }
            }
            sb.append("</collection>\n");
            out.write(sb.toString().trim());
            sb.setLength(0);
            in.close();
            out.close();
            input.renameTo(new File(input.getAbsolutePath() + ".done"));

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
