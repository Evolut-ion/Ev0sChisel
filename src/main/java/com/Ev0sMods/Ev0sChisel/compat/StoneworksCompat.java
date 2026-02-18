package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Compatibility layer for <b>Stoneworks Expanded</b> by LSChroma.
 * <p>
 * This is a pure asset pack (no Java plugin), so detection is done by
 * checking whether any of its block keys exist at runtime.  All 17
 * blocks are variants of {@code Rock_Stone}.
 */
public final class StoneworksCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Whether Stoneworks Expanded blocks were found in the asset registry. */
    private static boolean detected = false;

    /**
     * Every block key added by Stoneworks Expanded.
     * All are full-blocks belonging to the {@code Rock_Stone} set.
     */
    private static final String[] VARIANT_KEYS = {
            "Cobblestones",
            "CobblestonesCut1",
            "CobblestonesCut2",
            "CobblestonesCut3",
            "CobblestonesMossy",
            "EmbeddedCobblestones",
            "EmbeddedStonebricks",
            "EmbeddedStonebricksAged",
            "EmbeddedStonebricksMossy",
            "EmbeddedStonebricksSmallSmooth",
            "PillarBigCobblestonesCut1",
            "PillarBigCobblestonesCut2",
            "PillarBigStoneBrickSmoothCut",
            "Stonebricks",
            "StonebricksAged",
            "StonebricksMossy",
            "stonebricksSmallSmooth"
    };

    /** Unmodifiable view built once during {@link #init()}. */
    private static List<String> variantList = Collections.emptyList();

    private StoneworksCompat() {} // utility class

    // ─────────────────────────────────────────────────────────────────────
    // Initialisation – call once from plugin start() after assets load
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Checks whether Stoneworks Expanded is installed by looking up one of
     * its unique block keys.  If found, builds the variant list and
     * injects Chisel states onto every variant block.
     * <p>
     * Must be called <b>after</b> all asset packs have been loaded
     * (i.e. from {@code start()}, not {@code setup()}).
     */
    public static void init() {
        // Use a distinctive key for detection – "Cobblestones" is unique
        // enough (vanilla uses "Rock_Stone_Cobble", not "Cobblestones")
        try {
            if (BlockType.fromString("Cobblestones") != null) {
                detected = true;
                variantList = Collections.unmodifiableList(Arrays.asList(VARIANT_KEYS));
                LOGGER.atInfo().log("[Chisel] Stoneworks Expanded detected – "
                        + variantList.size() + " Rock_Stone variants loaded");
                injectChiselStates();
            } else {
                LOGGER.atInfo().log("[Chisel] Stoneworks Expanded not found – compat disabled");
            }
        } catch (Exception e) {
            LOGGER.atInfo().log("[Chisel] Stoneworks Expanded not found – compat disabled");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /** @return true when Stoneworks Expanded is installed */
    public static boolean isAvailable() {
        return detected;
    }

    /**
     * Returns the full-block variant keys added by Stoneworks Expanded.
     * All belong to the {@code Rock_Stone} family.
     */
    public static List<String> getVariants() {
        return variantList;
    }

    // ─────────────────────────────────────────────────────────────────────
    // State injection
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Reads the existing {@code Rock_Stone} Chisel substitutions, merges
     * with Stoneworks variants, and attaches a {@link Chisel.Data} to
     * each Stoneworks block.
     */
    private static void injectChiselStates() {
        // Read Rock_Stone's existing chisel data for the base substitution list
        String[] rockStoneSubs = getRockStoneSubstitutions();

        // Merge: existing Rock_Stone subs + Stoneworks variants
        LinkedHashSet<String> mergedBlocks = new LinkedHashSet<>();
        if (rockStoneSubs != null) Collections.addAll(mergedBlocks, rockStoneSubs);
        mergedBlocks.addAll(variantList);
        String[] mergedBlockArr = mergedBlocks.toArray(new String[0]);

        // Auto-derive stairs / halfs / roofing from the merged list
        String[] stairs  = MasonryCompat.deriveExistingVariants(mergedBlockArr, "_Stairs");
        String[] halfs   = MasonryCompat.deriveExistingVariants(mergedBlockArr, "_Half");
        String[] roofing = MasonryCompat.deriveExistingRoofing(mergedBlockArr);

        int injected = 0;
        int failed   = 0;

        for (String key : VARIANT_KEYS) {
            try {
                BlockType bt = BlockType.fromString(key);
                if (bt == null) { failed++; continue; }

                Chisel.Data data = new Chisel.Data();
                data.source        = "Stone";
                data.substitutions = mergedBlockArr;
                data.stairs        = stairs  != null ? stairs  : new String[0];
                data.halfSlabs     = halfs   != null ? halfs   : new String[0];
                data.roofing       = roofing != null ? roofing : new String[0];

                setField(StateData.class, data, "id", "Ev0sChisel");
                setField(BlockType.class, bt, "state", data);
                injected++;
            } catch (Exception e) {
                LOGGER.atWarning().log("[Chisel] Failed to inject state for "
                        + key + ": " + e.getMessage());
                failed++;
            }
        }

        LOGGER.atInfo().log("[Chisel] Injected Chisel state onto " + injected
                + " Stoneworks blocks" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    /** Reads the Chisel.Data substitutions already defined on Rock_Stone. */
    private static String[] getRockStoneSubstitutions() {
        try {
            BlockType rockStone = BlockType.fromString("Rock_Stone");
            if (rockStone == null) return null;
            StateData state = rockStone.getState();
            if (state instanceof Chisel.Data chiselData) {
                return chiselData.substitutions;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("[Chisel] Could not read Rock_Stone subs: " + e.getMessage());
        }
        return null;
    }

    /** Reflective field setter – handles protected / private fields. */
    private static void setField(Class<?> clazz, Object target,
                                 String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
