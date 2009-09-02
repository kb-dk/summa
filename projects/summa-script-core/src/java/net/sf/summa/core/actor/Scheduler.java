package net.sf.summa.core.actor;

/**
 *
 */
public abstract class Scheduler {

    public abstract Channel newChannel(Actor... actors);

    public abstract void broadcast(Channel chan, Actor actor, Object msg);

    public abstract void start();

    public abstract void shutdown();

    public abstract boolean awaitShutdown(long timeoutMillis);
}
