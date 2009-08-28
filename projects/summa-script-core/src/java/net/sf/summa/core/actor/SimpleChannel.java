package net.sf.summa.core.actor;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import static net.sf.summa.core.actor.SimpleScheduler.*;

/**
 *
 */
class SimpleChannel extends Channel {

    private List<Actor> actors;
    private SimpleScheduler scheduler;

    SimpleChannel(SimpleScheduler sched) {
        actors = new LinkedList<Actor>();
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
}
