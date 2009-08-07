package net.sf.summa.core;

import java.util.*;
import java.lang.reflect.Field;
import static java.util.AbstractMap.SimpleImmutableEntry;

import static net.sf.summa.core.Property.Access;

/**
 * FIXME: Missing class docs for net.sf.summa.core.ConfigTemplate
 *
 * @author mke
 * @since Aug 7, 2009
 */
public class Template extends Config {

    private static class Prop {
        Object value;
        Object defaultValue;
        String fieldName;
        Class type;
        Access access;
        boolean mandatory;
        boolean allowNull;
    }

    private Class templateClass;
    private Map<String, Prop> props;
    private String nick;

    public Template(Class cls) {
        if (cls == null) {
            throw new NullPointerException("Can not create config template " +
                                           "for class: null");
        }

        templateClass = cls;
        parseTemplate();
        props = new HashMap<String, Prop>();
        nick = null;
    }

    public Template(Class cls, String nick) {
        this(cls);
        this.nick = nick;
    }

    public void setTemplateClass(Class cls) {
        templateClass = cls;
        parseTemplate();
    }

    public Class getTemplateClass() {
        return templateClass;
    }

    public String getNick() {
        return nick;
    }

    public Object create() {
        try {
            Object inst = templateClass.newInstance();
            for (Prop p : props.values()) {
                // FIXME: Private members
                Field f = templateClass.getField(p.fieldName);
                f.set(inst, p.value);
            }
            return inst;
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchFieldException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //FIXME: BAD BAD BAD!
        return null;
    }

    private void parseTemplate() {
        props.clear();
        for (Field field : templateClass.getFields()) {
            Property a = field.getAnnotation(Property.class);

            if (a == null) {
                continue;
            }

            Prop p = new Prop();
            p.value = a.value();
            p.defaultValue = a.value();
            p.fieldName = field.getName();
            p.type = a.type();
            p.access = a.access();
            p.mandatory = a.mandatory();
            p.allowNull = a.allowNull();

            props.put(a.name(), p);
        }
    }

    public boolean admits(String key, Object value) {
        Prop p = props.get(key);
        return p != null && p.type.isInstance(value);

    }

    public int size() {
        return props.size();
    }

    public boolean isEmpty() {
        return props.isEmpty();
    }

    public boolean containsKey(Object key) {
        return props.containsKey(key);
    }

    public boolean containsValue(Object value) {
        for (Prop p : props.values()) {
            if (value == null) {
                if (p.value == null) {
                    return true;
                } else {
                    continue;
                }
            } else if (value.equals(p.value)){
                return true;
            }
        }
        return false;
    }

    public Object get(Object key) {
        Prop p = props.get(key);
        return p == null ? null : p.value;
    }

    public Object put(String key, Object value) {
        if (!admits(key,value)) {
            throw new IllegalArgumentException("Assignment '" + key + "' = "
                                               + value
                                               + ", not valid within template");
        }

        // Store the value and returns the previous value
        // as per the Map interface contract
        Prop p = props.get(key);
        Object prev = p.value;
        p.value = value;
        return prev;
    }

    public Object remove(Object key) {
        Prop p = props.get(key.toString());

        if (p == null) {
            return null;
        }

        // Reset the value and return the old one as per Map contract
        Object prev = p.value;
        p.value = p.defaultValue;
        return prev;
    }

    public void putAll(Map<? extends String, ?> m) {
        for (Entry<? extends String,?> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        for (Prop p : props.values()) {
            p.value = p.defaultValue;
        }
    }

    public Set<String> keySet() {
        return props.keySet();
    }

    public Collection<Object> values() {
        ArrayList<Object> l = new ArrayList<Object>(props.size());
        for (Prop p : props.values()) {
            l.add(p.value);
        }
        return l;
    }

    public Set<Entry<String,Object>> entrySet() {
        Set<Entry<String,Object>> entries =
                              new HashSet<Entry<String,Object>>(props.size());

        for (Entry<String,Prop> e : props.entrySet()) {
            entries.add(
                  new SimpleImmutableEntry<String,Object>(e.getKey(),
                                                          e.getValue().value));
        }
        return entries;
    }


}
