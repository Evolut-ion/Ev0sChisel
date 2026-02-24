package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Compatibility layer for Ymmersive Masonry.
 * <p>
 * When the mod is installed, this class provides masonry variant block keys
 * grouped by stone type.  Stairs and half-slabs are excluded – only full
 * blocks are offered as chisel substitutions.
 */
public final class MasonryCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Whether Ymmersive Masonry was detected on the classpath. */
    private static boolean detected = false;

    /** Stone-type (normalised) → unmodifiable list of full-block variant keys. */
    private static final Map<String, List<String>> VARIANTS_BY_TYPE = new LinkedHashMap<>();

    /** Stone-type (normalised) → unmodifiable list of stair variant keys. */
    private static final Map<String, List<String>> STAIR_VARIANTS_BY_TYPE = new LinkedHashMap<>();

    /** Stone-type (normalised) → unmodifiable list of half-slab variant keys. */
    private static final Map<String, List<String>> HALF_VARIANTS_BY_TYPE = new LinkedHashMap<>();

    /**
     * Rock-block prefix (normalised, e.g. "rock_stone") → stone type key
     * (normalised, e.g. "stone").  Sorted longest-first so "sandstone_red"
     * matches before "sandstone".
     */
    private static final List<Map.Entry<String, String>> ROCK_PREFIX_TO_TYPE = new ArrayList<>();

    /**
     * Bare type-name prefix (normalised, e.g. "basalt") → stone type key.
     * Sorted longest-first so "sandstone_red" matches before "sandstone".
     * Used to match masonry variant block keys (e.g. "Basalt_Crosshatch").
     */
    private static final List<Map.Entry<String, String>> TYPE_PREFIX_TO_TYPE = new ArrayList<>();

    // ── The 20 masonry patterns (no stairs / half) ──────────────────────
    private static final String[] PATTERNS = {
            "Crosshatch", "Diamond", "English_Bricks", "Flemish_Bricks",
            "Herringbone", "Hexagonal", "Hopscotch", "Large_Chevron",
            "Old_English", "Pattern", "Pebble_Bricks", "Pinwheel",
            "Rocks", "Small_Bricks", "Small_Hexagons", "Small_Tiles",
            "Stacked_Grid", "Straight_Herringbone", "Tiles", "Windmill"
    };

    // ── Stone types that have masonry variants ──────────────────────────
    private static final String[] STONE_TYPES = {
            "Aqua", "Ash", "Basalt", "Calcite", "Chalk", "Clay_Brick",
            "Crystal_Cyan", "Crystal_Green", "Crystal_Pink", "Crystal_Yellow",
            "Dirt", "Lime", "Marble", "Quartzite", "Sandstone", "Sandstone_Red", 
            "Sandstone_White", "Snow", "Stone"
    };

    private MasonryCompat() {} // utility class

    // ─────────────────────────────────────────────────────────────────────
    // Initialisation – call once during plugin setup / start
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Probes the classpath for <code>net.conczin.YmmersiveMasonry</code>.
     * If found, populates the variant map.
     */
    public static void init() {
        try {
            Class.forName("net.conczin.YmmersiveMasonry");
            detected = true;
            LOGGER.atInfo().log("[Chisel] Ymmersive Masonry detected – loading stone variants");
            buildVariantMap();
            buildRockPrefixMap();
        } catch (ClassNotFoundException e) {
            detected = false;
            LOGGER.atInfo().log("[Chisel] Ymmersive Masonry not found – masonry compat disabled");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /** @return true when Ymmersive Masonry is present on this server */
    public static boolean isAvailable() {
        return detected;
    }

    /**
     * Returns the masonry full-block variant keys for the given stone type,
     * or an empty list if the type is unknown or masonry is absent.
     */
    public static List<String> getVariants(String stoneType) {
        if (!detected || stoneType == null) return Collections.emptyList();
        return VARIANTS_BY_TYPE.getOrDefault(normalise(stoneType), Collections.emptyList());
    }

    /**
     * Returns the masonry stair variant keys for the given stone type.
     */
    public static List<String> getStairVariants(String stoneType) {
        if (!detected || stoneType == null) return Collections.emptyList();
        return STAIR_VARIANTS_BY_TYPE.getOrDefault(normalise(stoneType), Collections.emptyList());
    }

    /**
     * Returns the masonry half-slab variant keys for the given stone type.
     */
    public static List<String> getHalfVariants(String stoneType) {
        if (!detected || stoneType == null) return Collections.emptyList();
        return HALF_VARIANTS_BY_TYPE.getOrDefault(normalise(stoneType), Collections.emptyList());
    }

    /**
     * Returns every registered stone type that has masonry variants.
     */
    public static Set<String> getStoneTypes() {
        return Collections.unmodifiableSet(VARIANTS_BY_TYPE.keySet());
    }

    /**
     * Detects the stone type by first checking the clicked block's own key,
     * then falling back to scanning the substitution array.
     * <p>
     * Checking the block key first prevents mis-detection when a block's
     * substitution list contains entries from multiple stone families
     * (e.g. Rock_Basalt's subs may also include Rock_Stone entries).
     *
     * @param blockKey      the {@link BlockType#getId()} of the clicked block (nullable)
     * @param substitutions the existing chisel substitution keys
     * @return the detected stone type (normalised), or null
     */
    public static String detectStoneType(String blockKey, String[] substitutions) {
        if (!detected) return null;

        // Priority: match the block's own key
        if (blockKey != null) {
            String keyNorm = blockKey.toLowerCase(Locale.ROOT);
            // Check "Rock_{Type}" prefix  (e.g. "Rock_Basalt", "Rock_Basalt_Brick")
            for (Map.Entry<String, String> entry : ROCK_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (keyNorm.equals(prefix) || keyNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
            // Check bare type name  (e.g. "Basalt_Crosshatch" → basalt)
            for (Map.Entry<String, String> entry : TYPE_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (keyNorm.equals(prefix) || keyNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
        }

        // Fallback: scan substitutions (first prefix match wins)
        return detectStoneType(substitutions);
    }

    /**
     * Detects the stone type by examining the existing substitution keys.
     * Looks for keys that start with a known Rock-block prefix
     * (e.g. "Rock_Stone", "Rock_Basalt") and returns the matching stone
     * type, or {@code null} if none could be determined.
     *
     * @param substitutions the existing chisel substitution keys
     * @return the detected stone type (normalised), or null
     */
    public static String detectStoneType(String[] substitutions) {
        if (!detected || substitutions == null) return null;
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

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    private static void buildVariantMap() {
        for (String type : STONE_TYPES) {
            String normType = normalise(type);
            List<String> blockKeys = new ArrayList<>(PATTERNS.length);
            List<String> stairKeys = new ArrayList<>(PATTERNS.length);
            List<String> halfKeys  = new ArrayList<>(PATTERNS.length);
            for (String pattern : PATTERNS) {
                String base = type + "_" + pattern;
                blockKeys.add(base);
                stairKeys.add(base + "_Stairs");
                halfKeys.add(base + "_Half");
            }
            VARIANTS_BY_TYPE.put(normType, Collections.unmodifiableList(blockKeys));
            STAIR_VARIANTS_BY_TYPE.put(normType, Collections.unmodifiableList(stairKeys));
            HALF_VARIANTS_BY_TYPE.put(normType, Collections.unmodifiableList(halfKeys));
        }
        LOGGER.atInfo().log("[Chisel] Loaded masonry variants for " + VARIANTS_BY_TYPE.size()
                + " stone types (blocks + stairs + halfs)");
    }

    /**
     * Builds the rock-prefix lookup sorted by descending prefix length
     * so that "rock_sandstone_red" is tested before "rock_sandstone".
     */
    private static void buildRockPrefixMap() {
        for (String type : STONE_TYPES) {
            // The key in the normalised lookup
            String normType = normalise(type);
            // The known rock-block prefix for this stone type
            String rockPrefix = normalise("Rock_" + type);
            ROCK_PREFIX_TO_TYPE.add(Map.entry(rockPrefix, normType));
        }
        // Sort longest prefix first to avoid partial matches
        ROCK_PREFIX_TO_TYPE.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        // Build bare type-name prefix map (for matching masonry variant keys)
        for (String type : STONE_TYPES) {
            String normType = normalise(type);
            TYPE_PREFIX_TO_TYPE.add(Map.entry(normType, normType));
        }
        TYPE_PREFIX_TO_TYPE.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        LOGGER.atInfo().log("[Chisel] Built rock-prefix map with " + ROCK_PREFIX_TO_TYPE.size()
                + " entries + " + TYPE_PREFIX_TO_TYPE.size() + " type-prefix entries");
    }

    /** Lowercase normalisation so lookups are case-insensitive. */
    private static String normalise(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    // ─────────────────────────────────────────────────────────────────────
    // State injection – call once from plugin start() after assets load
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Attaches a {@link Chisel.Data} (with the merged substitution list) to
     * every masonry variant {@link BlockType} so they are recognised as
     * chiselable blocks at runtime.
     * <p>
     * Must be called <b>after</b> all asset packs have been loaded
     * (i.e. from {@code start()}, not {@code setup()}).</p>
     */
    public static void injectChiselStates() {
        if (!detected) return;

        int injected = 0;
        int failed   = 0;

        for (String stoneType : STONE_TYPES) {
            String normType = normalise(stoneType);
            List<String> masonryBlocks = VARIANTS_BY_TYPE.get(normType);
            List<String> masonryStairs = STAIR_VARIANTS_BY_TYPE.get(normType);
            List<String> masonryHalfs  = HALF_VARIANTS_BY_TYPE.get(normType);
            if (masonryBlocks == null || masonryBlocks.isEmpty()) continue;

            // Check if the base rock block exists - if not, skip this stone type
            // This prevents injecting masonry data for stone types that don't exist
            try {
                BlockType rockBlock = BlockType.fromString("Rock_" + stoneType);
                if (rockBlock == null) {
                    LOGGER.atInfo().log("[Chisel] Skipping masonry injection for " + stoneType + " - no base rock block found");
                    continue;
                }
            } catch (Exception e) {
                LOGGER.atInfo().log("[Chisel] Skipping masonry injection for " + stoneType + " - no base rock block found");
                continue;
            }

            // Read existing rock-chisel substitutions for this stone type
            String[] rockSubs = getRockSubstitutions(stoneType);

            // If no rock substitutions found, this stone type doesn't have a super type
            // Skip masonry injection for types without their super type
            if (rockSubs == null || rockSubs.length == 0) {
                LOGGER.atInfo().log("[Chisel] Skipping masonry injection for " + stoneType + " - no super type found");
                continue;
            }

            // Merge full blocks: rock subs + masonry blocks
            LinkedHashSet<String> mergedBlocks = new LinkedHashSet<>();
            if (rockSubs != null) Collections.addAll(mergedBlocks, rockSubs);
            mergedBlocks.addAll(masonryBlocks);
            String[] mergedBlockArr = mergedBlocks.toArray(new String[0]);

            // Auto-derive vanilla stairs/halfs/roofing from rock substitution keys
            String[] vanillaStairs  = deriveExistingVariants(rockSubs, "_Stairs");
            String[] vanillaHalfs   = deriveExistingVariants(rockSubs, "_Half");
            String[] vanillaRoofing = deriveExistingRoofing(rockSubs);

            // Merge stairs: vanilla stair derivation + masonry stairs
            LinkedHashSet<String> mergedStairs = new LinkedHashSet<>();
            if (vanillaStairs != null) Collections.addAll(mergedStairs, vanillaStairs);
            if (masonryStairs != null) mergedStairs.addAll(masonryStairs);
            String[] mergedStairArr = mergedStairs.toArray(new String[0]);

            // Merge halfs: vanilla half derivation + masonry halfs
            LinkedHashSet<String> mergedHalfs = new LinkedHashSet<>();
            if (vanillaHalfs != null) Collections.addAll(mergedHalfs, vanillaHalfs);
            if (masonryHalfs != null) mergedHalfs.addAll(masonryHalfs);
            String[] mergedHalfArr = mergedHalfs.toArray(new String[0]);

            // Roofing: only vanilla (Ymmersive has no roofing)
            String[] roofingArr = vanillaRoofing != null ? vanillaRoofing : new String[0];

            // Inject onto each masonry variant BlockType (full blocks, stairs, halfs)
            List<String> allVariantKeys = new ArrayList<>();
            allVariantKeys.addAll(masonryBlocks);
            if (masonryStairs != null) allVariantKeys.addAll(masonryStairs);
            if (masonryHalfs  != null) allVariantKeys.addAll(masonryHalfs);

            for (String variantKey : allVariantKeys) {
                try {
                    BlockType bt = BlockType.fromString(variantKey);
                    if (bt == null) {
                        failed++;
                        continue;
                    }

                    // Skip if already has Chisel.Data (preserves JsonModCompat data)
                    StateData existing = bt.getState();
                    if (existing instanceof Chisel.Data existingData) {
                        // If it already has a source (not empty), preserve it (likely from JsonModCompat)
                        if (existingData.source != null && !existingData.source.isEmpty()) {
                            continue;
                        }
                        // If source is empty/null, it might be from another compat system, allow overwrite
                    }

                    Chisel.Data data = new Chisel.Data();
                    data.source        = stoneType;
                    data.substitutions = mergedBlockArr;
                    data.stairs        = mergedStairArr;
                    data.halfSlabs     = mergedHalfArr;
                    data.roofing       = roofingArr;

                    setField(StateData.class, data, "id", "Ev0sChisel");
                    setField(BlockType.class, bt, "state", data);

                    injected++;
                } catch (Exception e) {
                    LOGGER.atWarning().log("[Chisel] Failed to inject state for "
                            + variantKey + ": " + e.getMessage());
                    failed++;
                }
            }
        }

        LOGGER.atInfo().log("[Chisel] Injected Chisel state onto " + injected
                + " masonry blocks" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    /**
     * Reads the Chisel substitutions already defined on the rock block for
     * the given stone type (e.g. {@code "Rock_Stone"}).
     */
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

    /** Reflective field setter – handles protected / private fields. */
    private static void setField(Class<?> clazz, Object target,
                                 String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Auto-derivation utilities (for vanilla rock blocks)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * For each substitution key, checks if {@code key + suffix} exists as a
     * registered BlockType and, if so, adds it to the result.
     *
     * @param substitutions the base substitution keys (e.g. {@code "Rock_Stone_Brick"})
     * @param suffix        the suffix to append (e.g. {@code "_Stairs"} or {@code "_Half"})
     * @return array of valid derived keys, or null if substitutions was null
     */
    public static String[] deriveExistingVariants(String[] substitutions, String suffix) {
        if (substitutions == null) return null;
        List<String> result = new ArrayList<>();
        for (String sub : substitutions) {
            String candidate = sub + suffix;
            try {
                if (BlockType.fromString(candidate) != null) {
                    result.add(candidate);
                }
            } catch (Exception ignored) { /* block type doesn't exist */ }
        }
        return result.toArray(new String[0]);
    }

    /** Roof suffixes to probe for each base block. */
    private static final String[] ROOF_SUFFIXES = {
            "_Roof", "_Roof_Flat", "_Roof_Hollow", "_Roof_Shallow", "_Roof_Steep"
    };

    /**
     * Derives roofing variants by checking all roof sub-variants
     * ({@code _Roof}, {@code _Roof_Flat}, etc.) for each substitution key.
     */
    public static String[] deriveExistingRoofing(String[] substitutions) {
        if (substitutions == null) return null;
        List<String> result = new ArrayList<>();
        for (String sub : substitutions) {
            for (String suffix : ROOF_SUFFIXES) {
                String candidate = sub + suffix;
                try {
                    if (BlockType.fromString(candidate) != null) {
                        result.add(candidate);
                    }
                } catch (Exception ignored) { /* block type doesn't exist */ }
            }
        }
        return result.toArray(new String[0]);
    }
}
