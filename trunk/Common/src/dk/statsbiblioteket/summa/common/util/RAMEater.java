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
package dk.statsbiblioteket.summa.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Allocates RAM and fills it with garbage until an OutOfMemoryError occurs.
 * Normally used as a stand-alone program for clearing the disc-cache.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class RAMEater {

    /**
     * Allocated memory in chunks of 1 MB and fills it with garbage. This goes
     * on until an OutOfMemoryError is thrown, at which point a clean exit is
     * performed.
     * @param args all arguments are ignored.
     */
    public static void main(String[] args) {
        boolean resident = false;
        int maxMB = Integer.MAX_VALUE;
        int updates = 100;
        int sleepTime = 20;
        int pos = 0;
        while (pos < args.length) {
            if ("-h".equals(args[pos])) {
                System.out.println(
                        "Usage: RAMEater [-r [-u updates] [s ms]] [-m MB]");
                System.out.println(
                        "-r: resident. Do not exit and perform periodical, "
                        + "random updates to memory to keep it from being "
                        + "swapped\n"
                        + "-u: random updates to perform periodically when "
                        + "resident (default " + updates + ")\n"
                        + "-s: delay between each update-run (default "
                        + sleepTime + " ms)\n"
                        + "-m: max MB to allocate");
                return;
            }
            if ("-r".equals(args[pos])) {
                resident = true;
                pos++;
                continue;
            }
            if ("-m".equals(args[pos])) {
                pos++;
                maxMB = Integer.parseInt(args[pos]);
                pos++;
                continue;
            }
            if ("-u".equals(args[pos])) {
                pos++;
                updates = Integer.parseInt(args[pos]);
                pos++;
                continue;
            }
            if ("-s".equals(args[pos])) {
                pos++;
                sleepTime = Integer.parseInt(args[pos]);
                pos++;
            }
        }
        if (maxMB == Integer.MAX_VALUE) {
            System.out.println("Allocating RAM until OutOfMemory");
        } else {
            System.out.println("Allocating a maximum of " + maxMB + " MB");
        }
        int BLOCK_SIZE = 1024*1024;
        List<byte[]> bytes = new ArrayList<byte[]>(1000);
        try {
            //noinspection InfiniteLoopStatement
            for (int mb = 0 ; mb < maxMB ; mb++) {
                byte[] newBytes = new byte[BLOCK_SIZE];
                for (int i = 0 ; i < BLOCK_SIZE ;i++) {
                    // We don't care about the content, but we put in something
                    // to prevent any fancy sparse memory allocations.
                    newBytes[i] = (byte)i;
                }
                bytes.add(newBytes);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Allocated "
                               + (long)bytes.size() * BLOCK_SIZE / 1024 / 1024
                               + "MB before OutOfMemoryError was thrown");
        }
        if (!resident) {
            return;
        }
        System.out.println(String.format("Allocated %d MB",
                                         (long)bytes.size() * BLOCK_SIZE 
                                         / 1048576));
        System.out.println(String.format(
                "Entering random updates mode: Updating %d random bytes each "
                + "%d ms", updates, sleepTime));
        Random random = new Random();
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted while sleeping in"
                                                + " resident-mode", e);
            }
            for (int i = 0 ; i < updates; i++) {
                bytes.get(random.nextInt(bytes.size()))
                        [random.nextInt(BLOCK_SIZE)] =
                        (byte)(random.nextInt(255) & 0xFF);
            }
        }
    }
}




