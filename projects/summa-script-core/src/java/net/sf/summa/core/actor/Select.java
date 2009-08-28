package net.sf.summa.core.actor;

import static java.lang.System.nanoTime;

/**
 *
 */
public abstract class Select {

    private static final Select shutDown = new Select(0, 0, null) {
        public boolean accepts(Object message) {
            return false;
        }
    };

    private static final Select ignore = new Select(0, 0, null) {
        public boolean accepts(Object message) {
            return false;
        }
    };

    protected long timeout;
    protected long selectionTime;
    protected Object continuation;

    protected Select(long timeout, long selectionTime, Object continuation) {
        this.timeout = timeout;
        this.selectionTime = selectionTime;
        this.continuation = continuation;
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isTimedOut() {
        return nanoTime() - selectionTime > timeout;
    }

    public long getSelectionTime() {
        return selectionTime;
    }

    public Object getContinuation() {
        return continuation;
    }

    public Object setContinuation(Object continuation) {
        this.continuation = continuation;
        return continuation;
    }

    public abstract boolean accepts(Object message);

    public static Select message(long timeout,
                                 Object continuation,
                                 final Class... classes) {
        return new Select(timeout, nanoTime(), continuation) {
            /*
             * Accepts any message of the listed classes. Message checking is
             * done in a for-loop since we can expect only a few valid classes,
             * hence direct iteration is faster than, say, a HashSet
             */
            public boolean accepts (Object message) {
                if (message == null) return false;
                
                for (Class allowed : classes) {
                    if (allowed.isAssignableFrom(message.getClass())) {
                        return true;
                    }
                }
                return false;
            }

            public String toString() {
                return "Select.message";
            }
        };
    }    

    // Use Select.any(Object) to accept anything
    public static Select message(final Class... classes) {
        return Select.message(Long.MAX_VALUE, null, classes);
    }

    public static Select any(long timeout,
                             Object continuation) {
        return new Select(timeout, nanoTime(), continuation) {
            public boolean accepts (Object message) {
                return message != null;
            }

            public String toString() {
                return "Select.any";
            }
        };
    }

    public static Select any(long timeout) {
        return Select.any(timeout, null);
    }

    public static Select any(Object continuation) {
        return Select.any(Long.MAX_VALUE, continuation);
    }

    public static Select any() {
        return Select.any(Long.MAX_VALUE, null);
    }

    public static Select defer(long timeout,
                               Object continuation) {
        return new Select(timeout, nanoTime(), continuation) {
            public boolean accepts (Object message) {
                return false;
            }

            public String toString() {
                return "Select.defer";
            }
        };
    }

    public static Select defer(long timeout) {
        return Select.defer(timeout, null);
    }

    public static Select shutdown() {
        return Select.shutDown;
    }

    public static Select ignore() {
        return Select.ignore;
    }
}
