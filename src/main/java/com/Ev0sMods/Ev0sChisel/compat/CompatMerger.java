package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Unified compatibility merger that collects contributions from all compat systems
 * and merges them into cohesive Chisel.Data for each material type.
 */
public final class CompatMerger {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Map<String, MergedChiselData> MERGED_DATA_BY_TYPE = new LinkedHashMap<>();
    private static final Set<String> PROCESSED_BLOCKS = new HashSet<>();

    private CompatMerger() {}

    public static void mergeAllCompatData() {
        LOGGER.atInfo().log("[Chisel] Starting unified compatibility merge...");

        MERGED_DATA_BY_TYPE.clear();
        PROCESSED_BLOCKS.clear();

        collectAllContributions();
        injectMergedData();

        LOGGER.atInfo().log("[Chisel] Unified compatibility merge completed. Processed "
                + PROCESSED_BLOCKS.size() + " blocks across " + MERGED_DATA_BY_TYPE.size() + " material types");
    }

    private static void collectAllContributions() {
        LOGGER.atInfo().log("[Chisel] Collecting all compat contributions...");
        
        String[] rockTypes = VanillaCompat.getRockTypes();
        
        for (String rockType : rockTypes) {
            String normType = rockType.toLowerCase(Locale.ROOT);
            MergedChiselData merged = MERGED_DATA_BY_TYPE.computeIfAbsent(normType, k -> new MergedChiselData(rockType));
            
            collectVanillaRockForType(rockType, merged);
            
            if (MasonryCompat.isAvailable()) {
                collectMasonryForType(rockType, merged);
            }
            
            if (StoneworksCompat.isAvailable() && rockType.equalsIgnoreCase("Stone")) {
                collectStoneworksForType(merged);
            }
            
            if (MacawCompat.isPathsAvailable()) {
                collectMacawPathsForType(rockType, merged);
            }
            
            if (MacawCompat.isStairsAvailable()) {
                collectMacawStairsForType(rockType, merged);
            }
        }
        
        String[] woodTypes = VanillaCompat.getWoodTypes();
        
        for (String woodType : woodTypes) {
            String normType = woodType.toLowerCase(Locale.ROOT);
            MergedChiselData merged = MERGED_DATA_BY_TYPE.computeIfAbsent(normType, k -> new MergedChiselData(woodType));
            
            collectVanillaWoodForType(woodType, merged);
            
            if (CarpentryCompat.isAvailable()) {
                collectCarpentryForType(woodType, merged);
            }
        }
        
        linkWoodTypes();
        
        LOGGER.atInfo().log("[Chisel] Collected contributions for " + MERGED_DATA_BY_TYPE.size() + " material types");
    }

    private static void linkWoodTypes() {
        // Each wood type gets its OWN planks/logs - no cross-linking between different wood types
        // This prevents Lightwood from getting Darkwood's data
        for (MergedChiselData merged : MERGED_DATA_BY_TYPE.values()) {
            String source = merged.getSource();
            if (!isWoodType(source)) continue;
            
            // Only add variants from THIS wood type
            String baseKey = "Wood_" + source;
            if (exists(baseKey)) merged.addSubstitution(baseKey);
            if (exists(baseKey + "_Planks")) merged.addSubstitution(baseKey + "_Planks");
            if (exists(baseKey + "_Log")) merged.addSubstitution(baseKey + "_Log");
            if (exists(baseKey + "_Stripped_Log")) merged.addSubstitution(baseKey + "_Stripped_Log");
            if (exists(baseKey + "_Bark")) merged.addSubstitution(baseKey + "_Bark");
            if (exists(baseKey + "_Stripped_Bark")) merged.addSubstitution(baseKey + "_Stripped_Bark");
            
            // Also add roofing variants for this wood type
            String[] woodRoofSuffixes = {"_Roof", "_Roof_Flat", "_Roof_Hollow", "_Roof_Shallow", "_Roof_Steep",
                                         "_Shingle", "_Shingle_Flat", "_Shingle_Hollow", "_Shingle_Shallow", "_Shingle_Steep"};
            for (String suffix : woodRoofSuffixes) {
                if (exists(baseKey + suffix)) merged.addRoofing(baseKey + suffix);
            }
        }
        
        // Handle wood aliases: certain wood names map to specific types
        // e.g., Amber -> Goldenwood
        applyWoodAliases();
    }
    
