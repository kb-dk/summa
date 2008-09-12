/* $Id: CacheTest.java,v 1.3 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.3 $
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
import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.dice.caching.*;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.util.RegistryManager;
import dk.statsbiblioteket.summa.dice.util.GZipBlob;
import dk.statsbiblioteket.summa.dice.util.Splitter;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.rmi.registry.Registry;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 11/09/2006
 * Time: 10:09:34
 * To change this template use File | Settings | File Templates.
 */
public class CacheTest extends TestCase {

    private static final Logger log = Logger.getLogger("dk.statsbiblioteket.summa");
    private String tmpDir;

    public CacheTest () {
        super();
        tmpDir = System.getProperty("user.home") + File.separator + "tmp";
    }

    public void setUp () throws Exception {

    }

    /**
     * Test simple writing and reading of a dew objects to the cache
     */
    public void testObjectCache () throws Exception {
        CacheService<String> cacheService = new CacheService<String> (ObjectCacheWriter.class,
                                 ObjectCacheReader.class,
                                 "/home/mikkel/tmp/test_cache");

        CacheClient<String> cache = new GenericCacheClient<String> (cacheService);

        Iterable<String> c1 = new TestCacheable(1);
        Iterable<String> c2 = new TestCacheable(2);
        Iterable<String> c3 = new TestCacheable(3);

        System.out.println ("Caching items");
        long c1_id = cache.put (c1);
        long c2_id = cache.put (c2);
        long c3_id = cache.put (c3);

        System.out.println ("Retrieving item2");
        Iterator<String> c2_iter = cache.get (c2_id);
        Iterator<String> c2_orig = c2.iterator();
        String part = null;
        while (c2_iter.hasNext()) {
            part = c2_iter.next();
            if (part == null) {
                break;
            }
            System.out.println ("Part: " + part);
            assertTrue("Part submission/retrieval should be idempotent.", c2_orig.next().equals(part));
        }
        System.out.println ("End of item2");

        assertTrue("CacheService item should have same number of parts as original.", !c2_orig.hasNext());
    }

    /**
     * Test caching over RMI
     */
    public void testRemoteCache () throws Exception {
        Config conf = new Config();
        conf.setDefaults();

        Cache<String> backend = new CacheService<String>
                                       (ObjectCacheWriter.class,
                                        ObjectCacheReader.class,
                                        "/home/mikkel/tmp/test_remote_cache");

        System.out.println ("Starting remote cache");
        RemoteCache<String> cache = new RemoteCacheService<String>
                                                  (backend,
                                                   "remote_cache",
                                                   20001,
                                                   conf.getClientSocketFactory(),
                                                   conf.getServerSocketFactory());

        System.out.println ("Connecting to remote cache");
        Registry reg = RegistryManager.getRemoteRegistry("pc134",
                conf.getRegistryPort(), conf);

        RemoteCache<String> remote_iface = (RemoteCache<String>) reg.lookup("remote_cache");
        CacheClient<String> client = new GenericCacheClient<String>(remote_iface);


        System.out.println ("Caching items");
        Iterable<String> c1 = new TestCacheable(1);
        Iterable<String> c2 = new TestCacheable(2);
        Iterable<String> c3 = new TestCacheable(3);

        long c1_id = client.put (c1);
        long c2_id = client.put (c2);
        long c3_id = client.put (c3);

        System.out.println ("Retrieving item2");
        Iterator<String> c2_iter = client.get (c2_id);
        Iterator<String> c2_orig = c2.iterator();
        String part = null;
        while (c2_iter.hasNext()) {
            part = c2_iter.next();
            if (part == null) {
                break;
            }
            System.out.println ("Part: " + part);
            assertTrue("Part submission/retrieval should be idempotent.", c2_orig.next().equals(part));
        }
        System.out.println ("End of item2");

        assertTrue("CacheService item should have same number of parts as original.", !c2_orig.hasNext());
    }

    /**
     * Test if the blocking cache really blocks and unblocks properly
     */
    public void testBlockingCache () throws Exception {

        Cache<String> backend = new CacheService<String>
                                       (ObjectCacheWriter.class,
                                        ObjectCacheReader.class,
                                        "/home/mikkel/tmp/test_blocking_cache");

        Cache<String> blockingCache = new BlockingCacheService<String> (backend);
        CacheClient<String> client = new GenericCacheClient<String> (blockingCache);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread (new CacheClientRunner(i, blockingCache, 10));
        }

        //DataBlob blob = new DataBlob(0, 0);

