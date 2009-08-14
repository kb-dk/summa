package net.sf.summa.core;

import sun.misc.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 *
 */
public class FilterFactory extends Template {

    private String filterNick;
    private Class<Filter> filterClass;
    private Template template;

    /** Used to map nick to implementation class */
    private static Map<String,FilterFactory> filterNicks
                                   = new HashMap<String,FilterFactory>();

    /** Controls whether filters registered on the classpath are loaded yet */
    private static boolean initialized = false;

    public static Filter newFilter(String nick) {
        FilterFactory fact = filterNicks.get(nick);
        if (fact == null) {
            // FIXME: Better exception
            throw new RuntimeException("No filter class for nick: " + nick);
        }
        return (Filter)fact.create();
    }

    public static void register(FilterFactory fact) {
        init();
        validateFactory(fact);

        if (filterNicks.containsKey(fact.getNick())) {
            //FIXME: More accurate exception than RuntimeException please
            throw new RuntimeException("Duplicate filter nick: "
                                       + fact.getNick());
        }

        filterNicks.put(fact.getNick(), fact);
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        Iterator filters = Service.providers(FilterFactory.class);
        while (filters.hasNext()) {
            FilterFactory fact = (FilterFactory)filters.next();
            register(fact);
        }
    }

    private static void validateFactory(FilterFactory fact) {
        if (fact == null) {
            throw new NullPointerException("Invalid filter factory: null");
        }

        if (fact.getNick() == null) {
            throw new NullPointerException("Invalid filter factory nick: null");
        }

        if (fact.getTemplateClass() == null) {
            throw new NullPointerException("Invalid filter class: null");
        }
    }

    public FilterFactory(String nick, Class<Filter> filterClass) {
        super(filterClass, nick);
    }

}
