package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.ComboState;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Compatibility layer for <b>Serenal</b> layered windows.
 *
 * <p>Key pattern: {@code Serenal_Layer_{material}_Small_Window}
 *
 * <p>All window variants are grouped into one shared Chisel + CarpenterHammer
 * state so players can chisel between brick types and access them from the
 * windows tab of the Carpenter's Hammer.
 *
 * <p>Material tokens are taken directly from the JAR listing of
 * {@code SerenalProjectBuilding-1.5.2.jar}.
 */
public final class SerenalCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static boolean detected = false;

    private SerenalCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Exact material tokens as they appear in block keys
    // ─────────────────────────────────────────────────────────────────────

    /** Cloth material tokens used in Serenal layer shapes. */
    private static final String[] CLOTH_MATERIALS = {
            "Cloth_Block_Wool_Black",        "Cloth_Block_Wool_Blue",
            "Cloth_Block_Wool_Blue_Light",   "Cloth_Block_Wool_Cyan",
            "Cloth_Block_Wool_Cyan_Light",   "Cloth_Block_Wool_Gray",
            "Cloth_Block_Wool_Gray_Light",   "Cloth_Block_Wool_Green",
            "Cloth_Block_Wool_Green_Light",  "Cloth_Block_Wool_Orange",
            "Cloth_Block_Wool_Orange_Light", "Cloth_Block_Wool_Pink",
            "Cloth_Block_Wool_Pink_Light",   "Cloth_Block_Wool_Purple",
            "Cloth_Block_Wool_Purple_Light", "Cloth_Block_Wool_Red",
            "Cloth_Block_Wool_Red_Light",    "Cloth_Block_Wool_White",
            "Cloth_Block_Wool_Yellow",       "Cloth_Block_Wool_Yellow_Light"
    };

    /** All Serenal smooth clay color material tokens. */
    private static final String[] CLAY_MATERIALS = {
            "Soil_Clay_Smooth_Black",  "Soil_Clay_Smooth_Blue",
            "Soil_Clay_Smooth_Cyan",   "Soil_Clay_Smooth_Green",
            "Soil_Clay_Smooth_Grey",   "Soil_Clay_Smooth_Lime",
            "Soil_Clay_Smooth_Orange", "Soil_Clay_Smooth_Pink",
            "Soil_Clay_Smooth_Purple", "Soil_Clay_Smooth_Red",
            "Soil_Clay_Smooth_White",  "Soil_Clay_Smooth_Yellow"
    };

    /** All Serenal layer shape suffixes (shared by cloth and smooth clay). */
    private static final String[] LAYER_SHAPES = {
            "Arrowslit", "Capital", "Eighth_Slab", "Gothic_Arch",
            "Half_Arch", "Half_Arch_Half", "Pillar", "Quarter_Slab",
            "Round_Arch", "Segmental_Arch", "Slab_Layer", "Small_Corner",
            "Small_Window", "Stairs", "Standard_Arch", "Standard_Arch_Half",
            "Vertical_Corner", "Vertical_Corner_Slab", "Vertical_Quarter", "Vertical_Slab"
    };

    /** 24 brick materials for per-material chisel groups. */
    private static final String[] BRICK_MATERIALS = {
            "Rock_Aqua_Brick",          "Rock_Basalt_Brick",
            "Rock_Calcite_Brick",       "Rock_Chalk_Brick",
            "Rock_Gold_Brick",          "Rock_Ledge_Brick",
            "Rock_Lime_Brick",          "Rock_Marble_Brick",
            "Rock_Peach_Brick",         "Rock_Quartzite_Brick",
            "Rock_Runic_Blue_Brick",    "Rock_Runic_Brick",
            "Rock_Runic_Dark_Brick",    "Rock_Runic_Teal_Brick",
            "Rock_Sandstone_Brick",     "Rock_Sandstone_Red_Brick",
            "Rock_Sandstone_White_Brick","Rock_Shale_Brick",
            "Rock_Stone_Brick",         "Rock_Stone_Brick_Mossy",
            "Rock_Volcanic_Brick",
            "Soil_Clay_Brick",          "Soil_Clay_Ocean_Brick",
            "Soil_Snow_Brick"
    };

    /** Full arch/pillar shapes — go into chisel {@code substitutions}. */
    private static final String[] BRICK_SUB_SHAPES = {
            "Arrowslit", "Capital", "Gothic_Arch", "Half_Arch", "Half_Arch_Half",
            "Pillar", "Round_Arch", "Segmental_Arch", "Small_Corner", "Small_Window",
            "Standard_Arch", "Standard_Arch_Half", "Vertical_Corner"
    };

    /** Slab-type shapes — go into chisel {@code halfSlabs}. */
    private static final String[] BRICK_SLAB_SHAPES = {
            "Eighth_Slab", "Quarter_Slab", "Slab_Layer",
            "Vertical_Corner_Slab", "Vertical_Quarter", "Vertical_Slab"
    };

    /** Stone floor tile suffixes — all resolve as {@code Serenal_Stone_Floor_Tile{suffix}}. */
    private static final String[] STONE_TILE_SUFFIXES = {
            "01", "02", "06", "07", "08", "09", "10", "11", "12", "13",
            "14", "15", "16", "17", "18", "19",
            "_Black_White",  "_Blue_Fancy",   "_Blue_Fancy02",  "_Blue_Horse",
            "_Blue_Round",   "_Blue_Round02",  "_Blue_White",
            "_Brown_White",  "_Brown_White02", "_Green_Mosaic",  "_Red01"
    };

    /** Sandstone floor tile suffixes — all resolve as {@code Serenal_Sandstone_Floor_Tile{suffix}}. */
    private static final String[] SANDSTONE_TILE_SUFFIXES = {
            "_Blue",   "_Brown",   "_Brown02",  "_Colored", "_Diagonal", "_Fancy",
            "_Green",  "_Green02", "_Green03",
            "_Grey",   "_Grey02",  "_Grey03",
            "_Red",    "_Red02",   "_Red03"
    };

    /** Wood types that have Serenal staircase variants (01–04). */
    private static final String[] STAIRCASE_WOOD_TYPES = {
            "Birch", "Dark_Oak", "Jungle", "Oak", "Spruce",
            "Darkwood", "Goldenwood", "Hardwood", "Lightwood", "Softwood"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public static boolean isDetected() { return detected; }

    public static void init() {
        try {
            int total = injectBrickChisel();
            total += injectFloorTiles();
            total += injectWoodStaircases();
            total += injectAllWindows();
            registerSerenalClothWithVanillaCompat();
            total += injectColoredShapes("Serenal_Clay",  CLAY_MATERIALS);
            if (total > 0) {
                detected = true;
                LOGGER.atWarning().log("[SerenalCompat] Injected onto " + total + " blocks.");
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[SerenalCompat] Init failed: " + t.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Injection
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Collects all existing Serenal cloth block keys and registers them with
     * {@link VanillaClothCompat} so they are included in the unified
     * {@code Cloth_All} paintbrush group alongside vanilla wool, cloth-roof,
     * and Mcw carpet variants.  VanillaClothCompat runs after this method, so
     * the registration happens before its injection pass.
     */
    private static void registerSerenalClothWithVanillaCompat() {
        List<String> keys = new ArrayList<>();
        for (String material : CLOTH_MATERIALS) {
            for (String shape : LAYER_SHAPES) {
                String key = "Serenal_Layer_" + material + "_" + shape;
                if (exists(key)) keys.add(key);
            }
        }
        if (!keys.isEmpty()) VanillaClothCompat.registerExtraClothKeys(keys);
    }

    /**
     * For each layer shape, groups all color variants of {@code materials}
     * and injects {@link Paintbrush.Data} so the Paintbrush UI can recolor them.
     * Each shape type (Pillar, Stairs, etc.) forms its own color group.
     */
    private static int injectColoredShapes(String source, String[] materials) {
        int count = 0;
        for (String shape : LAYER_SHAPES) {
            List<String> group = new ArrayList<>();
            for (String material : materials) {
                String key = "Serenal_Layer_" + material + "_" + shape;
                if (exists(key)) group.add(key);
            }
            if (group.isEmpty()) continue;
            String[] arr = group.toArray(new String[0]);

            Paintbrush.Data pb = new Paintbrush.Data();
            pb.source        = source;
            pb.colorVariants = arr;

            for (String key : arr) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                if (ComboStateHelper.inject(bt, null, pb, null)) count++;
            }
        }
        return count;
    }

    /**
     * Groups all Stone and all Sandstone floor tile variants into two
     * separate chisel groups. Stair variants ({@code *_Stairs}) are placed
     * in the {@code stairs} slot so the Chisel UI separates flat vs stair.
     * <p>
     * After building each group the tile keys are also merged into the
     * corresponding vanilla rock family ({@code Rock_Stone} /
     * {@code Rock_Sandstone}), so players can reach Serenal tiles from any
     * vanilla stone block and vice-versa.
     */
    private static int injectFloorTiles() {
        int count = 0;

        List<String> stoneSubs  = new ArrayList<>();
        List<String> stoneStairs = new ArrayList<>();
        for (String suffix : STONE_TILE_SUFFIXES) {
            String base = "Serenal_Stone_Floor_Tile" + suffix;
            if (exists(base)) stoneSubs.add(base);
            String stair = base + "_Stairs";
            if (exists(stair)) stoneStairs.add(stair);
        }
        count += injectTileGroup("Serenal_Stone_Tile", stoneSubs, stoneStairs);
        List<String> stoneAll = new ArrayList<>(stoneSubs);
        stoneAll.addAll(stoneStairs);
        mergeTilesIntoFamily("Rock_Stone", stoneAll);

        List<String> sandSubs  = new ArrayList<>();
        List<String> sandStairs = new ArrayList<>();
        for (String suffix : SANDSTONE_TILE_SUFFIXES) {
            String base = "Serenal_Sandstone_Floor_Tile" + suffix;
            if (exists(base)) sandSubs.add(base);
            String stair = base + "_Stairs";
            if (exists(stair)) sandStairs.add(stair);
        }
        count += injectTileGroup("Serenal_Sandstone_Tile", sandSubs, sandStairs);
        List<String> sandAll = new ArrayList<>(sandSubs);
        sandAll.addAll(sandStairs);
        mergeTilesIntoFamily("Rock_Sandstone", sandAll);

        return count;
    }

    private static int injectTileGroup(String source, List<String> subs, List<String> stairs) {
        if (subs.isEmpty()) return 0;
        Chisel.Data chisel    = new Chisel.Data();
        chisel.source         = source;
        chisel.substitutions  = subs.toArray(new String[0]);
        chisel.stairs         = stairs.toArray(new String[0]);
        chisel.halfSlabs      = new String[0];
        chisel.roofing        = new String[0];

        int count = 0;
        List<String> all = new ArrayList<>(subs);
        all.addAll(stairs);
        for (String key : all) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            if (ComboStateHelper.inject(bt, chisel, null, null)) count++;
        }
        return count;
    }

    /**
     * Groups Staircase01–04 per wood type into per-type chisel groups so
     * players can chisel between staircase designs within the same wood.
     */
    private static int injectWoodStaircases() {
        int count = 0;
        for (String wood : STAIRCASE_WOOD_TYPES) {
            List<String> group = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                String key = "Serenal_" + wood + "_Staircase0" + i;
                if (exists(key)) group.add(key);
            }
            if (group.isEmpty()) continue;

            Chisel.Data chisel    = new Chisel.Data();
            chisel.source         = "Serenal_Staircase";
            chisel.substitutions  = group.toArray(new String[0]);
            chisel.stairs         = new String[0];
            chisel.halfSlabs      = new String[0];
            chisel.roofing        = new String[0];

            for (String key : group) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                if (ComboStateHelper.inject(bt, chisel, null, null)) count++;
            }
        }
        return count;
    }

    /**
     * For each brick material, groups the base brick block and all its Serenal
     * shape variants into one isolated Chisel group so players can chisel
     * between shapes (Pillar ↔ Capital ↔ Small_Window, etc.) within the same
     * material.
     *
     * <p>The material block itself (e.g. {@code Rock_Aqua_Brick}) is included
     * in the substitutions as the "plain brick" revert option, and gets the
     * same chisel data injected onto it.  This replaces any cross-material
     * chisel group that VanillaCompat may have placed on the base brick block,
     * keeping groups strictly per-material.
     */
    private static int injectBrickChisel() {
        int count = 0;
        for (String material : BRICK_MATERIALS) {
            List<String> subs  = new ArrayList<>();
            List<String> slabs = new ArrayList<>();
            List<String> stirs = new ArrayList<>();

            // Include the base brick block as the "plain brick" revert option
            if (exists(material)) subs.add(material);

            for (String shape : BRICK_SUB_SHAPES) {
                String key = "Serenal_Layer_" + material + "_" + shape;
                if (exists(key)) subs.add(key);
            }
            for (String shape : BRICK_SLAB_SHAPES) {
                String key = "Serenal_Layer_" + material + "_" + shape;
                if (exists(key)) slabs.add(key);
            }
            String stairKey = "Serenal_Layer_" + material + "_Stairs";
            if (exists(stairKey)) stirs.add(stairKey);

            // Skip if the only entry is the base material block (no Serenal shapes found)
            boolean hasShapes = (subs.size() > 1) || !slabs.isEmpty() || !stirs.isEmpty();
            if (!hasShapes) continue;

            Chisel.Data chisel    = new Chisel.Data();
            chisel.source         = "Serenal_Brick";
            chisel.substitutions  = subs.toArray(new String[0]);
            chisel.halfSlabs      = slabs.toArray(new String[0]);
            chisel.stairs         = stirs.toArray(new String[0]);
            chisel.roofing        = new String[0];

            // Inject onto base brick block + all Serenal shape blocks
            List<String> allBlocks = new ArrayList<>(subs);
            allBlocks.addAll(slabs);
            allBlocks.addAll(stirs);
            for (String key : allBlocks) {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;
                if (ComboStateHelper.inject(bt, chisel, null, null)) count++;
            }

            // Serenal shape keys only (not the base brick block itself)
            List<String> serenalShapeKeys = new ArrayList<>();
            for (String k : subs) { if (!k.equals(material)) serenalShapeKeys.add(k); }
            serenalShapeKeys.addAll(slabs);
            serenalShapeKeys.addAll(stirs);

            // Merge Serenal shapes into every member of the base VanillaCompat rock family.
            // e.g. Rock_Aqua_Brick → baseKey = Rock_Aqua → family = [Rock_Aqua, Rock_Aqua_Cobble, ...]
            // This lets players chisel directly to Serenal brick designs from any rock form,
            // without touching the per-material Serenal group (Rock_Aqua_Brick stays clean).
            String baseKey = material.replace("_Brick", "");
            if (!baseKey.equals(material) && exists(baseKey) && !serenalShapeKeys.isEmpty()) {
                BlockType baseBt = BlockTypeCache.get(baseKey);
                if (baseBt != null) {
                    Chisel.Data baseChisel = getChiselData(baseBt);
                    if (baseChisel != null && baseChisel.substitutions != null) {
                        LinkedHashSet<String> merged = new LinkedHashSet<>();
                        Collections.addAll(merged, baseChisel.substitutions);
                        merged.addAll(serenalShapeKeys);
                        String[] mergedArr = merged.toArray(new String[0]);
                        // Update every family member (skip the brick block — it owns its own group)
                        for (String member : baseChisel.substitutions) {
                            if (member.equals(material)) continue;
                            BlockType memberBt = BlockTypeCache.get(member);
                            if (memberBt == null) continue;
                            Chisel.Data memberChisel = getChiselData(memberBt);
                            if (memberChisel != null) memberChisel.substitutions = mergedArr;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static Chisel.Data getChiselData(BlockType bt) {
        StateData s = bt.getState();
        if (s instanceof ComboState cs) return cs.chisel;
        if (s instanceof Chisel.Data cd) return cd;
        return null;
    }

    private static void mergeTilesIntoFamily(String familyBaseKey, List<String> tileKeys) {
        if (tileKeys.isEmpty()) return;
        BlockType baseBt = BlockTypeCache.get(familyBaseKey);
        if (baseBt == null) return;
        Chisel.Data baseChisel = getChiselData(baseBt);
        if (baseChisel == null || baseChisel.substitutions == null) return;

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        Collections.addAll(merged, baseChisel.substitutions);
        merged.addAll(tileKeys);
        String[] mergedArr = merged.toArray(new String[0]);

        for (String member : baseChisel.substitutions) {
            BlockType bt = BlockTypeCache.get(member);
            if (bt == null) continue;
            Chisel.Data cd = getChiselData(bt);
            if (cd != null) cd.substitutions = mergedArr;
        }
        baseChisel.substitutions = mergedArr;
        for (String tileKey : tileKeys) {
            BlockType bt = BlockTypeCache.get(tileKey);
            if (bt == null) continue;
            Chisel.Data cd = getChiselData(bt);
            if (cd != null) cd.substitutions = mergedArr;
        }
    }

    private static int injectAllWindows() {
        // Collect every window block that actually exists
        List<String> windowKeys = new ArrayList<>();
        for (String material : BRICK_MATERIALS) {
            String key = "Serenal_Layer_" + material + "_Small_Window";
            if (exists(key)) windowKeys.add(key);
        }
        if (windowKeys.isEmpty()) return 0;

        // Collect existing base brick blocks (e.g. Rock_Aqua_Brick) for "revert" option
        List<String> baseKeys = new ArrayList<>();
        for (String material : BRICK_MATERIALS) {
            if (exists(material)) baseKeys.add(material);
        }

        // Hammer shows: base materials first (revert option), then all window variants
        List<String> hammerList = new ArrayList<>();
        hammerList.addAll(baseKeys);
        hammerList.addAll(windowKeys);
        String[] hammerArr = hammerList.toArray(new String[0]);

        CarpenterHammer.Data hammer = buildHammerWindows("Serenal_Small_Window", hammerArr);

        int count = 0;
        // Inject hammer on each window block (chisel is handled per-material by injectBrickChisel)
        for (String key : windowKeys) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            if (ComboStateHelper.inject(bt, null, null, hammer)) count++;
        }
        // Inject hammer-only on each base material block (preserves existing chisel state)
        for (String baseKey : baseKeys) {
            BlockType bt = BlockTypeCache.get(baseKey);
            if (bt == null) continue;
            ComboStateHelper.inject(bt, null, null, hammer);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static CarpenterHammer.Data buildHammerWindows(String source, String[] windows) {
        CarpenterHammer.Data d = new CarpenterHammer.Data();
        d.source  = source;
        d.chairs  = new String[0];
        d.tables  = new String[0];
        d.storage = new String[0];
        d.windows = windows;
        d.lights  = new String[0];
        return d;
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
