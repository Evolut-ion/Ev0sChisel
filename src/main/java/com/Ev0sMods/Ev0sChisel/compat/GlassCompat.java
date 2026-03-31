package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Compatibility layer for glass mods.
 *
 * <h3>Chisel (plain glass)</h3>
 * <p>All clear/structural glass forms from <b>Essentials Glass</b> and
 * <b>haskill.SimpleGlassBlock</b> are grouped into one chisel family.
 *
 * <h3>Paintbrush — Medieval Glass (8 pattern groups)</h3>
 * <p>Each stained-glass pattern forms its own 16-color paintbrush group.
 *
 * <h3>Paintbrush — Essentials Glass (1 structural group)</h3>
 * <p>All structural glass forms are grouped so the Paintbrush switches shape.
 *
 * <p>Octa Panel Mod glass windows and colored panel groups are handled by
 * {@link OctaPanelCompat}.
 */
public final class GlassCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static boolean detected = false;

    private GlassCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // CHISEL: plain / structural glass
    // ─────────────────────────────────────────────────────────────────────

    /**
     * All clear-glass structural forms.  Includes the Essentials Glass shapes
     * and the two extra shapes added by haskill.SimpleGlassBlock.
     */
    private static final String[] PLAIN_GLASS_CHISEL = {
            // Essentials Glass
            "Glass_Block",
            "Glass_Beam",
            "Glass_Panel",
            "Glass_Pillar_Base",
            "Glass_Pillar_Middle",
            "Glass_Roof_Flat",
            "Glass_Roof_Slope",
            "Glass_Roof_Slope_Hollow",
            "Glass_Roof_Slope_Shallow",
            "Glass_Roof_Slope_Steep",
            "Glass_Stairs",
            "Glass_Wall",
            "Half_Glass",
            // haskill.SimpleGlassBlock extras
            "Glass_Wood_Block",
            "Glass_Wood_Panel"
    };

    // ─────────────────────────────────────────────────────────────────────
    // PAINTBRUSH: Medieval Glass (8 pattern × 16 color groups)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] MEDIEVAL_PATTERNS = {
            "Blotch", "Centre_Circle", "Criss_Cross", "Cross",
            "Diagonal_Squared", "Diamond", "Etched", "Medieval"
    };

    /**
     * Color-suffix tokens as they actually appear in the block key.
     * Note: the Lime entry has a typo in the mod ("Lime_Grass" instead of
     * "Lime_Glass") — the exact token is preserved so the key resolves.
     */
    private static final String[] MEDIEVAL_COLOR_SUFFIXES = {
            "Black_Glass",      "Blue_Glass",       "Brown_Glass",
            "Cyan_Glass",       "Gray_Glass",        "Green_Glass",
            "Light_Blue_Glass", "Light_Gray_Glass",
            "Lime_Grass",       // typo in Medieval Glass mod — intentional
            "Magenta_Glass",    "Orange_Glass",     "Pink_Glass",
            "Purple_Glass",     "Red_Glass",        "White_Glass",
            "Yellow_Glass"
    };

    // ─────────────────────────────────────────────────────────────────────
    // PAINTBRUSH: Essentials Glass (1 structural group)
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] ESSENTIALS_GLASS = {
            "Glass_Block",
            "Glass_Beam",
            "Glass_Panel",
            "Glass_Pillar_Base",
            "Glass_Pillar_Middle",
            "Glass_Roof_Flat",
            "Glass_Roof_Slope",
            "Glass_Roof_Slope_Hollow",
            "Glass_Roof_Slope_Shallow",
            "Glass_Roof_Slope_Steep",
            "Glass_Stairs",
            "Glass_Wall",
            "Half_Glass",
            // haskill.SimpleGlassBlock structural forms
            "Glass_Wood_Block",
            "Glass_Wood_Panel"
            // Glass_Door, Glass_Trapdoor, Potion_Empty omitted
    };

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public static boolean isDetected() { return detected; }

    /**
     * Injects Chisel states for all plain/structural glass.
     * Call this alongside {@link VanillaCompat#injectChiselStates()}.
     */
    public static void injectChiselStates() {
        int total = injectChiselGroup(PLAIN_GLASS_CHISEL, "Glass_Plain");
        if (total > 0) {
            detected = true;
            LOGGER.atWarning().log("[GlassCompat] Injected chisel onto " + total
                    + " plain glass blocks.");
        }
    }

    /**
     * Injects Paintbrush states for all colored glass groups.
     * Call this after all chisel passes (alongside
     * {@link VanillaClothCompat#injectPaintbrushStates()}).
     */
    public static void injectPaintbrushStates() {
        int total = 0;

        // ── Medieval Glass: one per-pattern color group ───────────────────
        for (String pattern : MEDIEVAL_PATTERNS) {
            List<String> group = new ArrayList<>();
            for (String suffix : MEDIEVAL_COLOR_SUFFIXES) {
                String key = pattern + "_" + suffix;
                if (exists(key)) group.add(key);
            }
            total += injectGroup(group, "Medieval_Glass_" + pattern);
        }

        // ── Essentials Glass: one structural shape group ──────────────────
        {
            List<String> group = new ArrayList<>();
            for (String key : ESSENTIALS_GLASS) {
                if (exists(key)) group.add(key);
            }
            total += injectGroup(group, "Glass_Structural");
        }

        if (total > 0) {
            detected = true;
            LOGGER.atWarning().log("[GlassCompat] Injected paintbrush onto " + total
                    + " glass/panel blocks.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Injects a {@link Chisel.Data} substitution group onto every existing block in the array. */
    private static int injectChiselGroup(String[] keys, String source) {
        List<String> present = new ArrayList<>();
        for (String key : keys) {
            if (exists(key)) present.add(key);
        }
        if (present.isEmpty()) return 0;

        String[] arr = present.toArray(new String[0]);
        int injected = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            Chisel.Data data = new Chisel.Data();
            data.source        = source;
            data.substitutions = arr;
            if (ComboStateHelper.inject(bt, data, null, null)) injected++;
        }
        return injected;
    }

    /** Injects a {@link Paintbrush.Data} color group onto every existing key in the list. */
    private static int injectGroup(List<String> keys, String source) {
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
