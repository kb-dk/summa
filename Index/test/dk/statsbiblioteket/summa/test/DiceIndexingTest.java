/* $Id: DiceIndexingTest.java,v 1.5 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.5 $
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
package dk.statsbiblioteket.summa.test;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.dice.util.DiceFactory;
import dk.statsbiblioteket.summa.dice.util.RegistryManager;
import dk.statsbiblioteket.summa.dice.EmployerBase;
import dk.statsbiblioteket.summa.dice.Worker;
import dk.statsbiblioteket.summa.dice.ConsumerBase;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.index.dice.IndexConfig;
import dk.statsbiblioteket.summa.index.dice.Merger;
import dk.statsbiblioteket.summa.index.IndexManipulationUtils;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.registry.Registry;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class DiceIndexingTest extends TestCase {

    public void testWECIndexing () throws Exception {

        EmployerBase employer;
        ConsumerBase consumer;
        Worker worker;

        Config conf = new IndexConfig();
        DiceFactory wec = new DiceFactory(conf);
        Registry reg = RegistryManager.getRegistry(conf);

        employer = wec.newEmployer();
        employer.run();

        consumer = wec.newConsumer();
        consumer.run();

        System.out.println ("Known services:");
        for (String s: reg.list()) {
            System.out.println ("\t - " + s);
        }

        worker = wec.newWorker ();
        worker.run();
    }

    public void testMergerNextTask () throws Exception {
        Merger merger = new Merger(new IndexConfig());

        String baseDir = System.getProperty("user.home") + File.separator + "tmp" + File.separator + "wec_test";
        new File (baseDir, "index.0" + File.separator + "tmp.0" + File.separator).mkdirs();
        new File (baseDir, "index.1" + File.separator + "tmp.0" + File.separator).mkdirs();
        new File (baseDir, "index.1" + File.separator + "tmp.1" + File.separator).mkdirs();
        new File (baseDir, "index.1" + File.separator + "tmp.2" + File.separator).mkdirs();
        new File (baseDir, "index.2" + File.separator + "tmp.0" + File.separator).mkdirs();

        Merger.MergeTask mt = merger.getNextMergeTask(baseDir);
        System.out.println (mt);
        assertTrue("Merge targets should be index.1/tmp.{0,1,2}", mt.targets.length == 3);

        IndexManipulationUtils.deletePath(baseDir);
    }

    public void testMergerUpmergeTarget () throws Exception {
        Merger merger = new Merger(new IndexConfig());

        String baseDir = System.getProperty("user.home") + File.separator + "tmp" + File.separator + "wec_test";
        new File (baseDir, "index.0" + File.separator + "tmp.0" + File.separator).mkdirs();
        new File (baseDir, "index.1" + File.separator + "tmp.0" + File.separator).mkdirs();
        new File (baseDir, "index.1" + File.separator + "tmp.1" + File.separator).mkdirs();
        new File (baseDir, "index.1" + File.separator + "tmp.2" + File.separator).mkdirs();
        new File (baseDir, "index.2" + File.separator + "tmp.0" + File.separator).mkdirs();

        Merger.MergeTask mt = merger.getNextUpmergeTask (baseDir);
        for (String target : mt.targets) {
            System.out.println ("Normal upmerge: " + mt);
        }
        assertTrue("MergeTask targets should be index.0/tmp.0 and index.1/tmp.0", mt.targets.length == 2);
        IndexManipulationUtils.deletePath(baseDir);


        new File (baseDir, "index.0" + File.separator + "tmp.0" + File.separator).mkdirs();
        mt = merger.getNextUpmergeTask (baseDir);
        System.out.println ("Upmerge without higher levels (should be null):" + mt);
        assertTrue("There should be no upmerge available if there is only one lonely dir but no higher levels exist", mt == null);
        IndexManipulationUtils.deletePath(baseDir);

    }

    public void testMergerDeletePath () throws Exception {
        String baseDir = System.getProperty("user.home") + File.separator + "tmp" + File.separator + "wec_test" + File.separator + "tmpDeletePath";

        File f = new File (baseDir, "tmp.0");
        f.mkdirs ();

        File.createTempFile("test", null, f);
        File.createTempFile("test", null, f);
        File.createTempFile("test", null, f.getParentFile());

        IndexManipulationUtils.deletePath(baseDir);

    }

    public void testFileBaseNameExtraction () throws Exception {
        String dummyFile = File.separator + "hello" + File.separator + "world.txt";
        assertTrue(Merger.getFileBaseName(dummyFile).equals ("world.txt"));
        System.out.println ("Basename for file: "  + dummyFile + " is " + Merger.getFileBaseName(dummyFile));

        System.out.println (new SimpleDateFormat("yyyyMMdd'-'HHmm").format(new Date()));

    }



}