    private static void applyWoodAliases() {
        // Map of ALL alternative wood names -> canonical wood type (11 base types from wiki)
        String[][] woodAliases = {
            // Whitewood variants
            {"Amber", "Whitewood"},
            {"Sun", "Whitewood"},
            {"Gold", "Whitewood"},
            {"Yellow", "Whitewood"},
            {"Honey", "Whitewood"},
            {"Golden", "Whitewood"},
            
            // Softwood variants
            {"Sakura", "Softwood"},
            {"Lavender", "Softwood"},
            {"Willow", "Softwood"},
            {"Pine", "Softwood"},
            {"Cedar", "Softwood"},
            {"Fir", "Softwood"},
            {"Spruce", "Softwood"},
            {"Juniper", "Softwood"},
            {"Cypress", "Softwood"},
            {"Hemlock", "Softwood"},
            {"Redwood", "Softwood"},
            
            // Darkwood variants
            {"Spirit", "Darkwood"},
            {"Shadow", "Darkwood"},
            {"Ebony", "Darkwood"},
            {"Walnut", "Darkwood"},
            {"Mahogany", "Darkwood"},
            {"Cocoa", "Darkwood"},
            {"Chocolate", "Darkwood"},
            {"Umber", "Darkwood"},
            
            // Lightwood variants
            {"Eternity", "Lightwood"},
            {"Moon", "Lightwood"},
            {"White", "Lightwood"},
            {"Birch", "Lightwood"},
            {"Ash", "Lightwood"},
            {"Meadow", "Lightwood"},
            {"Pale", "Lightwood"},
            {"Aura", "Lightwood"},
            
            // Hardwood variants
            {"Ocean", "Hardwood"},
            {"Teak", "Hardwood"},
            {"Jungle", "Hardwood"},
            {"Acacia", "Hardwood"},
            {"Mahogany", "Hardwood"},
            
            // Tropicalwood variants
            {"Palm", "Tropicalwood"},
            {"Coconut", "Tropicalwood"},
            {"Mangrove", "Tropicalwood"},
            {"Banana", "Tropicalwood"},
            {"Rainforest", "Tropicalwood"},
            {"Island", "Tropicalwood"},
            {"Reef", "Tropicalwood"},
            
            // Drywood variants
            {"Bamboo", "Drywood"},
            {"Savanna", "Drywood"},
            {"Desert", "Drywood"},
            {"Cactus", "Drywood"},
            {"Driftwood", "Drywood"},
            {"Arid", "Drywood"},
            
            // Deadwood variants
            {"Cursed", "Deadwood"},
            {"Blessed", "Deadwood"},
            {"Ancient", "Deadwood"},
            {"Rotten", "Deadwood"},
            {"Fossil", "Deadwood"},
            {"Dried", "Deadwood"},
            {"Withered", "Deadwood"},
            {"Spooky", "Deadwood"},
            {"Elder", "Deadwood"},
            
            // Greenwood variants
            {"Twilight", "Greenwood"},
            {"Mystic", "Greenwood"},
            {"Emerald", "Greenwood"},
            {"Forest", "Greenwood"},
            {"Mossy", "Greenwood"},
            {"Jade", "Greenwood"},
            {"Vine", "Greenwood"},
            {"Nature", "Greenwood"},
            
            // Redwood variants
            {"Sequoia", "Redwood"},
            {"Giant", "Redwood"},
            {"Bloodwood", "Redwood"},
            {"Cherry", "Redwood"},
            {"Mammoth", "Redwood"},
            
            // Blackwood variants
            {"Obsidian", "Blackwood"},
            {"Charcoal", "Blackwood"},
            {"Raven", "Blackwood"},
            {"Onyx", "Blackwood"},
            {"Void", "Blackwood"},
        };
        
        for (String[] alias : woodAliases) {
            String aliasName = alias[0];
            String canonicalName = alias[1];
            
            // If alias exists but canonical doesn't, create merged data from alias
            String aliasKey = "Wood_" + aliasName;
            String canonicalKey = "Wood_" + canonicalName;
            
            if (exists(aliasKey) && !exists(canonicalKey)) {
                // Alias exists but canonical doesn't - create merged data
                String normType = aliasName.toLowerCase(Locale.ROOT);
                MergedChiselData merged = MERGED_DATA_BY_TYPE.computeIfAbsent(normType, k -> new MergedChiselData(aliasName));
                
                // Copy all substitutions from canonical if it exists
                String normCanonical = canonicalName.toLowerCase(Locale.ROOT);
                MergedChiselData canonical = MERGED_DATA_BY_TYPE.get(normCanonical);
                if (canonical != null) {
                    for (String sub : canonical.getSubstitutionsArray()) {
                        merged.addSubstitution(sub);
                    }
                    for (String stair : canonical.getStairsArray()) {
                        merged.addStair(stair);
                    }
                    for (String half : canonical.getHalfsArray()) {
                        merged.addHalf(half);
                    }
                    for (String roof : canonical.getRoofingArray()) {
                        merged.addRoofing(roof);
                    }
                }
                
                // Add alias variants
                if (exists(aliasKey)) merged.addSubstitution(aliasKey);
                if (exists(aliasKey + "_Planks")) merged.addSubstitution(aliasKey + "_Planks");
                if (exists(aliasKey + "_Log")) merged.addSubstitution(aliasKey + "_Log");
                if (exists(aliasKey + "_Stripped_Log")) merged.addSubstitution(aliasKey + "_Stripped_Log");
                if (exists(aliasKey + "_Bark")) merged.addSubstitution(aliasKey + "_Bark");
                if (exists(aliasKey + "_Stripped_Bark")) merged.addSubstitution(aliasKey + "_Stripped_Bark");
            }
        }
    }
    
