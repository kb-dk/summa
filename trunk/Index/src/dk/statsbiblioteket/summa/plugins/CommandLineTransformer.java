/* $Id: CommandLineTransformer.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
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
package dk.statsbiblioteket.summa.plugins;


import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;

import java.io.FileReader;

import dk.statsbiblioteket.util.qa.QAInfo;


//todo: this is not a plugin and shold be removed.
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class CommandLineTransformer {

    public static void main(String args[]){

        final TransformerFactory tfactory = TransformerFactory.newInstance();
        final Transformer trans;
        InputStream in;
        try {
            in = new FileInputStream(args[1]);
            trans =tfactory.newTransformer(new StreamSource(in));
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            final StreamResult input = new StreamResult();
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    input.setOutputStream(out);
                    final Source so = new StreamSource(new FileReader(args[0]));
                    trans.transform(so,input);
                    final byte[] b = out.toByteArray();
                    System.out.println(new String(b));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}


