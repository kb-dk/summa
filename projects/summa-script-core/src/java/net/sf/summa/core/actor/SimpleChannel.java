package net.sf.summa.core.actor;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
class SimpleChannel extends Channel {

    private static class Message {
        public Actor sender;
        public Object message;

        public Message(Actor a, Object msg) {
            sender = a;
            message = msg;
        }
    }

    private List<Actor> actors;
    private Queue<Message> messages;

    SimpleChannel() {
        actors = new LinkedList<Actor>();
        messages = new ConcurrentLinkedQueue<Message>();
    }

    public void add (Actor actor) {
        actors.add(actor);
    }

    public Iterator<Actor> iterator () {
        return actors.iterator();
    }

    public void send (Actor sender, Object message) {
        messages.add(new Message(sender, message));
    }
}
