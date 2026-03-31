package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compatibility layer for label mods — Yer's Labels (Yernemm) and Boske's Chest Labels.
 *
 * <p>Detection is purely registry-based: if the mod's blocks are registered in the
 * BlockType registry the mod is loaded. No classpath or file-system reflection is used.
 *
 * <p>When either mod is present, every detected label block receives a {@link Chisel.Data}
 * state whose {@code substitutions} array contains <em>all</em> label variants from all
 * installed label mods, making them freely interchangeable in the Labels tab.
 */
public final class LabelsCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static boolean yerLabelsDetected   = false;
    private static boolean boskeLabelsDetected  = false;
    private static boolean auresDetected        = false;
    private static boolean initialized          = false;

    /** Combined list of all detected label block keys (used for {@link #isLabelKey} detection). */
    private static final List<String> VARIANTS = new ArrayList<>();

    /** Fast lookup set for {@link #isLabelKey}. */
    private static final Set<String> VARIANT_SET = new HashSet<>();

    /** Only Yer's Labels detected keys — substitutions stay within this mod. */
    private static final List<String> YER_DETECTED = new ArrayList<>();

    /** Only Boske's Labels detected keys — substitutions stay within this mod. */
    private static final List<String> BOSKE_DETECTED = new ArrayList<>();

    /** Only Aures Farm Decor detected keys — substitutions stay within this mod. */
    private static final List<String> AURES_DETECTED = new ArrayList<>();

    // ── Yer's Labels block keys ───────────────────────────────────────

    /** Detection sentinel – if this key exists in the registry, Yer's Labels is loaded. */
    private static final String YER_SENTINEL = "Yernemm_Labels_LabelNormal";

    private static final String[] YER_BLOCK_VARIANTS = {
        "Yernemm_Labels_LabelBlock",
        "Yernemm_Labels_LabelBrick",
        "Yernemm_Labels_LabelCobble",
        "Yernemm_Labels_LabelStair",
        "Yernemm_Labels_LabelWood"
    };

    private static final String[] YER_ITEM_VARIANTS = {
        "Yernemm_Labels_LabelNormal",
        "Yernemm_Labels_LabelBone",
        "Yernemm_Labels_LabelCrystal",
        "Yernemm_Labels_LabelEmpty",
        "Yernemm_Labels_LabelFabric",
        "Yernemm_Labels_LabelFlower",
        "Yernemm_Labels_LabelFood",
        "Yernemm_Labels_LabelKweebecPlush",
        "Yernemm_Labels_LabelLeather",
        "Yernemm_Labels_LabelOre",
        "Yernemm_Labels_LabelPickaxe",
        "Yernemm_Labels_LabelPotion",
        "Yernemm_Labels_LabelSword",
        "Yernemm_Labels_LabelWheat"
    };

    private static final String[] YER_SYMBOL_VARIANTS = {
        "Yernemm_Labels_LabelSymbol",
        "Yernemm_Labels_LabelSymbolArrowDown",
        "Yernemm_Labels_LabelSymbolArrowLeft",
        "Yernemm_Labels_LabelSymbolArrowRight",
        "Yernemm_Labels_LabelSymbolArrowUp",
        "Yernemm_Labels_LabelSymbolCross",
        "Yernemm_Labels_LabelSymbolHeart",
        "Yernemm_Labels_LabelSymbolNoEntry",
        "Yernemm_Labels_LabelSymbolTick",
        "Yernemm_Labels_LabelSymbolWarning"
    };

    private static final String[] YER_TEXT_SINGLE = {
        "Yernemm_Labels_LabelText0","Yernemm_Labels_LabelText1","Yernemm_Labels_LabelText2",
        "Yernemm_Labels_LabelText3","Yernemm_Labels_LabelText4","Yernemm_Labels_LabelText5",
        "Yernemm_Labels_LabelText6","Yernemm_Labels_LabelText7","Yernemm_Labels_LabelText8",
        "Yernemm_Labels_LabelText9",
        "Yernemm_Labels_LabelTextA","Yernemm_Labels_LabelTextB","Yernemm_Labels_LabelTextC",
        "Yernemm_Labels_LabelTextD","Yernemm_Labels_LabelTextE","Yernemm_Labels_LabelTextF",
        "Yernemm_Labels_LabelTextG","Yernemm_Labels_LabelTextH","Yernemm_Labels_LabelTextI",
        "Yernemm_Labels_LabelTextJ","Yernemm_Labels_LabelTextK","Yernemm_Labels_LabelTextL",
        "Yernemm_Labels_LabelTextM","Yernemm_Labels_LabelTextN","Yernemm_Labels_LabelTextO",
        "Yernemm_Labels_LabelTextP","Yernemm_Labels_LabelTextQ","Yernemm_Labels_LabelTextR",
        "Yernemm_Labels_LabelTextS","Yernemm_Labels_LabelTextT","Yernemm_Labels_LabelTextU",
        "Yernemm_Labels_LabelTextV","Yernemm_Labels_LabelTextW","Yernemm_Labels_LabelTextX",
        "Yernemm_Labels_LabelTextY","Yernemm_Labels_LabelTextZ"
    };

    private static final String[] YER_TEXT_ABBREV = {
        "Yernemm_Labels_LabelText_An","Yernemm_Labels_LabelText_Ap","Yernemm_Labels_LabelText_As",
        "Yernemm_Labels_LabelText_At","Yernemm_Labels_LabelText_Ca","Yernemm_Labels_LabelText_Cl",
        "Yernemm_Labels_LabelText_Cn","Yernemm_Labels_LabelText_Co","Yernemm_Labels_LabelText_Do",
        "Yernemm_Labels_LabelText_Eu","Yernemm_Labels_LabelText_Ex","Yernemm_Labels_LabelText_Fs",
        "Yernemm_Labels_LabelText_Ha","Yernemm_Labels_LabelText_Op","Yernemm_Labels_LabelText_Pe",
        "Yernemm_Labels_LabelText_Po","Yernemm_Labels_LabelText_Qm"
    };

    // ── Boske's Chest Labels block keys ──────────────────────────────

    /** Detection sentinel – if this key exists, Boske's Chest Labels is loaded. */
    private static final String BOSKE_SENTINEL = "Ammo_Frame";

    private static final String[] BOSKE_VARIANTS = {
        "Ammo_Frame","Armor_Frame","Arrow_Frame","Aubergine_Frame","Blocks_Frame",
        "Bone_Frame","Books_Frame","Bow_Frame","BrasilianReal_Frame","Bucket_Frame",
        "Carrot_Frame","Cauliflower_Frame","Check_Frame","ChessPiece_Frame","Chilli_Frame",
        "Circle_Frame","Containers_Frame","Corn_Frame","Cotton_Frame","Crossbow_Frame",
        "Crystals_Frame","Deco_Frame","Desert_Frame","Dirt_Frame","Dollar_Frame",
        "Egg_Frame","Empty_Frame","Energy_Frame","Essence_Frame","Euro_Frame",
        "ExclamationPoint_Frame","Fabric_Frame","Farm_Frame","Feather_Frame","Fences_Frame",
        "FishGrilled_Frame","FishRaw_Frame","Flowers_Frame","Food_Frame","Fuel_Frame",
        "Furniture_Frame","Gear_Frame","Gems_Frame","Glass_Frame","Grass_Frame",
        "Gun_Frame","HeavyLeather_Frame","House_Frame","Ingots_Frame","KnifeAndFork_Frame",
        "Ladder_Frame","Leather_Frame",
        "LetterA_Frame","LetterB_Frame","LetterC_Frame","LetterD_Frame","LetterE_Frame",
        "LetterF_Frame","LetterG_Frame","LetterH_Frame","LetterI_Frame","LetterJ_Frame",
        "LetterK_Frame","LetterL_Frame","LetterM_Frame","LetterN_Frame","LetterO_Frame",
        "LetterP_Frame","LetterQ_Frame","LetterR_Frame","LetterS_Frame","LetterT_Frame",
        "LetterU_Frame","LetterV_Frame","LetterW_Frame","LetterX_Frame","LetterY_Frame",
        "LetterZ_Frame",
        "Lettuce_Frame","LightLeather_Frame","Light_Frame","MagicStaff_Frame",
        "MeatCooked_Frame","MeatRaw_Frame","MediumLeather_Frame","Misc_Frame","Mushrooms_Frame",
        "Number0_Frame","Number1_Frame","Number2_Frame","Number3_Frame","Number4_Frame",
        "Number5_Frame","Number6_Frame","Number7_Frame","Number8_Frame","Number9_Frame",
        "Onion_Frame","Ores_Frame","Pets_Frame","Planks_Frame","PlantFiber_Frame",
        "Pokeball_Frame","Portals_Frame","Potato_Frame","Potions_Frame","Pumpkin_Frame",
        "QuestionMark_Frame","Rail_Frame","Rice_Frame","Roof_Frame","Rubble_Frame",
        "Seed_Frame","Shield_Frame","Sign_Frame","Skewers_Frame","Snow_Frame",
        "Square_Frame","Stairs_Frame","Stick_Frame","Stone_Frame","Tomato_Frame",
        "Tools_Frame","TrashCan_Frame","TreeSap_Frame","Triangle_Frame","Turnip_Frame",
        "Warning_Frame","Weapons_Frame","Wheat_Frame","Wood_Frame","Workbench_Frame",
        "X_Frame"
    };

    // ── Aures Farm Decor block keys ──────────────────────────────────

    /** Detection sentinel – if this key exists, Aures Farm Decor is loaded. */
    private static final String AURES_SENTINEL = "Aures_Label_1";

    private static final String[] AURES_VARIANTS = {
        "Aures_Label_1", "Aures_Label_2", "Aures_Label_3",
        "Aures_Label_4", "Aures_Label_5", "Aures_Label_6"
    };

    private LabelsCompat() {}

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        yerLabelsDetected   = BlockTypeCache.exists(YER_SENTINEL);
        boskeLabelsDetected  = BlockTypeCache.exists(BOSKE_SENTINEL);
        auresDetected        = BlockTypeCache.exists(AURES_SENTINEL);

        if (!isAvailable()) return;

        buildVariantList();
    }

    public static boolean isAvailable() {
        return yerLabelsDetected || boskeLabelsDetected || auresDetected;
    }

    public static boolean isYerLabelsDetected()    { return yerLabelsDetected; }
    public static boolean isBoskeLabelsDetected()  { return boskeLabelsDetected; }
    public static boolean isAuresDetected()        { return auresDetected; }

    /**
     * Returns an unmodifiable view of all discovered label variant keys,
     * merged from every installed label mod.
     */
    public static List<String> getVariants() {
        return Collections.unmodifiableList(VARIANTS);
    }

    /**
     * Returns {@code true} if the given block key belongs to any label variant
     * known to this compat layer.
     */
    public static boolean isLabelKey(String key) {
        if (key == null) return false;
        return VARIANT_SET.contains(key);
    }

    /**
     * Injects a {@link Chisel.Data} state onto every detected label {@link BlockType}
     * so the chisel and table UI can treat them as mutually interchangeable.
     * Must be called after {@link #init()}.
     */
    public static void injectChiselStates() {
        if (!isAvailable() || VARIANTS.isEmpty()) return;

        // Each mod's labels only substitute within their own set.
        String[] yerArray   = YER_DETECTED.toArray(new String[0]);
        String[] boskeArray = BOSKE_DETECTED.toArray(new String[0]);
        String[] auresArray = AURES_DETECTED.toArray(new String[0]);

        for (String key : VARIANTS) {
            try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;

                StateData existing = bt.getState();
                if (existing instanceof Chisel.Data) continue;

                String[] subs;
                if (key.startsWith("Yernemm_Labels_"))  subs = yerArray;
                else if (key.startsWith("Aures_Label_")) subs = auresArray;
                else                                    subs = boskeArray;

                Chisel.Data data = new Chisel.Data();
                data.source        = "LabelsCompat";
                data.substitutions = subs;
                data.stairs        = new String[0];
                data.halfSlabs     = new String[0];
                data.roofing       = new String[0];

                ReflectionCache.setField(StateData.class, data, "id", "Ev0sChisel");
                ReflectionCache.setField(BlockType.class, bt, "state", data);
            } catch (Throwable t) {
                LOGGER.atWarning().log("[LabelsCompat] Failed to inject Chisel.Data for " + key + ": " + t.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────

    private static void buildVariantList() {
        VARIANTS.clear();
        VARIANT_SET.clear();
        YER_DETECTED.clear();
        BOSKE_DETECTED.clear();
        AURES_DETECTED.clear();

        if (yerLabelsDetected) {
            addIfExists(YER_BLOCK_VARIANTS,  YER_DETECTED);
            addIfExists(YER_ITEM_VARIANTS,   YER_DETECTED);
            addIfExists(YER_SYMBOL_VARIANTS, YER_DETECTED);
            addIfExists(YER_TEXT_SINGLE,     YER_DETECTED);
            addIfExists(YER_TEXT_ABBREV,     YER_DETECTED);
        }

        if (boskeLabelsDetected) {
            addIfExists(BOSKE_VARIANTS, BOSKE_DETECTED);
        }

        if (auresDetected) {
            addIfExists(AURES_VARIANTS, AURES_DETECTED);
        }
    }

    private static void addIfExists(String[] keys, List<String> modList) {
        for (String key : keys) {
            try {
                if (BlockTypeCache.exists(key) && !VARIANT_SET.contains(key)) {
                    modList.add(key);
                    VARIANTS.add(key);
                    VARIANT_SET.add(key);
                }
            } catch (Throwable ignored) {}
        }
    }
}
