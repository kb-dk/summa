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
