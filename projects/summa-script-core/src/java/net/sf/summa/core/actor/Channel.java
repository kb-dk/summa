package net.sf.summa.core.actor;

import java.util.Iterator;

/**
 *
 */
public abstract class Channel implements Iterable<Actor>{

    public abstract void add(Actor actor);

    public abstract Iterator iterator(Actor actor);

    public abstract void send (Actor sender, Object message);
}
