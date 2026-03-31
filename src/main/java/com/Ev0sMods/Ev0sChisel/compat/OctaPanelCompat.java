package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Compatibility layer for <b>Octa_Panel_Mod</b>.
 *
 * <p>Groups all Octa panel sub-types so the Paintbrush can switch between
 * variants. Sub-types supported:
 * <ul>
 *   <li>Stained glass windows</li>
 *   <li>Colored numbered wall/floor panels (per-design color groups, cloth-style)</li>
 *   <li>Light bars — wall and floor</li>
 *   <li>Light panels — wall and floor</li>
 *   <li>Light covers — wall / flat / round</li>
 *   <li>Old-style floor panels (17 colors)</li>
 *   <li>Supports</li>
 *   <li>Panel doors — standard / large / medium / trapdoor</li>
 *   <li>Piano keys — black keys and white keys</li>
 *   <li>Image/art panels: animated slides, clocks, community art,
 *       cats (B/H/T), fish, snakes, pride, jelly lamps, foods,
 *       potions, weapons, tiffy glass, trims, lizard panels</li>
 * </ul>
 */
public final class OctaPanelCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static boolean detected = false;

    private OctaPanelCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // STAINED GLASS WINDOWS  (Octa_Old_WallPanel group)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] OCTA_WINDOWS = {
            "Stained_GlassBlack_Window",
            "Stained_GlassBlue_Window",
            "Stained_GlassBrown_Window",
            "Stained_GlassGreen_Window",
            "Stained_GlassLightBlue_Window",
            "Stained_GlassPastelPink_Window",
            "Stained_GlassPink_Window",
            "Stained_GlassPurple_Window",
            "Stained_GlassRed_Window",
            "Stained_GlassSkyAzure_Window",
            "Stained_GlassWhite_Window",
            "Stained_GlassWicketGreen_Window",
            "Stained_GlassYellow_Window",
            "Staine_GlassBrightOrange_Window", // mod typo (missing 'd')
            "LightRed",
            "Lavender_Glass_Window",
            "GoldWindow"
    };

    // ─────────────────────────────────────────────────────────────────────
    // COLORED NUMBERED PANELS  (cloth-style: per-design-N color groups)
    // Wall key:   {Color}{N}        e.g. Black1 … Black18
    // Floor key:  {Color}F{N}       e.g. BlackF1 … BlackF18
    // DC floor:   DC{N}F            e.g. DC1F   … DC15F
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] PANEL_COLORS = {
            "Black", "Blue", "Brown", "Cyan", "DC",
            "Green", "Metal", "Orange", "Pink",
            "Purple", "Red", "White", "Yellow"
    };

    /** Highest design number across all colors (Black has 18). */
    private static final int PANEL_DESIGN_MAX = 18;

    // ─────────────────────────────────────────────────────────────────────
    // LIGHT BARS
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] LIGHT_BARS_WALL = {
            "Light_Bar",
            "Light_Bar_JustFramed",
            "Light_BarBlack",
            "Light_BarBlue",
            "Light_BarCyan",
            "Light_BarGreen",
            "Light_BarPurple",
            "Light_BarRed",
            "Light_BarYellow"
    };

    private static final String[] LIGHT_BARS_FLOOR = {
            "Light_Bar_Floor",
            "Light_Bar_FloorJustFrame",
            "Light_Bar_FloorBlack",
            "Light_Bar_FloorBlue",
            "Light_Bar_FloorCyan",
            "Light_Bar_FloorGreen",
            "Light_Bar_FloorPurple",
            "Light_Bar_FloorRed",
            "Light_Bar_FloorYellow"
    };

    // ─────────────────────────────────────────────────────────────────────
    // LIGHT PANELS
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] LIGHT_PANELS_WALL = {
            "Framed_Panel",
            "Light_Panel",
            "Light_PanelBlack",
            "Light_PanelCyan",
            "Light_PanelGreen",
            "Light_PanelPurple",
            "Light_PanelRed",
            "Light_PanelYellow",
            "Light_PanleBlue"      // mod typo (PanleBlue)
    };

    private static final String[] LIGHT_PANELS_FLOOR = {
            "Floor_Light_Panel",
            "Floor_Panel_Frame",
            "Floor_Light_PanelBlack",
            "Floor_Light_PanelBlue",
            "Floor_Light_PanelCyan",
            "Floor_Light_PanelGreen",
            "Floor_Light_PanelPurple",
            "Floor_Light_PanelRed",
            "Floor_Light_PanelYellow"
    };

    // ─────────────────────────────────────────────────────────────────────
    // LIGHT COVERS  (wall / flat-face / round)
    // Keys: LightCover1-8, LightCoverF1-8, LightCoverR1-8
    // ─────────────────────────────────────────────────────────────────────

    private static final int LIGHT_COVER_MAX = 8;

    // ─────────────────────────────────────────────────────────────────────
    // OLD-STYLE FLOOR PANELS  (Octa_Old_FloorPanel group, 17 colors)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] OLD_FLOOR_PANELS = {
            "Floor_PanelBlack",
            "Floor_PanelBlue",
            "Floor_PanelBrown",
            "Floor_PanelGold",
            "Floor_PanelGreen",
            "Floor_PanelLightBlue",
            "Floor_PanelLightRed",
            "Floor_PanelOrange",
            "Floor_PanelPastelPink",
            "Floor_PanelPink",
            "Floor_PanelPurple",
            "Floor_PanelRed",
            "Floor_PanelSkyAzure",
            "Floor_PanelWhite",
            "Floor_PanelWicker",
            "Floor_PanelYellow",
            "Floor_PannelLavender"  // mod typo (double 'n')
    };

    // ─────────────────────────────────────────────────────────────────────
    // SUPPORTS
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] SUPPORTS = {
            "Support1",
            "SupportBarWR",
            "SupporBarWR1",   // mod typo (missing 't')
            "SupporBarWR2",
            "SupporBarWR3",
            "SupporBarWR4",
            "SupporBarWR5",
            "SupporBarWR6",
            "SupporBarWR7",
            "SupporBarWR8",
            "SupporBarWR9",
            "SupporBarWR10",
            "Supports2",
            "Supports3",
            "Supports4",
            "SupportsF1",
            "SupportsF2",
            "SupportsF3",
            "SupportsF4"
    };

    // ─────────────────────────────────────────────────────────────────────
    // PANEL DOORS
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] PANEL_DOORS_STD = {
            "PanelDoor",
            "PanelDoor1", "PanelDoor2", "PanelDoor3", "PanelDoor4",
            "PanelDoor5", "PanelDoor6", "PanelDoor7", "PanelDoor8"
    };

    private static final String[] PANEL_DOORS_LARGE = {
            "PanelDoorLarge",
            "PanelDoorLarge1", "PanelDoorLarge2", "PanelDoorLarge3",
            "PanelDoorLarge4", "PanelDoorLarge5", "PanelDoorLarge6",
            "PanelDoorLarge7", "PanelDoorLarge8"
    };

    private static final String[] PANEL_DOORS_MEDIUM = {
            "PanelDoorMedilum",         // mod typo (Medilum)
            "PanelDoorMedilum1", "PanelDoorMedilum2", "PanelDoorMedilum3",
            "PanelDoorMedilum4", "PanelDoorMedilum5", "PanelDoorMedilum6",
            "PanelDoorMedilum7", "PanelDoorMedilum8"
    };

    private static final String[] PANEL_TRAPDOORS = {
            "PanelTrapDoor",
            "PanelTrapDoor1", "PanelTrapDoor2", "PanelTrapDoor3",
            "PanelTrapDoor4", "PanelTrapDoor5", "PanelTrapDoor6",
            "PanelTrapDoor7", "PanelTrapDoor8"
    };

    // ─────────────────────────────────────────────────────────────────────
    // PIANO KEYS  (Keys: PianoKeyOcta1-24 black, PianoKeyOctaW1-24 white)
    // ─────────────────────────────────────────────────────────────────────

    private static final int PIANO_KEY_MAX = 24;

    // ─────────────────────────────────────────────────────────────────────
    // IMAGE / ART PANELS
    // ─────────────────────────────────────────────────────────────────────

    private static final int ANIMATED_MAX = 30;  // OctaN1 … OctaN30

    private static final String[] CLOCKS = {
            "OctaClock",
            "OctaClock1", "OctaClock2", "OctaClock3", "OctaClock4",
            "OctaClock5", "OctaClock6", "OctaClock7"
    };

    private static final String[] COMMUNITY = {
            "Community",
            "Community1",  "Community2",  "Community3",  "Community4",
            "Community5",  "Community6",  "Community7",  "Community8",
            "Community9",  "Community10", "Community11"
    };

    private static final String[] TIFFY_GLASS = {
            "OctaTiffyG", "OctaTiffyG1", "OctaTiffyG2"
    };

    /** L and R trims for design numbers 1-12: OctaTrim{N}L + OctaTrim{N}R. */
    private static final int TRIM_MAX = 12;

    private static final String[] LIZARD_FLOOR = {
            "LizaroctaFloor",
            "LizaroctaFloor2", "LizaroctaFloor3", "LizaroctaFloor4"
    };

    private static final String[] LIZARD_WALL = {
            "Octa_Lizard",
            "Octa_Lizard2", "Octa_Lizard3", "Octa_Lizard4"
    };

    private static final String[] FISH = {
            "FishH", "FishH2", "FishH3", "FishH4"
    };

    private static final String[] SNAKE = {
            "Snake1", "Snake2", "Snake3", "Snake4"
    };

    private static final String[] PRIDE = {
            "OctaPride",
            "OctaPride1",  "OctaPride2",  "OctaPride3",  "OctaPride4",
            "OctaPride5",  "OctaPride6",  "OctaPride7",  "OctaPride8",
            "OctaPride9",  "OctaPride10", "OctaPride11", "OctaPride12",
            "OctaPride13"
    };

    private static final int CATS_MAX    = 8;  // OctaCatsB1-8, OctaCatsH1-8, OctaCatsT1-8
    private static final int JELLY_MAX   = 5;  // OctaJelly1-5
    private static final int FOODS_MAX   = 5;  // OctaFoods1-5
    private static final int POTIONS_MAX = 5;  // OctaPotions1-5
    private static final int WEAPONS_MAX = 6;  // OctaWepon1-6

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public static boolean isDetected() { return detected; }

    /**
     * Injects Paintbrush states for all Octa Panel Mod blocks.
     * Call this after all chisel compat passes (alongside
     * {@link VanillaClothCompat#injectPaintbrushStates()}).
     */
    public static void injectPaintbrushStates() {
        int total = 0;

        // ── Stained glass windows ─────────────────────────────────────────
        total += injectFixed(OCTA_WINDOWS, "Octa_Windows");

        // ── Colored numbered wall panels (per-design-N color groups) ───────
        for (int n = 1; n <= PANEL_DESIGN_MAX; n++) {
            List<String> group = new ArrayList<>();
            for (String color : PANEL_COLORS) {
                String key = color + n;
                if (exists(key)) group.add(key);
            }
            total += inject(group, "Octa_WallPanel_Design" + n);
        }

        // ── Colored numbered floor panels (per-design-N color groups) ──────
        for (int n = 1; n <= PANEL_DESIGN_MAX; n++) {
            List<String> group = new ArrayList<>();
            for (String color : PANEL_COLORS) {
                // DC exception: DC1F, DC2F, … all others: BlackF1, BlueF1, …
                String key = color.equals("DC") ? "DC" + n + "F" : color + "F" + n;
                if (exists(key)) group.add(key);
            }
            total += inject(group, "Octa_FloorPanel_Design" + n);
        }

        // ── Light bars (wall + floor) ─────────────────────────────────────
        total += injectFixed(LIGHT_BARS_WALL,  "Octa_LightBar_Wall");
        total += injectFixed(LIGHT_BARS_FLOOR, "Octa_LightBar_Floor");

        // ── Light panels (wall + floor) ───────────────────────────────────
        total += injectFixed(LIGHT_PANELS_WALL,  "Octa_LightPanel_Wall");
        total += injectFixed(LIGHT_PANELS_FLOOR, "Octa_LightPanel_Floor");

        // ── Light covers (wall / flat / round) ────────────────────────────
        {
            List<String> wall  = new ArrayList<>();
            List<String> flat  = new ArrayList<>();
            List<String> round = new ArrayList<>();
            for (int n = 1; n <= LIGHT_COVER_MAX; n++) {
                if (exists("LightCover"  + n)) wall.add("LightCover"  + n);
                if (exists("LightCoverF" + n)) flat.add("LightCoverF" + n);
                if (exists("LightCoverR" + n)) round.add("LightCoverR" + n);
            }
            total += inject(wall,  "Octa_LightCover_Wall");
            total += inject(flat,  "Octa_LightCover_Flat");
            total += inject(round, "Octa_LightCover_Round");
        }

        // ── Old-style floor panels (17 colors) ────────────────────────────
        total += injectFixed(OLD_FLOOR_PANELS, "Octa_OldFloorPanel");

        // ── Supports ──────────────────────────────────────────────────────
        total += injectFixed(SUPPORTS, "Octa_Supports");

        // ── Panel doors ───────────────────────────────────────────────────
        total += injectFixed(PANEL_DOORS_STD,    "Octa_PanelDoor_Std");
        total += injectFixed(PANEL_DOORS_LARGE,  "Octa_PanelDoor_Large");
        total += injectFixed(PANEL_DOORS_MEDIUM, "Octa_PanelDoor_Medium");
        total += injectFixed(PANEL_TRAPDOORS,    "Octa_PanelTrapDoor");

        // ── Piano keys (black + white) ────────────────────────────────────
        {
            List<String> black = new ArrayList<>();
            List<String> white = new ArrayList<>();
            for (int n = 1; n <= PIANO_KEY_MAX; n++) {
                if (exists("PianoKeyOcta"  + n)) black.add("PianoKeyOcta"  + n);
                if (exists("PianoKeyOctaW" + n)) white.add("PianoKeyOctaW" + n);
            }
            total += inject(black, "Octa_PianoKey_Black");
            total += inject(white, "Octa_PianoKey_White");
        }

        // ── Animated panels (OctaN1-30 + AnamtedPanelTests) ──────────────
        {
            List<String> animated = new ArrayList<>();
            if (exists("AnamtedPanelTests")) animated.add("AnamtedPanelTests");
            for (int n = 1; n <= ANIMATED_MAX; n++) {
                if (exists("OctaN" + n)) animated.add("OctaN" + n);
            }
            total += inject(animated, "Octa_AnimatedPanel");
        }

        // ── Clocks ────────────────────────────────────────────────────────
        total += injectFixed(CLOCKS, "Octa_Clocks");

        // ── Community art ─────────────────────────────────────────────────
        total += injectFixed(COMMUNITY, "Octa_Community");

        // ── Tiffy Glass ───────────────────────────────────────────────────
        total += injectFixed(TIFFY_GLASS, "Octa_TiffyGlass");

        // ── Trims (all L and R for designs 1-12, one group) ───────────────
        {
            List<String> trims = new ArrayList<>();
            for (int n = 1; n <= TRIM_MAX; n++) {
                if (exists("OctaTrim" + n + "L")) trims.add("OctaTrim" + n + "L");
                if (exists("OctaTrim" + n + "R")) trims.add("OctaTrim" + n + "R");
            }
            total += inject(trims, "Octa_Trims");
        }

        // ── Lizard panels (floor + wall) ──────────────────────────────────
        total += injectFixed(LIZARD_FLOOR, "Octa_Lizard_Floor");
        total += injectFixed(LIZARD_WALL,  "Octa_Lizard_Wall");

        // ── Fish ──────────────────────────────────────────────────────────
        total += injectFixed(FISH,  "Octa_Fish");

        // ── Snake ─────────────────────────────────────────────────────────
        total += injectFixed(SNAKE, "Octa_Snake");

        // ── Pride ─────────────────────────────────────────────────────────
        total += injectFixed(PRIDE, "Octa_Pride");

        // ── Cats (body / head / tail) ─────────────────────────────────────
        {
            List<String> catsB = new ArrayList<>();
            List<String> catsH = new ArrayList<>();
            List<String> catsT = new ArrayList<>();
            for (int n = 1; n <= CATS_MAX; n++) {
                if (exists("OctaCatsB" + n)) catsB.add("OctaCatsB" + n);
                if (exists("OctaCatsH" + n)) catsH.add("OctaCatsH" + n);
                if (exists("OctaCatsT" + n)) catsT.add("OctaCatsT" + n);
            }
            total += inject(catsB, "Octa_Cats_Body");
            total += inject(catsH, "Octa_Cats_Head");
            total += inject(catsT, "Octa_Cats_Tail");
        }

        // ── Jelly lamps, foods, potions, weapons ──────────────────────────
        {
            List<String> jelly   = new ArrayList<>();
            List<String> foods   = new ArrayList<>();
            List<String> potions = new ArrayList<>();
            List<String> weapons = new ArrayList<>();
            for (int n = 1; n <= JELLY_MAX;   n++) if (exists("OctaJelly"   + n)) jelly.add("OctaJelly"   + n);
            for (int n = 1; n <= FOODS_MAX;   n++) if (exists("OctaFoods"   + n)) foods.add("OctaFoods"   + n);
            for (int n = 1; n <= POTIONS_MAX; n++) if (exists("OctaPotions" + n)) potions.add("OctaPotions" + n);
            for (int n = 1; n <= WEAPONS_MAX; n++) if (exists("OctaWepon"   + n)) weapons.add("OctaWepon"  + n);
            total += inject(jelly,   "Octa_Jelly");
            total += inject(foods,   "Octa_Foods");
            total += inject(potions, "Octa_Potions");
            total += inject(weapons, "Octa_Weapons");
        }

        if (total > 0) {
            detected = true;
            LOGGER.atWarning().log("[OctaPanelCompat] Injected paintbrush onto "
                    + total + " Octa panel blocks.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Injects a group from a fixed array, filtering to only present blocks. */
    private static int injectFixed(String[] keys, String source) {
        List<String> present = new ArrayList<>();
        for (String key : keys) {
            if (exists(key)) present.add(key);
        }
        return inject(present, source);
    }

    /** Injects a {@link Paintbrush.Data} color group onto every key in the list. */
    private static int inject(List<String> keys, String source) {
        if (keys.isEmpty()) return 0;
        String[] arr = keys.toArray(new String[0]);
        int injected = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            StateData existing = bt.getState();
            if (existing instanceof Paintbrush.Data) continue; // already handled

            Paintbrush.Data data = new Paintbrush.Data();
            data.source        = source;
            data.colorVariants = arr;
            if (ComboStateHelper.inject(bt, null, data, null)) injected++;
        }
        return injected;
    }

    private static boolean exists(String key) { return BlockTypeCache.exists(key); }
}
