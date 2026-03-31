package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

/**
 * Carpenter's Hammer support for furniture-pack blocks that use the
 * {@code Furniture_{Style}_{Type}_{WoodType}} naming scheme — the same
 * convention used by {@link FurnitureWindowCompat}.
 *
 * <p>Groups per <b>wood type</b> so the Carpenter's Hammer cycles between all
 * design variants available in that material (e.g. all Hardwood torches,
 * all Hardwood signs, …).  Probes both {@code {design}_{wood}} and
 * {@code {wood}_{design}} orderings, plus the bare design key as fallback —
 * identical to how {@link FurnitureWindowCompat#injectWoodWindows()} works.
 *
 * <ul>
 *   <li><b>Torches / lanterns</b>  → {@code lights} tab</li>
 *   <li><b>Signs</b>               → {@code windows} tab
 *       (+ per-wood chisel group for sign ↔ wall-sign cycling)</li>
 *   <li><b>Shelves</b>             → {@code storage} tab</li>
 *   <li><b>Beds</b>                → {@code storage} tab</li>
 * </ul>
 *
 * All keys are runtime-probed; any that are absent are silently skipped.
 */
public final class VanillaFurnitureCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood",  "Lightwood","Redwood",  "Softwood","Tropicalwood",
            "Whitewood", "Goldenwood",
            // Femboy's Delight exclusive wood types
            "Pinkwood", "Skywood", "Windsweptwood", "Bluewood", "Frostwood"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Design catalogues — same style as FurnitureWindowCompat
    // ─────────────────────────────────────────────────────────────────────

    /** Torch / lantern design keys available in wood variants. */
    private static final String[] TORCH_DESIGNS = {
            "Furniture_Village_Torch",
            "Furniture_Village_Wall_Torch",
            "Furniture_Village_Lantern",
            "Furniture_Village_Sconce",
            "Furniture_Tavern_Torch",
            "Furniture_Tavern_Wall_Torch",
            "Furniture_Tavern_Lantern",
            "Furniture_Jungle_Torch",
            "Furniture_Jungle_Lantern",
            "Furniture_Human_Ruins_Torch",
            "Furniture_Human_Ruins_Lantern",
            "Furniture_Crude_Torch",
            "Furniture_Crude_Lantern",
            "Furniture_Ancient_Torch",
            "Furniture_Ancient_Lantern"
    };

    /** Sign design keys available in wood variants. */
    private static final String[] SIGN_DESIGNS = {
            "Furniture_Village_Sign",
            "Furniture_Village_Wall_Sign",
            "Furniture_Village_Hanging_Sign",
            "Furniture_Tavern_Sign",
            "Furniture_Tavern_Wall_Sign",
            "Furniture_Jungle_Sign",
            "Furniture_Human_Ruins_Sign",
            "Furniture_Crude_Sign",
            "Furniture_Ancient_Sign"
    };

    /** Shelf design keys available in wood variants. */
    private static final String[] SHELF_DESIGNS = {
            "Furniture_Village_Shelf",
            "Furniture_Village_Shelf_Corner",
            "Furniture_Village_Shelf_Small",
            "Furniture_Village_Shelf_Double",
            "Furniture_Tavern_Shelf",
            "Furniture_Tavern_Shelf_Corner",
            "Furniture_Jungle_Shelf",
            "Furniture_Human_Ruins_Shelf",
            "Furniture_Crude_Shelf",
            "Furniture_Ancient_Shelf"
    };

    /** Bed design keys available in wood variants. */
    private static final String[] BED_DESIGNS = {
            "Furniture_Village_Bed",
            "Furniture_Village_Bed_Head",
            "Furniture_Village_Bed_Foot",
            "Furniture_Village_Bed_Single",
            "Furniture_Village_Bed_Double",
            "Furniture_Tavern_Bed",
            "Furniture_Tavern_Bed_Head",
            "Furniture_Tavern_Bed_Foot",
            "Furniture_Jungle_Bed",
            "Furniture_Human_Ruins_Bed",
            "Furniture_Crude_Bed",
            "Furniture_Ancient_Bed"
    };

    /** Aures fence keys (no wood suffix – injected as one global hammer group). */
    private static final String[] AURES_FENCE_KEYS = {
            "Aures_Fence",
            "Aures_Fence_1", "Aures_Fence_2", "Aures_Fence_3", "Aures_Fence_4",
            "Aures_Fence_Center_High", "Aures_Fence_Center_Short",
            "Aures_Fence_Corner_1", "Aures_Fence_Corner_2",
            "Aures_Fence_Short_1", "Aures_Fence_Short_2"
    };

    /** Aures trough keys (no wood suffix – injected as one global hammer group). */
    private static final String[] AURES_TROUGH_KEYS = {
            "Aures_Small_Trough",
            "Aures_Small_Trough_1",  "Aures_Small_Trough_2",  "Aures_Small_Trough_3",
            "Aures_Small_Trough_4",  "Aures_Small_Trough_5",  "Aures_Small_Trough_6",
            "Aures_Small_Trough_7",  "Aures_Small_Trough_8",  "Aures_Small_Trough_9",
            "Aures_Small_Trough_10", "Aures_Small_Trough_11", "Aures_Small_Trough_12"
    };

    /** Modular Table design suffixes; wood type is the key prefix (e.g. Hardwood_Modular_Table). */
    private static final String[] MODULAR_TABLE_DESIGNS = {
            "Decorative_Modular_Table",
            "Ornate_Modular_Table",
            "Modular_Table"
    };

    private VanillaFurnitureCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    public static void init() {
        int total = 0;

        // ── Unified per-wood groups (all mods merged into one hammer) ─────
        for (String wood : WOOD_TYPES) {
            List<String> lights  = collectDesigns(TORCH_DESIGNS, wood);
            List<String> windows = collectDesigns(SIGN_DESIGNS, wood);
            List<String> storage = new ArrayList<>();
            storage.addAll(collectDesigns(SHELF_DESIGNS, wood));
            storage.addAll(collectDesigns(BED_DESIGNS, wood));
            storage.addAll(FemboyDelightCompat.collectStorage(wood));
            List<String> tables = new ArrayList<>();
            tables.addAll(collectDesigns(MODULAR_TABLE_DESIGNS, wood));
            tables.addAll(MacawFurnitureCompat.collectTables(wood));
            tables.addAll(FemboyDelightCompat.collectTables(wood));
            List<String> chairs = new ArrayList<>();
            chairs.addAll(MacawFurnitureCompat.collectChairs(wood));
            chairs.addAll(FemboyDelightCompat.collectChairs(wood));

            if (lights.isEmpty() && windows.isEmpty() && storage.isEmpty()
                    && tables.isEmpty() && chairs.isEmpty()) continue;

            String[] chairArr   = chairs.toArray(new String[0]);
            String[] tableArr   = tables.toArray(new String[0]);
            String[] storageArr = storage.toArray(new String[0]);
            String[] windowArr  = windows.toArray(new String[0]);
            String[] lightArr   = lights.toArray(new String[0]);

            CarpenterHammer.Data hammer = new CarpenterHammer.Data();
            hammer.source  = "Furniture_" + wood;
            hammer.chairs  = chairArr;
            hammer.tables  = tableArr;
            hammer.storage = storageArr;
            hammer.windows = windowArr;
            hammer.lights  = lightArr;

            // Signs also get a chisel to cycle between sign forms
            Chisel.Data signChisel = windows.isEmpty() ? null
                    : buildChiselData("Furniture_Sign_" + wood, windowArr);

            total += injectAll(lightArr,   hammer, null);
            total += injectAll(storageArr, hammer, null);
            total += injectAll(tableArr,   hammer, null);
            total += injectAll(chairArr,   hammer, null);
            total += injectAll(windowArr,  hammer, signChisel);

            // Inject hammer onto plank as revert target
            String plank = findPlankKey(wood);
            if (plank != null) {
                BlockType plankBt = BlockTypeCache.get(plank);
                if (plankBt != null) ComboStateHelper.inject(plankBt, null, null, hammer);
            }
        }

        // ── Global groups (Aures – no wood suffix) ───────────────────────
        total += injectGlobal(AURES_FENCE_KEYS,  "Aures_Fences",  Tab.WINDOWS);
        total += injectGlobal(AURES_TROUGH_KEYS, "Aures_Troughs", Tab.STORAGE);

        if (total > 0)
            LOGGER.atWarning().log("[VanillaFurnitureCompat] Injected onto " + total + " blocks.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Per-wood data collector + injector helpers
    // ─────────────────────────────────────────────────────────────────────

    private enum Tab { LIGHTS, STORAGE, WINDOWS, TABLES }

    /** Collects keys for {@code designs} × {@code wood} using the three-probe order. */
    private static List<String> collectDesigns(String[] designs, String wood) {
        List<String> found = new ArrayList<>();
        for (String design : designs) {
            String k1 = design + "_" + wood;
            if (exists(k1)) { found.add(k1); continue; }
            String k2 = wood + "_" + design;
            if (exists(k2)) { found.add(k2); continue; }
            if (exists(design)) found.add(design);
        }
        return found;
    }

    private static int injectAll(String[] keys, CarpenterHammer.Data hammer, Chisel.Data chisel) {
        int count = 0;
        for (String key : keys) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            if (ComboStateHelper.inject(bt, chisel, null, hammer)) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Global injector (fixed key list, no per-wood grouping)
    // ─────────────────────────────────────────────────────────────────────

    private static int injectGlobal(String[] keys, String source, Tab tab) {
        List<String> found = new ArrayList<>();
        for (String k : keys) if (exists(k)) found.add(k);
        if (found.isEmpty()) return 0;
        String[] arr = found.toArray(new String[0]);
        CarpenterHammer.Data hammer = buildHammer(source, arr, tab);
        int count = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            if (ComboStateHelper.inject(bt, null, null, hammer)) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Returns the first existing plank/base key for the given wood type. */
    private static String findPlankKey(String wood) {
        String[] candidates = {
            "Wood_" + wood + "_Planks",
            "Wood_" + wood + "_Plank",
            "Wood_" + wood
        };
        for (String k : candidates) if (exists(k)) return k;
        return null;
    }

    private static CarpenterHammer.Data buildHammer(String source, String[] arr, Tab tab) {
        CarpenterHammer.Data d = new CarpenterHammer.Data();
        d.source  = source;
        d.chairs  = new String[0];
        d.tables  = tab == Tab.TABLES   ? arr : new String[0];
        d.storage = tab == Tab.STORAGE  ? arr : new String[0];
        d.windows = tab == Tab.WINDOWS  ? arr : new String[0];
        d.lights  = tab == Tab.LIGHTS   ? arr : new String[0];
        return d;
    }

    private static Chisel.Data buildChiselData(String source, String[] subs) {
        Chisel.Data d = new Chisel.Data();
        d.source        = source;
        d.substitutions = subs;
        d.stairs        = new String[0];
        d.halfSlabs     = new String[0];
        d.roofing       = new String[0];
        return d;
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
