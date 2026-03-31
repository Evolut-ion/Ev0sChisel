package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.util.*;

/**
 * Compatibility layer for <b>Furniture Windows</b>
 * ({@code Furniture_*_Window} family) and <b>NoCube Tavern Windows</b>
 * ({@code NoCube_Furniture_Tavern_Window_*}).
 *
 * <h3>Carpenter's Hammer — Furniture_* windows by material</h3>
 * Window designs that can be built from wood (any type):
 * <ul>
 *   <li>Jungle, Human_Ruins(_NoConnect), Tavern(_NoConnect), Village(_NoConnect),
 *       Crude(_NoConnect), Ancient</li>
 * </ul>
 * Sandstone-only:
 * <ul>
 *   <li>Temple_Wind(_NoConnect), Desert</li>
 * </ul>
 * Marble-only:
 * <ul>
 *   <li>Temple_Light(_NoConnect)</li>
 * </ul>
 * Stone-only:
 * <ul>
 *   <li>Temple_Emerald(_NoConnect)</li>
 * </ul>
 *
 * <p>For wood designs: one hammer group per vanilla wood type, probing
 * {@code {Design}_{WoodType}} then {@code {WoodType}_{Design}} then
 * the bare design key as fallback.
 * For stone/sandstone/marble: one combined hammer group per material class.
 *
 * <h3>NoCube Tavern Windows — Paintbrush + Carpenter's Hammer</h3>
 * {@code NoCube_Furniture_Tavern_Window_{Color}} — paintbrush cycles colors;
 * carpenter's hammer lists them in the Windows tab alongside same-material
 * wood windows.  A {@link com.Ev0sMods.Ev0sChisel.ComboState} is used to
 * carry both states on the same block.
 */
