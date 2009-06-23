/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.document.Field;
import dk.statsbiblioteket.util.Strings;


/**
 * Simple dump of stored fields in the index.
 */
public class IndexDumper {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println(
                    "Usage: IndexDumper indexLocation [delimiter field*]");
            System.out.println("If only the indexLocation is given, a list of "
                               + "fields in the index is dumped");
            return;
        }
        File location = new File(args[0]);
        if (!location.exists()) {
            throw new FileNotFoundException(String.format(
                    "The stated index location '%s' does not exist",
                    location.getAbsoluteFile()));
        }

        if (args.length == 1) {
            listFields(location);
            return;
        }
        String delimiter = args[1].equals("\\t") ? "\t" : args[1];
        List<String> fields = Arrays.asList(args).subList(2, args.length);
        dump(location, delimiter, fields);
    }

    public static void listFields(File location) throws IOException {
        IndexReader ir = IndexReader.open(location);
        Collection fieldNames = ir.getFieldNames(IndexReader.FieldOption.ALL);
        System.out.println(String.format(
                "Fields in '%s': %s", 
                location, Strings.join(fieldNames, ", ")));
        ir.close();
    }

    /**
     * Outputs the content of fields on stdout.
     * @param location  the location of the Lucene index to dump.
     * @param delimiter the delimiter to use between the field contents.
     * @param fields    the fields to dump.
     * @throws IOException if the index could not be accessed properly.
     */
    public static void dump(File location, String delimiter,
                             List<String> fields) throws IOException {
        IndexReader ir = IndexReader.open(location);
        FieldSelector selector = new MapFieldSelector(fields);
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            if (ir.isDeleted(i)) {
                continue;
            }
            Document doc = ir.document(i, selector);
            for (int f = 0 ; f < fields.size() ; f++) {
                Field field = doc.getField(fields.get(f));
                System.out.print(field == null ? "" : field.stringValue());
                System.out.print(f == fields.size() - 1 ? "\n" : delimiter);
            }
        }
        ir.close();
    }
}