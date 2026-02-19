package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Injects {@link Chisel.Data} onto vanilla Hytale blocks that belong to
 * rock, soil, or wood families but do not yet have chisel state data.
 * <p>
 * This runs <em>after</em> MasonryCompat, CarpentryCompat, MacawCompat, and
 * StoneworksCompat so that any blocks already handled by those passes are
 * skipped (the guard {@code if (existing instanceof Chisel.Data) continue;}
 * prevents double-injection).
 * <p>
 * All block keys are probed via {@link BlockType#fromString(String)} at
 * runtime; nothing is injected for keys that do not exist in the asset pack.
 */
public final class VanillaCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ── Rock families ────────────────────────────────────────────────────
    // These match MasonryCompat.STONE_TYPES so masonry variants are merged in.
    private static final String[] ROCK_TYPES = {
            "Aqua", "Ash", "Basalt", "Calcite", "Chalk", "Clay_Brick",
            "Crystal_Cyan", "Crystal_Green", "Crystal_Pink", "Crystal_Yellow",
            "Dirt", "Marble", "Lime", "Sandstone", "Sandstone_Red", "Sandstone_White",
            "Snow", "Stone"
    };

    /**
     * Natural (non-masonry-mod) suffixes to probe for each rock type.
     * e.g. "Rock_Aqua", "Rock_Aqua_Cobble", "Rock_Aqua_Polished", …
     */
    private static final String[] ROCK_NATURAL_SUFFIXES = {
            "", "_Cobble", "_Polished",
            "_Brick", "_Bricks",
            "_Tile",  "_Tiles",
            "_Slab",  "_Slabs",
            "_Cracked", "_Cracked_Bricks",
            "_Mossy", "_Mossy_Bricks", "_Mossy_Cobble",
            "_Chiseled", "_Smooth", "_Cut", "_Ornate", "_Decorative",
            "_Pillar"
    };

    // ── Soil families ────────────────────────────────────────────────────
    // Each inner array is one chisel group.  Members are probed; missing
    // entries are silently skipped.  Dirt and Snow are intentionally omitted
    // here because MasonryCompat already lists them as stone types.
    private static final String[][] SOIL_FAMILIES = {
            // Raw / unfired clay (Clay_Brick is the fired variant, handled by Masonry)
            { "Clay", "Clay_Packed", "Clay_Smooth" },
            // Sand family
            { "Sand", "Sand_Red", "Sand_White", "Sand_Black" },
            // Gravel
            { "Gravel", "Gravel_Dark", "Gravel_Fine" },
            // Mud variants
            { "Mud", "Mud_Packed", "Mud_Dried" },
            // Peat
            { "Peat", "Peat_Dry" },
            // Silt
            { "Silt", "Silt_Packed" },
            // Tundra / permafrost soils
            { "Permafrost", "Permafrost_Cracked" },
    };

    // ── Wood families ────────────────────────────────────────────────────
    // Matches CarpentryCompat.WOOD_TYPES – this path is used regardless of
    // whether Ymmersive Carpentry is installed.
    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Goldenwood",
            "Greenwood", "Hardwood", "Lightwood", "Redwood", "Softwood",
            "Tropicalwood"
    };

    /**
     * All vanilla wood block form suffixes to probe for each wood type.
     * Every entry is tested via {@link BlockType#fromString} at runtime;
     * non-existent keys are silently skipped, so this list can be broad.
     */
    private static final String[] VANILLA_WOOD_SUFFIXES = {
            // ── Core forms ───────────────────────────────────────────────
            "",
            "_Planks",
            // ── Log / trunk variants ─────────────────────────────────────
            "_Log",
            "_Log_Horizontal",
            "_Log_Corner",
            "_Stripped_Log",
            "_Stripped_Log_Horizontal",
            "_Stripped_Log_Corner",
            // ── Bark / outer shell ───────────────────────────────────────
            "_Bark",
            "_Stripped_Bark",
            "_Stripped",
            // ── Structural / decorative ──────────────────────────────────
            "_Beam",
            "_Beam_Horizontal",
            "_Beam_Corner",
            "_Post",
            "_Panel",
            "_Frame",
            "_Boards",
            "_Bundle",
            "_Pile",
            "_Lattice",
            "_Trellis",
            // ── Leaves / foliage ─────────────────────────────────────────
            "_Leaves",
            "_Leaves_Fancy",
            "_Leaves_Fallen",
            // ── Functional ───────────────────────────────────────────────
            "_Door",
            "_Trapdoor",
            "_Fence",
            "_Fence_Gate",
            "_Gate",
            "_Pressure_Plate",
            "_Button",
            "_Sign",
            "_Wall_Sign",
            "_Chest",
            // ── Slab / stair standalone forms ────────────────────────────
            // (planks-based stairs/halfs are also derived dynamically from
            //  "_Planks" key via deriveExistingVariants, but explicit entries
            //  here catch any that use a different naming scheme)
            "_Planks_Stairs",
            "_Planks_Half",
            "_Decorative",
            "_Ornate",
            "_Planks_Slab",
            "_Log_Stairs",
            "_Log_Half",
            "_Log_Slab",
            "_Wood",
            // ── Roof / shingle forms ──────────────────────────────────────
            // Listed explicitly here so they are discovered as primary keys
            // even when the bare base block ("Wood_<Type>") does not exist.
            "_Roof",
            "_Roof_Flat",
            "_Roof_Hollow",
            "_Roof_Shallow",
            "_Roof_Steep",
            "_Shingle",
            "_Shingle_Flat",
            "_Shingle_Hollow",
            "_Shingle_Shallow",
            "_Shingle_Steep",
            "_Railing"
    };

    /**
     * Roofing suffixes specific to wood blocks.
     * Wood uses shingle-style roofing ({@code _Shingle*}) in addition to
     * the standard stone {@code _Roof*} variants.
     */
    private static final String[] WOOD_ROOF_SUFFIXES = {
            "_Roof", "_Roof_Flat", "_Roof_Hollow", "_Roof_Shallow", "_Roof_Steep",
            "_Shingle", "_Shingle_Flat", "_Shingle_Hollow", "_Shingle_Shallow", "_Shingle_Steep"
    };

    private VanillaCompat() {} // utility class

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Probes the runtime asset registry and injects {@link Chisel.Data} onto
     * any discovered vanilla rock, soil, or wood block that does not already
     * carry chisel state data.
     * <p>
     * Must be called from {@code Ev0sChiselPlugin.start()} <em>after</em>
     * all other compat passes.
     */
    public static void injectChiselStates() {
        int total = 0;
        total += injectRockFamilies();
        total += injectSoilFamilies();
        total += injectWoodFamilies();
        LOGGER.atInfo().log("[Chisel] VanillaCompat: injected Chisel.Data onto "
                + total + " vanilla block(s)");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rock injection
    // ─────────────────────────────────────────────────────────────────────

    private static int injectRockFamilies() {
        int count = 0;
        for (String rockType : ROCK_TYPES) {
            // ── Discover all existing vanilla (natural) forms ─────────────
            List<String> naturalKeys = new ArrayList<>();
            for (String suffix : ROCK_NATURAL_SUFFIXES) {
                String candidate = "Rock_" + rockType + suffix;
                if (exists(candidate)) naturalKeys.add(candidate);
            }

            // ── Merge masonry-mod variants if available ───────────────────
            List<String> masonryBlocks = MasonryCompat.getVariants(rockType);
            List<String> masonryStairs = MasonryCompat.getStairVariants(rockType);
            List<String> masonryHalfs  = MasonryCompat.getHalfVariants(rockType);

            // Full substitution list = natural forms + masonry blocks
            LinkedHashSet<String> subsSet = new LinkedHashSet<>(naturalKeys);
            subsSet.addAll(masonryBlocks);
            String[] subs = subsSet.toArray(new String[0]);

            if (naturalKeys.isEmpty() && masonryBlocks.isEmpty()) continue;

            // Stairs: derived from natural forms + masonry stairs
            LinkedHashSet<String> stairsSet = new LinkedHashSet<>();
            addAll(stairsSet, MasonryCompat.deriveExistingVariants(
                    naturalKeys.toArray(new String[0]), "_Stairs"));
            stairsSet.addAll(masonryStairs);
            String[] stairs = stairsSet.toArray(new String[0]);

            // Half-slabs: derived from natural forms + masonry halfs
            LinkedHashSet<String> halfsSet = new LinkedHashSet<>();
            addAll(halfsSet, MasonryCompat.deriveExistingVariants(
                    naturalKeys.toArray(new String[0]), "_Half"));
            halfsSet.addAll(masonryHalfs);
            String[] halfs = halfsSet.toArray(new String[0]);

            // Roofing: derived from natural forms only (masonry has none)
            String[] roofing = MasonryCompat.deriveExistingRoofing(
                    naturalKeys.toArray(new String[0]));

            // substitutions = every known variant so any block can chisel to any other
            LinkedHashSet<String> allSet = new LinkedHashSet<>(subsSet);
            addAll(allSet, stairs);
            addAll(allSet, halfs);
            addAll(allSet, roofing);
            String[] allSubs = allSet.toArray(new String[0]);

            // Inject onto the full set of targets
            String source = "Rock_" + rockType;
            count += injectFamily(new ArrayList<>(allSet), allSubs, stairs, halfs, roofing, source);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Soil injection
    // ─────────────────────────────────────────────────────────────────────

    private static int injectSoilFamilies() {
        int count = 0;
        for (String[] family : SOIL_FAMILIES) {
            List<String> found = new ArrayList<>();
            for (String key : family) {
                if (exists(key)) found.add(key);
            }
            if (found.isEmpty()) continue;

            String[] subsArr   = found.toArray(new String[0]);
            String[] stairs    = MasonryCompat.deriveExistingVariants(subsArr, "_Stairs");
            String[] halfs     = MasonryCompat.deriveExistingVariants(subsArr, "_Half");
            String[] roofing   = MasonryCompat.deriveExistingRoofing(subsArr);
            String   source    = family[0];

            // substitutions = every known variant so any block can chisel to any other
            LinkedHashSet<String> allSet = new LinkedHashSet<>(found);
            addAll(allSet, stairs);
            addAll(allSet, halfs);
            addAll(allSet, roofing);
            String[] allSubs = allSet.toArray(new String[0]);

            count += injectFamily(new ArrayList<>(allSet), allSubs, stairs, halfs, roofing, source);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Wood injection
    // ─────────────────────────────────────────────────────────────────────

    /** Set of roof suffixes for fast membership testing during discovery. */
    private static final java.util.Set<String> WOOD_ROOF_SUFFIX_SET =
            new java.util.HashSet<>(java.util.Arrays.asList(WOOD_ROOF_SUFFIXES));

    private static int injectWoodFamilies() {
        int count = 0;
        for (String woodType : WOOD_TYPES) {
            // ── Split discovery: roof blocks go straight into roofKeys,
            //    everything else into baseKeys.  This prevents deriving
            //    nonsense like "Wood_Goldenwood_Planks_Roof_Flat".
            List<String> baseKeys = new ArrayList<>();
            List<String> roofKeys = new ArrayList<>();
            for (String suffix : VANILLA_WOOD_SUFFIXES) {
                String candidate = "Wood_" + woodType + suffix;
                if (!exists(candidate)) continue;
                if (WOOD_ROOF_SUFFIX_SET.contains(suffix)) {
                    roofKeys.add(candidate);
                } else {
                    baseKeys.add(candidate);
                }
            }
            if (baseKeys.isEmpty() && roofKeys.isEmpty()) continue;

            // Merge carpentry variants if the mod is available
            List<String> carpBlocks = CarpentryCompat.getVariants(woodType);
            List<String> carpStairs = CarpentryCompat.getStairVariants(woodType);
            List<String> carpHalfs  = CarpentryCompat.getHalfVariants(woodType);

            String[] baseArr = baseKeys.toArray(new String[0]);

            // Stairs / halfs: derived only from base keys (not roof forms)
            LinkedHashSet<String> stairsSet = new LinkedHashSet<>();
            addAll(stairsSet, MasonryCompat.deriveExistingVariants(baseArr, "_Stairs"));
            stairsSet.addAll(carpStairs);
            String[] stairs = stairsSet.toArray(new String[0]);

            LinkedHashSet<String> halfsSet = new LinkedHashSet<>();
            addAll(halfsSet, MasonryCompat.deriveExistingVariants(baseArr, "_Half"));
            halfsSet.addAll(carpHalfs);
            String[] halfs = halfsSet.toArray(new String[0]);

            // Roofing: directly discovered roof keys + any _Roof* derivations
            //          from base keys (catches e.g. Wood_Goldenwood_Roof if
            //          the bare base block exists but its suffix wasn't listed)
            LinkedHashSet<String> roofSet = new LinkedHashSet<>(roofKeys);
            addAll(roofSet, deriveExistingWoodRoofing(baseArr));
            String[] roofing = roofSet.toArray(new String[0]);

            String source = "Wood_" + woodType;

            // substitutions = every known variant so any block can chisel to any other
            LinkedHashSet<String> allSet = new LinkedHashSet<>(baseKeys);
            allSet.addAll(carpBlocks);
            addAll(allSet, stairs);
            addAll(allSet, halfs);
            allSet.addAll(roofKeys);
            String[] allSubs = allSet.toArray(new String[0]);

            count += injectFamily(new ArrayList<>(allSet), allSubs, stairs, halfs, roofing, source);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injects {@link Chisel.Data} onto each {@link BlockType} in {@code keys}
     * that does <em>not</em> already carry chisel state data.
     * <p>
     * If a block already has {@link Chisel.Data} from a prior compat pass but
     * its {@code roofing} array is empty, the roofing field is patched in-place
     * so that earlier passes (e.g. CarpentryCompat) do not silently miss
     * shingle-style roofing variants that only VanillaCompat knows about.
     *
     * @return number of blocks that were actually injected or patched
     */
    private static int injectFamily(List<String> keys,
                                    String[] substitutions,
                                    String[] stairs,
                                    String[] halfSlabs,
                                    String[] roofing,
                                    String source) {
        int count = 0;
        for (String key : keys) {
            try {
                BlockType bt = BlockType.fromString(key);
                if (bt == null) continue;

                StateData existing = bt.getState();

                if (existing instanceof Chisel.Data existingData) {
                    // Block already registered by a prior compat pass.
                    // Patch roofing in-place if it is currently missing.
                    boolean roofingMissing = existingData.roofing == null
                            || existingData.roofing.length == 0;
                    if (roofingMissing && roofing != null && roofing.length > 0) {
                        existingData.roofing = roofing;
                        count++;
                    }
                    continue;
                }

                Chisel.Data data = new Chisel.Data();
                data.source        = source;
                data.substitutions = substitutions;
                data.stairs        = stairs;
                data.halfSlabs     = halfSlabs;
                data.roofing       = roofing;

                setField(StateData.class, data, "id", "Ev0sChisel");
                setField(BlockType.class,  bt,   "state", data);
                count++;
            } catch (Exception e) {
                LOGGER.atWarning().log("[Chisel] VanillaCompat: failed to inject "
                        + key + ": " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * Derives wood roofing by probing both standard {@code _Roof*} and
     * wood-specific {@code _Shingle*} suffixes against each base key.
     * Public so the UI layer can use it as a fallback derivation.
     */
    public static String[] deriveExistingWoodRoofing(String[] bases) {
        if (bases == null) return new String[0];
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String base : bases) {
            for (String suffix : WOOD_ROOF_SUFFIXES) {
                String candidate = base + suffix;
                if (exists(candidate)) result.add(candidate);
            }
        }
        return result.toArray(new String[0]);
    }

    /** Returns {@code true} if a {@link BlockType} with this key exists in the registry. */
    private static boolean exists(String key) {
        try {
            return BlockType.fromString(key) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Null-safe {@code Collections.addAll} into a set. */
    private static void addAll(Set<String> set, String[] arr) {
        if (arr != null) Collections.addAll(set, arr);
    }

    /** Null-safe {@code Collections.addAll} into a list. */
    private static void addAll(List<String> list, String[] arr) {
        if (arr != null) Collections.addAll(list, arr);
    }

    /** Reflective field setter – works on private / final fields. */
    private static void setField(Class<?> clazz, Object target,
                                  String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
