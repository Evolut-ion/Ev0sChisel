package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Carpenter's Hammer + Paintbrush support for <b>Femboy's Delight</b>
 * ({@code N1F_*}).
 *
 * <h3>Carpenter's Hammer — per wood type (via VanillaFurnitureCompat loop)</h3>
 * <ul>
 *   <li><b>chairs</b>  — dining chairs + offset variants</li>
 *   <li><b>tables</b>  — outdoor tables + office desks</li>
 *   <li><b>storage</b> — kitchen cabinets, vent hoods, counters</li>
 * </ul>
 *
 * <h3>Carpenter's Hammer — global groups (via init())</h3>
 * <ul>
 *   <li>Industrial kitchen counters (storage)</li>
 *   <li>Bathroom furniture (storage)</li>
 *   <li>Video-game consoles (storage)</li>
 *   <li>Gym weights (storage)</li>
 *   <li>Industrial lights + cabinet lights + pole (lights)</li>
 *   <li>City road signs, symbols, and lines (windows)</li>
 * </ul>
 *
 * <h3>Paintbrush groups</h3>
 * <ul>
 *   <li>Couches — one group per shape type cycling colors</li>
 *   <li>Sectionals + pillows — one group per shape cycling colors</li>
 *   <li>Retro bar stools — single group of all color variants</li>
 *   <li>Wallpaper base colors — single group of all 15 base colors</li>
 * </ul>
 */
