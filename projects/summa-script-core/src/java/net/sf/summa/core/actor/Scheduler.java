package net.sf.summa.core.actor;

/**
 *
 */
public abstract class Scheduler {

    public abstract Channel newChannel();

    public abstract void broadcast(Channel chan, Actor actor, Object msg);
}
