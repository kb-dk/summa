package net.sf.summa.core.actor;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * FIXME: Missing class docs for net.sf.summa.core.actor.SimpleSchedulerTest
 *
 * @author mke
 * @since Aug 28, 2009
 */
public class SimpleSchedulerTest {

    static class StringReceiver extends Actor {

        public int count = 0;
        public String lastSeen = null;

        public StringReceiver() {
            super(Select.message(String.class));
        }

        public synchronized Select act(Channel chan,
                                       Object continuation, Object message) {
            assertNull(continuation);
            assertNotNull(chan);
            Class messageClass = message.getClass();

            if (message == Actor.Timeout || message == Actor.Shutdown) {
                return Select.shutdown();
            } else if (messageClass == String.class) {
                count++;
                lastSeen = message.toString();
                return Select.message(1000, String.class);
            }

            fail("Unexpected message type " + message.getClass().getName());
            return null;
        }
    }

    static class StringSender extends Actor {

        int count = 0;

        public StringSender(int count) {
            super(Select.any());
            this.count = count;
        }

        public Select act(Channel chan, Object continuation, Object message) {
            assertNull(continuation);
            assertNotNull(chan);

            if (message == Actor.Start) {
                for (int i = 0; i < count; i++) {
                    chan.send(this, "" + i);
                }
            } else if (message == Actor.Shutdown) {
                return Select.shutdown();
            }

            fail("Control should never reach here");
            return Select.shutdown();
        }
    }

    @Test
    public void synchronousOneWayMessage() {
        Scheduler sched = new SimpleScheduler(10);
        StringSender sender = new StringSender(10);
        StringReceiver receiver = new StringReceiver();
        sched.newChannel(sender, receiver);

        sched.start();
        sched.shutdown();
        boolean completed = sched.awaitShutdown(1000);

        assertTrue(completed, "Scheduler timed out, expected clean shutdown");

        assertEquals(receiver.count, 10);
        assertEquals(receiver.lastSeen, "9");
    }
}
