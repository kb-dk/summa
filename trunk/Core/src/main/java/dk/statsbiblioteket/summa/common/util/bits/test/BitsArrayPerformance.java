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
package dk.statsbiblioteket.summa.common.util.bits.test;

import dk.statsbiblioteket.summa.common.util.bits.*;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tests the performance of different BitsArrays.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayPerformance {
    /**
     * Main method.
     * @param args Arguments from command line.
     * @throws Exception If error.
     */
    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            if ("-h".equals(args[0])) {
                usage();
                return;
            } else if ("-s".equals(args[0])) {
                new BitsArrayPerformance().testPerformance(false);
                return;
            } else {
                System.err.println("Unknown argument: " + args[0]);
                usage();
                return;
            }
        }
        new BitsArrayPerformance().testPerformance(true);
    }

    /**
     * Print usage to {@link System#out}.
     */
    public static void usage() {
        System.out.println("Usage: BitsArrayPerformance [-s]");
        System.out.println("");
        System.out.println(
                "-s: Test using safe mode with range-checking and "
                + "auto-adjustment of length and bits/value");

    }

    /**
     * No exansion-testing.
     */
    public void testPerformance(boolean unsafe) throws Exception {
        int[] actionCounts = new int[]{
                10*1000*1000};
        int[] lengths =      new int[]{
                1000, 100*1000, 1000*1000, 5*1000*1000, 10*1000*1000,
                50*1000*1000};
        int[] valueMaxs =    new int[]{
                1, 15, 255, 256, 65535, 65536, 2097151, 2147483646};
        // 2^31-2 is Random.nextInt(x)-limit
        ACTION[] actions = new ACTION[]{ACTION.write, ACTION.read};
        List<BitsArrayGenerator> bags =
                new ArrayList<BitsArrayGenerator>(
                        getGenerators());
        bags.add(new SignalBAG(ARRAYSIGNAL));
        System.out.println(getPerformanceHeader(bags));
        for (int ac : actionCounts) {
            for (int l : lengths) {
                for (ACTION action : actions) {
                    for (int vm : valueMaxs) {
                        testPerformance(bags, action, l, vm, ac, l, vm, unsafe);
                    }
                }
            }
        }
    }

    /**
     * @param bags List over generators 'bags'.
     * @return Return performance header.
     */
    public String getPerformanceHeader(
            List<BitsArrayGenerator> bags) {
        String header = String.format(
                "%12s%12s%12s%12s",
                "actionCount", "arrayLength", "actionType", "valueMax");
        for (String name: getArrayNames(bags)) {
            header += String.format("%12s", name);
        }
        return header;
    }

    public static List<BitsArrayGenerator> getGenerators() {
        return Arrays.asList(
                new BitsArrayGenerator() {
                    @Override
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayPacked(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    @Override
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayAligned(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    @Override
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayInt(length);
                    }
                },
                new BitsArrayGenerator() {
                    @Override
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArray64Packed(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    @Override
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArray64Aligned(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    @Override
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayConstant();
                    }
                });
    }

    /** Private enum. */
    private enum ACTION { read, write }
    /**
     * A dry-run is performed before actual measuring.
     */
    public void testPerformance(List<BitsArrayGenerator> bags, ACTION action,
                                int initialLength, int initialValueMax,
                                int mutatorActions, int mutatorIndexMax,
                                int writeValueMax, boolean unsafe)
                                                              throws Exception {
        final int warmup = 1;
        final int tests = 3;
        final int buffer = 1000;

        for (int i = 0; i < warmup; i++) {
            for (BitsArrayGenerator bag : bags) {
                measureActionPerformance(
                        bag, action, initialLength, initialValueMax,
                        mutatorActions, mutatorIndexMax, writeValueMax,
                        unsafe);
            }
        }

        StringWriter sw = new StringWriter(buffer);
        sw.append(String.format(
                "%12d%12d%12s%12d",
                mutatorActions, mutatorIndexMax, action, writeValueMax));
        long base = 0;
        for (int test = 0; test < tests; test++) {
            base += measureActionPerformanceNull(
                    action, initialLength, mutatorActions,
                    mutatorIndexMax, writeValueMax);
        }
        for (BitsArrayGenerator bag : bags) {
            long time = 0;
            for (int test = 0; test < tests; test++) {
                time += measureActionPerformance(
                        bag, action, initialLength, initialValueMax,
                        mutatorActions,
                        mutatorIndexMax, writeValueMax,
                        unsafe);
            }
            sw.append(String.format("%12d", Math.max(0, time - base) / tests));
        }
        System.out.println(sw.toString());
    }

    public long measureActionPerformance(BitsArrayGenerator bag, ACTION action,
                                         int initialLength, int initialValueMax,
                                         int numberOfActions, int writeIndexMax,
                                         int writeValueMax, boolean unsafe)
                                                              throws Exception {
        final int sleep = 100;
        BitsArray testBA = bag.create(1, 1);
        if (testBA == ARRAYSIGNAL) {
            return measureActionPerformanceArray(
                    action, initialLength, numberOfActions,
                    writeIndexMax, writeValueMax);
        }

        System.gc();
        Thread.sleep(sleep);
        long startTime = System.currentTimeMillis();
        BitsArray ba = createAndFillBitsArray(
                bag, initialLength, initialValueMax, numberOfActions,
                writeIndexMax, writeValueMax, unsafe);
        if (action == ACTION.write) {
            return System.currentTimeMillis() - startTime;
        }
        ba.set(writeIndexMax - 1, 0); // Ensure filled
        return measureReadPerformance(ba, numberOfActions, unsafe);
    }

    private long measureActionPerformanceArray(ACTION action, int initialLength,
                                               int numberOfActions,
                                               int writeIndexMax,
                                               int writeValueMax)
                                                              throws Exception {
        final int sleep = 100;
        System.gc();
        Thread.sleep(sleep);
        long startTime = System.currentTimeMillis();
        int[] direct = createAndFillDirectArray(
                initialLength, numberOfActions, writeIndexMax, writeValueMax);
        if (action == ACTION.write) {
            return System.currentTimeMillis() - startTime;
        }
        return measureReadPerformance(direct, numberOfActions);
    }

    private long measureActionPerformanceNull(ACTION action, int initialLength,
                                              int numberOfActions,
                                              int writeIndexMax,
                                              int writeValueMax)
                                                              throws Exception {
        final int sleep = 100;
        System.gc();
        Thread.sleep(sleep);
        long startTime = System.currentTimeMillis();
        createAndFillNullArray(
                initialLength, numberOfActions, writeIndexMax, writeValueMax);
        if (action == ACTION.write) {
            return System.currentTimeMillis() - startTime;
        }
        return measureReadNullPerformance(writeIndexMax, numberOfActions);
    }

    private long measureReadPerformance(BitsArray ba, int numberOfReads,
                                        boolean unsafe) {
        final int randomNum = 87;
        int max = ba.size();
        long startTime = System.currentTimeMillis();
        Random random = new Random(randomNum);
        int lastVal = 0;
        if (unsafe) {
            for (int read = 0; read < numberOfReads; read++) {
                lastVal = ba.fastGetAtomic(random.nextInt(max));
            }
        } else {
            for (int read = 0; read < numberOfReads; read++) {
                lastVal = ba.getAtomic(random.nextInt(max));
            }
        }
        if (lastVal < 0) {
            throw new IllegalStateException(
                    "Values < 0 should not be possible (JIT-fooling check)");
        }
        return System.currentTimeMillis() - startTime;
    }

    private long measureReadPerformance(int[] direct, int numberOfReads) {
        final int randomNum = 87;
        int max = direct.length;
        long startTime = System.currentTimeMillis();
        Random random = new Random(randomNum);
        int lastVal = 0;
        for (int read = 0; read < numberOfReads; read++) {
            lastVal = direct[random.nextInt(max)];
        }
        if (lastVal < 0) {
            throw new IllegalStateException(
                    "Values < 0 should not be possible (JIT-fooling check)");
        }
        return System.currentTimeMillis() - startTime;
    }

    private long measureReadNullPerformance(int length, int numberOfReads) {
        final int randomNum = 87;
        long startTime = System.currentTimeMillis();
        Random random = new Random(randomNum);
        for (int read = 0; read < numberOfReads; read++) {
            random.nextInt(length);
        }
        return System.currentTimeMillis() - startTime;
    }

    public BitsArray createAndFillBitsArray(BitsArrayGenerator bag,
                                            int initialLength,
                                            int initialValueMax,
                                            int numberOfWrites,
                                            int writeIndexMax,
                                            int writeValueMax, boolean unsafe)
                                                              throws Exception {
        final int randomNum = 87;
        Random random = new Random(randomNum);
        BitsArray ba = bag.create(initialLength, initialValueMax);
        if (unsafe) {
            for (int write = 0; write < numberOfWrites; write++) {
                ba.fastSet(random.nextInt(writeIndexMax),
                           random.nextInt(writeValueMax + 1));
            }
        } else {
            for (int write = 0; write < numberOfWrites; write++) {
                ba.set(random.nextInt(writeIndexMax),
                       random.nextInt(writeValueMax + 1));
            }
        }
        int top = Math.max(initialLength, writeIndexMax) - 1;
        ba.set(top, ba.fastGetAtomic(top)); // Fix size
        return ba;
    }

    private int[] createAndFillDirectArray(int initialLength,
                                           int numberOfWrites, 
                                           int writeIndexMax,
                                           int writeValueMax) {
        final int randomNum = 87;
        Random random = new Random(randomNum);
        int[] direct = new int[writeIndexMax];
        for (int write = 0; write < numberOfWrites; write++) {
            direct[random.nextInt(writeIndexMax)] =
                   random.nextInt(writeValueMax + 1);
        }
        return direct;
    }

    private void createAndFillNullArray(int initialLength, int numberOfWrites,
                                        int writeIndexMax, int writeValueMax) {
        final int randomNum = 87;
        Random random = new Random(randomNum);
        for (int write = 0; write < numberOfWrites; write++) {
            random.nextInt(writeIndexMax);
            random.nextInt(writeValueMax + 1);
        }
    }

    public static List<String> getArrayNames(List<BitsArrayGenerator> bags) {
        List<String> names = new ArrayList<String>(bags.size());
        for (BitsArrayGenerator bag : bags) {
            BitsArray ba = bag.create(1, 1);
            String name;
            if (ba == ARRAYSIGNAL) {
                name = "int[]";
            } else if (ba == NULLSIGNAL) {
                name = "null";
            } else {
                name = ba.getClass().getSimpleName().replace("BitsArray", "");
            }
            names.add(name);
        }
        return names;
    }

    private class SignalBAG implements BitsArrayGenerator {
        /** Signals. */
        private BitsArray signal;
        
        public SignalBAG(BitsArray signal) {
            this.signal = signal;
        }
        @Override
        public BitsArray create(int length, int maxValue) {
            return signal;
        }
    }

    // Use int[]
    public static final BitsArray ARRAYSIGNAL = new BitsArrayImpl() {
        @Override
        public void fastSet(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        protected void ensureSpace(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public int getAtomic(int index) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public int fastGetAtomic(int index) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public void assign(BitsArray other) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public int getMaxValue() {
            throw new UnsupportedOperationException("Not supported");
        }
    };

    // No read or write, only random values
    public static final BitsArray NULLSIGNAL = new BitsArrayImpl() {
        @Override
        public void fastSet(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        protected void ensureSpace(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public int getAtomic(int index) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public int fastGetAtomic(int index) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public void assign(BitsArray other) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        public int getMaxValue() {
            throw new UnsupportedOperationException("Not supported");
        }
    };

    public static interface BitsArrayGenerator {
        BitsArray create(int length, int maxValue);
    }
}
