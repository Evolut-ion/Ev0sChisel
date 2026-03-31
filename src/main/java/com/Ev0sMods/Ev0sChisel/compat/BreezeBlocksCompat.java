package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.util.*;

/**
 * Compatibility layer for <b>RedHadron's Breeze Blocks</b>
 * ({@code RedHadron.BreezeBlocks-*.zip}).
 *
 * <h3>Block pattern</h3>
 * <pre>{Material}_{SubType}_Breeze_{Design}</pre>
 * where:
 * <ul>
 *   <li>{@code Material} is {@code Wood_{WoodType}}, {@code Rock_{RockType}},
 *       or {@code Metal_{MetalType}}</li>
 *   <li>{@code Design} is one of the 9 design codes listed in
 *       {@link #BREEZE_DESIGNS}</li>
 * </ul>
 *
 * <h3>Carpenter's Hammer support</h3>
 * Each breeze block gets a {@link CarpenterHammer.Data} injected whose
 * {@code windows} array contains all other Breeze Blocks of the same
 * material sub-type (e.g. all {@code Rock_Stone_Breeze_*}).  This allows the
 * player to swap visual patterns using the Carpenter's Hammer.
 */
public final class BreezeBlocksCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static boolean detected = false;

    /** All design-name suffixes found in the mod. */
    private static final String[] BREEZE_DESIGNS = {
            "G1x1T4pHair",
            "G1x1T4pHead",
            "G2x2F2pB4pL2pHead",
            "G2x2F2pD4pDice",
            "G2x2T2pHead",
            "G2x2T2pNope",
            "G2x2T2pSlowNeckNeckSlow",
            "G4x4T1pVoid",
            "G4x4T2pVoid"
    };

    // ── Material families (reuse vanilla type lists) ──────────────────────
    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };
    private static final String[] ROCK_TYPES = {
            "Aqua", "Ash", "Basalt", "Calcite", "Chalk", "Clay_Brick",
            "Crystal_Cyan", "Crystal_Green", "Crystal_Pink", "Crystal_Yellow",
            "Ledge", "Ledge_Brick", "Lime", "Magma_Cooled", "Marble", "Peach",
            "Quartzite", "Sandstone", "Sandstone_Red", "Sandstone_White",
            "Snow", "Stone", "Gold", "Copper", "Bronze", "Iron", "Zinc"
    };
    private static final String[] METAL_TYPES = {
            "Gold", "Copper", "Bronze", "Iron", "Zinc"
    };

    private BreezeBlocksCompat() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean isAvailable() { return detected; }

    /**
     * Called once during {@code Ev0sChiselPlugin.start()}.
     * Discovers breeze-block keys and injects {@link CarpenterHammer.Data} onto them (Windows tab).
     */
    public static void init() {
        try {
            int count = 0;
            count += injectFamily("Wood", WOOD_TYPES);
            count += injectFamily("Rock", ROCK_TYPES);
            count += injectFamily("Metal", METAL_TYPES);
            if (count > 0) {
                detected = true;
                LOGGER.atWarning().log("[BreezeBlocksCompat] Injected CarpenterHammer.Data onto " + count + " Breeze Block variants (Windows tab).");
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[BreezeBlocksCompat] Init failed: " + t.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal – injection per material family
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * For each sub-type in {@code subTypes}, discovers all existing Breeze
     * Block variants and injects {@link CarpenterHammer.Data} onto them (Windows tab).
     *
     * @param prefix   "Wood", "Rock", or "Metal"
     * @param subTypes type names within that prefix (e.g. "Stone", "Hardwood")
     */
    private static int injectFamily(String prefix, String[] subTypes) {
        int count = 0;
        for (String sub : subTypes) {
            // Collect all existing designs for this sub-type
            List<String> variants = new ArrayList<>();
            for (String design : BREEZE_DESIGNS) {
                String key = prefix + "_" + sub + "_Breeze_" + design;
                if (BlockTypeCache.exists(key)) variants.add(key);
            }
            if (variants.isEmpty()) continue;

            String[] varArr = variants.toArray(new String[0]);
            String   source = prefix + "_" + sub + "_Breeze";

            // Inject CarpenterHammer.Data onto each discovered variant (Windows tab)
            for (String key : varArr) {
                try {
                    BlockType bt = BlockTypeCache.get(key);
                    if (bt == null) continue;
                    StateData existing = bt.getState();
                    if (existing instanceof CarpenterHammer.Data) continue; // already handled

                    CarpenterHammer.Data data = new CarpenterHammer.Data();
                    data.source  = source;
                    data.windows = varArr;
                    data.chairs  = new String[0];
                    data.tables  = new String[0];
                    data.storage = new String[0];
                    data.lights  = new String[0];

                    ReflectionCache.setField(StateData.class, data, "id", "Ev0sCarpenterHammer");
                    ReflectionCache.setField(BlockType.class, bt,   "state", data);
                    count++;
                } catch (Throwable t) {
                    LOGGER.atWarning().log("[BreezeBlocksCompat] Inject failed for " + key + ": " + t.getMessage());
                }
            }
        }
        return count;
    }
}
