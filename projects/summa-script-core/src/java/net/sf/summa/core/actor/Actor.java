package net.sf.summa.core.actor;

/**
 *
 */
public abstract class Actor {

    private Select select;

    public static final Object Shutdown = new Object();
    public static final Object Timeout = new Object();

    public abstract Select act(Channel chan,
                               Object continuation,
                               Object message);

    void setSelect(Select s) {
        select = s;
    }

    Select getSelect() {
        return select;
    }
}
