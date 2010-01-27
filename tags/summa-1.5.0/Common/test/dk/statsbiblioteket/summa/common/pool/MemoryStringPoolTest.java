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
package dk.statsbiblioteket.summa.common.pool;

import dk.statsbiblioteket.summa.common.pool.MemoryStringPool;
import dk.statsbiblioteket.summa.common.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;

/**
 * MemoryStringPool Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MemoryStringPoolTest extends StringPoolSuperTest {
    public MemoryStringPoolTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(MemoryStringPoolTest.class);
    }

    public SortedPool<String> getPool(File location,
                                      String name) throws Exception {
        SortedPool<String> pool = new MemoryStringPool(defaultCollator);
        pool.open(location, name, false, true);
        return pool;
    }

    public void testIO() throws Exception {
        testIO(getPool(1), getPool(1));
    }
    public static void testIO(SortedPool<String> pool1,
                              SortedPool<String> pool2) throws Exception {
        pool1.add("Duksedreng");
        pool1.add("Daskelars");
        pool1.add("Dumrian");
        pool1.add("Drøbel");
        StringPoolSuperTest.compareOrder(
                "The constructed pool should be as expected",
                pool1, 
                new String[]{"Daskelars", "Drøbel", "Duksedreng", "Dumrian"});
        pool1.store();
        pool2.open(poolDir, pool1.getName(), false, false);
        StringPoolSuperTest.compareOrder("The loaded pool should be equal to the"
                                    + " saved", pool1, pool2);
        StringPoolSuperTest.compareOrder(
                "The loaded pool should be as expected",
                pool2,
                new String[]{"Daskelars", "Drøbel", "Duksedreng", "Dumrian"});

        pool1.add("Drillenisse");
        StringPoolSuperTest.compareOrder(
                "Adding after store",
                pool1,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});

        pool2.add("Drillenisse");
        StringPoolSuperTest.compareOrder(
                "Adding after load",
                pool2,
                new String[]{"Daskelars", "Drillenisse", "Drøbel", "Duksedreng",
                             "Dumrian"});
    }

}