    private static boolean isWoodType(String type) {
        for (String wood : VanillaCompat.getWoodTypes()) {
            if (type.equalsIgnoreCase(wood)) return true;
        }
        return false;
    }

    private static void collectMasonryForType(String stoneType, MergedChiselData merged) {
        List<String> masonryBlocks = MasonryCompat.getVariants(stoneType);
        List<String> masonryStairs = MasonryCompat.getStairVariants(stoneType);
        List<String> masonryHalfs = MasonryCompat.getHalfVariants(stoneType);
        
        for (String block : masonryBlocks) {
            if (exists(block)) merged.addSubstitution(block);
        }
        for (String stair : masonryStairs) {
            if (exists(stair)) merged.addStair(stair);
        }
        for (String half : masonryHalfs) {
            if (exists(half)) merged.addHalf(half);
        }
    }

    private static void collectStoneworksForType(MergedChiselData merged) {
        List<String> stoneworksBlocks = StoneworksCompat.getVariants();
        for (String block : stoneworksBlocks) {
            if (exists(block)) merged.addSubstitution(block);
        }
    }

    private static void collectMacawPathsForType(String rockType, MergedChiselData merged) {
        List<String> pathsBlocks = MacawCompat.getPathsBlocks(rockType);
        List<String> pathsStairs = MacawCompat.getPathsStairs(rockType);
        List<String> pathsHalfs = MacawCompat.getPathsHalfs(rockType);
        
        for (String block : pathsBlocks) {
            if (exists(block)) merged.addSubstitution(block);
        }
        for (String stair : pathsStairs) {
            if (exists(stair)) merged.addStair(stair);
        }
        for (String half : pathsHalfs) {
            if (exists(half)) merged.addHalf(half);
        }
    }

