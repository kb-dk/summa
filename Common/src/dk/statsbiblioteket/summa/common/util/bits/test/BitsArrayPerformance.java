/**
 * Created: te 02-01-2010 19:26:22
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.util.bits.test;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.util.bits.*;

import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

/**
 * Tests the performance of different BitsArrays.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayPerformance {
    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0 && "-h".equals(args[0])) {
            System.out.println("Usage: BitsArrayPerformance");
            System.out.println("");
            System.out.println("No arguments as the test itself iterates "
                               + "through cases. This might change later.");
            return;
        }
        new BitsArrayPerformance().testPerformance();
    }

    /* No exansion-testing */
    public void testPerformance() throws Exception {
        int[] actionCounts = new int[]{
                10*1000*1000};
        int[] lengths =      new int[]{
                1000, 100*1000, 1000*1000, 10*1000*1000, 50*1000*1000};
        int[] valueMaxs =    new int[]{
                1, 15, 255, 256, 65535, 65536, 2097151, 2147483646};
        // 2^31-2 is Random.nextInt(x)-limit
        ACTION[] actions = new ACTION[]{ACTION.write, ACTION.read};
        List<BitsArrayGenerator> bags =
                new ArrayList<BitsArrayGenerator>(
                        getGenerators());
        bags.add(new SignalBAG(ARRAYSIGNAL));
        System.out.println(getPerformanceHeader(bags));
        for (int ac: actionCounts) {
            for (int l: lengths) {
                for (ACTION action: actions) {
                    for (int vm: valueMaxs) {
                        testPerformance(bags, action, l, vm, ac, l, vm);
                    }
                }
            }
        }
    }

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
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayPacked(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayAligned(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayInt(length);
                    }
                },
                new BitsArrayGenerator() {
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArray64Packed(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArray64Aligned(length, maxValue);
                    }
                },
                new BitsArrayGenerator() {
                    public BitsArray create(int length, int maxValue) {
                        return new BitsArrayConstant();
                    }
                });
    }


    private enum ACTION {read, write}
    /* A dry-run is performed before actual measuering */
    public void testPerformance(
            List<BitsArrayGenerator> bags, ACTION action,
            int initialLength, int initialValueMax,
            int mutatorActions, int mutatorIndexMax, int writeValueMax)
                                                              throws Exception {
        final int WARMUP = 1;
        final int TESTS = 3;

        for (int i = 0 ; i < WARMUP ; i++) {
            for (BitsArrayGenerator bag: bags) {
                measureActionPerformance(
                        bag, action, initialLength, initialValueMax,
                        mutatorActions, mutatorIndexMax, writeValueMax);
            }
        }

        StringWriter sw = new StringWriter(1000);
        sw.append(String.format(
                "%12d%12d%12s%12d",
                mutatorActions, mutatorIndexMax, action, writeValueMax));
        long base = 0;
        for (int test = 0 ; test < TESTS ; test++) {
            base += measureActionPerformanceNull(
                    action, initialLength, mutatorActions,
                    mutatorIndexMax, writeValueMax);
        }
        for (BitsArrayGenerator bag: bags) {
            long time = 0;
            for (int test = 0 ; test < TESTS ; test++) {
                time += measureActionPerformance(
                        bag, action, initialLength, initialValueMax,
                        mutatorActions,
                        mutatorIndexMax, writeValueMax);
            }
            sw.append(String.format("%12d", Math.max(0, time-base) / TESTS));
        }
        System.out.println(sw.toString());
    }

    public long measureActionPerformance(
            BitsArrayGenerator bag, ACTION action,
            int initialLength, int initialValueMax,
            int numberOfActions, int writeIndexMax, int writeValueMax)
                                                              throws Exception {
        BitsArray testBA = bag.create(1, 1);
        if (testBA == ARRAYSIGNAL) {
            return measureActionPerformanceArray(
                    action, initialLength, numberOfActions,
                    writeIndexMax, writeValueMax);
        }

        System.gc();
        Thread.sleep(100);
        long startTime = System.currentTimeMillis();
        BitsArray ba = createAndFillBitsArray(
                bag, initialLength, initialValueMax, numberOfActions,
                writeIndexMax, writeValueMax);
        if (action == ACTION.write) {
            return System.currentTimeMillis() - startTime;
        }
        ba.set(writeIndexMax-1, 0); // Ensure filled
        return measureReadPerformance(ba, numberOfActions);
    }

    private long measureActionPerformanceArray(
            ACTION action, int initialLength, int numberOfActions,
            int writeIndexMax, int writeValueMax) throws Exception {
        System.gc();
        Thread.sleep(100);
        long startTime = System.currentTimeMillis();
        int[] direct = createAndFillDirectArray(
                initialLength, numberOfActions, writeIndexMax, writeValueMax);
        if (action == ACTION.write) {
            return System.currentTimeMillis() - startTime;
        }
        return measureReadPerformance(direct, numberOfActions);
    }

    private long measureActionPerformanceNull(
            ACTION action, int initialLength, int numberOfActions,
            int writeIndexMax, int writeValueMax) throws Exception {
        System.gc();
        Thread.sleep(100);
        long startTime = System.currentTimeMillis();
        createAndFillNullArray(
                initialLength, numberOfActions, writeIndexMax, writeValueMax);
        if (action == ACTION.write) {
            return System.currentTimeMillis() - startTime;
        }
        return measureReadNullPerformance(writeIndexMax, numberOfActions);
    }

    private long measureReadPerformance(BitsArray ba, int numberOfReads) {
        int max = ba.size();
        long startTime = System.currentTimeMillis();
        Random random = new Random(87);
        int lastVal = 0;
        for (int read = 0 ; read < numberOfReads ; read++) {
            lastVal = ba.get(random.nextInt(max));
        }
        if (lastVal < 0) {
            throw new IllegalStateException(
                    "Values < 0 should not be possible (JIT-fooling check)");
        }
        return System.currentTimeMillis() - startTime;
    }

    private long measureReadPerformance(int[] direct, int numberOfReads) {
        int max = direct.length;
        long startTime = System.currentTimeMillis();
        Random random = new Random(87);
        int lastVal = 0;
        for (int read = 0 ; read < numberOfReads ; read++) {
            lastVal = direct[random.nextInt(max)];
        }
        if (lastVal < 0) {
            throw new IllegalStateException(
                    "Values < 0 should not be possible (JIT-fooling check)");
        }
        return System.currentTimeMillis() - startTime;
    }

    private long measureReadNullPerformance(int length, int numberOfReads) {
        long startTime = System.currentTimeMillis();
        Random random = new Random(87);
        for (int read = 0 ; read < numberOfReads ; read++) {
            random.nextInt(length);
        }
        return System.currentTimeMillis() - startTime;
    }

    public BitsArray createAndFillBitsArray(
            BitsArrayGenerator bag,
            int initialLength, int initialValueMax,
            int numberOfWrites, int writeIndexMax, int writeValueMax)
                                                              throws Exception {
        Random random = new Random(87);
        BitsArray ba = bag.create(initialLength, initialValueMax);
        for (int write = 0 ; write < numberOfWrites ; write++) {
            ba.set(random.nextInt(writeIndexMax),
                   random.nextInt(writeValueMax+1));
        }
        return ba;
    }

    private int[] createAndFillDirectArray(
            int initialLength, int numberOfWrites,
            int writeIndexMax, int writeValueMax) {
        Random random = new Random(87);
        int[] direct = new int[writeIndexMax];
        for (int write = 0 ; write < numberOfWrites ; write++) {
            direct[random.nextInt(writeIndexMax)] =
                   random.nextInt(writeValueMax+1);
        }
        return direct;
    }

    private void createAndFillNullArray(
            int initialLength, int numberOfWrites,
            int writeIndexMax, int writeValueMax) {
        Random random = new Random(87);
        for (int write = 0 ; write < numberOfWrites ; write++) {
            random.nextInt(writeIndexMax);
            random.nextInt(writeValueMax+1);
        }
    }

    public static List<String> getArrayNames(List<BitsArrayGenerator> bags) {
        List<String> names = new ArrayList<String>(bags.size());
        for (BitsArrayGenerator bag: bags) {
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
        private BitsArray signal;
        public SignalBAG(BitsArray signal) {
            this.signal = signal;
        }
        public BitsArray create(int length, int maxValue) {
            return signal;
        }
    }

    // Use int[]
    public static final BitsArray ARRAYSIGNAL = new BitsArrayImpl() {
        @Override
        protected void unsafeSet(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        protected void ensureSpace(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        public int getAtomic(int index) {
            throw new UnsupportedOperationException("Not supported");
        }
        public void assign(BitsArray other) {
            throw new UnsupportedOperationException("Not supported");
        }
        public int getMaxValue() {
            throw new UnsupportedOperationException("Not supported");
        }
    };

    // No read or write, only random values
    public static final BitsArray NULLSIGNAL = new BitsArrayImpl() {
        @Override
        protected void unsafeSet(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        @Override
        protected void ensureSpace(int index, int value) {
            throw new UnsupportedOperationException("Not supported");
        }
        public int getAtomic(int index) {
            throw new UnsupportedOperationException("Not supported");
        }
        public void assign(BitsArray other) {
            throw new UnsupportedOperationException("Not supported");
        }
        public int getMaxValue() {
            throw new UnsupportedOperationException("Not supported");
        }
    };

    public static interface BitsArrayGenerator {
        BitsArray create(int length, int maxValue);
    }
}
