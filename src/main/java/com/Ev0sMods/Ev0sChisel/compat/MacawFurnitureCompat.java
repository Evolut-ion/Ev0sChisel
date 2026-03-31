package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Carpenter's Hammer support for <b>Macaw's Furniture</b>
 * ({@code Mcw_Furniture_*}).
 *
 * <h3>Grouping</h3>
 * One hammer group per wood type.  Each group populates:
 * <ul>
 *   <li><b>chairs</b> — {@code Mcw_Furniture_{Wood}_{Back_Rest_Chair|Back_Rest_Stool|Chair|Cottage_Stool}}</li>
 *   <li><b>tables</b> — {@code Mcw_Furniture_{Wood}_{T_Desk|Desk|Counter|Counter_Sink}}</li>
 * </ul>
 * All keys are runtime-probed; absent variants are silently skipped.
 */
public final class MacawFurnitureCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private MacawFurnitureCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };

    private static final String[] CHAIR_DESIGNS = {
            "Back_Rest_Chair", "Back_Rest_Stool", "Chair", "Cottage_Stool"
    };

    private static final String[] TABLE_DESIGNS = {
            "T_Desk", "Desk", "Counter", "Counter_Sink"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Data providers — called by VanillaFurnitureCompat's unified per-wood loop
    // ─────────────────────────────────────────────────────────────────────

    static List<String> collectChairs(String wood) {
        List<String> list = new ArrayList<>();
        for (String design : CHAIR_DESIGNS) {
            String k = "Mcw_Furniture_" + wood + "_" + design;
            if (exists(k)) list.add(k);
        }
        return list;
    }

    static List<String> collectTables(String wood) {
        List<String> list = new ArrayList<>();
        for (String design : TABLE_DESIGNS) {
            String k = "Mcw_Furniture_" + wood + "_" + design;
            if (exists(k)) list.add(k);
        }
        return list;
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
