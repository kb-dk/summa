package net.sf.summa.core.actor;

import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;

/**
 * FIXME: Missing class docs for net.sf.summa.core.actor.SimpleScheduler
 *
 * @author mke
 * @since Aug 28, 2009
 */
public class SimpleScheduler extends Scheduler {

    static class Message {
        public Channel channel;
        public Actor sender;
        public Object message;

        public Message(Channel chan, Actor a, Object msg) {
            sender = a;
            message = msg;
        }

        @Override
        public int hashCode() {
            return channel.hashCode() ^ sender.hashCode() ^ message.hashCode();
        }
    }

    static class BroadcastTask implements Runnable {

        public Message message;
        private SimpleScheduler scheduler;

        public BroadcastTask(SimpleScheduler sched, Message msg) {
            message = msg;
            scheduler = sched;
        }

        public void run() {
            Select select = message.sender.getSelect();

            scheduler.broadcast(message.channel,
                                message.sender,
                                message.message);
        }
    }

    static class TimeoutTask implements Runnable {
        public Message message;
        private SimpleScheduler scheduler;

        public TimeoutTask(SimpleScheduler sched, Message msg) {
            message = msg;
            scheduler = sched;
        }

        public void run() {
            scheduler.timeout(message.channel,
                              message.sender,
                              message.message);
        }
    }

    private ScheduledExecutorService sched;
    private Map<Message,Future> timeouts;

    public SimpleScheduler(int threadPoolSize) {
        //messages = ConcurrentLinkedQueue<Message>();
        sched = new ScheduledThreadPoolExecutor(threadPoolSize);
        timeouts = new HashMap<Message,Future>();
    }

    public Channel newChannel() {
        return new SimpleChannel(this);
    }

    void registerMessage(Message msg) {
        sched.submit(new BroadcastTask(this, msg));
    }

    void registerTimeout(Message msg) {
        Select select = msg.sender.getSelect();

        if (select == null || select.getTimeout() == 0) {
            return;
        }

        Future task = sched.schedule(new TimeoutTask(this, msg),
                                     select.getTimeout(), TimeUnit.NANOSECONDS);
        timeouts.put(msg, task);
    }

    void removeTimeout(Message msg) {
        Future task = timeouts.get(msg);
        task.cancel(false);
    }

    public void broadcast(Channel chan, Actor actor, Object msg) {
        for (Actor test : chan) {
            Select select = test.getSelect();

            if (select != null) {
                if (select.getTimeout() != 0) {
                    removeTimeout(new Message(chan, actor, msg));
                }

                if (select.accepts(msg)) {
                    Select result = test.act(chan, select.getContinuation(), msg);
                    test.setSelect(result);

                    if (result != null && result.getTimeout() != 0) {
                        registerTimeout(new Message(chan, actor, msg));
                    }
                }
            }
        }
    }

    void timeout(Channel chan, Actor actor, Object msg) {
        Object continuation = actor.getSelect() != null ?
                                   actor.getSelect().getContinuation() : null;

        Select result = actor.act(chan, continuation, msg);
        actor.setSelect(result);

        if (result != null && result.getTimeout() != 0) {
            registerTimeout(new Message(chan, actor, msg));
        }
    }
}
