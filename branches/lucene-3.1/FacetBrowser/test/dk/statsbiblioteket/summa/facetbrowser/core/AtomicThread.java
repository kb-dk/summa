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
 * CVS:  $Id: AtomicThread.java,v 1.4 2007/10/04 13:28:17 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.Random;
import java.util.Arrays;

/**
 * Helper class to test the speed of operations on AtomicInteger vs. int.
 */
public class AtomicThread extends Thread {
    public int[] plain;
    AtomicIntegerArray atomic;
    Random random = new Random();
    int runs;
    TYPE type;
    enum TYPE {SIMULATE, PLAIN, ATOMIC,
        CLEAR_PLAIN, CLEAR_ATOMIC, CLEAR_SIMULATE}
    public int atomicClearStart;
    public int atomicClearEnd;


    public AtomicThread(int[] plain, AtomicIntegerArray atomic, TYPE type,
                        int runs) {
        this.plain = plain;
        this.atomic = atomic;
        this.type = type;
        this.runs = runs;
    }

    public void run() {
        int length;
        switch (type) {
            case PLAIN:
                length = plain.length;
                for (int i = 0; i < runs; i++) {
                    plain[random.nextInt(length)]++;
                }
                break;
            case CLEAR_PLAIN:
                Arrays.fill(plain, 0);
                break;
            case ATOMIC:
                length = atomic.length();
                for (int i = 0; i < runs; i++) {
                    atomic.incrementAndGet(random.nextInt(length));
                }
                break;
            case SIMULATE:
                for (int i = 0; i < runs; i++) {
                    random.nextInt();
                }
                break;
            case CLEAR_ATOMIC:
                for (int i = atomicClearStart ; i < atomicClearEnd ; i++) {
                    atomic.set(i, 0);
                }
                break;
            case CLEAR_SIMULATE:
                break;
        }
    }
}




