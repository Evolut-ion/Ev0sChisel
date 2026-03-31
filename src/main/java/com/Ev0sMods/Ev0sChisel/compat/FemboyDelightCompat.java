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
 * <h3>Carpenter's Hammer — per wood type</h3>
 * <ul>
 *   <li><b>chairs</b> — {@code N1F_Dining_Chair_{Wood}} and {@code N1F_Dining_Chair_{Wood}_Offset}</li>
 *   <li><b>tables</b> — {@code N1F_{DesignPrefix}_{Wood}} for all desk / table variants</li>
 * </ul>
 *
 * <h3>Paintbrush — couch color groups</h3>
 * One {@link Paintbrush.Data} group per couch type
 * ({@code Chair1, Loveseat1, Wide1, Wide2, Wide3}) cycling all found color variants
 * of {@code N1F_Living_Couch_{Type}_{Color}}.
 */
public final class FemboyDelightCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FemboyDelightCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };

    /** Desk / table design prefixes; key = "N1F_{design}_{wood}". */
    private static final String[] TABLE_DESIGNS = {
            "Outdoor_Table",
            "Office_Desk10", "Office_Desk20", "Office_Desk30",
            "Office_DeskM10", "Office_DeskM11",
            "Office_DeskM20", "Office_DeskM21",
            "Office_DeskM30", "Office_DeskM31",
            "Office_DeskM40", "Office_DeskM41", "Office_DeskM42",
            "Office_DeskM43", "Office_DeskM44", "Office_DeskM45"
    };

    private static final String[] COUCH_TYPES = {
            "Chair1", "Loveseat1", "Wide1", "Wide2", "Wide3"
    };

    /** Color names probed for living-couch variants. */
    private static final String[] COUCH_COLORS = {
            "Blue", "Brown", "Cream", "Green", "Red", "Rose", "Yellow",
            "White", "Black", "Orange", "Purple", "Pink", "Cyan",
            "Gray", "Light_Gray", "Lime", "Magenta", "Light_Blue"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Carpenter's Hammer
    // ─────────────────────────────────────────────────────────────────────

    public static void init() {
        int total = 0;
        for (String wood : WOOD_TYPES) {
            List<String> chairs = new ArrayList<>();
            String chair    = "N1F_Dining_Chair_" + wood;
            String chairOff = "N1F_Dining_Chair_" + wood + "_Offset";
            if (exists(chair))    chairs.add(chair);
            if (exists(chairOff)) chairs.add(chairOff);

            List<String> tables = new ArrayList<>();
            for (String design : TABLE_DESIGNS) {
                String k = "N1F_" + design + "_" + wood;
                if (exists(k)) tables.add(k);
            }

            if (chairs.isEmpty() && tables.isEmpty()) continue;

            String[] chairArr = chairs.toArray(new String[0]);
            String[] tableArr = tables.toArray(new String[0]);

            CarpenterHammer.Data hammer = new CarpenterHammer.Data();
            hammer.source  = "N1F_Furniture_" + wood;
            hammer.chairs  = chairArr;
            hammer.tables  = tableArr;
            hammer.storage = new String[0];
            hammer.windows = new String[0];
            hammer.lights  = new String[0];

            for (String key : chairArr) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                if (ComboStateHelper.inject(bt, null, null, hammer)) total++;
            }
            for (String key : tableArr) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                if (ComboStateHelper.inject(bt, null, null, hammer)) total++;
            }
        }
        if (total > 0)
            LOGGER.atWarning().log("[FemboyDelightCompat] Injected CarpenterHammer.Data onto " + total + " blocks.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Paintbrush — living couch color cycling
    // ─────────────────────────────────────────────────────────────────────

    public static void injectPaintbrushStates() {
        int total = 0;
        for (String type : COUCH_TYPES) {
            List<String> found = new ArrayList<>();
            for (String color : COUCH_COLORS) {
                String k = "N1F_Living_Couch_" + type + "_" + color;
                if (exists(k)) found.add(k);
            }
            if (found.isEmpty()) continue;

            String[] variants = found.toArray(new String[0]);
            int tc = 0;
            for (String key : variants) {
                try {
                    BlockType bt = BlockTypeCache.get(key);
                    if (bt == null) continue;
                    StateData existing = bt.getState();
                    if (existing instanceof Paintbrush.Data) continue;

                    Paintbrush.Data data = new Paintbrush.Data();
                    data.source        = "N1F_Couch_" + type;
                    data.colorVariants = variants;

                    ReflectionCache.setField(StateData.class, data, "id", "Ev0sPaintbrush");
                    ReflectionCache.setField(BlockType.class, bt,   "state", data);
                    tc++;
                } catch (Throwable t) {
                    LOGGER.atWarning().log("[FemboyDelightCompat] Couch paintbrush inject failed for "
                            + key + ": " + t.getMessage());
                }
            }
            if (tc > 0)
                LOGGER.atWarning().log("[FemboyDelightCompat] Injected Paintbrush.Data onto " + tc
                        + " " + type + " couch blocks.");
            total += tc;
        }
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
