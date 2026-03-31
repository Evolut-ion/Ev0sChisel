package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.ComboState;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.util.*;

/**
 * Compatibility layer for <b>Macaw's Windows</b> and <b>Macaw's Doors</b>
 * ({@code Mcw_Windows_*} and {@code Mcw_Doors_*}).
 *
 * <h3>Chisel — Mcw Windows per wood type</h3>
 * {@code Mcw_Windows_{WoodType}_{Connected|Short_Clean_Window|Short_Window|Tall_Clean_Window|Tall_Window}}
 * — one chisel group per wood type so you cycle between window forms for that material.
 *
 * <h3>Chisel + Paintbrush — Curtains</h3>
 * {@code Mcw_Windows_{Color}_Curtain} and {@code Mcw_Windows_{Color}_Tall_Curtain}
 * — chisel cycles between curtain types for the same color; paintbrush cycles colors.
 * Both states are stored in a {@link ComboState}.
 *
 * <h3>Chisel — Blinds per wood type</h3>
 * {@code Mcw_Windows_{WoodType}_{Tall_Blinds|Short_Blinds}}
 * — one chisel group per wood type; also added to that type's windows hammer group.
 *
 * <h3>Chisel — Glass windows</h3>
 * {@code Mcw_Windows_Clean_Glass_Block}, {@code Mcw_Windows_Clean_Glass_Pane},
 * {@code Half_Glass}, {@code Glass} plus vanilla glass pane patterns
 * {@code {Design}_Color_{Glass|Grass}} — all in a single "glass" chisel group.
 *
 * <h3>Chisel — Doors per wood type</h3>
 * {@code Mcw_Doors_{WoodType}_{DoorDesign}_Door}
 * — one chisel group per wood type.
 */
