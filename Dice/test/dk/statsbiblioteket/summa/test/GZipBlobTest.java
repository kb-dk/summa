/* $Id: GZipBlobTest.java,v 1.2 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:17 $
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
package dk.statsbiblioteket.summa.test;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.dice.util.GZipBlob;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 19/09/2006
 * Time: 09:47:39
 * To change this template use File | Settings | File Templates.
 */
public class GZipBlobTest extends TestCase {

    public GZipBlobTest () {
        super();
    }

    public void setUp () throws Exception {

    }

    public void testGZipBlobIdemPotency() throws Exception {
        GZipBlob blob;
        List list;

        list = new ArrayList ();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        System.out.println ("Creating GZipBlob");
        blob = new GZipBlob(list.iterator());
        System.out.println ("Data size: " + blob.getBuffer().length);

        System.out.println ("Getting iterators");
        Iterator iterOrig = list.iterator();
        Iterator iterBlob = blob.iterator();

        System.out.println ("Testing data");
        while (iterBlob.hasNext()) {
            Object blobElement = iterBlob.next ();
            System.out.println ("Blob data object: " + blobElement);
            assertTrue("iterator must be idempotent", iterOrig.next().equals (blobElement));
        }

        System.out.println ("Testing length");
        assertTrue("iterator must have some number of elements as original", !iterOrig.hasNext());
    }

    public void testGenericGZipBlobIdemPotency() throws Exception {
        GZipBlob<String> blob;
        List<String> list;

        list = new ArrayList<String> ();
        for (int i = 0; i < 10; i++) {
            list.add("String number " + i);
        }

        System.out.println ("Creating GZipBlob");
        blob = new GZipBlob<String>(list.iterator());
        System.out.println ("Data size: " + blob.getBuffer().length);

        System.out.println ("Getting iterators");
        Iterator<String> iterOrig = list.iterator();
        Iterator<String> iterBlob = blob.iterator();

        System.out.println ("Testing data");
        while (iterBlob.hasNext()) {
            Object blobElement = iterBlob.next ();
            System.out.println ("Blob data object: " + blobElement);
            assertTrue("iterator must be idempotent", iterOrig.next().equals (blobElement));
        }

        System.out.println ("Testing length");
        assertTrue("iterator must have some number of elements as original", !iterOrig.hasNext());
    }

    public void testGZipBlobBufferConstructor() throws Exception {
        GZipBlob<String> origBlob;
        GZipBlob<String> blob;
        List<String> list;

        list = new ArrayList<String> ();
        for (int i = 0; i < 10; i++) {
            list.add("String number " + i);
        }

        System.out.println ("Creating GZipBlob");
        origBlob = new GZipBlob<String>(list.iterator());

        System.out.println ("Creating GZipBlob from raw data");
        blob = new GZipBlob (origBlob.getBuffer());

        System.out.println ("Getting iterators");
        Iterator<String> iterOrig = list.iterator();
        Iterator<String> iterBlob = blob.iterator();

        System.out.println ("Testing data");
        while (iterBlob.hasNext()) {
            Object blobElement = iterBlob.next ();
            System.out.println ("Blob data object: " + blobElement);
            assertTrue("iterator must be idempotent", iterOrig.next().equals (blobElement));
        }

        System.out.println ("Testing length");
        assertTrue("iterator must have some number of elements as original", !iterOrig.hasNext());
    }

    public void testGZipBlobDumpRead () throws Exception {
        String dumpFile = "/home/mikkel/tmp/gzipblob.gz";

        GZipBlob<String> origBlob;
        GZipBlob<String> blob;
        List<String> list;

        list = new ArrayList<String> ();
        for (int i = 0; i < 10; i++) {
            list.add("String number " + i);
        }

        System.out.println ("Creating GZipBlob from iter");
        origBlob = new GZipBlob<String>(list.iterator());

        System.out.println ("Dumping to file " + dumpFile);
        origBlob.dump (dumpFile, false);

        System.out.println ("Creating GZipBlob from file " + dumpFile);
        blob = new GZipBlob (dumpFile);

        System.out.println ("Getting iterators");
        Iterator<String> iterOrig = list.iterator();
        Iterator<String> iterBlob = blob.iterator();

        System.out.println ("Testing data");
        while (iterBlob.hasNext()) {
            Object blobElement = iterBlob.next ();
            System.out.println ("Blob data object: " + blobElement);
            assertTrue("iterator must be idempotent", iterOrig.next().equals (blobElement));
        }

        System.out.println ("Testing length");
        assertTrue("iterator must have some number of elements as original", !iterOrig.hasNext());

        System.out.println ("Deleting temporary files");
        new File (dumpFile).delete();
    }

    public void testGZipBlobIterToFile () throws Exception {
        String dumpFile = "/home/mikkel/tmp/gzipblob.gz";

        GZipBlob<String> blob;
        List<String> list;

        list = new ArrayList<String> ();
        for (int i = 0; i < 10; i++) {
            list.add("String number " + i);
        }

        System.out.println ("Comparing contents");
        blob = new GZipBlob<String> (list.iterator(), dumpFile, false);
        Iterator<String> blobIter = blob.iterator();
        Iterator<String> listIter = list.iterator();
        while (blobIter.hasNext()) {
            String copy = blobIter.next();
            System.out.println (copy);
            assertTrue("Original and copy should match", copy.equals(listIter.next()));
        }

        System.out.println ("Comparing length");
        assertTrue("lenght of input and output should match", !listIter.hasNext());

        System.out.println ("Deleting temporary files");
        new File (dumpFile).delete();
    }
}
