package net.sf.summa.core;

import sun.misc.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 *
 */
public abstract class ServiceTemplate extends Template {

    private String serviceNick;
    private Class serviceInterface;

    /** Used to map nick to implementation class */
    private static Map<String,Map<String,ServiceTemplate>> serviceNicks
                            = new HashMap<String,Map<String,ServiceTemplate>>();

    protected ServiceTemplate(Class serviceInterface,
                              String serviceNick,
                              String instanceNick) {
        super(serviceInterface, instanceNick);
        this.serviceInterface = serviceInterface;
        this.serviceNick = serviceNick;

        synchronized (ServiceTemplate.class) {
            if (!serviceNicks.containsKey(serviceNick)) {
                serviceNicks.put(serviceNick, new HashMap<String,ServiceTemplate>());
                initServiceInterface();
            }

            Map<String,ServiceTemplate> templates = serviceNicks.get(serviceNick);
            templates.put(instanceNick, this);
        }
    }

    private void initServiceInterface() {
        Iterator filters = Service.providers(serviceInterface);
        Map<String,ServiceTemplate> templates = serviceNicks.get(serviceNick);

        while (filters.hasNext()) {
            ServiceTemplate fact = (ServiceTemplate)filters.next();
            templates.put(fact.getNick(), fact);
        }
    }

    public static Iterable<String> getServiceNicks() {
        return serviceNicks.keySet();
    }

    /**
     * Returns an unmodifiable map of instance nicks to ServiceTemplates
     * @param serviceNick
     * @return
     */
    public static Map<String,ServiceTemplate> getServiceTemplates(
                                                          String serviceNick) {
        return Collections.unmodifiableMap(serviceNicks.get(serviceNick));
    }
    
}
