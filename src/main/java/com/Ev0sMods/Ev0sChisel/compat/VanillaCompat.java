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

    /** Vanilla wood block name suffixes to probe for each wood type. */
    private static final String[] VANILLA_WOOD_SUFFIXES = {
            "", "_Planks", "_Log", "_Stripped_Log", "_Stripped",
            "_Bark", "_Stripped_Bark"
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

            // Inject onto ALL discovered blocks: base forms + stairs + halfs + roofing
            // so that right-clicking any of them opens the chisel UI.
            List<String> allTargets = new ArrayList<>(naturalKeys);
            addAll(allTargets, stairs);
            addAll(allTargets, halfs);
            addAll(allTargets, roofing);

            String source = "Rock_" + rockType;
            count += injectFamily(allTargets, subs, stairs, halfs, roofing, source);
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

            // Inject onto base blocks + all derived forms
            List<String> allTargets = new ArrayList<>(found);
            addAll(allTargets, stairs);
            addAll(allTargets, halfs);
            addAll(allTargets, roofing);

            count += injectFamily(allTargets, subsArr, stairs, halfs, roofing, source);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Wood injection
    // ─────────────────────────────────────────────────────────────────────

    private static int injectWoodFamilies() {
        int count = 0;
        for (String woodType : WOOD_TYPES) {
            // Discover existing vanilla wood blocks for this type
            List<String> vanillaKeys = new ArrayList<>();
            for (String suffix : VANILLA_WOOD_SUFFIXES) {
                String candidate = "Wood_" + woodType + suffix;
                if (exists(candidate)) vanillaKeys.add(candidate);
            }
            if (vanillaKeys.isEmpty()) continue;

            // Merge carpentry variants if the mod is available
            List<String> carpBlocks = CarpentryCompat.getVariants(woodType);
            List<String> carpStairs = CarpentryCompat.getStairVariants(woodType);
            List<String> carpHalfs  = CarpentryCompat.getHalfVariants(woodType);

            String[] vanillaArr = vanillaKeys.toArray(new String[0]);

            LinkedHashSet<String> subsSet = new LinkedHashSet<>(vanillaKeys);
            subsSet.addAll(carpBlocks);
            String[] subs = subsSet.toArray(new String[0]);

            LinkedHashSet<String> stairsSet = new LinkedHashSet<>();
            addAll(stairsSet, MasonryCompat.deriveExistingVariants(vanillaArr, "_Stairs"));
            stairsSet.addAll(carpStairs);
            String[] stairs = stairsSet.toArray(new String[0]);

            LinkedHashSet<String> halfsSet = new LinkedHashSet<>();
            addAll(halfsSet, MasonryCompat.deriveExistingVariants(vanillaArr, "_Half"));
            halfsSet.addAll(carpHalfs);
            String[] halfs = halfsSet.toArray(new String[0]);

            String[] roofing = MasonryCompat.deriveExistingRoofing(vanillaArr);
            String   source  = "Wood_" + woodType;

            // Inject onto vanilla blocks + all derived forms
            List<String> allTargets = new ArrayList<>(vanillaKeys);
            addAll(allTargets, stairs);
            addAll(allTargets, halfs);
            addAll(allTargets, roofing);

            count += injectFamily(allTargets, subs, stairs, halfs, roofing, source);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injects {@link Chisel.Data} onto each {@link BlockType} in {@code keys}
     * that does <em>not</em> already carry chisel state data.
     *
     * @return number of blocks that were actually injected
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

                // Skip blocks already registered by another compat pass
                StateData existing = bt.getState();
                if (existing instanceof Chisel.Data) continue;

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
