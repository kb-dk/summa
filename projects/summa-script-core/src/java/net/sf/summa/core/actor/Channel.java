package net.sf.summa.core.actor;

import java.util.Iterator;

/**
 *
 */
public abstract class Channel implements Iterable<Actor>{

    public abstract void add(Actor actor);

    public abstract boolean remove(Actor actor);

    public abstract Iterator<Actor> iterator();

    public abstract void send (Actor sender, Object message);
}
