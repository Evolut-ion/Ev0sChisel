package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.ComboState;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Compatibility layer for <b>ESI.Chipped_StandardStonePack</b>.
 *
 * <p>Merges all 17 ESI stone variant blocks into the {@code Rock_Stone}
 * vanilla chisel family so players can chisel between them and every other
 * stone form.
 *
 * <p>Must be called after {@link VanillaCompat#injectChiselStates()} so the
 * {@code Rock_Stone} family data is already populated.
 */
public final class ChippedCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static boolean detected = false;

    private ChippedCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // ESI stone pack block keys (filename without .json)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] ESI_STONE_BLOCKS = {
            "ESI_Stone_Brick_Inset",
            "ESI_Stone_Brick_Intricate_Swirl",
            "ESI_Stone_Intricate_Diamond_Tile",
            "ESI_Stone_Intricate_Square_Tile",
            "ESI_Stone_Pattern_Celtic_Knot",
            "ESI_Stone_Pattern_Roman",
            "ESI_Stone_Pattern_Volute",
            "ESI_Stone_Pebble_Pathway",
            "ESI_Stone_Soft_Tile",
            "ESI_Stone_Symbol_Compass",
            "ESI_Stone_Symbol_Griffen",
            "ESI_Stone_Symbol_Plus",
            "ESI_Stone_Symbol_Sun",
            "ESI_Stone_Symbol_Swirl",
            "ESI_Stone_Tile_Octagon",
            "ESI_Stone_Worn_Grid_Tile",
            "ESI_Stone_Worn_Pathway"
    };

    public static boolean isDetected() { return detected; }

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Merges ESI stone blocks into the {@code Rock_Stone} vanilla chisel
     * family.  Existing family members have the ESI blocks appended to their
     * {@code substitutions}; the ESI blocks themselves receive a full
     * {@link Chisel.Data} so chiseling from them opens the stone picker.
     */
    public static void init() {
        try {
            // Collect ESI blocks that actually exist in this session
            List<String> esiKeys = new ArrayList<>();
            for (String key : ESI_STONE_BLOCKS) {
                if (exists(key)) esiKeys.add(key);
            }
            if (esiKeys.isEmpty()) return;

            // Read Rock_Stone's chisel data (built by VanillaCompat)
            BlockType stoneBt = BlockTypeCache.get("Rock_Stone");
            if (stoneBt == null) return;
            Chisel.Data stoneChisel = getChiselData(stoneBt);
            if (stoneChisel == null || stoneChisel.substitutions == null) return;

            // Build merged substitutions: full vanilla family + ESI blocks
            LinkedHashSet<String> mergedSet = new LinkedHashSet<>();
            Collections.addAll(mergedSet, stoneChisel.substitutions);
            mergedSet.addAll(esiKeys);
            String[] mergedArr = mergedSet.toArray(new String[0]);

            // Update every existing family member to include the ESI blocks
            int updated = 0;
            for (String member : stoneChisel.substitutions) {
                BlockType bt = BlockTypeCache.get(member);
                if (bt == null) continue;
                Chisel.Data cd = getChiselData(bt);
                if (cd == null) continue;
                cd.substitutions = mergedArr;
                updated++;
            }

            // Inject Chisel.Data onto the ESI blocks themselves
            String[] stairs  = stoneChisel.stairs    != null ? stoneChisel.stairs    : new String[0];
            String[] halfs   = stoneChisel.halfSlabs  != null ? stoneChisel.halfSlabs  : new String[0];
            String[] roofing = stoneChisel.roofing    != null ? stoneChisel.roofing    : new String[0];
            int injected = 0;
            for (String key : esiKeys) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                if (getChiselData(bt) != null) continue; // already has state

                Chisel.Data data = new Chisel.Data();
                data.source        = "Rock_Stone";
                data.substitutions = mergedArr;
                data.stairs        = stairs;
                data.halfSlabs     = halfs;
                data.roofing       = roofing;
                if (ComboStateHelper.inject(bt, data, null, null)) injected++;
            }

            if (updated > 0 || injected > 0) {
                detected = true;
                LOGGER.atWarning().log("[ChippedCompat] Merged " + esiKeys.size()
                        + " ESI stone blocks into Rock_Stone family ("
                        + updated + " members updated, " + injected + " ESI blocks injected).");
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[ChippedCompat] Init failed: " + t.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static Chisel.Data getChiselData(BlockType bt) {
        StateData s = bt.getState();
        if (s instanceof ComboState cs) return cs.chisel;
        if (s instanceof Chisel.Data cd) return cd;
        return null;
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
