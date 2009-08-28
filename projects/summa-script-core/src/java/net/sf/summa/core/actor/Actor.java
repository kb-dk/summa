package net.sf.summa.core.actor;

/**
 *
 */
public abstract class Actor {

    private Select select;
    private boolean shutdown = false;

    public static final Object Shutdown = new Object() {
        public String toString() { return "Actor.Shutdown"; }
    };
    public static final Object Timeout = new Object() {
        public String toString() { return "Actor.Timeout"; }
    };
    public static final Object Start = new Object(){
        public String toString() { return "Actor.Start"; }
    };

    public Actor(Select initialSelect) {
        select = (initialSelect != null ? initialSelect : Select.ignore());
    }

    public Actor() {
        this(null);
    }

    public abstract Select act(Channel chan,
                               Object continuation,
                               Object message);

    void setSelect(Select s) {
        select = (s != null ? s : Select.ignore());
    }

    Select getSelect() {
        return select;
    }

    void setShutdown() {
        shutdown = true;
    }

    boolean isShutdown() {
        return shutdown;
    }
}
