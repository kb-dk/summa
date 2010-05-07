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

/**
 * Helper class to schedule a call to {@link Thread#interrupt} on a given
 * thread at some later point in time.
 */
public class ThreadInterrupt {

    /**
     * Schedule an interrupt on {@code t} after {@code delay} milli seconds
     * @param t the thread to interrupt
     * @param delay number of milli seconds to delay before interrupting
     */
    public ThreadInterrupt (final Thread t, final long delay) {
        Thread waiter = new Thread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }
                t.interrupt();
            }
        });

        waiter.start();
    }
}

