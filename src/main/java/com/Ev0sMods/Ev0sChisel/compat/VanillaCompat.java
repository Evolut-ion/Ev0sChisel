package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

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
 * <p>
 * <b>Vanilla-only support:</b> This compat layer works independently of other
 * mods. When MasonryCompat or CarpentryCompat are not available, VanillaCompat
 * will discover and inject chisel states for vanilla rock/wood blocks directly.
 */
public final class VanillaCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ── Rock families ────────────────────────────────────────────────────
    // These match MasonryCompat.STONE_TYPES so masonry variants are merged in.
    // Also used as fallback when other mods are not present (vanilla-only).
    private static final String[] ROCK_TYPES = {
            "Aqua", "Ash", "Basalt", "Calcite", "Chalk", "Clay_Brick",
            "Crystal_Cyan", "Crystal_Green", "Crystal_Pink", "Crystal_Yellow",
            "Dirt", "Ledge", "Ledge_Brick", "Lime", "Magma_Cooled", "Marble", "Peach",
            "Quartzite", "Sandstone", "Sandstone_Red", "Sandstone_White", "Snow", "Stone",
            "Gold", "Copper", "Bronze", "Iron", "Zinc"
    };

     private static final String[] METAL_TYPES = {
            "Gold", "Copper", "Bronze", "Iron", "Zinc"
    };

    /**
     * Returns the rock types array for external use.
     * This allows VanillaCompat to work as a fallback when MasonryCompat is not available.
     */
    public static String[] getRockTypes() {
        return ROCK_TYPES;
    }

    public static String[] getMetalTypes() {
        return METAL_TYPES;
    }

    public static boolean isMetalType(String type) {
        for (String metalType : METAL_TYPES) {
            if (metalType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getRockNaturalSuffixes() {
        return ROCK_NATURAL_SUFFIXES;
    }

    public static String[] getVanillaWoodSuffixes() {
        return VANILLA_WOOD_SUFFIXES;
    }

    public static boolean isWoodRoofSuffix(String suffix) {
        return WOOD_ROOF_SUFFIX_SET.contains(suffix);
    }

    /**
     * Pipe suffixes to probe only for metal rock types.
     * e.g. "Metal_Iron_Pipe_Corner", "Metal_Iron_Pipe_Long", …
     */
    private static final String[] METAL_PIPE_SUFFIXES = {
            "_Pipe_Corner", "_Pipe_Chimney", "_Pipe_Large",
            "_Pipe_Large_Corner", "_Pipe_Large_Mouthpiece",
            "_Pipe_Long", "_Pipe_Short"
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
            "_Chiseled", "_Smooth", "_Cut", "_Ornate", "_Decorative","_Brick_Ornate","_Brick_Decorative",
            "_Pillar", "_Pillar_Base", "_Pillar_Middle"
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
    // 11 canonical wood types from Hytale wiki
    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Greenwood",
            "Hardwood", "Lightwood", "Redwood", "Softwood", "Tropicalwood",
            "Whitewood", "Goldenwood"
    };

    /**
     * Returns the wood types array for external use.
     * This allows VanillaCompat to work as a fallback when CarpentryCompat is not available.
     */
    public static String[] getWoodTypes() {
        return WOOD_TYPES;
    }

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
            "_Trunk",
            "_Stripped_Log",
            "_Stripped_Log_Horizontal",
            "_Stripped_Log_Corner",
            "_Stripped_Trunk",
            // ── Bark / outer shell ───────────────────────────────────────
            "_Bark",
            "_Stripped_Bark",
            "_Stripped",
            // ── Structural / decorative ───────────────────────────────────
            "_Beam",
            "_Beam_Horizontal",
            "_Beam_Corner",
            "_Beam_Fivepiece",
            "_Beam_Quad_Corner_Bottom",
            "_Beam_Triple_Corner_Bottom",
            "_Beam_Triple_Corner_Top",
            "_Beam_Corner_Vertical",
            "_Beam_Single_Top",
            "_Beam_Single_Bottom",
            "_Beam_Single_Side",
            "_Beam_Single_Horizontal",
            "_Beam_Cross_Horizontal",
            "_Beam_ExtendedDoublecross",
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
            "_Stairs",
            "_Half",
            "_Decorative",
            "_Ornate",
            "_Brick_Decorative",
            "_Brick_Ornate",
            "_Planks_Slab",
            "_Slab",
            "_Log_Stairs",
            "_Log_Half",
            "_Log_Slab",
            "_Trunk_Stairs",
            "_Trunk_Half",
            "_Trunk_Slab",
            "_Wood",
            "_Wall",
            // ── Shared masonry-style block forms seen on some wood sets ──
            "_Brick",
            "_Bricks",
            "_Tile",
            "_Tiles",
            "_Slab",
            "_Slabs",
            "_Polished",
            "_Chiseled",
            "_Smooth",
            "_Cut",
            "_Pillar",
            "_Pillar_Base",
            "_Pillar_Middle",
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
        // vanilla compat injection summary (info log removed)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rock injection
    // ─────────────────────────────────────────────────────────────────────

    private static int injectRockFamilies() {
        int count = 0;
        String[] structureSuffixes = {"_Beam", "_Wall", "_Fence"};
        for (String rockType : ROCK_TYPES) {
            List<String> naturalKeys = discoverNaturalRockKeys(rockType);

            // Also check for block keys without "Rock_" prefix (e.g., "Basalt_Brick")
            // This handles alternative naming conventions in the game
            List<String> altKeys = new ArrayList<>();
            for (String suffix : ROCK_NATURAL_SUFFIXES) {
                // Try the bare type name prefix (e.g., "Basalt_Brick" instead of "Rock_Basalt_Brick")
                if (!suffix.isEmpty()) {
                    String candidate = rockType + suffix;
                    if (exists(candidate)) altKeys.add(candidate);
                }
            }
            

            // ── Merge masonry-mod variants if available ───────────────────
            // Only call MasonryCompat methods if MasonryCompat is available
            List<String> masonryBlocks = MasonryCompat.isAvailable() ? MasonryCompat.getVariants(rockType) : Collections.emptyList();
            List<String> masonryStairs = MasonryCompat.isAvailable() ? MasonryCompat.getStairVariants(rockType) : Collections.emptyList();
            List<String> masonryHalfs  = MasonryCompat.isAvailable() ? MasonryCompat.getHalfVariants(rockType) : Collections.emptyList();

                LinkedHashSet<String> structureBlocks = new LinkedHashSet<>();
                addAll(structureBlocks, deriveExistingVariants(
                    naturalKeys.toArray(new String[0]), structureSuffixes));
                addAll(structureBlocks, deriveExistingVariants(
                    altKeys.toArray(new String[0]), structureSuffixes));

            // Full substitution list = natural forms + alternative forms + masonry blocks
            LinkedHashSet<String> subsSet = new LinkedHashSet<>(naturalKeys);
            subsSet.addAll(altKeys);
                subsSet.addAll(structureBlocks);
            subsSet.addAll(masonryBlocks);
            String[] subs = subsSet.toArray(new String[0]);

            if (naturalKeys.isEmpty() && altKeys.isEmpty() && structureBlocks.isEmpty() && masonryBlocks.isEmpty()) continue;

            // Stairs: derived from natural forms + alternative forms + masonry stairs
            LinkedHashSet<String> stairsSet = new LinkedHashSet<>();
            addAll(stairsSet, deriveExistingVariants(
                    naturalKeys.toArray(new String[0]), "_Stairs"));
            addAll(stairsSet, deriveExistingVariants(
                    altKeys.toArray(new String[0]), "_Stairs"));
            stairsSet.addAll(masonryStairs);
            String[] stairs = stairsSet.toArray(new String[0]);

            // Half-slabs: derived from natural forms + alternative forms + masonry halfs
            LinkedHashSet<String> halfsSet = new LinkedHashSet<>();
            addAll(halfsSet, deriveExistingVariants(
                    naturalKeys.toArray(new String[0]), "_Half"));
            addAll(halfsSet, deriveExistingVariants(
                    altKeys.toArray(new String[0]), "_Half"));
            halfsSet.addAll(masonryHalfs);
            String[] halfs = halfsSet.toArray(new String[0]);

            // Roofing: derived from natural forms + alternative forms only (masonry has none)
            String[] roofing = deriveExistingRoofing(
                    naturalKeys.toArray(new String[0]));
            if (roofing == null || roofing.length == 0) {
                roofing = deriveExistingRoofing(altKeys.toArray(new String[0]));
            }

            // substitutions = every known variant so any block can chisel to any other
            LinkedHashSet<String> allSet = new LinkedHashSet<>(subsSet);
            addAll(allSet, stairs);
            addAll(allSet, halfs);
            addAll(allSet, roofing);
            String[] allSubs = allSet.toArray(new String[0]);

            // Inject onto the full set of targets
            String source = resolveRockSource(rockType, naturalKeys, altKeys, masonryBlocks);
            count += injectFamily(new ArrayList<>(allSet), allSubs, stairs, halfs, roofing, source);
        }

        return count;
    }

    private static List<String> discoverNaturalRockKeys(String rockType) {
        LinkedHashSet<String> naturalKeys = new LinkedHashSet<>();
        addRockVariants(naturalKeys, "Rock_" + rockType, true);
        if (isMetalType(rockType)) {
            addRockVariants(naturalKeys, "Metal_" + rockType, true);
            // Probe pipe variants for metal types (e.g. Metal_Iron_Pipe_Corner)
            String metalBase = "Metal_" + rockType;
            for (String pipeSuffix : METAL_PIPE_SUFFIXES) {
                String candidate = metalBase + pipeSuffix;
                if (exists(candidate)) naturalKeys.add(candidate);
            }
        }
        return new ArrayList<>(naturalKeys);
    }

    private static void addRockVariants(Set<String> found, String baseKey, boolean includeBareBase) {
        for (String suffix : ROCK_NATURAL_SUFFIXES) {
            if (!includeBareBase && suffix.isEmpty()) {
                continue;
            }
            String candidate = baseKey + suffix;
            if (exists(candidate)) {
                found.add(candidate);
            }
        }
    }

    private static String resolveRockSource(String rockType, List<String> naturalKeys, List<String> altKeys,
            List<String> masonryBlocks) {
        String rockSource = "Rock_" + rockType;
        if (exists(rockSource)) {
            return rockSource;
        }

        if (isMetalType(rockType)) {
            String metalSource = "Metal_" + rockType;
            if (exists(metalSource)) {
                return metalSource;
            }
        }

        if (!naturalKeys.isEmpty()) {
            return naturalKeys.get(0);
        }
        if (!altKeys.isEmpty()) {
            return altKeys.get(0);
        }
        if (!masonryBlocks.isEmpty()) {
            return masonryBlocks.get(0);
        }

        return rockSource;
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
            String[] stairs    = deriveExistingVariants(subsArr, "_Stairs");
            String[] halfs     = deriveExistingVariants(subsArr, "_Half");
            String[] roofing   = deriveExistingRoofing(subsArr);
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
            // Only call CarpentryCompat methods if CarpentryCompat is available
            List<String> carpBlocks = CarpentryCompat.isAvailable() ? CarpentryCompat.getVariants(woodType) : Collections.emptyList();
            List<String> carpStairs = CarpentryCompat.isAvailable() ? CarpentryCompat.getStairVariants(woodType) : Collections.emptyList();
            List<String> carpHalfs  = CarpentryCompat.isAvailable() ? CarpentryCompat.getHalfVariants(woodType) : Collections.emptyList();

            String[] baseArr = baseKeys.toArray(new String[0]);

            // Stairs / halfs: derived only from base keys (not roof forms)
            LinkedHashSet<String> stairsSet = new LinkedHashSet<>();
            addAll(stairsSet, deriveExistingVariants(baseArr, "_Stairs"));
            stairsSet.addAll(carpStairs);
            String[] stairs = stairsSet.toArray(new String[0]);

            LinkedHashSet<String> halfsSet = new LinkedHashSet<>();
            addAll(halfsSet, deriveExistingVariants(baseArr, "_Half"));
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
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;

                StateData existing = bt.getState();

                if (existing instanceof Chisel.Data existingData) {
                    // Block already registered by a prior compat pass.
                    // Merge in any vanilla-discovered arrays that were missing.
                    String[] mergedSubs = mergeUnique(existingData.substitutions, substitutions);
                    String[] mergedStairs = mergeUnique(existingData.stairs, stairs);
                    String[] mergedHalfs = mergeUnique(existingData.halfSlabs, halfSlabs);
                    String[] mergedRoofing = mergeUnique(existingData.roofing, roofing);

                    boolean changed = !java.util.Arrays.equals(existingData.substitutions, mergedSubs)
                            || !java.util.Arrays.equals(existingData.stairs, mergedStairs)
                            || !java.util.Arrays.equals(existingData.halfSlabs, mergedHalfs)
                            || !java.util.Arrays.equals(existingData.roofing, mergedRoofing);
                    if (changed) {
                        existingData.substitutions = mergedSubs;
                        existingData.stairs = mergedStairs;
                        existingData.halfSlabs = mergedHalfs;
                        existingData.roofing = mergedRoofing;
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
     * Derives variants by checking if base + suffix exists as a BlockType.
     * This is a local version that doesn't depend on MasonryCompat being available.
     */
    private static String[] deriveExistingVariants(String[] bases, String suffix) {
        if (bases == null) return null;
        List<String> result = new ArrayList<>();
        for (String base : bases) {
            String candidate = base + suffix;
            if (exists(candidate)) result.add(candidate);
        }
        return result.isEmpty() ? null : result.toArray(new String[0]);
    }

    private static String[] deriveExistingVariants(String[] bases, String[] suffixes) {
        if (bases == null || suffixes == null) return null;
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String suffix : suffixes) {
            addAll(result, deriveExistingVariants(bases, suffix));
        }
        return result.isEmpty() ? null : result.toArray(new String[0]);
    }

    /**
     * Derives roofing variants by checking all roof sub-variants
     * ({@code _Roof}, {@code _Roof_Flat}, etc.) for each base key.
     * This is a local version that doesn't depend on MasonryCompat being available.
     */
    private static String[] deriveExistingRoofing(String[] bases) {
        if (bases == null) return null;
        String[] roofSuffixes = {"_Roof", "_Roof_Flat", "_Roof_Hollow", "_Roof_Shallow", "_Roof_Steep"};
        List<String> result = new ArrayList<>();
        for (String base : bases) {
            for (String suffix : roofSuffixes) {
                String candidate = base + suffix;
                if (exists(candidate)) result.add(candidate);
            }
        }
        return result.isEmpty() ? null : result.toArray(new String[0]);
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

    private static String[] mergeUnique(String[] first, String[] second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addAll(merged, first);
        addAll(merged, second);
        return merged.toArray(new String[0]);
    }

    /** Returns {@code true} if a {@link BlockType} with this key exists in the registry. */
    private static boolean exists(String key) {
        try {
            return BlockTypeCache.exists(key);
        } catch (Exception e) {
            return false;
        }
    }

    /** Null-safe {@code Collections.addAll} into a set. */
    private static void addAll(Set<String> set, String[] arr) {
        if (arr != null) {
            for (String s : arr) {
                if (s != null) set.add(s); // Ignore missing/null keys
            }
        }
    }

    /** Null-safe {@code Collections.addAll} into a list. */
    private static void addAll(List<String> list, String[] arr) {
        if (arr != null) {
            for (String s : arr) {
                if (s != null) list.add(s); // Ignore missing/null keys
            }
        }
    }

    /** Reflective field setter – delegates to ReflectionCache to avoid repeated lookups. */
    private static void setField(Class<?> clazz, Object target,
                                  String fieldName, Object value) throws Exception {
        ReflectionCache.setField(clazz, target, fieldName, value);
    }
}