public final class FurnitureWindowCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static boolean detected = false;

    private FurnitureWindowCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Material lists
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };

    /** Sandstone sub-types to probe */
    private static final String[] SANDSTONE_TYPES = {
            "Sandstone", "Sandstone_Red", "Sandstone_White"
    };

    // ── Window design names ───────────────────────────────────────────────

    /** Designs available in any wood type */
    private static final String[] WOOD_WINDOW_DESIGNS = {
            "Furniture_Jungle_Window",
            "Furniture_Human_Ruins_Window",
            "Furniture_Human_Ruins_Window_NoConnect",
            "Furniture_Tavern_Window",
            "Furniture_Tavern_Window_NoConnect",
            "Furniture_Village_Window",
            "Furniture_Village_Window_NoConnect",
            "Furniture_Crude_Window",
            "Furniture_Crude_Window_NoConnect",
            "Furniture_Ancient_Window"
    };

    /** Designs available in sandstone variants */
    private static final String[] SANDSTONE_WINDOW_DESIGNS = {
            "Furniture_Temple_Wind_Window",
            "Furniture_Temple_Wind_Window_NoConnect",
            "Furniture_Desert_Window"
    };

    /** Designs available in marble */
    private static final String[] MARBLE_WINDOW_DESIGNS = {
            "Furniture_Temple_Light_Window",
            "Furniture_Temple_Light_Window_NoConnect"
    };

    /** Designs available in stone */
    private static final String[] STONE_WINDOW_DESIGNS = {
            "Furniture_Temple_Emerald_Window",
            "Furniture_Temple_Emerald_Window_NoConnect"
    };

    /** NoCube Tavern window colors */
    private static final String[] TAVERN_COLORS = {
            "White", "Light_Gray", "Gray", "Black",
            "Brown", "Red", "Orange", "Yellow",
            "Lime", "Green", "Cyan", "Light_Blue",
            "Blue", "Purple", "Magenta", "Pink",
            "Cream", "Rose", "Glass"  // Glass = base/transparent variant
    };

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public static boolean isDetected() { return detected; }

    public static void init() {
        try {
            int total = 0;
            total += injectWoodWindows();
            total += injectSandstoneWindows();
            total += injectMarbleWindows();
            total += injectStoneWindows();
            total += injectNoCubeTavernWindows();
            if (total > 0) {
                detected = true;
                LOGGER.atWarning().log("[FurnitureWindowCompat] Injected onto " + total + " blocks.");
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[FurnitureWindowCompat] Init failed: " + t.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Wood windows — one hammer group per wood type
    // ─────────────────────────────────────────────────────────────────────

    private static int injectWoodWindows() {
        int count = 0;
        for (String wood : WOOD_TYPES) {
            List<String> found = new ArrayList<>();
            for (String design : WOOD_WINDOW_DESIGNS) {
                // Try suffix: {design}_{wood}
                String k1 = design + "_" + wood;
                if (exists(k1)) { found.add(k1); continue; }
                // Try prefix: {wood}_{design}
                String k2 = wood + "_" + design;
                if (exists(k2)) { found.add(k2); continue; }
                // Bare design key (design is the full block key itself)
                if (exists(design)) found.add(design);
            }
            if (found.isEmpty()) continue;
            dedupe(found);

            // Prepend base plank so players can revert this window back to wood
            String plank = findPlankKey(wood);
            if (plank != null && !found.contains(plank)) found.add(0, plank);

            String[] arr = found.toArray(new String[0]);
            CarpenterHammer.Data hammer = buildHammerWindows("FurnitureWindow_Wood_" + wood, arr);
            // Inject on window blocks
            for (String key : arr) {
                if (key.equals(plank)) continue; // handled separately below
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                injectHammerSafe(bt, hammer);
                count++;
            }
            // Inject hammer-only on plank (preserves chisel state)
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null) ComboStateHelper.inject(plankBt, null, null, hammer);
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sandstone windows
    // ─────────────────────────────────────────────────────────────────────

    private static int injectSandstoneWindows() {
        List<String> found = new ArrayList<>();
        for (String ss : SANDSTONE_TYPES) {
            for (String design : SANDSTONE_WINDOW_DESIGNS) {
                probe(found, design + "_" + ss, ss + "_" + design, design);
            }
        }
        // Add a base sandstone block so players can revert
        String baseSS = exists("Rock_Sandstone") ? "Rock_Sandstone" : null;
        if (baseSS != null && !found.contains(baseSS)) found.add(0, baseSS);
        return injectHammerGroupWithBase("FurnitureWindow_Sandstone", found, baseSS);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Marble windows
    // ─────────────────────────────────────────────────────────────────────

    private static int injectMarbleWindows() {
        List<String> found = new ArrayList<>();
        for (String design : MARBLE_WINDOW_DESIGNS) {
            probe(found, design + "_Marble", "Marble_" + design, design);
        }
        String baseMarble = exists("Rock_Marble") ? "Rock_Marble" : null;
        if (baseMarble != null && !found.contains(baseMarble)) found.add(0, baseMarble);
        return injectHammerGroupWithBase("FurnitureWindow_Marble", found, baseMarble);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Stone windows
    // ─────────────────────────────────────────────────────────────────────

    private static int injectStoneWindows() {
        List<String> found = new ArrayList<>();
        for (String design : STONE_WINDOW_DESIGNS) {
            probe(found, design + "_Stone", "Stone_" + design, design);
        }
        String baseStone = exists("Rock_Stone") ? "Rock_Stone" : null;
        if (baseStone != null && !found.contains(baseStone)) found.add(0, baseStone);
        return injectHammerGroupWithBase("FurnitureWindow_Stone", found, baseStone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // NoCube Tavern Windows — Paintbrush + Hammer (ComboState)
    // ─────────────────────────────────────────────────────────────────────

    private static int injectNoCubeTavernWindows() {
        List<String> found = new ArrayList<>();
        for (String color : TAVERN_COLORS) {
            String key = "NoCube_Furniture_Tavern_Window_" + color;
            if (exists(key)) found.add(key);
        }
        if (found.isEmpty()) return 0;

        String[] arr = found.toArray(new String[0]);

        // Paintbrush — cycle all color variants
        Paintbrush.Data pb = new Paintbrush.Data();
        pb.source        = "NoCube_Tavern_Window";
        pb.colorVariants = arr;

        // Hammer — also appears in wood windows group (generic slot)
        CarpenterHammer.Data hammer = buildHammerWindows("NoCube_Tavern_Window", arr);

        int count = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            ComboStateHelper.inject(bt, null, pb, hammer);
            count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Like {@link #injectHammerGroup} but also injects hammer-only on {@code baseKey}
     * (the raw material block) so players can revert window → base material.
     * The base key is skipped in the normal {@link #injectHammerSafe} loop so that
     * its existing chisel/combo state is preserved via {@link ComboStateHelper}.
     */
    private static int injectHammerGroupWithBase(String source, List<String> found, String baseKey) {
        if (found.isEmpty()) return 0;
        dedupe(found);
        String[] arr = found.toArray(new String[0]);
        CarpenterHammer.Data hammer = buildHammerWindows(source, arr);
        int count = 0;
        for (String key : arr) {
            if (key.equals(baseKey)) continue; // handled below with ComboStateHelper
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            injectHammerSafe(bt, hammer);
            count++;
        }
        if (baseKey != null) {
            BlockType baseBt = BlockTypeCache.get(baseKey);
            if (baseBt != null) ComboStateHelper.inject(baseBt, null, null, hammer);
        }
        return count;
    }

    /**
     * Returns the first existing plank/base key for the given wood type.
     * Tries {@code Wood_{wood}_Planks} → {@code Wood_{wood}_Plank} → {@code Wood_{wood}}.
     */
    private static String findPlankKey(String wood) {
        String[] candidates = {
            "Wood_" + wood + "_Planks",
            "Wood_" + wood + "_Plank",
            "Wood_" + wood
        };
        for (String k : candidates) if (exists(k)) return k;
        return null;
    }

    private static CarpenterHammer.Data buildHammerWindows(String source, String[] windows) {
        CarpenterHammer.Data d = new CarpenterHammer.Data();
        d.source  = source;
        d.chairs  = new String[0];
        d.tables  = new String[0];
        d.storage = new String[0];
        d.windows = windows;
        d.lights  = new String[0];
        return d;
    }

    /**
     * Injects CarpenterHammer.Data onto a block, upgrading to ComboState if
     * the block already has Chisel or Paintbrush data.
     */
    private static void injectHammerSafe(BlockType bt, CarpenterHammer.Data hammer) {
        StateData existing = bt.getState();
        if (existing instanceof CarpenterHammer.Data) return; // already handled
        ComboStateHelper.inject(bt, null, null, hammer);
    }

    private static void probe(List<String> out, String... candidates) {
        for (String k : candidates) {
            if (!out.contains(k) && exists(k)) { out.add(k); break; }
        }
    }

    private static void dedupe(List<String> list) {
        LinkedHashSet<String> set = new LinkedHashSet<>(list);
        list.clear();
        list.addAll(set);
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
