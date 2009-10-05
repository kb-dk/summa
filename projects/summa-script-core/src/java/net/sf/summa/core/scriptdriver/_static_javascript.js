/*
 * This file contains the static JS code that is loaded into the
 * global scope of each Javascript ScriptEngine created by the
 * JavaScript ScriptDriver implementation
 */

function _object_to_map (obj) {
    /*if (java.util.Map.isAssignableFrom(obj.getClass())) {
        print ("All good!\n");
        return;
    }*/

    map = java.util.HashMap();
    for (prop in obj) {
        map.put(prop, obj[prop]);
    }
    return map;
}

function create (template) {
    return Packages.net.sf.summa.core.Template.create(_object_to_map(template));
}