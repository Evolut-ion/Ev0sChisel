package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.util.*;

/**
 * Compatibility layer for <b>Gui's Furniture and Stuff</b>
 * ({@code Gui.FurnitureAndStuff_*.zip}).
 *
 * <h3>Carpenter's Hammer support</h3>
 * <ul>
 *   <li><b>Gui wood group</b> – LogChair, BenchLog (chairs), LogTable (tables) as
 *       one group since they are unique items with no material sub-type.</li>
 *   <li><b>Vanilla wood groups</b> – one group per wood type (e.g. Hardwood, Softwood).
 *       Only same-material furniture (chairs/tables/storage/windows) appears together.</li>
 *   <li><b>Gui stone group</b> – StoneChair, StoneTable, StoneCoffeTable and Kitchen
 *       items as one group.</li>
 *   <li><b>Vanilla rock groups</b> – one group per rock type (e.g. Stone, Marble).</li>
 *   <li><b>Sofa groups</b> – one group per sofa color; all shape variants (Single,
 *       RightSide, etc.) are listed in the Chair tab so the hammer can cycle
 *       between configurations of the same color.</li>
 * </ul>
 *
 * <h3>Paintbrush support</h3>
 * <ul>
 *   <li>Each sofa <em>type</em> gets a {@link Paintbrush.Data} listing every
 *       color variant of that specific shape so the paintbrush shifts colors
 *       while keeping the sofa configuration.</li>
 * </ul>
 *
 * <h3>Vanilla furniture</h3>
 * After processing Gui's items this class also probes for vanilla Hytale
 * furniture patterns and injects the hammer state onto any blocks found.
 */