    private static void collectMacawStairsForType(String stoneType, MergedChiselData merged) {
        List<String> mcwStairs = MacawCompat.getMcwStairs(stoneType);
        for (String stair : mcwStairs) {
            if (exists(stair)) merged.addStair(stair);
        }
    }

    private static void collectCarpentryForType(String woodType, MergedChiselData merged) {
        List<String> carpBlocks = CarpentryCompat.getVariants(woodType);
        List<String> carpStairs = CarpentryCompat.getStairVariants(woodType);
        List<String> carpHalfs = CarpentryCompat.getHalfVariants(woodType);
        
        for (String block : carpBlocks) {
            if (exists(block)) merged.addSubstitution(block);
        }
        for (String stair : carpStairs) {
            if (exists(stair)) merged.addStair(stair);
        }
        for (String half : carpHalfs) {
            if (exists(half)) merged.addHalf(half);
        }
    }

    private static void collectVanillaRockForType(String rockType, MergedChiselData merged) {
        List<String> variants = discoverAllRockVariants(rockType);
        
        for (String variant : variants) {
            merged.addSubstitution(variant);
        }
        
        String[] variantArray = variants.toArray(new String[0]);
        
        String[] derivedStairs = deriveVariants(variantArray, "_Stairs");
        if (derivedStairs != null) {
            for (String stair : derivedStairs) merged.addStair(stair);
        }
        
        String[] derivedHalfs = deriveVariants(variantArray, "_Half");
        if (derivedHalfs != null) {
            for (String half : derivedHalfs) merged.addHalf(half);
        }
        
        String[] derivedRoofing = deriveRoofing(variantArray);
        if (derivedRoofing != null) {
            for (String roof : derivedRoofing) merged.addRoofing(roof);
        }
    }

    private static void collectVanillaWoodForType(String woodType, MergedChiselData merged) {
        List<String> variants = discoverVanillaWoodBlocks(woodType);
        
        for (String variant : variants) {
            merged.addSubstitution(variant);
        }
        
        String[] variantArray = variants.toArray(new String[0]);
        
        String[] derivedStairs = deriveVariants(variantArray, "_Stairs");
        if (derivedStairs != null) {
            for (String stair : derivedStairs) merged.addStair(stair);
        }
        
        String[] derivedHalfs = deriveVariants(variantArray, "_Half");
        if (derivedHalfs != null) {
            for (String half : derivedHalfs) merged.addHalf(half);
        }
        
        String[] derivedRoofing = deriveWoodRoofing(variantArray);
        if (derivedRoofing != null) {
            for (String roof : derivedRoofing) merged.addRoofing(roof);
        }
    }

    private static void injectMergedData() {
        int injected = 0;
        int failed = 0;

        for (Map.Entry<String, MergedChiselData> entry : MERGED_DATA_BY_TYPE.entrySet()) {
            MergedChiselData merged = entry.getValue();

            String[] substitutions = merged.getSubstitutionsArray();
            String[] stairs = merged.getStairsArray();
            String[] halfs = merged.getHalfsArray();
            String[] roofing = merged.getRoofingArray();
            String source = merged.getSource();

            LOGGER.atInfo().log("[Chisel] Injecting unified data for " + source 
                    + ": " + substitutions.length + " substitutions, " 
                    + stairs.length + " stairs, " + halfs.length + " halfs, " + roofing.length + " roofing");

            for (String blockKey : substitutions) {
                if (!PROCESSED_BLOCKS.add(blockKey)) {
                    continue;
                }

                try {
                    BlockType bt = BlockType.fromString(blockKey);
                    if (bt == null) {
                        failed++;
                        continue;
                    }

                    Chisel.Data data = new Chisel.Data();
                    data.source = source;
                    data.substitutions = substitutions;
                    data.stairs = stairs;
                    data.halfSlabs = halfs;
                    data.roofing = roofing;

                    setField(StateData.class, data, "id", "Ev0sChisel");
                    setField(BlockType.class, bt, "state", data);
                    injected++;
                } catch (Exception e) {
                    LOGGER.atWarning().log("[Chisel] Failed to inject merged data for "
                            + blockKey + ": " + e.getMessage());
                    failed++;
                }
            }

            for (String stairKey : stairs) {
                if (!PROCESSED_BLOCKS.add(stairKey)) {
                    continue;
                }
                try {
                    BlockType bt = BlockType.fromString(stairKey);
                    if (bt == null) failed++;
                } catch (Exception e) {
                    failed++;
                }
            }

            for (String halfKey : halfs) {
                if (!PROCESSED_BLOCKS.add(halfKey)) {
                    continue;
                }
                try {
                    BlockType bt = BlockType.fromString(halfKey);
                    if (bt == null) failed++;
                } catch (Exception e) {
                    failed++;
                }
            }
        }

        LOGGER.atInfo().log("[Chisel] Merged compatibility injection completed: "
                + injected + " blocks injected, " + failed + " failed");
    }

