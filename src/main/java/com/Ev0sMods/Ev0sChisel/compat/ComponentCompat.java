package com.Ev0sMods.Ev0sChisel.compat;

import java.lang.reflect.Method;

/**
 * Minimal reflective helpers to interact with prerelease block-component APIs.
 * Tries several common method names and signatures; returns null if unavailable.
 */
public final class ComponentCompat {
    private ComponentCompat() {}

    public static Object getBlockComponent(Object chunk, int x, int y, int z, Class<?> compClass) {
        if (chunk == null) return null;
        Class<?> c = chunk.getClass();
        String[] names = new String[]{"getComponentAt","getBlockComponent","getBlockComponentAt","getComponent","getBlockData","getBlockStateAt"};
        for (String n : names) {
            try {
                Method m = c.getMethod(n, int.class, int.class, int.class, Class.class);
                Object o = m.invoke(chunk, x, y, z, compClass);
                if (o != null && compClass.isInstance(o)) return o;
            } catch (Throwable ignored) {}
        }
        for (String n : names) {
            try {
                Method m = c.getMethod(n, int.class, int.class, int.class);
                Object o = m.invoke(chunk, x, y, z);
                if (o != null && compClass.isInstance(o)) return o;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static void registerComponent(Class<?> compClass, String id, Object codec) {
        try {
            Class<?> reg = Class.forName("com.hypixel.hytale.component.ComponentRegistry");
            Method get = null;
            try { get = reg.getMethod("getInstance"); } catch (Throwable ignored) {}
            Object inst = null;
            if (get != null) inst = get.invoke(null);
            if (inst == null) {
                try { inst = reg.getField("INSTANCE").get(null); } catch (Throwable ignored) {}
            }
            if (inst != null) {
                for (Method m : reg.getMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getName().toLowerCase().contains("register")) {
                        try { m.invoke(inst, compClass, id, codec); return; } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
