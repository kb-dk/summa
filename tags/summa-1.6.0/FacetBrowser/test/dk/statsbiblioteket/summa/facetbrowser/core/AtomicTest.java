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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: AtomicTest.java,v 1.5 2007/10/04 13:28:17 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Experimenting with atomic power.
 *
 * AtomicIntArray might be fast enough for updates, but clearing for next run
 * takes far too much time and reallocating every time taxes the garbage
 * collector. Seems like we're stuck with traditional intarrays...
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class AtomicTest extends TestCase {
    private static final int SIZE = 100 * 1000 * 1000;
    private static final int WARMUP = 1;
    private static final int RUNS = 3;

    // Run this with -Xmx512m -Xms512m
    public void testClear() throws Exception {
        long mb = 4 * SIZE / 1048576;
        long startTime = System.nanoTime();
        int[] plain = new int[SIZE];
        long allocateTime = System.nanoTime() - startTime;
        System.out.println(mb + " MB allocated in "
                           + allocateTime / 1000000.0 + " ms");
        System.gc();

        startTime = System.nanoTime();
        fillWithGarbage(plain);
        long fillTime = System.nanoTime() - startTime;
        System.out.println(mb + " MB filled with garbage in "
                           + fillTime / 1000000.0 + " ms");
        
        startTime = System.nanoTime();
        Arrays.fill(plain, 0);
        long fillNS = System.nanoTime() - startTime;
        double fillMS = fillNS / 1000000.0;
        System.out.println(
                "Array.fill(array, 0) of " + mb + " MB finished in "
                + fillMS  + " ms => "
                + Math.round(mb / fillMS * 1000) + " MB/second");

        startTime = System.nanoTime();
        Arrays.fill(plain, 0);
        fillNS = System.nanoTime() - startTime;
        fillMS = fillNS / 1000000.0;
        System.out.println(
                "Array.fill(array, 0) of " + mb + " MB take 2 finished in "
                + fillMS  + " ms => "
                + Math.round(mb / fillMS * 1000) + " MB/second");

        startTime = System.nanoTime();
        clear(plain);
        long clearNS = System.nanoTime() - startTime;
        double clearMS = clearNS / 1000000.0;
        System.out.println(
                "System.arraycopy(...) clear of " + mb + " MB finished in "
                + clearMS  + " ms => "
                + Math.round(mb / clearMS * 1000) + " MB/second");
    }

    private void fillWithGarbage(int[] array) {
        int counter = 0;
        for (int i = 0 ; i < array.length ; i++) {
            array[i] = counter++;
        }
    }
    
    public void testDumpMemoryUsage() throws Exception {
        System.out.println("Initial mem: " + mem());
        int[] l = new int[SIZE];
        System.out.println("Imt mem: " + mem());
        l = null;
        AtomicIntegerArray al = new AtomicIntegerArray(SIZE);
        long nano = System.nanoTime();
        for (int i = 0 ; i < SIZE ; i++) {
            al.set(i, 0);
        }
        System.out.println("AInt mem: " + mem() + "/" + SIZE
                           + " ints  filled in "
                           + (System.nanoTime() - nano) / 1000000.0
                           + " milliseconds");
    }

    // disabled due to    "java.lang.OutOfMemoryError: Java heap space"
    public void disabledtestDumpSpeed() throws Exception {
        Random random = new Random();
        System.out.println("Creating arrays of size " + SIZE);
        int[] l = new int[SIZE];
        AtomicInteger[] al = new AtomicInteger[SIZE];
        for (int i = 0 ; i < SIZE ; i++) {
            al[i] = new AtomicInteger(0);
        }
        AtomicIntegerArray ala = new AtomicIntegerArray(SIZE);
        System.gc();
        System.out.println("Warming up");
        for (int i = 0 ; i < WARMUP ; i++) {
            for (int j = 0 ; j < SIZE ; j++) {
                int pos = random.nextInt(SIZE);
                l[pos]++;
                al[pos].incrementAndGet();
                ala.incrementAndGet(pos);
            }
        }
        System.out.println("Running");
        for (int i = 0 ; i < RUNS ; i++) {

            System.gc();
            long startTime = System.currentTimeMillis();
            for (int j = 0 ; j < SIZE ; j++) {
                random.nextInt(SIZE);
            }
            long randomTime = System.currentTimeMillis()-startTime;
            System.out.println("\nRandom time: "
                               + Math.round(SIZE / randomTime)
                               + " randoms/ms");

            System.gc();
            startTime = System.currentTimeMillis();
            for (int j = 0 ; j < SIZE ; j++) {
                l[random.nextInt(SIZE)]++;
            }
            double spend = (System.currentTimeMillis() - startTime -randomTime);
            System.out.println("Plain int:  "
                               + Math.round(SIZE / spend)
                               + " increments/ms");
            startTime = System.currentTimeMillis();
            Arrays.fill(l, 0);
            spend = (System.currentTimeMillis() - startTime);
            System.out.println("Plain int clear in " + spend + " ms");

            System.gc();
            startTime = System.currentTimeMillis();
            for (int j = 0 ; j < SIZE ; j++) {
                l[random.nextInt(SIZE)]++;
            }
            spend = (System.currentTimeMillis() - startTime -randomTime);
            System.out.println("Plain int take 2:  "
                               + Math.round(SIZE / spend)
                               + " increments/ms");
            startTime = System.currentTimeMillis();
            clear(l);
            spend = (System.currentTimeMillis() - startTime);
            System.out.println("Plain int custom clear in " + spend + " ms");

            System.gc();
            startTime = System.currentTimeMillis();
            for (int j = 0 ; j < SIZE ; j++) {
                al[random.nextInt(SIZE)].getAndIncrement();
            }
            spend = (System.currentTimeMillis() - startTime - randomTime);
            System.out.println("Atomic int: "
                               + Math.round(SIZE / spend)
                               + " increments/ms");
            startTime = System.currentTimeMillis();
            for (AtomicInteger anAl : al) {
                anAl.set(0);
            }
            spend = (System.currentTimeMillis() - startTime);
            System.out.println("Atomic int clear in " + spend + " ms");

            System.gc();
            startTime = System.currentTimeMillis();
            for (int j = 0 ; j < SIZE ; j++) {
                ala.getAndIncrement(random.nextInt(SIZE));
            }
            spend = (System.currentTimeMillis() - startTime - randomTime);
            System.out.println("Atomic int array: "
                               + Math.round(SIZE / spend)
                               + " increments/ms");
            startTime = System.currentTimeMillis();
            for (int j = 0 ; j < ala.length() ; j++) {
                ala.set(j, 0);
            }
            spend = (System.currentTimeMillis() - startTime);
            System.out.println("Atomic int array clear in " + spend + " ms");
        }
    }


    public static void main(String[] args) throws Exception {
        AtomicTest t = new AtomicTest();
        t.dumpThreadSpeed();
        t.disabledtestDumpSpeed();
    }

    private static final int[] ZERO = new int[20000];
    public static void clear(int[] list) {
        int listLength = list.length;
        for (int i = 0 ; i < listLength / ZERO.length ; i++) {
            System.arraycopy(ZERO, 0, list, i * ZERO.length, ZERO.length);
        }
        Arrays.fill(list,
                    listLength / ZERO.length * ZERO.length, listLength,
                    0);
    }


    public void dumpThreadSpeed() {
        int[] THREADS = new int[]{1, 2, 3, 4, 5};
        int[] UPDATES = new int[]{100, 1000, 10000, 100000, 1000000, 10000000,
                                  100000000};
        System.out.println(
                "Updates\tThreads\tType\tUpd/sec\tRel.time\tClearOnGet");
        for (int threads : THREADS) {
            for (int updates: UPDATES) {
                for (boolean cleanOnGet: new boolean[]{true, false}) {
//                System.out.println("\nTrying with " + threads + " threads, "
//                                   + updates + " updates");
//                long sim = time(AtomicThread.TYPE.SIMULATE, threads, updates);
//                    time(AtomicThread.TYPE.SIMULATE, threads, updates,
//                         -1, cleanOnGet);
                    long plain = time(AtomicThread.TYPE.PLAIN, threads, updates,
                                      -1, cleanOnGet);
                    time(AtomicThread.TYPE.ATOMIC, threads, updates, plain,
                         cleanOnGet);
//                System.out.println("Atomic time was "
//                                   + Math.round(1.0 * plain  / atomic * 100)
//                                   + "% of plain time");
                }
            }
        }
    }

    protected long time(AtomicThread.TYPE type, int threadCount, int updates,
                        long previous, boolean clearOnGet) {
        ArrayList<AtomicThread> threads =
                new ArrayList<AtomicThread>(threadCount);
        AtomicIntegerArray al = new AtomicIntegerArray(SIZE);

        int[][] plains = new int[threadCount][SIZE];

        for (int j = 0 ; j < threadCount ; j++) {
            threads.add(new AtomicThread(plains[j], al, type,
                                         updates / threadCount));
        }
        System.gc();
        long startTime = System.currentTimeMillis();
        executeThreads(threads);

        long mergeStart = System.currentTimeMillis();
        if (type == AtomicThread.TYPE.PLAIN && threadCount > 1) {
            // Merge
            int[] first = threads.get(0).plain;
            for (int i = 1 ; i < threadCount ; i++) {
                int[] another = threads.get(i).plain;
                if (clearOnGet) {
                    for (int j = 0 ; j < first.length ; j++) {
                        int t = another[j];
                        if (t != 0) {
                            first[j] += t;
                            another[j] = 0;
                        }
                    }
                } else {
                    for (int j = 0 ; j < first.length ; j++) {
                        first[j] += another[j];
                    }
                }
            }
        }
        long mergeTime = System.currentTimeMillis()-mergeStart;

        // Iterate

        if (type == AtomicThread.TYPE.PLAIN) {
            int[] first = threads.get(0).plain;
            long temp = 0;
            for (int i = 0 ; i < SIZE ; i++) {
                temp = first[i];
                first[i] = 0;
            }
            if (temp == 0) { temp += 1; } // Dummy
        }

        if (type == AtomicThread.TYPE.ATOMIC) {
            long temp = 0;
            if (clearOnGet)  {
                for (int i = 0 ; i < SIZE ; i++) {
                    int t = al.get(i);
                    if (t != 0) {
                        temp += t;
                        al.set(i, 0);
                    }
                }
            } else {
                for (int i = 0 ; i < SIZE ; i++) {
                    temp = al.get(i);
                }
            }
            if (temp == 0) { temp += 1; } // Dummy
        }

        // Clear
        AtomicThread.TYPE clearType = AtomicThread.TYPE.PLAIN;

        if (type == AtomicThread.TYPE.ATOMIC) {
            clearType = AtomicThread.TYPE.CLEAR_ATOMIC;
        }
        if (type == AtomicThread.TYPE.SIMULATE) {
            clearType = AtomicThread.TYPE.CLEAR_SIMULATE;
        }
        threads.clear();
        if (!clearOnGet) {
            for (int j = 0 ; j < threadCount ; j++) {
                AtomicThread at = new AtomicThread(
                        plains[j], al, clearType,
                        updates / threadCount);
                at.atomicClearStart = j * SIZE / threadCount;
                at.atomicClearEnd = Math.min(SIZE, (j+1) * SIZE / threadCount);
                threads.add(at);
            }
            executeThreads(threads);
        }

        double spend = (System.currentTimeMillis()-startTime) / 1000.0;
        long speed = Math.round(updates / spend);
        System.out.println(updates + "\t" + threadCount + "\t" + type + "\t"
                           + speed + "\t" +
                           (previous == -1 ? "" :
                            Math.round(1.0 * previous  / speed * 100) + "%")
                           + "\t" + clearOnGet);
//        System.out.println(type + ": " + Math.round(updates / spend)
//                           + " updates/sec"); //. Merge time: " + mergeTime + " ms");
        return speed;
    }

    private void executeThreads(ArrayList<AtomicThread> threads) {
        for (AtomicThread thread: threads) {
            thread.start();
        }
        for (AtomicThread thread: threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String mem() {
        System.gc();
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = total - free;
        if (used > 1024*1024) {
            return used / 1024 / 1024 + "MB";
        } else if (used > 1024) {
            return used / 1024 + "KB";
        } else {
            return used + "bytes";
        }
    }
}




