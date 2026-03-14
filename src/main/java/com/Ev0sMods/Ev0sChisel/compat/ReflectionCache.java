package com.Ev0sMods.Ev0sChisel.compat;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small utility to cache reflective Field lookups and avoid repeated setAccessible calls.
 */
public final class ReflectionCache {
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private ReflectionCache() {}

    public static void setField(Class<?> clazz, Object target, String fieldName, Object value) throws Exception {
        String key = clazz.getName() + "#" + fieldName;
        Field f = FIELD_CACHE.get(key);
        if (f == null) {
            Field nf = clazz.getDeclaredField(fieldName);
            nf.setAccessible(true);
            Field prev = FIELD_CACHE.putIfAbsent(key, nf);
            f = (prev != null) ? prev : nf;
        }
        f.set(target, value);
    }

    public static Field getField(Class<?> clazz, String fieldName) throws Exception {
        String key = clazz.getName() + "#" + fieldName;
        Field f = FIELD_CACHE.get(key);
        if (f == null) {
            Field nf = clazz.getDeclaredField(fieldName);
            nf.setAccessible(true);
            Field prev = FIELD_CACHE.putIfAbsent(key, nf);
            f = (prev != null) ? prev : nf;
        }
        return f;
    }
}