        // Start all "workers" submitting stuff to the cache
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }


        // Read cache like there's no tomorrow
        int i = 0;
        while (true) {
            System.out.println ("Fetching item " + i);
            Iterator<String> item = client.get(i);
            int count = 0;
            String part = "";
            while (item.hasNext()) {
                part = item.next ();
                count++;
            }
            System.out.println ("Found " + count + " parts in item " + i + ", last part: " + part);
            i++;
        }

    }

    /**
     * Test writing GZipBlobs to a cache using raw DataCacheR/W.
     */
    public void testComplexDataCache () throws Exception {
        System.out.println ("Creating cache");
        Cache<byte[]> cacheService = new CacheService<byte[]> (DataCacheWriter.class,
                                                        DataCacheReader.class,
                                                        "/home/mikkel/tmp/gz_data_cache");

        CacheClient<byte[]> cache = new GenericCacheClient<byte[]> (cacheService);

        // Create data sample and put it in the cache in in chunked up gzip format
        System.out.println ("Storing data in cache");
        List<String> dataList = new ArrayList<String> ();
        for (int i = 0; i < 10; i++) {
            dataList.add ("Data item " + i);
        }

        GZipBlob<String> blob = new GZipBlob<String> (dataList.iterator());
        Splitter splitter = new Splitter(blob.getBuffer(), 16);
        long id = cache.put (splitter);


        // Retrieve data chunks from cache and reassemble the GZipBlob
        System.out.println ("Retrieving data from cache");
        Iterator<byte[]> resultRaw = cache.get (id);
        byte[] collectedGZData = Splitter.collect (resultRaw);
        GZipBlob<String> resultBlob = new GZipBlob<String> (collectedGZData);

        // Compare result vs original
        System.out.println ("Comparing result and original contents");
        Iterator<String> resultIter = resultBlob.iterator();
        Iterator<String> dataIter = dataList.iterator();
        while (resultIter.hasNext()) {
            String dataItem = resultIter.next();
            System.out.println ("Found data item: " + dataItem);
            assertTrue("Processed data parts should be unchanged", dataItem.equals(dataIter.next()));
        }

        System.out.println("Comparing length");
        assertTrue("Number of data elements must be unchanged", !dataIter.hasNext());

        System.out.println ("All good");
    }

    /**
     * Test a pipelining layout of two caches. The first cache writes gzipped items
     * and returns raw data. The second writes raw data and returns unzipped items.
     */
    public void testDataGZipPipelining () throws Exception {
        System.out.println ("Creating caches");

        // The first cache gzip on writing
        Cache<String> gzippingCache = new CacheService<String> (GZipCacheWriter.class,
                                                               DataCacheReader.class,
                                                               "/home/mikkel/tmp/gz_data_cache");

        CacheClient<String> gzcache = new GenericCacheClient<String> (gzippingCache);

        // The second cache writes raw data and gunzips on reading
        Cache<String> gunzippingCache = new CacheService<String> (DataCacheWriter.class,
                                                               GZipCacheReader.class,
                                                               "/home/mikkel/tmp/gunz_data_cache");

        CacheClient<String> gunzcache = new GenericCacheClient<String> (gunzippingCache);

        List<String> dataList = new ArrayList<String> ();
        for (int i = 0; i < 10; i++) {
            dataList.add ("Data item " + i);
        }

        System.out.println ("Putting data in zipping cache");
        long id = gzcache.put (dataList);
        Iterator gzData = gzcache.get (id);

        System.out.println ("Pipelining data to unzip-on-read cache");
        long idd = gunzcache.put (new ProxyIterable(gzData));

        System.out.println ("Testing pipelined data");
        Iterator<String> result = gunzcache.get (idd);
        Iterator<String> orig = dataList.iterator();
        while (result.hasNext()) {
            String gunzData = result.next();
            assertTrue("Original and pipelined data should match", gunzData.equals(orig.next()));
        }

        assertTrue ("Length of data should match", !orig.hasNext());

    }

    /**
     * Test that write/read doesn't change data for the given R/W pair
     * @param writer a CacheWriter class
     * @param reader a CacheReader class
     */
    private void testRWPair (Class writer, Class reader) throws Exception {
        System.out.println ("Testing: " + writer.getSimpleName() + ", " + reader.getSimpleName());

        CacheWriter<String> w = (CacheWriter<String>) writer.getConstructor().newInstance();
        CacheReader<String> r = (CacheReader<String>) reader.getConstructor().newInstance();
        String tmpFile = tmpDir + "/" + w.getClass().getSimpleName() + ".test";

        List<String> items = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            items.add ("item " + i);
        }

        // Write data
        System.out.println ("Writing data");
        w.prepare (tmpFile);
        for (String s : items) {
            w.writePart(s);
        }
        w.close();

        // Read data
        System.out.println ("Reading data");
        r.open(tmpFile);
        String data;
        Iterator orig = items.iterator();
        while ((data = r.readPart()) != null) {
            System.out.println ("Read data: " + data);
            assertTrue("write/read should be idempotent", data.equals(orig.next()));
        }

        System.out.println ("Checking data length");
        assertTrue(!orig.hasNext());

        System.out.println ("Deleting temp file");
        new File (tmpFile).delete();
    }

    public void testGZipRW () throws Exception {
        testRWPair(GZipCacheWriter.class, GZipCacheReader.class);
    }

    public void testObjectRW () throws Exception {
        testRWPair(ObjectCacheWriter.class, ObjectCacheReader.class);
    }

    /*public void testMemoryRW () throws Exception {
        testRWPair (MemoryCacheWriter.class, MemoryCacheReader.class);
    }*/

    public void testDataRW () throws Exception {
        // These RW requires byte[] as objects, so the generic test doesn't apply
    }

    public void testFindFreeFilename () throws Exception {

        // Create a file in the cache directory stealing the name of the first cache item
        File tmpDir = new File (System.getProperty("user.home") + File.separator + "tmp" + File.separator);
        File tmpItem = File.createTempFile("blah-", null, tmpDir);

        Cache<String> cacheService = new CacheService<String>(ObjectCacheWriter.class,
                                                              ObjectCacheReader.class,
                                                              tmpDir.getAbsolutePath());

        CacheClient<String> cache = new GenericCacheClient<String> (cacheService);

        ArrayList<String> list = new ArrayList<String>();
        list.add ("My first cache item! Wow!");

        tmpItem.renameTo( new File(cache.getPath(0)) );
        cache.put (list);

    }
}



