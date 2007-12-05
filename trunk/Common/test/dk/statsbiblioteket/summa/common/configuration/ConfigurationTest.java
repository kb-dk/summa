/* $Id: ConfigurationTest.java,v 1.6 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/04 13:28:18 $
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
package dk.statsbiblioteket.summa.common.configuration;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.Serializable;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Configuration Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ConfigurationTest extends TestCase {
    public ConfigurationTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSetGet() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetStrings() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetString() throws Exception {
        //TODO: Test goes here...
    }

    public void testToString () throws Exception {
        Configuration conf = new Configuration(new FileStorage("configuration.xml"));
        System.out.println(conf.toString());
    }

    public void testSimpleEquals () throws Exception {
        Configuration c1 = new Configuration(new FileStorage("configuration.xml"));
        Configuration c2 = new Configuration(new FileStorage("configuration.xml"));
        assertTrue("Empty configurations should equal", c1.equals(c2));
    }

    public void testGetInt() throws Exception {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("a", "12");
        configuration.set("b", "-12");
        configuration.set("c", "krabardaf");
        assertEquals("a should give 12", 12, configuration.getInt("a"));
        assertEquals("b should give -12", -12, configuration.getInt("b"));
        try {
            configuration.getInt("c");
            fail("c should throw an exception");
        } catch (Exception e) {
            // Expected behaviour
        }
    }

    public void testGetIntValues() throws Exception {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("ok", "a 1, b(2),c(-3),  d ( 4  ) , e, f (5)");
        configuration.set("invalid1", "a(3a)");
        configuration.set("invalid2", "a((3))");
        // TODO: File a bug on no error with non-static inner class
        List<Configuration.Pair<String, Integer>> elements =
                configuration.getIntValues("ok", 87);
        assertEquals("The value for a 1 should be 87", 
                     new Integer(87), elements.get(0).getSecond());
        assertEquals("The value for b should be 2",
                     new Integer(2), elements.get(1).getSecond());
        assertEquals("The value for c should be -3",
                     new Integer(-3), elements.get(2).getSecond());
        assertEquals("The value for d should be 4",
                     new Integer(4), elements.get(3).getSecond());
        assertEquals("The value for e should be 87",
                     new Integer(87), elements.get(4).getSecond());
        assertEquals("The value for f should be 5",
                     new Integer(5), elements.get(5).getSecond());
    }

    public void testInvalidIntValues() {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("bad", "a(3a), b((3))");
        List<Configuration.Pair<String, Integer>> elements =
                configuration.getIntValues("bad", 87);
        assertEquals("The first element should be named a(3a)",
                     "a(3a)", elements.get(0).getFirst());
        assertEquals("The first element should be named b(",
                     "b(", elements.get(1).getFirst());
    }

    public void testGetSystemConfigLocal () throws Exception {
        String confPath = "configuration.xml";//Thread.currentThread().getContextClassLoader().getResource("configuration.xml").toString();
        System.setProperty(Configuration.CONFIGURATION_PROPERTY, confPath);

        Configuration conf = Configuration.getSystemConfiguration();
        Configuration originalConf = new Configuration (new FileStorage("configuration.xml"));

        assertTrue("Loading via getSystemConfiguration and directly should "
                     + "result in identical configurations",
                     conf.equals(originalConf));
    }

    public void testGetSystemConfigRemote () throws Exception {
        RemoteStorageTest remote = new RemoteStorageTest();
        remote.setUp();
        String serviceUrl = remote.direct_storage.getServiceUrl();
        System.out.println ("Service URL is: " + serviceUrl);

        System.setProperty(Configuration.CONFIGURATION_PROPERTY, serviceUrl);

        Configuration testConf = Configuration.getSystemConfiguration();
        Configuration originalConf = new Configuration (new FileStorage("configuration.xml"));

        assertTrue("Loading via getSystemConfiguration and directly should "
                     + "result in identical configurations",
                     testConf.equals(originalConf));
    }

    public void testNewMemoryBasedEmpty () {
        Configuration conf = Configuration.newMemoryBased();
        int count = 0;

        for (Map.Entry<String, Serializable> entry: conf) {
            count ++;
        }

        assertEquals(count, 0);
    }

    public void testNewMemoryBased () {
        int count = 0;
        ArrayList<String> list = new ArrayList<String> ();
        list.add("foo");
        list.add ("bar");

        Configuration conf = Configuration.newMemoryBased(
                "hello.world", 10,
                "hello.universe", list,
                "hello.nothing", new String[]{"baz"}
        );

        for (Map.Entry<String, Serializable> entry: conf) {
            count ++;
        }

        assertEquals(count, 3);
        assertEquals(conf.getInt("hello.world"), 10);

        List<String> l = conf.getStrings("hello.universe");
        assertEquals(l.size(), 2);
        assertEquals("foo", l.get(0));
        assertEquals("bar", l.get(1));

        l = conf.getStrings("hello.nothing");
        assertEquals(l.size(), 1);
        assertEquals("baz", l.get(0));
    }

    public void testGetClass() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetStorage() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetFirst() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetSecond() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(ConfigurationTest.class);
    }
}
