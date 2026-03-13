package com.Ev0sMods.Ev0sChisel.compat;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Collection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight concurrent cache for BlockType lookups to avoid repeated
 * expensive calls to BlockType.fromString during compatibility discovery
 * and injection passes.
 */
public final class BlockTypeCache {
    // fastutil Object2ObjectOpenHashMap wrapped with Collections.synchronizedMap
    // to provide safe concurrent access with lower GC pressure than plain maps.
    private static final Object2ObjectOpenHashMap<String, BlockType> FAST_MAP = new Object2ObjectOpenHashMap<>();
    private static final Map<String, BlockType> CACHE = Collections.synchronizedMap(FAST_MAP);

    private BlockTypeCache() {}

    /**
     * Get the cached BlockType for the given key, probing the registry if absent.
     */
    public static BlockType get(String key) {
        if (key == null) return null;
        BlockType v = CACHE.get(key);
        if (v != null) return v;
        synchronized (CACHE) {
            // double-check inside synchronized wrapper
            v = CACHE.get(key);
            if (v != null) return v;
            try {
                v = BlockType.fromString(key);
            } catch (Throwable t) {
                v = null;
            }
            CACHE.put(key, v);
            return v;
        }
    }

    public static boolean exists(String key) {
        return get(key) != null;
    }

    public static void clear() { CACHE.clear(); }

    /**
     * Bulk-preload a collection of keys in parallel using N threads.
     * Returns the number of keys successfully loaded (non-null BlockType).
     */
    public static int preload(Collection<String> keys, int threads, long timeoutSeconds) {
        if (keys == null || keys.isEmpty()) return 0;
        ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, threads));
        final java.util.concurrent.atomic.AtomicInteger loaded = new java.util.concurrent.atomic.AtomicInteger(0);
        for (String k : keys) {
            ex.submit(() -> {
                try {
                    BlockType bt = get(k);
                    if (bt != null) loaded.incrementAndGet();
                } catch (Throwable ignored) { }
            });
        }
        ex.shutdown();
        try {
            ex.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        return loaded.get();
    }
}