public final class MacawWindowDoorCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static boolean detected = false;

    private MacawWindowDoorCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };

    /** Window forms for Mcw_Windows_{WoodType}_{form} */
    private static final String[] WINDOW_FORMS = {
            "Connected", "Short_Clean_Window", "Short_Window",
            "Tall_Clean_Window", "Tall_Window", "Shutters"
    };

    /** Blind forms for Mcw_Windows_{WoodType}_{form} */
    private static final String[] BLIND_FORMS = {
            "Tall_Blinds", "Short_Blinds"
    };

    /** Door designs for Mcw_Doors_{WoodType}_{design}_Door */
    private static final String[] DOOR_DESIGNS = {
            "Barn", "Barn_Glass", "Four_Panel", "Glass", "Half_Glass",
            "Medieval", "Modern", "Paper", "Shoji", "Shoji_Simple",
            "Simple", "Stylic", "Tropical", "Western"
    };

    /** Curtain colors for Mcw_Windows_{Color}_{Curtain|Tall_Curtain} */
    private static final String[] CURTAIN_COLORS = {
            "White", "Light_Gray", "Gray", "Black",
            "Brown", "Red", "Orange", "Yellow",
            "Lime", "Green", "Cyan", "Light_Blue",
            "Blue", "Purple", "Magenta", "Pink",
            "Cream", "Rose"
    };

    /** Glass pane patterns for {Design}_Color_{Glass|Grass} */
    private static final String[] GLASS_DESIGNS = {
            "Blotch", "Centre_Circle", "Criss_Cross", "Cross",
            "Diagonal_Squared", "Diamond", "Etched", "Medieval"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public static boolean isDetected() { return detected; }

    public static void init() {
        try {
            int total = 0;
            total += injectWindowsPerWood();
            total += injectCurtains();
            total += injectBlindsPerWood();
            total += injectGlassWindows();
            total += injectDoorsPerWood();
            total += injectVanillaDoors();
            total += injectVanillaChests();
            total += injectVanillaLattice();
            if (total > 0) {
                detected = true;
                LOGGER.atWarning().log("[MacawWindowDoorCompat] Injected onto " + total + " blocks.");
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[MacawWindowDoorCompat] Init failed: " + t.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mcw Windows — per wood type
    // ─────────────────────────────────────────────────────────────────────

    private static int injectWindowsPerWood() {
        int count = 0;
        for (String wood : WOOD_TYPES) {
            List<String> windows = new ArrayList<>();
            for (String form : WINDOW_FORMS) {
                String key = "Mcw_Windows_" + wood + "_" + form;
                if (exists(key)) windows.add(key);
            }
            if (windows.isEmpty()) continue;

            String[] windowArr = windows.toArray(new String[0]);
            String plank = findPlankKey(wood);
            String[] hammerArr = plank != null ? prepend(plank, windowArr) : windowArr;

            Chisel.Data chisel = buildChiselData("Mcw_Windows_" + wood, windowArr, null, null, null);
            CarpenterHammer.Data hammer = buildHammerWindows("Mcw_Windows_" + wood, hammerArr);
            for (String key : windowArr) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                ComboStateHelper.inject(bt, chisel, null, hammer);
                count++;
            }
            // Merge window keys onto plank so it can also access window conversion
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null) mergeWindowsOntoBlock(plankBt, windowArr);
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mcw Windows — curtains (Chisel + Paintbrush combo)
    // ─────────────────────────────────────────────────────────────────────

    private static int injectCurtains() {
        int count = 0;

        // Short curtains — group by color: Mcw_Windows_{Color}_Curtain
        List<String> allShort = new ArrayList<>();
        List<String> allTall  = new ArrayList<>();
        for (String color : CURTAIN_COLORS) {
            String sk = "Mcw_Windows_" + color + "_Curtain";
            String tk = "Mcw_Windows_" + color + "_Tall_Curtain";
            if (exists(sk)) allShort.add(sk);
            if (exists(tk)) allTall.add(tk);
        }

        // Per-color chisel groups (cycle short ↔ tall variants for that same color) +
        // Paintbrush across all colors for each curtain length.
        String[] shortArr = allShort.toArray(new String[0]);
        String[] tallArr  = allTall.toArray(new String[0]);

        // Build paintbrush data for all-shorts and all-talls
        Paintbrush.Data pbShort = buildPaintbrushData("Mcw_Curtain_Short", shortArr);
        Paintbrush.Data pbTall  = buildPaintbrushData("Mcw_Curtain_Tall",  tallArr);

        for (String color : CURTAIN_COLORS) {
            String sk = "Mcw_Windows_" + color + "_Curtain";
            String tk = "Mcw_Windows_" + color + "_Tall_Curtain";

            // Per-color chisel group = [short, tall] of SAME color
            List<String> pair = new ArrayList<>();
            if (exists(sk)) pair.add(sk);
            if (exists(tk)) pair.add(tk);
            if (pair.isEmpty()) continue;

            String[] pairArr = pair.toArray(new String[0]);
            Chisel.Data chisel = buildChiselData("Mcw_Curtain_" + color, pairArr, null, null, null);

            if (exists(sk)) {
                ComboStateHelper.inject(sk, chisel, pbShort, null);
                count++;
            }
            if (exists(tk)) {
                ComboStateHelper.inject(tk, chisel, pbTall, null);
                count++;
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mcw Windows — blinds per wood type
    // ─────────────────────────────────────────────────────────────────────

    private static int injectBlindsPerWood() {
        int count = 0;
        for (String wood : WOOD_TYPES) {
            List<String> blinds = new ArrayList<>();
            for (String form : BLIND_FORMS) {
                String key = "Mcw_Windows_" + wood + "_" + form;
                if (exists(key)) blinds.add(key);
            }
            if (blinds.isEmpty()) continue;

            String[] blindArr = blinds.toArray(new String[0]);
            String plank = findPlankKey(wood);
            String[] hammerArr = plank != null ? prepend(plank, blindArr) : blindArr;

            Chisel.Data chisel = buildChiselData("Mcw_Blinds_" + wood, blindArr, null, null, null);
            CarpenterHammer.Data hammer = buildHammerWindows("Mcw_Blinds_" + wood, hammerArr);
            for (String key : blindArr) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                ComboStateHelper.inject(bt, chisel, null, hammer);
                count++;
            }
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null) mergeWindowsOntoBlock(plankBt, blindArr);
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Glass windows (chisel group, all glass in one)
    // ─────────────────────────────────────────────────────────────────────

    private static int injectGlassWindows() {
        List<String> glass = new ArrayList<>();

        // Mcw glass blocks
        probe(glass, "Mcw_Windows_Clean_Glass_Block", "Mcw_Windows_Clean_Glass_Pane");
        // Vanilla
        probe(glass, "Half_Glass", "Glass");
        // Vanilla glass pane: {Design}_Color_{Glass|Grass}
        for (String design : GLASS_DESIGNS) {
            probe(glass, design + "_Color_Glass", design + "_Color_Grass");
        }

        if (glass.isEmpty()) return 0;
        String[] arr = glass.toArray(new String[0]);
        Chisel.Data chisel = buildChiselData("Glass_Windows", arr, null, null, null);
        CarpenterHammer.Data hammer = buildHammerWindows("Glass_Windows", arr);
        int count = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            ComboStateHelper.inject(bt, chisel, null, hammer);
            count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mcw Doors — per wood type
    // ─────────────────────────────────────────────────────────────────────

    private static int injectDoorsPerWood() {
        int count = 0;
        for (String wood : WOOD_TYPES) {
            List<String> doors = new ArrayList<>();
            for (String design : DOOR_DESIGNS) {
                String key = "Mcw_Doors_" + wood + "_" + design + "_Door";
                if (exists(key)) doors.add(key);
            }
            if (doors.isEmpty()) continue;

            String[] doorArr = doors.toArray(new String[0]);
            String plank = findPlankKey(wood);
            String[] hammerArr = plank != null ? prepend(plank, doorArr) : doorArr;

            Chisel.Data chisel = buildChiselData("Mcw_Doors_" + wood, doorArr, null, null, null);
            CarpenterHammer.Data hammer = buildHammerWindows("Mcw_Doors_" + wood, hammerArr);
            for (String key : doorArr) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                ComboStateHelper.inject(bt, chisel, null, hammer);
                count++;
            }
            // Merge door keys onto plank so planks can access door conversion (subject to stacking check)
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null && mergeWindowsOntoBlock(plankBt, doorArr)) count++;
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vanilla doors — same wood-type identifiers as vanilla windows
    // ─────────────────────────────────────────────────────────────────────

    private static int injectVanillaDoors() {
        int count = 0;
        for (String wood : WOOD_TYPES) {
            // Vanilla door key: Wood_{WoodType}_Door
            String key = "Wood_" + wood + "_Door";
            if (!exists(key)) continue;
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;

            String plank = findPlankKey(wood);
            String[] doorArr = new String[]{key};
            String[] hammerArr = plank != null ? prepend(plank, doorArr) : doorArr;

            Chisel.Data chisel = buildChiselData("Vanilla_Door_" + wood, doorArr, null, null, null);
            CarpenterHammer.Data hammer = buildHammerWindows("Vanilla_Door_" + wood, hammerArr);
            ComboStateHelper.inject(bt, chisel, null, hammer);
            count++;
            // Merge vanilla door onto plank
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null) mergeWindowsOntoBlock(plankBt, doorArr);
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vanilla lattice — Wood_{WoodType}_Lattice → hammer windows tab
    // ─────────────────────────────────────────────────────────────────────

    private static int injectVanillaLattice() {
        int count = 0;
        for (String wood : WOOD_TYPES) {
            String key = "Wood_" + wood + "_Lattice";
            if (!exists(key)) continue;
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;

            String plank = findPlankKey(wood);
            String[] latticeArr = new String[]{key};
            String[] hammerArr = plank != null ? prepend(plank, latticeArr) : latticeArr;

            CarpenterHammer.Data hammer = buildHammerWindows("Vanilla_Lattice_" + wood, hammerArr);
            ComboStateHelper.inject(bt, null, null, hammer);
            count++;
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null) mergeWindowsOntoBlock(plankBt, latticeArr);
            }
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vanilla chests — same wood-type identifiers
    // ─────────────────────────────────────────────────────────────────────

    private static int injectVanillaChests() {
        int count = 0;
        List<String> allChests = new ArrayList<>();
        for (String wood : WOOD_TYPES) {
            String key = "Wood_" + wood + "_Chest";
            if (exists(key)) allChests.add(key);
        }
        if (allChests.isEmpty()) return 0;

        String[] arr = allChests.toArray(new String[0]);
        Chisel.Data chisel = buildChiselData("Vanilla_Chests", arr, null, null, null);
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            injectChiselSafe(bt, chisel);
            count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Injection helpers
    // ─────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────
    // Base-block helpers
    // ─────────────────────────────────────────────────────────────────────

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

    /** Returns a new array with {@code key} prepended to {@code arr}. */
    private static String[] prepend(String key, String[] arr) {
        String[] result = new String[arr.length + 1];
        result[0] = key;
        System.arraycopy(arr, 0, result, 1, arr.length);
        return result;
    }

    /**
     * Merges {@code keysToAdd} into the block's existing
     * {@link CarpenterHammer.Data#windows} list (via {@link ComboState} if present).
     * If no hammer data exists yet, injects a fresh one containing the block's
     * own key followed by {@code keysToAdd}.
     *
     * @return {@code true} if at least one key was newly added
     */
    private static boolean mergeWindowsOntoBlock(BlockType bt, String[] keysToAdd) {
        if (bt == null || keysToAdd == null || keysToAdd.length == 0) return false;
        try {
            StateData existing = bt.getState();
            CarpenterHammer.Data hammer = null;
            if (existing instanceof CarpenterHammer.Data d) hammer = d;
            else if (existing instanceof com.Ev0sMods.Ev0sChisel.ComboState cs) hammer = cs.hammer;

            if (hammer != null) {
                // Merge: preserve existing windows, append new keys
                java.util.LinkedHashSet<String> merged = hammer.windows != null
                    ? new java.util.LinkedHashSet<>(java.util.Arrays.asList(hammer.windows))
                    : new java.util.LinkedHashSet<>();
                int before = merged.size();
                java.util.Collections.addAll(merged, keysToAdd);
                hammer.windows = merged.toArray(new String[0]);
                return merged.size() > before;
            } else {
                // No existing hammer — inject a fresh one with own key as first option
                String selfKey = bt.getId() != null ? bt.getId().toString() : null;
                String[] arr = selfKey != null ? prepend(selfKey, keysToAdd) : keysToAdd;
                CarpenterHammer.Data fresh = buildHammerWindows("Base_Windows_" + selfKey, arr);
                return ComboStateHelper.inject(bt, null, null, fresh);
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[MacawWindowDoorCompat] mergeWindowsOntoBlock failed for "
                    + bt.getId() + ": " + t.getMessage());
            return false;
        }
    }

    /** Injects a Chisel.Data if the block has no state yet (does not overwrite ComboState). */
    private static void injectChiselSafe(BlockType bt, Chisel.Data chisel) {
        StateData existing = bt.getState();
        if (existing instanceof ComboState cs) {
            if (cs.chisel == null) cs.chisel = chisel;
        } else if (!(existing instanceof Chisel.Data)) {
            try {
                ReflectionCache.setField(StateData.class, chisel, "id", "Ev0sChisel");
                ReflectionCache.setField(BlockType.class, bt, "state", chisel);
            } catch (Throwable t) {
                LOGGER.atWarning().log("[MacawWindowDoorCompat] chisel inject failed: " + t.getMessage());
            }
        }
    }

    private static Chisel.Data buildChiselData(String source,
            String[] subs, String[] stairs, String[] halfs, String[] roofs) {
        Chisel.Data d = new Chisel.Data();
        d.source       = source;
        d.substitutions = subs  != null ? subs  : new String[0];
        d.stairs        = stairs != null ? stairs : new String[0];
        d.halfSlabs     = halfs  != null ? halfs  : new String[0];
        d.roofing       = roofs  != null ? roofs  : new String[0];
        return d;
    }

    private static Paintbrush.Data buildPaintbrushData(String source, String[] variants) {
        Paintbrush.Data d = new Paintbrush.Data();
        d.source       = source;
        d.colorVariants = variants;
        return d;
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

    private static void probe(List<String> out, String... keys) {
        for (String k : keys) if (exists(k)) out.add(k);
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