public final class GuiFurnitureCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Set to true once at least one Gui Furniture block was found. */
    private static boolean detected = false;

    private GuiFurnitureCompat() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Item catalogues
    // ─────────────────────────────────────────────────────────────────────────

    /** Colors available for the FirstSofa series (probe order). */
    private static final String[] SOFA_COLORS = {
            "Blue", "Brown", "Cream", "Green", "Red", "Rose", "Yellow"
    };

    /** Color variants probed for Gui's carpet blocks. */
    private static final String[] CARPET_COLORS = {
            "Blue", "Brown", "Cream", "Green", "Red", "Rose", "Yellow",
            "White", "Black", "Orange", "Purple", "Pink", "Cyan", "Gray",
            "Light_Gray", "Lime", "Magenta", "Light_Blue"
    };

    /** Shape types for the FirstSofa series. */
    private static final String[] SOFA_TYPES = {
            "Single", "RightSide", "LeftSide", "Base", "Corner",
            "RightSideLong", "LeftSideLong"
    };

    /** Kitchen-folder items treated as storage furniture. */
    private static final String[] KITCHEN_ITEMS = {
            "Gui_KitchenCounterCorner",
            "Gui_KitchenCounterLarge",
            "Gui_KitchenCounterSmall",
            "Gui_KitchenCounterTall",
            "Gui_KitchenCupboardBase",
            "Gui_KitchenCupboardInsideCorner",
            "Gui_KitchenCupboardOutsideCorner",
            "Gui_KitchenCupboardShelf1",
            "Gui_KitchenCupboardShelf2",
            "Gui_KitchenTopOnly",
            "Gui_StoneSink"
    };

    // ── Vanilla furniture probe patterns ─────────────────────────────────
    private static final String[] WOOD_TYPES_VANILLA = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };

    private static final String[] ROCK_TYPES_VANILLA = {
            "Aqua", "Ash", "Basalt", "Calcite", "Chalk", "Clay_Brick",
            "Ledge", "Ledge_Brick", "Lime", "Marble", "Peach",
            "Quartzite", "Sandstone", "Stone"
    };

    private static final String[] VANILLA_CHAIR_SUFFIXES = {
            "_Chair", "_Stool", "_Bench", "_Seat", "_Throne"
    };
    private static final String[] VANILLA_TABLE_SUFFIXES = {
            "_Table", "_Desk", "_Counter", "_Workbench"
    };
    private static final String[] VANILLA_STORAGE_SUFFIXES = {
            "_Chest", "_Cabinet", "_Shelf", "_Cupboard", "_Dresser", "_Bookshelf"
    };
    private static final String[] VANILLA_WINDOW_SUFFIXES = {
            "_Window", "_Shutter", "_Window_Frame", "_Window_Pane"
    };
    private static final String[] VANILLA_LIGHT_PREFIXES = {
            "Lantern", "Torch", "Candle", "Brazier", "Fireplace",
            "Lamp", "Chandelier", "Sconce", "Glowstone"
    };
    private static final String[] VANILLA_LIGHT_SUFFIXES = {
            "_Lantern", "_Torch", "_Candle", "_Lamp", "_Light", "_Glow"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** @return {@code true} when at least one Gui Furniture block was discovered. */
    public static boolean isAvailable() { return detected; }

    /**
     * Called once during {@code Ev0sChiselPlugin.start()}.
     * Probes block keys and, if any are found, injects state data.
     */
    public static void init() {
        try {
            injectCarpenterHammerStates();
            injectPaintbrushStates();
            injectCarpetPaintbrushStates();
        } catch (Throwable t) {
            LOGGER.atWarning().log("[GuiFurnitureCompat] Init failed: " + t.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carpenter's Hammer injection
    // ─────────────────────────────────────────────────────────────────────────

    public static void injectCarpenterHammerStates() {
        int total = 0;
        total += injectGuiWoodGroup();
        total += injectVanillaWoodGroups();
        total += injectGuiStoneGroup();
        total += injectVanillaRockGroups();
        total += injectSofaGroups();
        total += injectVanillaFurniture();
        if (total > 0) {
            detected = true;
            LOGGER.atWarning().log("[GuiFurnitureCompat] Injected CarpenterHammer.Data onto " + total + " blocks.");
        }
    }

    // ── Gui-specific wood furniture (unique items, one combined group) ──────────

    private static int injectGuiWoodGroup() {
        String[] woodChairs = probe("Gui_LogChair", "Gui_BenchLog");
        String[] woodTables = probe("Gui_LogTable");
        if (woodChairs.length == 0 && woodTables.length == 0) return 0;
        String[] all = merge(woodChairs, woodTables);
        return injectHammerData(all, "Gui_Wood",
                woodChairs, woodTables, new String[0], new String[0], new String[0]);
    }

    // ── Vanilla wood furniture – one group per wood type ──────────────────────

    private static int injectVanillaWoodGroups() {
        int count = 0;
        for (String woodType : WOOD_TYPES_VANILLA) {
            String base    = "Wood_" + woodType;
            String[] chairs  = discoverSuffixes(base, VANILLA_CHAIR_SUFFIXES);
            String[] tables  = discoverSuffixes(base, VANILLA_TABLE_SUFFIXES);
            String[] storage = discoverSuffixes(base, VANILLA_STORAGE_SUFFIXES);
            String[] windows = discoverSuffixes(base, VANILLA_WINDOW_SUFFIXES);
            if (chairs.length + tables.length + storage.length + windows.length == 0) continue;

            // Find the base plank block so deco items can revert to it
            String baseKey = exists(base + "_Planks") ? base + "_Planks"
                           : exists(base + "_Plank")  ? base + "_Plank"
                           : exists(base)             ? base
                           : null;
            if (baseKey != null) {
                if (chairs.length  > 0) chairs  = prepend(baseKey, chairs);
                if (tables.length  > 0) tables  = prepend(baseKey, tables);
                if (storage.length > 0) storage = prepend(baseKey, storage);
                if (windows.length > 0) windows = prepend(baseKey, windows);
            }

            // Inject hammer on deco targets (not the base block — ComboStateHelper handles that below)
            String[] all = merge(chairs, tables, storage, windows);
            String[] targets = baseKey != null ? removeKey(all, baseKey) : all;
            count += injectHammerData(targets, base, chairs, tables, storage, windows, new String[0]);

            // Inject hammer-only on the base block (preserves its existing chisel/combo state)
            if (baseKey != null) {
                BlockType baseBt = BlockTypeCache.get(baseKey);
                if (baseBt != null) {
                    CarpenterHammer.Data hd = new CarpenterHammer.Data();
                    hd.source  = base;
                    hd.chairs  = chairs;
                    hd.tables  = tables;
                    hd.storage = storage;
                    hd.windows = windows;
                    hd.lights  = new String[0];
                    if (com.Ev0sMods.Ev0sChisel.compat.ComboStateHelper.inject(baseBt, null, null, hd)) count++;
                }
            }
        }
        return count;
    }

    // ── Gui-specific stone furniture (unique items, one combined group) ───────

    private static int injectGuiStoneGroup() {
        String[] stoneChairs  = probe("Gui_StoneChair");
        String[] stoneTables  = probe("Gui_StoneTable", "Gui_StoneCoffeTable");
        String[] stoneStorage = probe(KITCHEN_ITEMS);
        if (stoneChairs.length + stoneTables.length + stoneStorage.length == 0) return 0;
        String[] all = merge(stoneChairs, stoneTables, stoneStorage);
        return injectHammerData(all, "Gui_Stone",
                stoneChairs, stoneTables, stoneStorage, new String[0], new String[0]);
    }

    // ── Vanilla rock furniture – one group per rock type ──────────────────────

    private static int injectVanillaRockGroups() {
        int count = 0;
        for (String rockType : ROCK_TYPES_VANILLA) {
            String base    = "Rock_" + rockType;
            String[] chairs  = discoverSuffixes(base, VANILLA_CHAIR_SUFFIXES);
            String[] tables  = discoverSuffixes(base, VANILLA_TABLE_SUFFIXES);
            String[] storage = discoverSuffixes(base, VANILLA_STORAGE_SUFFIXES);
            String[] windows = discoverSuffixes(base, VANILLA_WINDOW_SUFFIXES);
            if (chairs.length + tables.length + storage.length + windows.length == 0) continue;

            // Prepend base rock block as a "revert" option in each category
            String baseKey = exists(base) ? base : null;
            if (baseKey != null) {
                if (chairs.length  > 0) chairs  = prepend(baseKey, chairs);
                if (tables.length  > 0) tables  = prepend(baseKey, tables);
                if (storage.length > 0) storage = prepend(baseKey, storage);
                if (windows.length > 0) windows = prepend(baseKey, windows);
            }

            String[] all = merge(chairs, tables, storage, windows);
            String[] targets = baseKey != null ? removeKey(all, baseKey) : all;
            count += injectHammerData(targets, base, chairs, tables, storage, windows, new String[0]);

            if (baseKey != null) {
                BlockType baseBt = BlockTypeCache.get(baseKey);
                if (baseBt != null) {
                    CarpenterHammer.Data hd = new CarpenterHammer.Data();
                    hd.source  = base;
                    hd.chairs  = chairs;
                    hd.tables  = tables;
                    hd.storage = storage;
                    hd.windows = windows;
                    hd.lights  = new String[0];
                    if (com.Ev0sMods.Ev0sChisel.compat.ComboStateHelper.inject(baseBt, null, null, hd)) count++;
                }
            }
        }
        return count;
    }

    // ── Sofa groups (one per color) ───────────────────────────────────────────

    private static int injectSofaGroups() {
        int count = 0;
        for (String color : SOFA_COLORS) {
            // Collect all sofa type variants for this color
            List<String> chairsForColor = new ArrayList<>();
            for (String type : SOFA_TYPES) {
                String key = "Gui_FirstSofa" + type + color;
                if (exists(key)) chairsForColor.add(key);
            }
            if (chairsForColor.isEmpty()) continue;

            String[] chairs = chairsForColor.toArray(new String[0]);
            count += injectHammerData(chairs, "Gui_Sofa_" + color,
                    chairs, new String[0], new String[0], new String[0], new String[0]);
        }
        return count;
    }

    // ── Vanilla standalone furniture (not tied to wood/rock prefixes) ─────────

    private static int injectVanillaFurniture() {
        int count = 0;

        // Lights (standalone probe – prefix-only patterns)
        List<String> lights = new ArrayList<>();
        for (String prefix : VANILLA_LIGHT_PREFIXES) {
            if (exists(prefix)) lights.add(prefix);
            // also try plural / colored variants
            for (String color : new String[]{"Red", "Green", "Blue", "Yellow", "White", "Black",
                    "Orange", "Purple", "Pink", "Cyan"}) {
                String k = prefix + "_" + color;
                if (exists(k)) lights.add(k);
            }
        }
        // Suffix variants on wood / rock bases
        for (String woodType : WOOD_TYPES_VANILLA)
            for (String suf : VANILLA_LIGHT_SUFFIXES) { String k = "Wood_" + woodType + suf; if (exists(k)) lights.add(k); }
        for (String rockType : ROCK_TYPES_VANILLA)
            for (String suf : VANILLA_LIGHT_SUFFIXES) { String k = "Rock_" + rockType + suf; if (exists(k)) lights.add(k); }

        if (!lights.isEmpty()) {
            String[] la = lights.toArray(new String[0]);
            count += injectHammerData(la, "Vanilla_Lights",
                    new String[0], new String[0], new String[0], new String[0], la);
        }

        // Generic/non-material windows (glass, iron) — per-material windows are
        // handled by injectVanillaWoodGroups() and injectVanillaRockGroups().
        String[] genericWindows = probe(
                "Glass_Window", "Glass_Window_Frame", "Glass_Window_Pane",
                "Iron_Window_Frame", "Iron_Window_Pane");
        if (genericWindows.length > 0)
            count += injectHammerData(genericWindows, "Vanilla_Windows",
                    new String[0], new String[0], new String[0], genericWindows, new String[0]);

        return count;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Paintbrush injection (FirstSofa color variants)
    // ─────────────────────────────────────────────────────────────────────────

    public static void injectPaintbrushStates() {
        int count = 0;
        for (String type : SOFA_TYPES) {
            List<String> colorVariants = new ArrayList<>();
            for (String color : SOFA_COLORS) {
                String key = "Gui_FirstSofa" + type + color;
                if (exists(key)) colorVariants.add(key);
            }
            if (colorVariants.isEmpty()) continue;

            String[] variants = colorVariants.toArray(new String[0]);
            // Inject onto every variant of this type so the paintbrush works from any color
            for (String key : variants) {
                try {
                    BlockType bt = BlockTypeCache.get(key);
                    if (bt == null) continue;
                    StateData existing = bt.getState();
                    if (existing instanceof Paintbrush.Data) continue; // already injected

                    Paintbrush.Data data = new Paintbrush.Data();
                    data.source       = "Gui_FirstSofa_" + type;
                    data.colorVariants = variants;

                    ReflectionCache.setField(StateData.class, data, "id", "Ev0sPaintbrush");
                    ReflectionCache.setField(BlockType.class, bt,   "state", data);
                    count++;
                } catch (Throwable t) {
                    LOGGER.atWarning().log("[GuiFurnitureCompat] Paintbrush inject failed for " + key + ": " + t.getMessage());
                }
            }
        }
        if (count > 0)
            LOGGER.atWarning().log("[GuiFurnitureCompat] Injected Paintbrush.Data onto " + count + " sofa blocks.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Paintbrush injection (Gui carpet color variants)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Discovers Gui carpet blocks by probing multiple naming patterns and
     * injects a single {@link Paintbrush.Data} group covering all found colors.
     * Patterns tried per color (first match wins): {@code Gui_Carpet_{Color}},
     * {@code Gui_Carpet{Color}}.
     */
    public static void injectCarpetPaintbrushStates() {
        List<String> found = new ArrayList<>();
        for (String color : CARPET_COLORS) {
            String k1 = "Gui_CarpetBlock_" + color;
            if (exists(k1)) { found.add(k1); continue; }
            String k2 = "Gui_CarpetBlock" + color;
            if (exists(k2)) found.add(k2);
        }
        if (found.isEmpty()) return;

        String[] variants = found.toArray(new String[0]);
        int count = 0;
        for (String key : variants) {
            try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                StateData existing = bt.getState();
                if (existing instanceof Paintbrush.Data) continue;

                Paintbrush.Data data = new Paintbrush.Data();
                data.source        = "Gui_Carpet";
                data.colorVariants = variants;

                ReflectionCache.setField(StateData.class, data, "id", "Ev0sPaintbrush");
                ReflectionCache.setField(BlockType.class, bt,   "state", data);
                count++;
            } catch (Throwable t) {
                LOGGER.atWarning().log("[GuiFurnitureCompat] Carpet paintbrush inject failed for " + key + ": " + t.getMessage());
            }
        }
        if (count > 0)
            LOGGER.atWarning().log("[GuiFurnitureCompat] Injected Paintbrush.Data onto " + count + " base carpet blocks.");

        // Temperature variant groups (Cold / Pastel / Warm)
        for (String temp : new String[]{"Cold", "Pastel", "Warm"}) {
            List<String> tempFound = new ArrayList<>();
            for (String color : CARPET_COLORS) {
                String tk = "Gui_CarpetBlock_" + temp + "_" + color;
                if (exists(tk)) tempFound.add(tk);
            }
            if (tempFound.isEmpty()) continue;
            String[] tv = tempFound.toArray(new String[0]);
            int tc = 0;
            for (String key : tv) {
                try {
                    BlockType bt = BlockTypeCache.get(key);
                    if (bt == null) continue;
                    StateData ex2 = bt.getState();
                    if (ex2 instanceof Paintbrush.Data) continue;
                    Paintbrush.Data data = new Paintbrush.Data();
                    data.source        = "Gui_Carpet_" + temp;
                    data.colorVariants = tv;
                    ReflectionCache.setField(StateData.class, data, "id", "Ev0sPaintbrush");
                    ReflectionCache.setField(BlockType.class, bt,   "state", data);
                    tc++;
                } catch (Throwable t2) {
                    LOGGER.atWarning().log("[GuiFurnitureCompat] " + temp + " carpet inject failed for " + key + ": " + t2.getMessage());
                }
            }
            if (tc > 0)
                LOGGER.atWarning().log("[GuiFurnitureCompat] Injected Paintbrush.Data onto " + tc + " " + temp + " carpet blocks.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Injects {@link CarpenterHammer.Data} onto every block in {@code targets}.
     * Skips keys whose BlockType is absent or already carries hammer state.
     */
    private static int injectHammerData(
            String[] targets, String source,
            String[] chairs, String[] tables, String[] storage,
            String[] windows, String[] lights) {
        int count = 0;
        for (String key : targets) {
            if (key == null) continue;
            try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                StateData existing = bt.getState();
                if (existing instanceof CarpenterHammer.Data) continue;

                CarpenterHammer.Data data = new CarpenterHammer.Data();
                data.source  = source;
                data.chairs  = chairs  != null ? chairs  : new String[0];
                data.tables  = tables  != null ? tables  : new String[0];
                data.storage = storage != null ? storage : new String[0];
                data.windows = windows != null ? windows : new String[0];
                data.lights  = lights  != null ? lights  : new String[0];

                ReflectionCache.setField(StateData.class, data, "id", "Ev0sCarpenterHammer");
                ReflectionCache.setField(BlockType.class, bt,   "state", data);
                count++;
            } catch (Throwable t) {
                LOGGER.atWarning().log("[GuiFurnitureCompat] Inject failed for " + key + ": " + t.getMessage());
            }
        }
        return count;
    }

    /**
     * Probes a fixed list of candidate keys and returns those that actually
     * exist in the asset registry.
     */
    private static String[] probe(String... candidates) {
        List<String> found = new ArrayList<>();
        for (String k : candidates) if (exists(k)) found.add(k);
        return found.toArray(new String[0]);
    }

    /** Returns a new array with {@code key} prepended to {@code arr}. */
    private static String[] prepend(String key, String[] arr) {
        String[] result = new String[arr.length + 1];
        result[0] = key;
        System.arraycopy(arr, 0, result, 1, arr.length);
        return result;
    }

    /** Returns {@code arr} without any occurrences of {@code key}. */
    private static String[] removeKey(String[] arr, String key) {
        List<String> list = new ArrayList<>(Arrays.asList(arr));
        list.remove(key);
        return list.toArray(new String[0]);
    }

    /**
     * Discovers furniture variants of the form {@code {base}{suffix}} for each
     * suffix in the given array (e.g. base={@code Wood_Hardwood}, suffix={@code _Chair}).
     */
    private static String[] discoverSuffixes(String base, String[] suffixes) {
        List<String> found = new ArrayList<>();
        for (String suf : suffixes) {
            String k = base + suf;
            if (exists(k)) found.add(k);
        }
        return found.toArray(new String[0]);
    }

    /** Merges multiple string arrays, preserving order and deduplicating. */
    @SafeVarargs
    private static String[] merge(String[]... arrays) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String[] arr : arrays) if (arr != null) for (String s : arr) if (s != null) set.add(s);
        return set.toArray(new String[0]);
    }

    private static boolean exists(String key) {
        return BlockTypeCache.exists(key);
    }
}
