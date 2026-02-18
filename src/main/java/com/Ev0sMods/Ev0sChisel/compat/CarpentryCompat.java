package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Compatibility layer for Ymmersive Carpentry.
 * <p>
 * When the mod is installed, this class provides carpentry variant block keys
 * grouped by wood type.  Each wood type has 12 design patterns, each available
 * as a full block, stairs, and half-slab.
 * <p>
 * Also injects {@link Chisel.Data} onto vanilla wood trunk / plank blocks so
 * they become chiselable into carpentry designs at runtime.
 */
public final class CarpentryCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Whether Ymmersive Carpentry was detected on the classpath. */
    private static boolean detected = false;

    /** Wood type (normalised) → unmodifiable list of full-block variant keys. */
    private static final Map<String, List<String>> VARIANTS_BY_TYPE = new LinkedHashMap<>();

    /** Wood type (normalised) → unmodifiable list of stair variant keys. */
    private static final Map<String, List<String>> STAIR_VARIANTS_BY_TYPE = new LinkedHashMap<>();

    /** Wood type (normalised) → unmodifiable list of half-slab variant keys. */
    private static final Map<String, List<String>> HALF_VARIANTS_BY_TYPE = new LinkedHashMap<>();

    /**
     * Wood-block prefix (normalised, e.g. "wood_hardwood") → wood type key
     * (normalised, e.g. "hardwood").  Sorted longest-first so
     * "wood_tropicalwood" matches before shorter prefixes.
     */
    private static final List<Map.Entry<String, String>> WOOD_PREFIX_TO_TYPE = new ArrayList<>();

    /**
     * Bare type-name prefix (normalised, e.g. "hardwood") → wood type key.
     * Sorted longest-first.  Used to match carpentry variant block keys.
     */
    private static final List<Map.Entry<String, String>> TYPE_PREFIX_TO_TYPE = new ArrayList<>();

    // ── The 12 carpentry design patterns ────────────────────────────────
    private static final String[] DESIGNS = {
            "Arenberg", "Chantilly", "Chevron", "Cube", "Double_Herringbone",
            "Mosaic", "Panel", "Parquet", "Strips", "Thin_Herringbone",
            "Versailles", "Weaves"
    };

    // ── Wood types that have carpentry variants ─────────────────────────
    private static final String[] WOOD_TYPES = {
            "Blackwood", "Darkwood", "Deadwood", "Drywood", "Goldenwood",
            "Greenwood", "Hardwood", "Lightwood", "Redwood", "Softwood",
            "Tropicalwood"
    };

    /** Vanilla wood block name suffixes to probe for injection. */
    private static final String[] VANILLA_WOOD_SUFFIXES = {
            "", "_Planks", "_Log", "_Stripped_Log", "_Stripped"
    };

    private CarpentryCompat() {} // utility class

    // ─────────────────────────────────────────────────────────────────────
    // Initialisation – call once during plugin setup
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Probes the classpath for <code>net.conczin.YmmersiveCarpentry</code>.
     * If found, populates the variant maps.
     */
    public static void init() {
        try {
            Class.forName("net.conczin.YmmersiveCarpentry");
            detected = true;
            LOGGER.atInfo().log("[Chisel] Ymmersive Carpentry detected – loading wood variants");
            buildVariantMap();
            buildWoodPrefixMap();
        } catch (ClassNotFoundException e) {
            detected = false;
            LOGGER.atInfo().log("[Chisel] Ymmersive Carpentry not found – carpentry compat disabled");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /** @return true when Ymmersive Carpentry is present on this server */
    public static boolean isAvailable() {
        return detected;
    }

    /**
     * Returns the carpentry full-block variant keys for the given wood type,
     * or an empty list if the type is unknown or carpentry is absent.
     */
    public static List<String> getVariants(String woodType) {
        if (!detected || woodType == null) return Collections.emptyList();
        return VARIANTS_BY_TYPE.getOrDefault(normalise(woodType), Collections.emptyList());
    }

    /**
     * Returns the carpentry stair variant keys for the given wood type.
     */
    public static List<String> getStairVariants(String woodType) {
        if (!detected || woodType == null) return Collections.emptyList();
        return STAIR_VARIANTS_BY_TYPE.getOrDefault(normalise(woodType), Collections.emptyList());
    }

    /**
     * Returns the carpentry half-slab variant keys for the given wood type.
     */
    public static List<String> getHalfVariants(String woodType) {
        if (!detected || woodType == null) return Collections.emptyList();
        return HALF_VARIANTS_BY_TYPE.getOrDefault(normalise(woodType), Collections.emptyList());
    }

    /**
     * Returns every registered wood type that has carpentry variants.
     */
    public static Set<String> getWoodTypes() {
        return Collections.unmodifiableSet(VARIANTS_BY_TYPE.keySet());
    }

    /**
     * Detects the wood type by examining the existing substitution keys.
     * Looks for keys that start with a known Wood-block prefix
     * (e.g. "Wood_Hardwood", "Wood_Softwood") and returns the matching wood
     * type, or falls back to recognising Ymmersive Carpentry block-name
     * prefixes ({@code WoodType_Design}).
     *
     * @param substitutions the existing chisel substitution keys
     * @return the detected wood type (normalised), or null
     */
    /**
     * Detects the wood type by first checking the clicked block's own key,
     * then falling back to scanning the substitution array.
     *
     * @param blockKey      the {@link BlockType#getId()} of the clicked block (nullable)
     * @param substitutions the existing chisel substitution keys
     * @return the detected wood type (normalised), or null
     */
    public static String detectWoodType(String blockKey, String[] substitutions) {
        if (!detected) return null;

        // Priority: match the block's own key
        if (blockKey != null) {
            String keyNorm = blockKey.toLowerCase(Locale.ROOT);
            // Check "Wood_{Type}" prefix  (e.g. "Wood_Hardwood_Planks")
            for (Map.Entry<String, String> entry : WOOD_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (keyNorm.equals(prefix) || keyNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
            // Check bare type name  (e.g. "Hardwood_Arenberg" → hardwood)
            for (Map.Entry<String, String> entry : TYPE_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (keyNorm.equals(prefix) || keyNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
        }

        // Fallback: scan substitutions
        return detectWoodType(substitutions);
    }

    /**
     * Detects the wood type by examining the existing substitution keys.
     *
     * @param substitutions the existing chisel substitution keys
     * @return the detected wood type (normalised), or null
     */
    public static String detectWoodType(String[] substitutions) {
        if (!detected || substitutions == null) return null;
        for (String sub : substitutions) {
            if (sub == null) continue;
            String subNorm = sub.toLowerCase(Locale.ROOT);
            // Check vanilla wood prefix (Wood_Hardwood, Wood_Softwood, …)
            for (Map.Entry<String, String> entry : WOOD_PREFIX_TO_TYPE) {
                String prefix = entry.getKey();
                if (subNorm.equals(prefix) || subNorm.startsWith(prefix + "_")) {
                    return entry.getValue();
                }
            }
        }
        // Fallback: check for Carpentry block-name prefix ({WoodType}_{Design})
        for (String sub : substitutions) {
            if (sub == null) continue;
            for (String woodType : WOOD_TYPES) {
                String typePrefix = woodType + "_";
                if (sub.startsWith(typePrefix)) {
                    String rest = sub.substring(typePrefix.length());
                    for (String design : DESIGNS) {
                        if (rest.equals(design)
                                || rest.equals(design + "_Stairs")
                                || rest.equals(design + "_Half")) {
                            return normalise(woodType);
                        }
                    }
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    private static void buildVariantMap() {
        for (String type : WOOD_TYPES) {
            String normType = normalise(type);
            List<String> blockKeys = new ArrayList<>(DESIGNS.length);
            List<String> stairKeys = new ArrayList<>(DESIGNS.length);
            List<String> halfKeys  = new ArrayList<>(DESIGNS.length);
            for (String design : DESIGNS) {
                String base = type + "_" + design;
                blockKeys.add(base);
                stairKeys.add(base + "_Stairs");
                halfKeys.add(base + "_Half");
            }
            VARIANTS_BY_TYPE.put(normType, Collections.unmodifiableList(blockKeys));
            STAIR_VARIANTS_BY_TYPE.put(normType, Collections.unmodifiableList(stairKeys));
            HALF_VARIANTS_BY_TYPE.put(normType, Collections.unmodifiableList(halfKeys));
        }
        LOGGER.atInfo().log("[Chisel] Loaded carpentry variants for " + VARIANTS_BY_TYPE.size()
                + " wood types (blocks + stairs + halfs)");
    }

    /**
     * Builds the wood-prefix lookup sorted by descending prefix length
     * so that "wood_tropicalwood" is tested before shorter prefixes.
     */
    private static void buildWoodPrefixMap() {
        for (String type : WOOD_TYPES) {
            String normType = normalise(type);
            String woodPrefix = normalise("Wood_" + type);
            WOOD_PREFIX_TO_TYPE.add(Map.entry(woodPrefix, normType));
        }
        WOOD_PREFIX_TO_TYPE.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        // Build bare type-name prefix map (for matching carpentry variant keys)
        for (String type : WOOD_TYPES) {
            String normType = normalise(type);
            TYPE_PREFIX_TO_TYPE.add(Map.entry(normType, normType));
        }
        TYPE_PREFIX_TO_TYPE.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        LOGGER.atInfo().log("[Chisel] Built wood-prefix map with " + WOOD_PREFIX_TO_TYPE.size()
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
     * every carpentry variant {@link BlockType} <b>and</b> to vanilla wood
     * trunk / plank blocks so they are all chiselable at runtime.
     * <p>
     * Must be called <b>after</b> all asset packs have been loaded
     * (i.e. from {@code start()}, not {@code setup()}).
     */
    public static void injectChiselStates() {
        if (!detected) return;

        int injected = 0;
        int failed   = 0;

        for (String woodType : WOOD_TYPES) {
            String normType = normalise(woodType);
            List<String> carpBlocks = VARIANTS_BY_TYPE.get(normType);
            List<String> carpStairs = STAIR_VARIANTS_BY_TYPE.get(normType);
            List<String> carpHalfs  = HALF_VARIANTS_BY_TYPE.get(normType);
            if (carpBlocks == null || carpBlocks.isEmpty()) continue;

            // Discover vanilla wood blocks for this wood type
            List<String> vanillaBlocks = discoverVanillaWoodBlocks(woodType);

            // Read any existing chisel substitutions from the vanilla wood block
            String[] existingSubs = getWoodSubstitutions(woodType);

            // ── Merge full blocks: existing subs + vanilla blocks + carpentry ─
            LinkedHashSet<String> mergedBlocks = new LinkedHashSet<>();
            if (existingSubs != null) Collections.addAll(mergedBlocks, existingSubs);
            mergedBlocks.addAll(vanillaBlocks);
            mergedBlocks.addAll(carpBlocks);
            String[] mergedBlockArr = mergedBlocks.toArray(new String[0]);

            // ── Auto-derive vanilla stairs / halfs / roofing ─────────────
            String[] vanillaBlockArr = vanillaBlocks.toArray(new String[0]);
            String[] vanillaStairs   = MasonryCompat.deriveExistingVariants(vanillaBlockArr, "_Stairs");
            String[] vanillaHalfs    = MasonryCompat.deriveExistingVariants(vanillaBlockArr, "_Half");
            String[] vanillaRoofing  = MasonryCompat.deriveExistingRoofing(vanillaBlockArr);

            // Also derive from existing subs (if any)
            if (existingSubs != null) {
                vanillaStairs  = mergeArrays(vanillaStairs,
                        MasonryCompat.deriveExistingVariants(existingSubs, "_Stairs"));
                vanillaHalfs   = mergeArrays(vanillaHalfs,
                        MasonryCompat.deriveExistingVariants(existingSubs, "_Half"));
                vanillaRoofing = mergeArrays(vanillaRoofing,
                        MasonryCompat.deriveExistingRoofing(existingSubs));
            }

            // ── Merge stairs ────────────────────────────────────────────
            LinkedHashSet<String> mergedStairs = new LinkedHashSet<>();
            if (vanillaStairs != null) Collections.addAll(mergedStairs, vanillaStairs);
            if (carpStairs != null) mergedStairs.addAll(carpStairs);
            String[] mergedStairArr = mergedStairs.toArray(new String[0]);

            // ── Merge halfs ─────────────────────────────────────────────
            LinkedHashSet<String> mergedHalfs = new LinkedHashSet<>();
            if (vanillaHalfs != null) Collections.addAll(mergedHalfs, vanillaHalfs);
            if (carpHalfs != null) mergedHalfs.addAll(carpHalfs);
            String[] mergedHalfArr = mergedHalfs.toArray(new String[0]);

            // Roofing (carpentry has none – just vanilla derivations)
            String[] roofingArr = vanillaRoofing != null ? vanillaRoofing : new String[0];

            // ── Inject onto every carpentry variant block ────────────────
            List<String> allCarpentryKeys = new ArrayList<>();
            allCarpentryKeys.addAll(carpBlocks);
            if (carpStairs != null) allCarpentryKeys.addAll(carpStairs);
            if (carpHalfs  != null) allCarpentryKeys.addAll(carpHalfs);

            for (String variantKey : allCarpentryKeys) {
                try {
                    BlockType bt = BlockType.fromString(variantKey);
                    if (bt == null) { failed++; continue; }

                    Chisel.Data data = new Chisel.Data();
                    data.source        = woodType;
                    data.substitutions = mergedBlockArr;
                    data.stairs        = mergedStairArr;
                    data.halfSlabs     = mergedHalfArr;
                    data.roofing       = roofingArr;

                    setField(StateData.class, data, "id", "Ev0sChisel");
                    setField(BlockType.class, bt, "state", data);
                    injected++;
                } catch (Exception e) {
                    LOGGER.atWarning().log("[Chisel] Failed to inject carpentry state for "
                            + variantKey + ": " + e.getMessage());
                    failed++;
                }
            }

            // ── Inject onto vanilla wood blocks (trunks, planks, etc.) ──
            for (String vanillaKey : vanillaBlocks) {
                try {
                    BlockType bt = BlockType.fromString(vanillaKey);
                    if (bt == null) { failed++; continue; }

                    // Don't overwrite if the block already has Chisel state
                    StateData existing = bt.getState();
                    if (existing instanceof Chisel.Data) continue;

                    Chisel.Data data = new Chisel.Data();
                    data.source        = woodType;
                    data.substitutions = mergedBlockArr;
                    data.stairs        = mergedStairArr;
                    data.halfSlabs     = mergedHalfArr;
                    data.roofing       = roofingArr;

                    setField(StateData.class, data, "id", "Ev0sChisel");
                    setField(BlockType.class, bt, "state", data);
                    injected++;
                } catch (Exception e) {
                    LOGGER.atWarning().log("[Chisel] Failed to inject wood state for "
                            + vanillaKey + ": " + e.getMessage());
                    failed++;
                }
            }
        }

        LOGGER.atInfo().log("[Chisel] Injected Chisel state onto " + injected
                + " carpentry / wood blocks" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Vanilla wood block discovery / reading
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Discovers vanilla wood blocks for the given wood type by probing
     * known name patterns ({@code Wood_{WoodType}}, {@code Wood_{WoodType}_Planks},
     * {@code Wood_{WoodType}_Log}, etc.).
     */
    private static List<String> discoverVanillaWoodBlocks(String woodType) {
        List<String> found = new ArrayList<>();
        for (String suffix : VANILLA_WOOD_SUFFIXES) {
            String candidate = "Wood_" + woodType + suffix;
            try {
                if (BlockType.fromString(candidate) != null) {
                    found.add(candidate);
                }
            } catch (Exception ignored) { /* block type doesn't exist */ }
        }
        if (!found.isEmpty()) {
            LOGGER.atInfo().log("[Chisel] Discovered " + found.size()
                    + " vanilla wood blocks for " + woodType + ": " + found);
        }
        return found;
    }

    /**
     * Reads the Chisel substitutions already defined on the vanilla wood
     * block for the given wood type (e.g. {@code "Wood_Hardwood"}).
     */
    private static String[] getWoodSubstitutions(String woodType) {
        try {
            BlockType woodBlock = BlockType.fromString("Wood_" + woodType);
            if (woodBlock == null) return null;
            StateData state = woodBlock.getState();
            if (state instanceof Chisel.Data chiselData) {
                return chiselData.substitutions;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("[Chisel] Could not read wood subs for Wood_"
                    + woodType + ": " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────

    /** Merges two nullable arrays into one de-duplicated array. */
    private static String[] mergeArrays(String[] a, String[] b) {
        if (a == null && b == null) return null;
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (a != null) Collections.addAll(set, a);
        if (b != null) Collections.addAll(set, b);
        return set.toArray(new String[0]);
    }

    /** Reflective field setter – handles protected / private fields. */
    private static void setField(Class<?> clazz, Object target,
                                 String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
