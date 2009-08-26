package net.sf.summa.core;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import static java.util.AbstractMap.SimpleImmutableEntry;

import static net.sf.summa.core.Property.*;

/**
 * FIXME: Missing class docs for net.sf.summa.core.ConfigTemplate
 *
 * @author mke
 * @since Aug 7, 2009
 */
public class Template<E> implements Map<String,Object> {

    private static class Prop {
        Object value;
        Object defaultValue;
        String fieldName;
        Class type;
        Access access;
        boolean mandatory;
        boolean allowNull;
    }

    protected Class<E> templateClass;
    protected String nick;
    private Map<String, Prop> props;

    Template(Class<E> cls) {
        if (cls == null) {
            throw new NullPointerException("Can not create config template " +
                                           "for class: null");
        }

        props = new HashMap<String, Prop>();
        templateClass = cls;
        parseTemplate();
        nick = cls.getName();
    }

    Template(Class<E> cls, String nick) {
        this(cls);
        this.nick = nick;
    }

    public static <K> Template<K> forClass(Class<K> cls) {
        return new Template<K>(cls);
    }

    public static <K> Template<K> forClass(Class<K> cls, String nick) {
        return new Template<K>(cls, nick);        
    }

    public Class<E> getTemplateClass() {
        return templateClass;
    }

    public String getNick() {
        return nick;
    }

    public E create() {
        try {
            Object inst = templateClass.newInstance();
            for (Prop p : props.values()) {
                // For private fields we do a quick twiddling of the
                // accessibility flag to grant our selves access
                Field f = templateClass.getDeclaredField(p.fieldName);
                boolean fieldAccessible = f.isAccessible();

                if (!fieldAccessible) {
                    f.setAccessible(true);
                }

                f.set(inst, p.value);

                if (!fieldAccessible) {
                    f.setAccessible(false);
                }

            }
            return (E)inst;
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
        for (Field field : templateClass.getDeclaredFields()) {
            Property a = field.getAnnotation(Property.class);

            if (a == null) {
                continue;
            }

            Prop p = new Prop();
            p.defaultValue = a.value();
            p.fieldName = field.getName();
            p.type = a.type();
            p.access = a.access();
            p.mandatory = a.mandatory();
            p.allowNull = a.allowNull();

            // See if we must convert the default value to the correct type,
            // note that assigned values are checked on assignment time
            if (p.defaultValue instanceof String && p.type != String.class) {
                // We extract the static 'valueOf(String)' method on the class
                // of the p.defaultValue member, and use that for conversion
                try {
                    Method convert = p.type.getMethod("valueOf", String.class);
                    p.defaultValue = convert.invoke(null, p.defaultValue);
                } catch (NoSuchMethodException e) {
                    throw new InvalidPropertyDeclaration(
                                 a.name() + "=" + p.defaultValue
                                 + " has no conversion method "
                                 + p.type.getSimpleName() + ".valueOf(String)");
                } catch (InvocationTargetException e) {
                    throw new InvalidPropertyDeclaration(
                                                 a.name() + "=" + p.defaultValue
                                                 + " can not be converted to a "
                                                 + p.type.getSimpleName());
                } catch (IllegalAccessException e) {
                    throw new InvalidPropertyDeclaration(
                                 a.name() + "=" + p.defaultValue
                                 + " has non-accessible conversion method "
                                 + p.type.getSimpleName() + ".valueOf(String)");
                }

            }

            p.value = p.defaultValue;
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
