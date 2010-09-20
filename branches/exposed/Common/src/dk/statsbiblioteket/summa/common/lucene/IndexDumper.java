/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.document.Field;
import dk.statsbiblioteket.util.Strings;
import org.apache.lucene.store.*;

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
        IndexReader ir = IndexReader.open(new NIOFSDirectory(location));
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
        IndexReader ir = IndexReader.open(new NIOFSDirectory(location));
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
