package net.sf.summa.core;

import sun.misc.Service;

import javax.script.ScriptEngineManager;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 *
 */
public abstract class ServiceTemplate<E> extends Template<E> implements Provider {

    private String serviceNick;
    private Class serviceInterface;

    /** Used to map nick to implementation class */
    private static final Map<String,Map<String,ServiceTemplate>> serviceNicks;

    static {
        serviceNicks = new HashMap<String,Map<String,ServiceTemplate>>();
        synchronized (serviceNicks) {
            System.out.println("Loading providers");
            Iterator providers = Service.providers(ServiceTemplate.class);
            while (providers.hasNext()) {
                // Instantiation of the service template
                // will have the desired side effects
                Object provider = providers.next();
                System.out.println(
                        "Loaded provider " + provider.getClass().getName());
            }
        }
    }




    protected ServiceTemplate(Class<E> serviceInterface,
                              String serviceNick,
                              String instanceNick) {
        super(serviceInterface, instanceNick);
        this.serviceInterface = serviceInterface;
        this.serviceNick = serviceNick;        

        // Load registered service implementations for this class
        // if we haven't done so already
        synchronized (ServiceTemplate.class) {
            if (!serviceNicks.containsKey(serviceNick)) {
                serviceNicks.put(
                        serviceNick, new HashMap<String,ServiceTemplate>());
                initServiceInterface();
            }

            Map<String,ServiceTemplate> templates = serviceNicks.get(serviceNick);
            templates.put(instanceNick, this);
        }
    }

    private void initServiceInterface() {
        Iterator providers = Service.providers(serviceInterface);
        Map<String,ServiceTemplate> templates = serviceNicks.get(serviceNick);

        while (providers.hasNext()) {
            ServiceTemplate fact = (ServiceTemplate)providers.next();
            templates.put(fact.getNick(), fact);
        }
    }

    public static Iterable<String> getServiceNicks() {
        synchronized (serviceNicks) {
            return serviceNicks.keySet();
        }
    }

    /**
     * Returns an unmodifiable map of instance nicks to ServiceTemplates or
     * {@code null} if there is no service registered with nickname
     * {@code nick}.
     *
     * @param serviceNick
     * @return
     */
    public static Map<String,ServiceTemplate> getServiceTemplates(
                                                          String serviceNick) {
        synchronized (serviceNicks) {
            Map<String,ServiceTemplate> service = serviceNicks.get(serviceNick);
            if (service != null) {
                return Collections.unmodifiableMap(service);
            }
            return null;
        }
    }
    
}
