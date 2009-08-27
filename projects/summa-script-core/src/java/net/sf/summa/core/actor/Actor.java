package net.sf.summa.core.actor;

/**
 *
 */
public abstract class Actor {

    public abstract Select act(Channel chan, Select sel, Object... args);
}
