package net.sf.summa.core.actor;

import java.util.concurrent.*;
import java.util.*;

/**
 * FIXME: Missing class docs for net.sf.summa.core.actor.SimpleScheduler
 *
 * @author mke
 * @since Aug 28, 2009
 */
public class SimpleScheduler extends Scheduler {

    /**
     *
     */
    static class SimpleChannel extends Channel {

        private Queue<Actor> actors;
        private SimpleScheduler scheduler;

        SimpleChannel(SimpleScheduler sched) {
            actors = new ConcurrentLinkedQueue<Actor>();
            scheduler = sched;

        }

        public void add (Actor actor) {
            actors.add(actor);
        }

        public Iterator<Actor> iterator () {
            return actors.iterator();
        }

        public void send (Actor sender, Object message) {
            scheduler.registerMessage(new Message(this, sender, message));
        }

        public boolean remove(Actor actor) {
            return actors.remove(actor);
        }
    }

    static class Message {
        public Channel channel;
        public Actor sender;
        public Object message;

        public Message(Channel chan, Actor a, Object msg) {
            channel = chan;
            sender = a;
            message = msg;
        }

        @Override
        public int hashCode() {
            return channel.hashCode() ^
                   (sender != null ? sender.hashCode() : 0) ^
                   (message != null ? message.hashCode() : 0);
        }
    }

    static abstract class Task implements Runnable {

        public Message message;
        protected SimpleScheduler scheduler;

        public Task(SimpleScheduler sched, Message msg) {
            message = msg;
            scheduler = sched;
        }
    }

    static class BroadcastTask extends Task {

        public BroadcastTask(SimpleScheduler sched, Message msg) {
            super(sched, msg);
        }

        public void run() {
            scheduler.broadcast(message.channel,
                                message.sender,
                                message.message);
        }
    }

    static class TimeoutTask extends Task {

        public TimeoutTask(SimpleScheduler sched, Message msg) {
            super(sched, msg);
        }

        public void run() {
            scheduler.timeout(message.channel,
                              message.sender,
                              message.message);
        }
    }

    private ScheduledExecutorService sched;
    private Map<Message,Future> timeouts;
    private Queue<Actor> pendingShutdown;
    private List<Channel> channels;

    public SimpleScheduler(int threadPoolSize) {
        //messages = ConcurrentLinkedQueue<Message>();
        sched = new ScheduledThreadPoolExecutor(threadPoolSize);
        timeouts = new HashMap<Message,Future>();
        pendingShutdown = new ConcurrentLinkedQueue<Actor>();
        channels = Collections.synchronizedList(new ArrayList<Channel>());
    }

    public Channel newChannel() {
        Channel chan = new SimpleChannel(this);
        channels.add(chan);
        return chan;
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
        if (task != null) task.cancel(false);
    }

    public void broadcast(Channel chan, Actor sender, Object msg) {
        boolean isCoreMessage = isCoreMessage(sender, msg);

        for (Actor test : chan) {
            // Don't send messages to the sender itself
            if (test == sender) continue;

            Select select = test.getSelect();

            if (select != null) {
                if (select.getTimeout() != 0) {
                    removeTimeout(new Message(chan, test, msg));
                }

                if (select.accepts(msg) || isCoreMessage) {
                    runActor(chan, test, select.getContinuation(), msg);
                    continue;
                }
            }
        }

        for (Actor actor : pendingShutdown) {
            chan.remove(actor);
        }
    }

    private boolean isCoreMessage(Actor sender, Object msg) {
        return sender == null && msg == Actor.Shutdown;
    }

    private void runActor(Channel chan, Actor actor,
                          Object continuation, Object msg){
        Select result = actor.act(chan, continuation, msg);

        if (result == Select.shutdown()) {
            actor.setShutdown();
            pendingShutdown.offer(actor);
            return;
        } else if (result == null) {
            result = Select.ignore();
        }

        actor.setSelect(result);

        if (result != null && result.getTimeout() != 0) {
            registerTimeout(new Message(chan, actor, msg));
        }
    }

    @Override
    public void start() {
        for (Channel chan : channels) {
            registerMessage(new Message(chan, null, Actor.Start));
        }
    }

    @Override
    public void shutdown() {
        for (Channel chan : channels) {
            registerMessage(new Message(chan, null, Actor.Shutdown));
        }
    }

    @Override
    public boolean awaitShutdown(long timeoutMillis) {
        long start = System.currentTimeMillis();
        long elapsed = System.currentTimeMillis() - start;
        boolean isShutdown = true;

        while (elapsed < timeoutMillis) {
            isShutdown = true;

            for (Channel chan : channels) {
                for (Actor actor : chan) {
                    if (!actor.isShutdown()) {
                        isShutdown = false;
                        break;
                    }
                }

                if (!isShutdown) break;
            }

            if (isShutdown) return true;

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }

            elapsed = System.currentTimeMillis() - start;
        }

        return false;
    }

    void timeout(Channel chan, Actor actor, Object msg) {
        Object continuation = actor.getSelect().getContinuation();

        Select result = actor.act(chan, continuation, Actor.Timeout);

        if (result == Select.shutdown()) {
            actor.setShutdown();
            chan.remove(actor);
            return;
        }

        actor.setSelect(result);

        if (result != null && result.getTimeout() != 0) {
            registerTimeout(new Message(chan, actor, msg));
        }
    }

}
