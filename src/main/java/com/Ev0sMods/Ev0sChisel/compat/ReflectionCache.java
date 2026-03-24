package com.Ev0sMods.Ev0sChisel.compat;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Small utility to cache reflective Field lookups and avoid repeated setAccessible calls.
 */
public final class ReflectionCache {
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private ReflectionCache() {}

    public static void setField(Class<?> clazz, Object target, String fieldName, Object value) throws Exception {
        Field f = null;
        try {
            try {
                f = getField(clazz, fieldName);
            } catch (NoSuchFieldException e) {
                if (target != null) {
                    try {
                        f = getField(target.getClass(), fieldName);
                    } catch (NoSuchFieldException ignored) {
                        // Field does not exist on either the declared class or the runtime class.
                        // This can happen on prerelease when fields are renamed/removed (e.g. StateData.id).
                        // Store the value in the synthetic field store so we can preserve the data
                        // for later reads from plugin code.
                        SyntheticFieldStore.put(target, fieldName, value);
                        return;
                    }
                } else {
                    // No target to fall back to; nothing to set.
                    return;
                }
            }

            if (f != null) {
                f.set(target, value);
            }
        } catch (IllegalAccessException iae) {
            throw iae;
        }
    }

    /**
     * Read a field value reflectively. If the real field does not exist, fall back to
     * the synthetic field store.
     */
    public static Object getFieldValue(Class<?> clazz, Object target, String fieldName) throws Exception {
        try {
            Field f;
            try {
                f = getField(clazz, fieldName);
            } catch (NoSuchFieldException e) {
                if (target != null) {
                    try {
                        f = getField(target.getClass(), fieldName);
                    } catch (NoSuchFieldException ex) {
                        return SyntheticFieldStore.get(target, fieldName);
                    }
                } else {
                    return null;
                }
            }
            return f.get(target);
        } catch (IllegalAccessException iae) {
            throw iae;
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) throws Exception {
        String key = clazz.getName() + "#" + fieldName;
        Field f = FIELD_CACHE.get(key);
        if (f != null) return f;

        Class<?> search = clazz;
        while (search != null) {
            try {
                Field nf = search.getDeclaredField(fieldName);
                nf.setAccessible(true);
                Field prev = FIELD_CACHE.putIfAbsent(key, nf);
                return (prev != null) ? prev : nf;
            } catch (NoSuchFieldException ignored) {
                search = search.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
