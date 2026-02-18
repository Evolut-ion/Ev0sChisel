package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Compatibility layer for <b>Macaw's Hy Paths</b> and <b>Macaw's Hy Stairs</b>
 * by Sketch Macaw &amp; Peachy Macaw.
 * <p>
 * Both are pure asset packs (no Java plugin), so detection is done by
 * probing for a unique block key from each pack at runtime.
 * <p>
 * <b>Paths</b> naming: {@code Mcw_Paths_Rock_{RockType}_Brick_{Design}[_Stairs|_Half]}
 * <br>
 * <b>Stairs</b> naming: {@code Mcw_Stairs_{StoneType}_{Design}_Stairs[_Balustrade|_Harp]}
 * plus {@code Mcw_Stairs_{StoneType}_Railing[_Balustrade|_Harp]}
 */
public final class MacawCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ── Detection flags ─────────────────────────────────────────────────
    private static boolean pathsDetected  = false;
    private static boolean stairsDetected = false;

    // =====================================================================
    //  Paths constants
    // =====================================================================

    /** Rock types present in Macaw's Paths. */
    private static final String[] PATHS_ROCK_TYPES = {
            "Aqua", "Basalt", "Calcite", "Marble", "Mossy_Stone", "Ocean",
            "Quartzite", "Sandstone_Red", "Sandstone_White", "Shale",
            "Stone", "Volcanic"
    };

    /**
     * Every full-block design name in Macaw's Paths (superset across all
     * rock types – if a design doesn't exist for a type the injection
     * silently skips it).
     */
    private static final String[] PATHS_ALL_DESIGNS = {
            "Basket_Weave", "Basket_Weave_Dark", "Circle", "Diamond",
            "Dumble", "Dumble_Dark", "Dumble_Mixed",
            "Quad",
            "Running", "Running_Dark", "Running_Dark_Vertical",
            "Running_Mixed", "Running_Mixed_Vertical", "Running_Vertical",
            "Square",
            "Tiles", "Tiles_Vertical",
            "Weave", "Weave_Dark"
    };

    /** Designs that also have {@code _Stairs} and {@code _Half} variants. */
    private static final String[] PATHS_STAIR_HALF_DESIGNS = {
            "Dumble", "Dumble_Dark", "Dumble_Mixed",
            "Quad",
            "Running", "Running_Dark", "Running_Dark_Vertical",
            "Running_Mixed", "Running_Mixed_Vertical", "Running_Vertical",
            "Tiles", "Tiles_Vertical",
            "Weave", "Weave_Dark"
    };

    // =====================================================================
    //  Stairs constants
    // =====================================================================

    /** Stone types present in Macaw's Stairs. */
    private static final String[] STAIRS_STONE_TYPES = {
            "Basalt", "Marble", "Sandstone", "Shale",
            "Softwood", "Stone", "Volcanic"
    };

    /** Stair design names. */
    private static final String[] STAIR_DESIGNS = {
            "Classic", "Compact", "Loft", "Skyline", "Terrace"
    };

    /**
     * Per-design suffixes for stair variants shown in the chisel UI.
     * Connected-block sub-variants (_Left, _Middle, _Right) are excluded
     * from the UI list but still receive injected chisel state.
     */
    private static final String[] STAIR_UI_SUFFIXES = {
            "_Stairs", "_Stairs_Balustrade", "_Stairs_Harp"
    };

    /** Connected-block sub-variants (injected but not displayed). */
    private static final String[] STAIR_CONNECTED_SUFFIXES = {
            "_Stairs_Left", "_Stairs_Left_Balustrade", "_Stairs_Left_Harp",
            "_Stairs_Middle",
            "_Stairs_Right", "_Stairs_Right_Balustrade", "_Stairs_Right_Harp"
    };

    /** Railing suffixes. */
    private static final String[] RAILING_SUFFIXES = {
            "_Railing", "_Railing_Balustrade", "_Railing_Harp"
    };

    // =====================================================================
    //  Variant maps (normalised type key → unmodifiable list)
    // =====================================================================

    /** Paths full-block keys by rock type. */
    private static final Map<String, List<String>> PATHS_BLOCKS_BY_TYPE = new LinkedHashMap<>();

    /** Paths stair-variant keys by rock type. */
    private static final Map<String, List<String>> PATHS_STAIRS_BY_TYPE = new LinkedHashMap<>();

    /** Paths half-slab-variant keys by rock type. */
    private static final Map<String, List<String>> PATHS_HALFS_BY_TYPE  = new LinkedHashMap<>();

    /** McwStairs stair + railing keys by stone type (UI-only list). */
    private static final Map<String, List<String>> MCW_STAIRS_BY_TYPE = new LinkedHashMap<>();

    /**
     * McwStairs <em>all</em> block keys by stone type, including
     * connected-block sub-variants (_Left, _Middle, _Right).
     * Used only for state injection.
     */
    private static final Map<String, List<String>> MCW_STAIRS_ALL_BY_TYPE = new LinkedHashMap<>();

    /** Rock-prefix → normalised type, sorted longest first. */
    private static final List<Map.Entry<String, String>> ROCK_PREFIX_TO_TYPE = new ArrayList<>();

    private MacawCompat() {} // utility class

    // =====================================================================
    //  Initialisation – call once from plugin start()
    // =====================================================================

    /**
     * Detects both Macaw packs and, if found, builds variant maps and
     * injects chisel states onto every variant block.
     */
    public static void init() {
        // ── Detect Paths ────────────────────────────────────────────────
        try {
            if (BlockType.fromString("Mcw_Paths_Rock_Stone_Brick_Dumble") != null) {
                pathsDetected = true;
                buildPathsVariantMaps();
                LOGGER.atInfo().log("[Chisel] Macaw's Paths detected – loaded variants for "
                        + PATHS_ROCK_TYPES.length + " rock types");
            }
        } catch (Exception e) { /* not installed */ }
        if (!pathsDetected) {
            LOGGER.atInfo().log("[Chisel] Macaw's Paths not found – compat disabled");
        }

        // ── Detect Stairs ───────────────────────────────────────────────
        try {
            if (BlockType.fromString("Mcw_Stairs_Stone_Classic_Stairs") != null) {
                stairsDetected = true;
                buildStairsVariantMaps();
                LOGGER.atInfo().log("[Chisel] Macaw's Stairs detected – loaded variants for "
                        + STAIRS_STONE_TYPES.length + " stone types");
            }
        } catch (Exception e) { /* not installed */ }
        if (!stairsDetected) {
            LOGGER.atInfo().log("[Chisel] Macaw's Stairs not found – compat disabled");
        }

        if (pathsDetected || stairsDetected) {
            buildRockPrefixMap();
            injectChiselStates();
        }
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    /** @return true when Macaw's Paths is installed */
    public static boolean isPathsAvailable() { return pathsDetected; }

    /** @return true when Macaw's Stairs is installed */
    public static boolean isStairsAvailable() { return stairsDetected; }

    /**
     * Returns Paths full-block variant keys for the given rock type.
     * @param rockType normalised or original-case type (e.g. "stone")
     */
    public static List<String> getPathsBlocks(String rockType) {
        if (!pathsDetected || rockType == null) return Collections.emptyList();
        return PATHS_BLOCKS_BY_TYPE.getOrDefault(normalise(rockType), Collections.emptyList());
    }

    /** Returns Paths stair-variant keys for the given rock type. */
    public static List<String> getPathsStairs(String rockType) {
        if (!pathsDetected || rockType == null) return Collections.emptyList();
        return PATHS_STAIRS_BY_TYPE.getOrDefault(normalise(rockType), Collections.emptyList());
    }

    /** Returns Paths half-slab-variant keys for the given rock type. */
    public static List<String> getPathsHalfs(String rockType) {
        if (!pathsDetected || rockType == null) return Collections.emptyList();
        return PATHS_HALFS_BY_TYPE.getOrDefault(normalise(rockType), Collections.emptyList());
    }

    /**
     * Returns McwStairs stair + railing keys for the given stone type.
     * Only base variants are included (no connected _Left/_Middle/_Right).
     */
    public static List<String> getMcwStairs(String stoneType) {
        if (!stairsDetected || stoneType == null) return Collections.emptyList();
        return MCW_STAIRS_BY_TYPE.getOrDefault(normalise(stoneType), Collections.emptyList());
    }

    /**
     * Detects rock / stone type by first checking the clicked block's own
     * key, then falling back to scanning substitutions.
     *
     * @param blockKey      the {@code BlockType.getId()} of the clicked block (nullable)
     * @param substitutions the existing chisel substitution keys
     * @return normalised type (e.g. "basalt") or {@code null}
     */
    public static String detectRockType(String blockKey, String[] substitutions) {
        if (blockKey != null) {
            String keyNorm = blockKey.toLowerCase(Locale.ROOT);
            // Rock_{Type} prefix (e.g. "Rock_Basalt_Brick")
            for (Map.Entry<String, String> entry : ROCK_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (keyNorm.equals(prefix) || keyNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
            // Mcw_Paths_Rock_{Type}_Brick_ prefix
            if (keyNorm.startsWith("mcw_paths_rock_")) {
                String rest = keyNorm.substring("mcw_paths_rock_".length());
                for (Map.Entry<String, String> entry : ROCK_PREFIX_TO_TYPE) {
                    String typeNorm = entry.getValue();
                    if (rest.equals(typeNorm) || rest.startsWith(typeNorm + "_")) {
                        return typeNorm;
                    }
                }
            }
            // Mcw_Stairs_{Type}_ prefix
            if (keyNorm.startsWith("mcw_stairs_")) {
                String rest = keyNorm.substring("mcw_stairs_".length());
                for (Map.Entry<String, String> entry : ROCK_PREFIX_TO_TYPE) {
                    String typeNorm = entry.getValue();
                    if (rest.equals(typeNorm) || rest.startsWith(typeNorm + "_")) {
                        return typeNorm;
                    }
                }
            }
        }
        return detectRockType(substitutions);
    }

    /**
     * Detects rock / stone type from substitution keys by scanning for
     * {@code Rock_{Type}} prefixes.  Longest prefix wins.
     *
     * @return normalised type (e.g. "stone") or {@code null}
     */
    public static String detectRockType(String[] substitutions) {
        if (substitutions == null) return null;
        for (String sub : substitutions) {
            if (sub == null) continue;
            String subNorm = sub.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : ROCK_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (subNorm.equals(prefix) || subNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    // =====================================================================
    //  Build variant maps
    // =====================================================================

    private static void buildPathsVariantMaps() {
        Set<String> stairHalfSet = new HashSet<>(Arrays.asList(PATHS_STAIR_HALF_DESIGNS));

        for (String type : PATHS_ROCK_TYPES) {
            String norm = normalise(type);
            List<String> blocks = new ArrayList<>();
            List<String> stairs = new ArrayList<>();
            List<String> halfs  = new ArrayList<>();

            for (String design : PATHS_ALL_DESIGNS) {
                String base = "Mcw_Paths_Rock_" + type + "_Brick_" + design;
                blocks.add(base);
                if (stairHalfSet.contains(design)) {
                    stairs.add(base + "_Stairs");
                    halfs.add(base + "_Half");
                }
            }

            PATHS_BLOCKS_BY_TYPE.put(norm, Collections.unmodifiableList(blocks));
            PATHS_STAIRS_BY_TYPE.put(norm, Collections.unmodifiableList(stairs));
            PATHS_HALFS_BY_TYPE.put(norm, Collections.unmodifiableList(halfs));
        }
    }

    private static void buildStairsVariantMaps() {
        for (String type : STAIRS_STONE_TYPES) {
            String norm = normalise(type);
            List<String> uiKeys  = new ArrayList<>();
            List<String> allKeys = new ArrayList<>();

            // Design stairs (base + Balustrade + Harp for UI; + connected for all)
            for (String design : STAIR_DESIGNS) {
                String designBase = "Mcw_Stairs_" + type + "_" + design;
                for (String suffix : STAIR_UI_SUFFIXES) {
                    String key = designBase + suffix;
                    uiKeys.add(key);
                    allKeys.add(key);
                }
                for (String suffix : STAIR_CONNECTED_SUFFIXES) {
                    allKeys.add(designBase + suffix);
                }
            }

            // Railings (shown in UI and injected)
            for (String suffix : RAILING_SUFFIXES) {
                String key = "Mcw_Stairs_" + type + suffix;
                uiKeys.add(key);
                allKeys.add(key);
            }

            MCW_STAIRS_BY_TYPE.put(norm, Collections.unmodifiableList(uiKeys));
            MCW_STAIRS_ALL_BY_TYPE.put(norm, Collections.unmodifiableList(allKeys));
        }
    }

    /**
     * Builds a prefix map covering all rock types from both Paths and
     * Stairs, sorted longest-first.
     */
    private static void buildRockPrefixMap() {
        Set<String> allTypes = new LinkedHashSet<>();
        for (String t : PATHS_ROCK_TYPES)   allTypes.add(t);
        for (String t : STAIRS_STONE_TYPES) allTypes.add(t);

        for (String type : allTypes) {
            String rockPrefix = normalise("Rock_" + type);
            ROCK_PREFIX_TO_TYPE.add(Map.entry(rockPrefix, normalise(type)));
        }
        // Longest prefix first so "rock_sandstone_red" matches before "rock_sandstone"
        ROCK_PREFIX_TO_TYPE.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
    }

    // =====================================================================
    //  State injection
    // =====================================================================

    /**
     * Reads each {@code Rock_{Type}}'s existing chisel state, merges with
     * Macaw variants, and injects a {@link Chisel.Data} onto every Macaw
     * block so the chisel tool recognises them at runtime.
     */
    private static void injectChiselStates() {
        int injected = 0;
        int failed   = 0;

        // Collect every normalised type that has variants from either pack
        Set<String> allNormTypes = new LinkedHashSet<>();
        allNormTypes.addAll(PATHS_BLOCKS_BY_TYPE.keySet());
        allNormTypes.addAll(MCW_STAIRS_BY_TYPE.keySet());

        for (String normType : allNormTypes) {
            String origType = findOriginalCase(normType);
            if (origType == null) continue;

            // Read existing Rock_{Type} chisel substitutions
            String[] rockSubs = getRockSubstitutions(origType);

            // ── Merge blocks ────────────────────────────────────────────
            LinkedHashSet<String> mergedBlocks = new LinkedHashSet<>();
            if (rockSubs != null) Collections.addAll(mergedBlocks, rockSubs);
            mergedBlocks.addAll(getPathsBlocks(normType));
            String[] mergedBlockArr = mergedBlocks.toArray(new String[0]);

            // ── Merge stairs ────────────────────────────────────────────
            String[] vanillaStairs = MasonryCompat.deriveExistingVariants(rockSubs, "_Stairs");
            LinkedHashSet<String> mergedStairs = new LinkedHashSet<>();
            if (vanillaStairs != null) Collections.addAll(mergedStairs, vanillaStairs);
            mergedStairs.addAll(getPathsStairs(normType));
            mergedStairs.addAll(getMcwStairs(normType));
            String[] mergedStairArr = mergedStairs.toArray(new String[0]);

            // ── Merge halfs ─────────────────────────────────────────────
            String[] vanillaHalfs = MasonryCompat.deriveExistingVariants(rockSubs, "_Half");
            LinkedHashSet<String> mergedHalfs = new LinkedHashSet<>();
            if (vanillaHalfs != null) Collections.addAll(mergedHalfs, vanillaHalfs);
            mergedHalfs.addAll(getPathsHalfs(normType));
            String[] mergedHalfArr = mergedHalfs.toArray(new String[0]);

            // ── Roofing (vanilla only) ──────────────────────────────────
            String[] vanillaRoofing = MasonryCompat.deriveExistingRoofing(rockSubs);
            String[] roofingArr = vanillaRoofing != null ? vanillaRoofing : new String[0];

            // ── Injection targets ───────────────────────────────────────
            List<String> targets = new ArrayList<>();
            targets.addAll(getPathsBlocks(normType));
            targets.addAll(getPathsStairs(normType));
            targets.addAll(getPathsHalfs(normType));
            // Use ALL stair keys (including connected block sub-variants)
            List<String> allStairs = MCW_STAIRS_ALL_BY_TYPE.getOrDefault(normType, Collections.emptyList());
            targets.addAll(allStairs);

            for (String key : targets) {
                try {
                    BlockType bt = BlockType.fromString(key);
                    if (bt == null) { failed++; continue; }

                    Chisel.Data data = new Chisel.Data();
                    data.source        = origType;
                    data.substitutions = mergedBlockArr;
                    data.stairs        = mergedStairArr;
                    data.halfSlabs     = mergedHalfArr;
                    data.roofing       = roofingArr;

                    setField(StateData.class, data, "id", "Ev0sChisel");
                    setField(BlockType.class, bt, "state", data);
                    injected++;
                } catch (Exception e) {
                    LOGGER.atWarning().log("[Chisel] Failed to inject state for "
                            + key + ": " + e.getMessage());
                    failed++;
                }
            }
        }

        LOGGER.atInfo().log("[Chisel] Injected Chisel state onto " + injected
                + " Macaw blocks" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    // =====================================================================
    //  Internals
    // =====================================================================

    /** Maps a normalised type back to original casing. */
    private static String findOriginalCase(String normType) {
        for (String t : PATHS_ROCK_TYPES) {
            if (normalise(t).equals(normType)) return t;
        }
        for (String t : STAIRS_STONE_TYPES) {
            if (normalise(t).equals(normType)) return t;
        }
        return null;
    }

    /** Reads the Chisel.Data substitutions already defined on Rock_{type}. */
    private static String[] getRockSubstitutions(String stoneType) {
        try {
            BlockType rockBlock = BlockType.fromString("Rock_" + stoneType);
            if (rockBlock == null) return null;
            StateData state = rockBlock.getState();
            if (state instanceof Chisel.Data chiselData) {
                return chiselData.substitutions;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("[Chisel] Could not read rock subs for Rock_"
                    + stoneType + ": " + e.getMessage());
        }
        return null;
    }

    private static String normalise(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    /** Reflective field setter – handles protected / private fields. */
    private static void setField(Class<?> clazz, Object target,
                                 String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