public final class FemboyDelightCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FemboyDelightCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Wood types (Femboy adds Pinkwood / Skywood / Windsweptwood / Bluewood / Frostwood
    // beyond the vanilla 12 — used here for documentation; VanillaFurnitureCompat's
    // WOOD_TYPES array is the authoritative driver of the per-wood loop)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood",  "Lightwood","Redwood",  "Softwood","Tropicalwood",
            "Whitewood", "Goldenwood",
            "Pinkwood",  "Skywood",  "Windsweptwood", "Bluewood", "Frostwood"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Per-wood storage: kitchen cabinets + counters
    // ─────────────────────────────────────────────────────────────────────

    /** Cabinet design prefixes; actual key = "{design}_{Wood}". */
    private static final String[] CABINET_DESIGNS = {
            "N1F_Kitchen_Cabinet1",   "N1F_Kitchen_Cabinet2",  "N1F_Kitchen_Cabinet3",
            "N1F_Kitchen_Cabinet4",   "N1F_Kitchen_Cabinet6",  "N1F_Kitchen_Cabinet7",
            "N1F_Kitchen_CabinetDoL", "N1F_Kitchen_CabinetDoR",
            "N1F_Kitchen_CabinetEalt","N1F_Kitchen_CabinetVent"
    };

    /** Counter designs that use ONLY a wood suffix (no stone). */
    private static final String[] COUNTER_WOOD_DESIGNS = {
            "N1F_Kitchen_CounterDaL",  "N1F_Kitchen_CounterDaR",
            "N1F_Kitchen_CounterDish", "N1F_Kitchen_CounterEalt",
            "N1F_Kitchen_CounterOFL",  "N1F_Kitchen_CounterOFR"
    };

    /**
     * Counter designs that MAY have an optional stone suffix.
     * Probed as "{design}_{Wood}_{Stone}" for each stone, then bare "{design}_{Wood}".
     */
    private static final String[] COUNTER_STONE_DESIGNS = {
            "N1F_Kitchen_Counter1", "N1F_Kitchen_Counter2", "N1F_Kitchen_Counter3",
            "N1F_Kitchen_Counter4", "N1F_Kitchen_Counter5", "N1F_Kitchen_Counter6",
            "N1F_Kitchen_Counter_Open", "N1F_Kitchen_Counter_Sink", "N1F_Kitchen_Counter_Sink2"
    };

    /** Stone suffixes probed in order; last entry "" = bare (wood-only) key. */
    private static final String[] STONE_SUFFIXES = {
            "_Marble", "_Basalt", "_Shale", "_Sandstone", ""
    };

    // ─────────────────────────────────────────────────────────────────────
    // Desk / table design prefixes (per-wood)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] TABLE_DESIGNS = {
            "Outdoor_Table",
            "Office_Desk10", "Office_Desk20", "Office_Desk30",
            "Office_DeskM10", "Office_DeskM11",
            "Office_DeskM20", "Office_DeskM21",
            "Office_DeskM30", "Office_DeskM31",
            "Office_DeskM40", "Office_DeskM41", "Office_DeskM42",
            "Office_DeskM43", "Office_DeskM44", "Office_DeskM45"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Global key lists (no wood suffix)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] INDUSTRIAL_COUNTER_KEYS = {
            "N1F_Kitchen_Counter_Industrial1",      "N1F_Kitchen_Counter_Industrial1_Open",
            "N1F_Kitchen_Counter_Industrial2",      "N1F_Kitchen_Counter_Industrial2_Open"
    };

    private static final String[] BATHROOM_KEYS = {
            "N1F_Bathroom_Bathtub",          "N1F_Bathroom_Bathtub2",
            "N1F_Bathroom_Shower_Glass",
            "N1F_Bathroom_Shower_Head",      "N1F_Bathroom_Shower_Head_Wall",
            "N1F_Bathroom_Toilet_Porcelain1","N1F_Bathroom_Toilet_Porcelain2",
            "N1F_Bathroom_Toiletpaper_Holder1","N1F_Bathroom_Toiletpaper_Holder2",
            "N1F_Bathroom_Toiletpaper_Holder3",
            "N1F_Mirror","N1F_Mirror_Block"
    };

    private static final String[] CONSOLE_KEYS = {
            // Nintendo
            "N1F_Console_Nintendo_CGen0310","N1F_Console_Nintendo_CGen0311",
            "N1F_Console_Nintendo_CGen0320","N1F_Console_Nintendo_CGen0321",
            "N1F_Console_Nintendo_CGen0410","N1F_Console_Nintendo_CGen0420",
            "N1F_Console_Nintendo_CGen0510","N1F_Console_Nintendo_CGen0610",
            "N1F_Console_Nintendo_CGen0710","N1F_Console_Nintendo_CGen0711",
            "N1F_Console_Nintendo_CGen0712","N1F_Console_Nintendo_CGen0713",
            "N1F_Console_Nintendo_CGen0714",
            "N1F_Console_Nintendo_CGen0720","N1F_Console_Nintendo_CGen0721",
            "N1F_Console_Nintendo_CGen0722","N1F_Console_Nintendo_CGen0723",
            "N1F_Console_Nintendo_CGen0830","N1F_Console_Nintendo_CGen0831",
            "N1F_Console_Nintendo_CGen0832","N1F_Console_Nintendo_CGen0833",
            "N1F_Console_Nintendo_CGen0834","N1F_Console_Nintendo_CGen0835",
            "N1F_Console_Nintendo_CGen0836","N1F_Console_Nintendo_CGen0837",
            // Playstation
            "N1F_Console_Playstation_CGen0510","N1F_Console_Playstation_CGen0520",
            "N1F_Console_Playstation_CGen0610","N1F_Console_Playstation_CGen0620",
            "N1F_Console_Playstation_CGen0630","N1F_Console_Playstation_CGen0640",
            "N1F_Console_Playstation_CGen0810","N1F_Console_Playstation_CGen0811",
            "N1F_Console_Playstation_CGen0820","N1F_Console_Playstation_CGen0830",
            "N1F_Console_Playstation_CGen0840","N1F_Console_Playstation_CGen0841",
            "N1F_Console_Playstation_CGen0850","N1F_Console_Playstation_CGen0860",
            // Sega
            "N1F_Console_Sega_CGen0310","N1F_Console_Sega_CGen0610",
            // Xbox
            "N1F_Console_Xbox_CGen0610",
            "N1F_Console_Xbox_CGen0910","N1F_Console_Xbox_CGen0911",
            "N1F_Console_Xbox_CGen0920","N1F_Console_Xbox_CGen0921",
            // PC & Arcade
            "N1F_Gaming_Tower_PC01","N1F_Frat_Arcade_Cabinet1"
    };

    private static final String[] WEIGHT_KEYS = {
            "N1F_Frat_Weight1","N1F_Frat_Weight1_Double",
            "N1F_Frat_Weight2","N1F_Frat_Weight2_Double",
            "N1F_Frat_Weight3","N1F_Frat_Weight3_Double",
            "N1F_Frat_Weight4","N1F_Frat_Weight4_Double"
    };

    /** Industrial lights including the pole, all 5 industrial shapes, all cabinet lights, and hanging lights. */
    private static final String[] INDUSTRIAL_LIGHT_KEYS;
    static {
        List<String> keys = new ArrayList<>();
        keys.add("N1F_Lights_Industrial_Light_Pole");
        for (int i = 1; i <= 5; i++) keys.add("N1F_Lights_Industrial" + i + "_Light");
        String[] cabColors = {"Blue","Green","Neutral","Orange","Pink","Purple","Red","Warm","Yellow"};
        for (int i = 1; i <= 9; i++) {
            for (String c : cabColors) keys.add("N1F_Lights_Kitchen_Cabinetlight" + i + "_" + c);
        }
        keys.add("N1F_Lights_Kitchen_Hanginglight1_Neutral");
        keys.add("N1F_Lights_Kitchen_Hanginglight1_Warm");
        keys.add("N1F_Lights_Kitchen_Hanginglight2_Neutral");
        keys.add("N1F_Lights_Kitchen_Hanginglight2_Warm");
        INDUSTRIAL_LIGHT_KEYS = keys.toArray(new String[0]);
    }

    /** Road signs, road symbols (normal + diagonal), and all road line markings. */
    private static final String[] STREET_SIGN_KEYS;
    static {
        List<String> keys = new ArrayList<>();
        keys.add("N1F_City_Signs_Stop");
        String[] symbols = {
                "Handicapped","Left","Left_Keep","LeftRight","Noparking",
                "Right","Right_Keep","Straight","Straight_Left","Straight_Left_Right",
                "Straight_Right","Uturn","UturnR"
        };
        for (String s : symbols) keys.add("N1F_City_Symbols_" + s);
        for (String s : symbols) keys.add("N1F_City_Symbols_Diagonal_" + s);
        String[] lineColors = {"Blue","Green","Purple","Red","White","Yellow"};
        String[] lineTypes  = {
                "N1F_City_StraightDLBline32x8",
                "N1F_City_Straight_Edge32x8",
                "N1F_City_Straightline32x8",
                "N1F_City_TJunction32x8",
                "N1F_City_TJunction_Edge32x8"
        };
        for (String lt : lineTypes) for (String c : lineColors) keys.add(lt + "_" + c);
        keys.add("N1F_City_Straightline32x8_Diagonal_Yellow");
        keys.add("N1F_City_Straightline32x8_White_Half");
        keys.add("N1F_City_Straightline32x8_Yellow_Half");
        STREET_SIGN_KEYS = keys.toArray(new String[0]);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Paintbrush constant lists
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] COUCH_TYPES = {
            "Chair1", "Loveseat1", "Wide1", "Wide2", "Wide3"
    };

    private static final String[] COUCH_COLORS = {
            "Blue", "Brown", "Cream", "Green", "Red", "Rose", "Yellow",
            "White", "Black", "Orange", "Purple", "Pink", "Cyan",
            "Gray", "Light_Gray", "Lime", "Magenta", "Light_Blue"
    };

    private static final String[] SECTIONAL_SHAPES = {
            "Arm1_Left","Arm1_Right","Arm2_Left","Arm2_Right",
            "Center1","Center2","Chair","Corner1","Corner2",
            "Ottoman1","Ottoman2","Pillow1","Pillow2","Pillow3"
    };

    private static final String[] SECTIONAL_COLORS = {
            "Black","Blue","Brown","Cyan","Gray","Green",
            "Orange","Pink","Purple","Red","White","Yellow"
    };

    private static final String[] BARSTOOL_KEYS = {
            "N1F_Kitchen_Retro_Barstool_Black",
            "N1F_Kitchen_Retro_Barstool_Blue",        "N1F_Kitchen_Retro_Barstool_Blue_Light",
            "N1F_Kitchen_Retro_Barstool_Brown",       "N1F_Kitchen_Retro_Barstool_Brown_Light",
            "N1F_Kitchen_Retro_Barstool_Cyan",        "N1F_Kitchen_Retro_Barstool_Cyan_Light",
            "N1F_Kitchen_Retro_Barstool_Gray",        "N1F_Kitchen_Retro_Barstool_Gray_Light",
            "N1F_Kitchen_Retro_Barstool_Green",       "N1F_Kitchen_Retro_Barstool_Green_Light",
            "N1F_Kitchen_Retro_Barstool_Orange",      "N1F_Kitchen_Retro_Barstool_Orange_Light",
            "N1F_Kitchen_Retro_Barstool_Pink",        "N1F_Kitchen_Retro_Barstool_Pink_Light",
            "N1F_Kitchen_Retro_Barstool_Purple",      "N1F_Kitchen_Retro_Barstool_Purple_Light",
            "N1F_Kitchen_Retro_Barstool_Red",
            "N1F_Kitchen_Retro_Barstool_White",
            "N1F_Kitchen_Retro_Barstool_Yellow",      "N1F_Kitchen_Retro_Barstool_Yellow_Light"
    };

    private static final String[] WALLPAPER_BASE_KEYS = {
            "N1F_Wallpaper_Block_Black",   "N1F_Wallpaper_Block_Blue",
            "N1F_Wallpaper_Block_Brown",   "N1F_Wallpaper_Block_Cyan",
            "N1F_Wallpaper_Block_Gray",    "N1F_Wallpaper_Block_Green",
            "N1F_Wallpaper_Block_Light_Blue","N1F_Wallpaper_Block_Lime",
            "N1F_Wallpaper_Block_Magenta", "N1F_Wallpaper_Block_Orange",
            "N1F_Wallpaper_Block_Pink",    "N1F_Wallpaper_Block_Purple",
            "N1F_Wallpaper_Block_Red",     "N1F_Wallpaper_Block_White",
            "N1F_Wallpaper_Block_Yellow"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Data providers — called by VanillaFurnitureCompat's unified per-wood loop
    // ─────────────────────────────────────────────────────────────────────

    static List<String> collectChairs(String wood) {
        List<String> list = new ArrayList<>();
        String chair    = "N1F_Dining_Chair_" + wood;
        String chairOff = "N1F_Dining_Chair_" + wood + "_Offset";
        if (exists(chair))    list.add(chair);
        if (exists(chairOff)) list.add(chairOff);
        return list;
    }

    static List<String> collectTables(String wood) {
        List<String> list = new ArrayList<>();
        for (String design : TABLE_DESIGNS) {
            String k = "N1F_" + design + "_" + wood;
            if (exists(k)) list.add(k);
        }
        return list;
    }

    /**
     * Collects kitchen cabinets, vent hoods, and counter variants for the given wood type.
     * Called by VanillaFurnitureCompat's unified per-wood loop (storage tab).
     */
    static List<String> collectStorage(String wood) {
        List<String> list = new ArrayList<>();
        // Cabinets (including vent hood)
        for (String design : CABINET_DESIGNS) {
            String k = design + "_" + wood;
            if (exists(k)) list.add(k);
        }
        // Counters — wood suffix only
        for (String design : COUNTER_WOOD_DESIGNS) {
            String k = design + "_" + wood;
            if (exists(k)) list.add(k);
        }
        // Counters — optional stone suffix (probe each stone then bare)
        for (String design : COUNTER_STONE_DESIGNS) {
            for (String stone : STONE_SUFFIXES) {
                String k = design + "_" + wood + stone;
                if (exists(k)) { list.add(k); break; }
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Global hammer groups (invoked once from Ev0sChiselPlugin.start())
    // ─────────────────────────────────────────────────────────────────────

    public static void init() {
        int total = 0;
        total += injectGlobal(INDUSTRIAL_COUNTER_KEYS, "N1F_Kitchen_Industrial_Counters", Tab.STORAGE);
        total += injectGlobal(BATHROOM_KEYS,           "N1F_Bathroom",                   Tab.STORAGE);
        total += injectGlobal(CONSOLE_KEYS,            "N1F_Consoles",                   Tab.STORAGE);
        total += injectGlobal(WEIGHT_KEYS,             "N1F_Frat_Weights",               Tab.STORAGE);
        total += injectGlobal(INDUSTRIAL_LIGHT_KEYS,   "N1F_Industrial_Lights",          Tab.LIGHTS);
        total += injectGlobal(STREET_SIGN_KEYS,        "N1F_City_Signs_Symbols",         Tab.WINDOWS);
        if (total > 0)
            LOGGER.atWarning().log("[FemboyDelightCompat] Injected global groups onto " + total + " blocks.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Paintbrush states (invoked once from Ev0sChiselPlugin.start())
    // ─────────────────────────────────────────────────────────────────────

    public static void injectPaintbrushStates() {
        // Couches (5 shape types × up to 18 colors)
        for (String type : COUCH_TYPES) {
            List<String> found = new ArrayList<>();
            for (String color : COUCH_COLORS) {
                String k = "N1F_Living_Couch_" + type + "_" + color;
                if (exists(k)) found.add(k);
            }
            injectPaintbrushGroup(found, "N1F_Couch_" + type);
        }
        // Sectionals + pillows (14 shape variants × 12 colors)
        for (String shape : SECTIONAL_SHAPES) {
            List<String> found = new ArrayList<>();
            for (String color : SECTIONAL_COLORS) {
                String k = "N1F_Living_Sectionals_" + shape + "_" + color;
                if (exists(k)) found.add(k);
            }
            injectPaintbrushGroup(found, "N1F_Sectional_" + shape);
        }
        // Retro bar stools (all color variants in one group)
        List<String> stools = new ArrayList<>();
        for (String k : BARSTOOL_KEYS) if (exists(k)) stools.add(k);
        injectPaintbrushGroup(stools, "N1F_Retro_Barstools");
        // Wallpaper base colors (15 colors, no material variants)
        List<String> wallpapers = new ArrayList<>();
        for (String k : WALLPAPER_BASE_KEYS) if (exists(k)) wallpapers.add(k);
        injectPaintbrushGroup(wallpapers, "N1F_Wallpaper_Base");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private enum Tab { STORAGE, LIGHTS, WINDOWS }

    private static int injectGlobal(String[] keys, String source, Tab tab) {
        List<String> found = new ArrayList<>();
        for (String k : keys) if (exists(k)) found.add(k);
        if (found.isEmpty()) return 0;
        String[] arr = found.toArray(new String[0]);
        CarpenterHammer.Data hammer = new CarpenterHammer.Data();
        hammer.source  = source;
        hammer.chairs  = new String[0];
        hammer.tables  = new String[0];
        hammer.storage = tab == Tab.STORAGE ? arr : new String[0];
        hammer.windows = tab == Tab.WINDOWS ? arr : new String[0];
        hammer.lights  = tab == Tab.LIGHTS  ? arr : new String[0];
        int count = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            if (ComboStateHelper.inject(bt, null, null, hammer)) count++;
        }
        return count;
    }

    private static void injectPaintbrushGroup(List<String> found, String source) {
        if (found.isEmpty()) return;
        String[] variants = found.toArray(new String[0]);
        int tc = 0;
        for (String key : variants) {
            try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                StateData existing = bt.getState();
                if (existing instanceof Paintbrush.Data) continue;
                Paintbrush.Data data = new Paintbrush.Data();
                data.source        = source;
                data.colorVariants = variants;
                ReflectionCache.setField(StateData.class, data, "id", "Ev0sPaintbrush");
                ReflectionCache.setField(BlockType.class, bt,   "state", data);
                tc++;
            } catch (Throwable t) {
                LOGGER.atWarning().log("[FemboyDelightCompat] Paintbrush inject failed for "
                        + key + ": " + t.getMessage());
            }
        }
        if (tc > 0)
            LOGGER.atWarning().log("[FemboyDelightCompat] Injected Paintbrush.Data onto " + tc
                    + " " + source + " blocks.");
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