    private static String[] deriveVariants(String[] bases, String suffix) {
        if (bases == null || bases.length == 0) return null;
        List<String> result = new ArrayList<>();
        for (String base : bases) {
            String candidate = base + suffix;
            if (exists(candidate)) {
                result.add(candidate);
            }
        }
        return result.isEmpty() ? null : result.toArray(new String[0]);
    }

    private static String[] deriveRoofing(String[] bases) {
        if (bases == null || bases.length == 0) return null;
        String[] roofSuffixes = {"_Roof", "_Roof_Flat", "_Roof_Hollow", "_Roof_Shallow", "_Roof_Steep"};
        List<String> result = new ArrayList<>();
        for (String base : bases) {
            for (String suffix : roofSuffixes) {
                String candidate = base + suffix;
                if (exists(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return result.isEmpty() ? null : result.toArray(new String[0]);
    }

    private static String[] deriveWoodRoofing(String[] bases) {
        if (bases == null || bases.length == 0) return null;
        String[] roofSuffixes = {"_Roof", "_Roof_Flat", "_Roof_Hollow", "_Roof_Shallow", "_Roof_Steep",
                                  "_Shingle", "_Shingle_Flat", "_Shingle_Hollow", "_Shingle_Shallow", "_Shingle_Steep"};
        List<String> result = new ArrayList<>();
        for (String base : bases) {
            for (String suffix : roofSuffixes) {
                String candidate = base + suffix;
                if (exists(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return result.isEmpty() ? null : result.toArray(new String[0]);
    }

    private static List<String> discoverAllRockVariants(String rockType) {
        List<String> found = new ArrayList<>();
        Set<String> foundSet = new HashSet<>();
        
        String[] suffixes = {
            "", "_Cobble", "_Polished",
            "_Brick", "_Bricks",
            "_Tile", "_Tiles",
            "_Slab", "_Slabs",
            "_Cracked", "_Cracked_Bricks",
            "_Mossy", "_Mossy_Bricks", "_Mossy_Cobble",
            "_Chiseled", "_Smooth", "_Cut", "_Ornate", "_Decorative",
            "_Pillar"
        };
        
        // 1. Rock_{Type}
        String rockPrefixKey = "Rock_" + rockType;
        for (String suffix : suffixes) {
            String key = rockPrefixKey + suffix;
            if (exists(key) && foundSet.add(key)) {
                found.add(key);
            }
        }
        
        // 2. Just {Type} for compound types
        if (rockType.contains("_")) {
            for (String suffix : suffixes) {
                String key = rockType + suffix;
                if (exists(key) && foundSet.add(key)) {
                    found.add(key);
                }
            }
        }
        
        // 3. Special handling for Basalt
        if (rockType.equals("Basalt")) {
            String[] basaltSuffixes = {
                "", "_Cobble", "_Polished", "_Brick", "_Bricks",
                "_Tile", "_Tiles", "_Slab", "_Slabs",
                "_Cracked", "_Cracked_Bricks",
                "_Mossy", "_Mossy_Bricks", "_Mossy_Cobble",
                "_Chiseled", "_Smooth", "_Cut", "_Ornate", "_Decorative", "_Pillar"
            };
            if (exists("Basalt_Brick")) {
                for (String suffix : basaltSuffixes) {
                    String key = "Basalt_Brick" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
            if (exists("Rock_Basalt_Brick")) {
                for (String suffix : basaltSuffixes) {
                    String key = "Rock_Basalt_Brick" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
            if (exists("Rock_Basalt")) {
                for (String suffix : suffixes) {
                    String key = "Rock_Basalt" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
        }
        
        // 4. Sandstone
        if (rockType.equals("Sandstone") && exists("Rock_Sandstone")) {
            String[] sandstoneSuffixes = {"", "_Cut", "_Chiseled", "_Smooth", "_Cobble", "_Polished", "_Brick", "_Bricks", "_Tile", "_Tiles", "_Slab", "_Slabs"};
            for (String suffix : sandstoneSuffixes) {
                String key = "Rock_Sandstone" + suffix;
                if (exists(key) && foundSet.add(key)) found.add(key);
            }
        }
        
        // 5. Sandstone_Red
        if (rockType.equals("Sandstone_Red")) {
            if (exists("Rock_Sandstone_Red")) {
                for (String suffix : suffixes) {
                    String key = "Rock_Sandstone_Red" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
            if (exists("Sandstone_Red")) {
                for (String suffix : suffixes) {
                    String key = "Sandstone_Red" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
        }
        
        // 6. Sandstone_White
        if (rockType.equals("Sandstone_White")) {
            if (exists("Rock_Sandstone_White")) {
                for (String suffix : suffixes) {
                    String key = "Rock_Sandstone_White" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
            if (exists("Sandstone_White")) {
                for (String suffix : suffixes) {
                    String key = "Sandstone_White" + suffix;
                    if (exists(key) && foundSet.add(key)) found.add(key);
                }
            }
        }
        
        return found;
    }

    private static List<String> discoverVanillaWoodBlocks(String woodType) {
        List<String> found = new ArrayList<>();
        String baseKey = "Wood_" + woodType;
        if (exists(baseKey)) {
            found.add(baseKey);
        }
        String[] variants = {
            baseKey + "_Planks", baseKey + "_Log", baseKey + "_Stripped_Log",
            baseKey + "_Bark", baseKey + "_Stripped_Bark"
        };
        for (String variant : variants) {
            if (exists(variant)) {
                found.add(variant);
            }
        }
        return found;
    }

    private static boolean exists(String key) {
        try {
            return BlockType.fromString(key) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setField(Class<?> clazz, Object target,
                                 String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class MergedChiselData {
        private final String source;
        private final LinkedHashSet<String> substitutions = new LinkedHashSet<>();
        private final LinkedHashSet<String> stairs = new LinkedHashSet<>();
        private final LinkedHashSet<String> halfs = new LinkedHashSet<>();
        private final LinkedHashSet<String> roofing = new LinkedHashSet<>();

        public MergedChiselData(String source) {
            this.source = source;
        }

        public void addSubstitution(String key) { substitutions.add(key); }
        public void addStair(String key) { stairs.add(key); }
        public void addHalf(String key) { halfs.add(key); }
        public void addRoofing(String key) { roofing.add(key); }
        public String getSource() { return source; }

        public String[] getSubstitutionsArray() { return substitutions.toArray(new String[0]); }
        public String[] getStairsArray() { return stairs.toArray(new String[0]); }
        public String[] getHalfsArray() { return halfs.toArray(new String[0]); }
        public String[] getRoofingArray() { return roofing.toArray(new String[0]); }
    }
}
