package net.sf.summa.core.actor;

/**
 *
 */
public abstract class Select {

    public abstract boolean accepts(Object... message);

    public abstract long getTimeout();

    public abstract long getSelectionTime();

    public abstract Object getContinuation();

    public abstract Object setContinuation(Object continuation);
}
