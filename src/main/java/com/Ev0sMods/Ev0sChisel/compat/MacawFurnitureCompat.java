package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

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
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    public static void init() {
        int total = 0;
        for (String wood : WOOD_TYPES) {
            List<String> chairs = new ArrayList<>();
            for (String design : CHAIR_DESIGNS) {
                String k = "Mcw_Furniture_" + wood + "_" + design;
                if (exists(k)) chairs.add(k);
            }

            List<String> tables = new ArrayList<>();
            for (String design : TABLE_DESIGNS) {
                String k = "Mcw_Furniture_" + wood + "_" + design;
                if (exists(k)) tables.add(k);
            }

            if (chairs.isEmpty() && tables.isEmpty()) continue;

            String[] chairArr = chairs.toArray(new String[0]);
            String[] tableArr = tables.toArray(new String[0]);

            CarpenterHammer.Data hammer = new CarpenterHammer.Data();
            hammer.source  = "Mcw_Furniture_" + wood;
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
            LOGGER.atWarning().log("[MacawFurnitureCompat] Injected CarpenterHammer.Data onto " + total + " blocks.");
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
