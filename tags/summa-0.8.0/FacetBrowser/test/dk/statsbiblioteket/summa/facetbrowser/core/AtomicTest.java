/* $Id: AtomicTest.java,v 1.5 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.5 $
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
public class AtomicTest extends TestCase {
    private static final int SIZE = 10 * 1000 * 1000;
    private static final int WARMUP = 1;
    private static final int RUNS = 3;

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

    public void testDumpSpeed() throws Exception {
        Random random = new Random();
        System.out.println("Creating");
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


    public static void main(String[] args) {
        new AtomicTest().dumpThreadSpeed();
    }

    public void dumpThreadSpeed() {
        int[] THREADS = new int[]{1, 2, 3, 4, 5};
        int[] UPDATES = new int[]{100, 1000, 10000, 100000, 1000000, 10000000};
        System.out.println("Updates\tThreads\tType\tUpd/sec\tRel.time");
        for (int threads : THREADS) {
            for (int updates: UPDATES) {
//                System.out.println("\nTrying with " + threads + " threads, "
//                                   + updates + " updates");
//                long sim = time(AtomicThread.TYPE.SIMULATE, threads, updates);
                long plain =
                        time(AtomicThread.TYPE.PLAIN, threads, updates, -1);
                time(AtomicThread.TYPE.ATOMIC, threads, updates, plain);
//                System.out.println("Atomic time was "
//                                   + Math.round(1.0 * plain  / atomic * 100)
//                                   + "% of plain time");
            }
        }
    }

    protected long time(AtomicThread.TYPE type, int threadCount, int updates,
                        long previous) {
        ArrayList<AtomicThread> threads =
                new ArrayList<AtomicThread>(threadCount);
        AtomicInteger[] al = new AtomicInteger[SIZE];
        for (int j = 0 ; j < SIZE ; j++) {
            al[j] = new AtomicInteger(0);
        }

        for (int j = 0 ; j < threadCount ; j++) {
            threads.add(new AtomicThread(new int[SIZE], al, type,
                                         updates / threadCount));
        }
        System.gc();
        long startTime = System.currentTimeMillis();
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
        long mergeStart = System.currentTimeMillis();
        if (type == AtomicThread.TYPE.PLAIN && threadCount > 1) {
            // Merge
            int[] first = threads.get(0).plain;
            for (int i = 1 ; i < threadCount ; i++) {
                int[] another = threads.get(i).plain;
                for (int j = 0 ; j < first.length ; j++) {
                    first[j] += another[j];
                }
            }
        }
        long mergeTime = System.currentTimeMillis()-mergeStart;

        if (type == AtomicThread.TYPE.PLAIN) {
            int[] first = threads.get(0).plain;
            long temp = 0;
            for (int i = 0 ; i < SIZE ; i++) {
                temp = first[i];
            }
            if (temp == 0) { temp += 1; } // Dummy
        }

        if (type == AtomicThread.TYPE.ATOMIC) {
            long temp = 0;
            for (int i = 0 ; i < SIZE ; i++) {
                temp = al[i].get();
            }
            if (temp == 0) { temp += 1; } // Dummy
        }

        double spend = (System.currentTimeMillis()-startTime) / 1000.0;
        long speed = Math.round(updates / spend);
        System.out.println(updates + "\t" + threadCount + "\t" + type + "\t"
                           + speed + "\t" +
                           (previous == -1 ? "" :
                            Math.round(1.0 * previous  / speed * 100) + "%"));
//        System.out.println(type + ": " + Math.round(updates / spend)
//                           + " updates/sec"); //. Merge time: " + mergeTime + " ms");
        return speed;
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
