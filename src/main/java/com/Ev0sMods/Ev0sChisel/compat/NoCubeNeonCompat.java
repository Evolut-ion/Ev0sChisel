package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Compat layer for NoCube Neon Blocks (Usermods under AppData). 
 *
 * Detects the NoCube mod directory in the user's Mods folder, attempts
 * to discover block keys, and injects a Paintbrush.Data state onto
 * discovered block types so the Paintbrush UI can operate on them.
 */
public final class NoCubeNeonCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static boolean detected = false;
    private static final List<String> VARIANTS = new ArrayList<>();

    private NoCubeNeonCompat() {}

    public static void init() {
        try {
            detected = true;
            buildVariantList();
        } catch (Throwable t) {
            detected = false;
            LOGGER.atWarning().log("[Paintbrush] Failed to init NoCube compat: " + t.getMessage());
        }
    }

    public static boolean isAvailable() { return detected; }

    public static List<String> getVariants() { return Collections.unmodifiableList(VARIANTS); }

    /**
     * Builds the variant list by probing the registry for all likely NoCube Neon block keys.
     * No runtime directory or file scans are performed.
     */
    private static final String[] BASE_COLORS = {
        "Red", "Blue", "Green", "Yellow", "White", "Black",
        "Orange", "Purple", "Pink", "Cyan", "Magenta",
        "Lime", "Brown", "Gray", "Beige", "Cream", "Ivory",
        "Violet", "Indigo", "Teal", "Maroon", "Navy", "Olive",
        "Aqua", "Rose", "Coral", "Peach", "Salmon", "Crimson",
        "Amber", "Gold", "Silver", "Tan", "Khaki"
    };

    private static final String[] ALL_COLORS;
    static {
        List<String> all = new ArrayList<>();
        Collections.addAll(all, BASE_COLORS);
        for (String c : BASE_COLORS) { all.add(c + "_Light"); all.add(c + "_Dark"); }
        for (String c : BASE_COLORS) { all.add("Light_" + c); all.add("Dark_" + c); }
        ALL_COLORS = all.toArray(new String[0]);
    }

    private static void buildVariantList() {
        VARIANTS.clear();
        String[] probes = {"NoCube_Neon_Block_", "NoCube_Neon_", "NoCube_Neon", "nocube_neon_", "nocube_neon"};
        for (String prefix : probes) {
            for (String color : ALL_COLORS) {
                String key = prefix + color;
                try {
                    if (BlockTypeCache.exists(key) && !VARIANTS.contains(key)) {
                        VARIANTS.add(key);
                    }
                } catch (Throwable ignored) { }
            }
        }
        // discovered NoCube Neon variants (info log removed)
    }

    /** Generate likely BlockType keys for a given mod and filename. */
    private static List<String> generateCandidates(String modName, String baseName) {
        List<String> out = new ArrayList<>();
        String normMod = modName.replaceAll("[^A-Za-z0-9_]", "_");
        String normBase = baseName.replaceAll("[^A-Za-z0-9_]", "_");
        // Add exact base name (matches block key)
        out.add(baseName);
        out.add(normBase);
        out.add(normMod + "_" + normBase);
        out.add(normMod + ":" + normBase);
        out.add("NoCube_" + normBase);
        out.add("NoCube_Neon_" + normBase);
        out.add("nocube_neon_" + normBase);
        // Add common block key pattern for NoCube Neon blocks
        out.add("NoCube_Neon_Block_" + normBase);
        return out;
    }

    /** Inject a Paintbrush.Data state onto discovered NoCube Neon BlockTypes. */
    public static void injectPaintbrushStates() {
        if (!detected || VARIANTS.isEmpty()) return;

        int injected = 0, failed = 0;
        // beginning injection onto discovered variants (info log removed)
        for (String key : VARIANTS) {
                try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) { LOGGER.atWarning().log("[Paintbrush] BlockType not found for key: " + key); failed++; continue; }

                StateData existing = bt.getState();
                if (existing instanceof Paintbrush.Data) {
                    continue; // already has paintbrush data
                }

                Paintbrush.Data data = new Paintbrush.Data();
                data.source = "NoCube_Neon";
                data.colorVariants = VARIANTS.toArray(new String[0]);

                setField(StateData.class, data, "id", "Ev0sPaintbrush");
                setField(BlockType.class, bt, "state", data);
                injected++;
            } catch (Throwable t) {
                LOGGER.atWarning().log("[Paintbrush] Failed to inject Paintbrush state for " + key + ": " + t.getMessage());
                failed++;
            }
        }
        // injected paintbrush state summary (info log removed)
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) throws Exception {
        ReflectionCache.setField(clazz, target, fieldName, value);
    }
}
