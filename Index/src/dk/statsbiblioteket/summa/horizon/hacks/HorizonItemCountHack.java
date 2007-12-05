/* $Id: HorizonItemCountHack.java,v 1.7 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.7 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.horizon.hacks;

import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.xpath.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.InputSource;

/**
 * As stated this is a hack compontent and will be deleted as soon as posible.
 *
 * This hack extraxts itemcounts from horizon MARC records - Simply be cause the current Horizon Library system at State and University Library
 * cannot retrive this number within a usable timelimit.
 *
 * Using this hack triggers that 'dynamic' data is indexed
 * @deprecated
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class HorizonItemCountHack {

    private static XPathExpression countItems;
    private static Log log =  LogFactory.getLog(HorizonItemCountHack.class);
    private static String fileName;
    private static HashMap<String, Integer> hash;

    public static void main(String args[]) throws IOException, NotBoundException, RemoteException, XPathExpressionException {

        Access io = (Access) Naming.lookup(args[0]);
        RecordIterator iter = io.getRecords("horizon");
        XPathFactory fac = XPathFactory.newInstance();
        XPath xp = fac.newXPath();

        fileName = args[1];



        try {
            countItems = xp.compile("count(record/datafield[@tag='096']" +
                  "[subfield[@code='i' and (text()='vu' or text()='cu' or text()='fu' or text()='kort' or text()='lu' or text()='m' or text()='nod-st')]]" +
                    "[subfield[@code='z' and (starts-with(text(), 'SB-') or starts-with(text(), 'SVB'))]])");
        } catch (XPathExpressionException e) {
            log.error("Could not compile countItem xpath");
        }


        hash = new HashMap<String, Integer>();

        while (iter.hasNext()){
            Record r = iter.next();
            if (!r.isDeleted()){
                String record = new String(r.getContent());
                //todo: remove this ugly hack;
            if (record != null){
                    record = record.replace("xmlns=\"http://www.loc.gov/MARC21/slim\"", "");
                    record = record.replace("xmlns=\"http://www.openarchives.org/OAI/2.0/\"" , "");
                    int count = getItemCount(record);
                    if (count != 1) {
                        hash.put(r.getId(), count);
                    }
            }
        }
        }
        serialize();
    }


    static int getItemCount(String record) {
        try {
           return record != null ?  ((Double)countItems.evaluate(new InputSource(new StringReader(record)), XPathConstants.NUMBER)).intValue() : 0;
        } catch (XPathExpressionException e) {
            log.warn("something wrong with xpath", e);
            return 0;
        }
    }

    static void serialize() throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fileName));
        os.writeObject(hash);
        os.flush();
        os.close();
    }

}
