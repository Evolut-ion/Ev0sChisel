package com.Ev0sMods.Ev0sChisel.compat;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores synthetic fields for objects when the real field doesn't exist on the class.
 * Uses a WeakHashMap for keys so entries are garbage-collected with the target objects.
 */
public final class SyntheticFieldStore {
    private static final WeakHashMap<Object, ConcurrentHashMap<String, Object>> STORE = new WeakHashMap<>();

    private SyntheticFieldStore() {}

    public static synchronized void put(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null) return;
        ConcurrentHashMap<String, Object> map = STORE.get(target);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            STORE.put(target, map);
        }
        map.put(fieldName, value);
    }

    public static synchronized Object get(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        Map<String, Object> map = STORE.get(target);
        if (map == null) return null;
        return map.get(fieldName);
    }
}
