package net.sf.summa.core.examples;

import net.sf.summa.core.ServiceTemplate;

import java.util.Map;
import java.util.Iterator;

import sun.misc.Service;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngineFactory;

/**
 * FIXME: Missing class docs for net.sf.summa.core.examples.Main
 *
 * @author mke
 * @since Sep 11, 2009
 */
public class Main {

    public static void main(String[] args) {
        Iterator pingables = Service.providers(ServiceTemplate.class);
        /*System.out.println("Service Templates:");
        while (pingables.hasNext()) {
            System.out.println("\t* "
                               + pingables.next().getClass().getName());
        }*/

        if (args.length == 0) {
            printServices();
        } else if (args.length == 1) {
            printInstances(args[0]);
        } else if (args.length == 2) {
            runServiceInstance(args[0], args[1]);
        }
    }

    private static void printServices() {
        System.out.println("USAGE:\n\tMain [serviceNick] [instanceNick]\n");

        boolean noProviders = true;

        System.out.println("Known services:");
        for (String serviceNick : ServiceTemplate.getServiceNicks()) {
            noProviders = false;
            System.out.println(" * " + serviceNick);
        }

        if (noProviders) {
            System.out.println("No providers registered");
        }

        System.exit(0);
    }

    private static void printInstances(String serviceNick) {
        Map<String,ServiceTemplate> services =
                ServiceTemplate.getServiceTemplates(serviceNick);
        if (services != null) {
            System.out.println(
                    "Known service instances for '" + serviceNick + "':");
            for (String instanceNick : services.keySet()) {
                System.out.println(
                        " * " + instanceNick + " : "
                        + services.get(instanceNick).getClass().getName());
            }
        } else {
            System.out.println(
                    "No service instances registered for '" + serviceNick+ "'");
        }
        System.exit(0);
    }

    private static void runServiceInstance(
                                      String serviceNick, String instanceNick) {
        Map<String,ServiceTemplate> services =
                ServiceTemplate.getServiceTemplates(serviceNick);

        if (services == null) {
            System.err.println(
                    "No service instances registered for '" + serviceNick +"'");
            System.exit(1);
        }

        ServiceTemplate instance = services.get(instanceNick);
        if (instance == null) {
            System.err.println(
                    "No such instance '" + instanceNick
                    + "' registered for service '" + serviceNick + "'");
            return;
        }

        Pingable o = (Pingable)instance.create();
        System.out.println("Created " + o.getClass().getName() + " instance");
        System.out.println(o.ping("test"));

    }
}
