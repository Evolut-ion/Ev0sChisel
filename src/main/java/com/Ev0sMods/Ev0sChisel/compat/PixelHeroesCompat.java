package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Paintbrush support for the <b>PixelHeroes</b> asset pack.
 * <p>
 * All 30 {@code Voxel_*} pixel-art panel blocks are grouped into a single
 * Paintbrush color group so the Paintbrush tool can cycle between them.
 * This is the same "image panel" concept as used by OctaPanelCompat.
 */
public final class PixelHeroesCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** All known PixelHeroes panel block keys (exact filenames without .json). */
    private static final String[] VOXEL_ART = {
            "Voxel_AmyRose_GBA",
            "Voxel_Goomba",
            "Voxel_Hello_Kitty",
            "Voxel_KirbySword",
            "Voxel_Luigi_Pixelart",
            "Voxel_Mario_Brick_NES",
            "Voxel_Mario_GBA",
            "Voxel_Mario_NES",
            "Voxel_Mario_SNES",
            "Voxel_MarioBlock",
            "Voxel_MarioBlockNES",
            "Voxel_MarioBrick",
            "Voxel_Minecraft_DiamondSword",
            "Voxel_Pokeball",
            "Voxel_Pokemon_Buneary",
            "Voxel_Pokemon_Celebi",
            "Voxel_Pokemon_Charmander",
            "Voxel_Pokemon_Corphish",
            "Voxel_Pokemon_Eevee",
            "Voxel_Pokemon_Espeon",
            "Voxel_Pokemon_Glaceon",
            "Voxel_Pokemon_Guardevoir",
            "Voxel_Pokemon_Ivysaur",
            "Voxel_Pokemon_Mew",
            "Voxel_Pokemon_Pikachu",
            "Voxel_Pokemon_Shaymin",
            "Voxel_Pokemon_Squirtle",
            "Voxel_Pokemon_Umbreon",
            "Voxel_Pokemon_Victini",
            "Voxel_SonicTheHedgehog_GBA"
    };

    private PixelHeroesCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injects a single {@link Paintbrush.Data} group onto all existing
     * {@code Voxel_*} panel blocks so the Paintbrush cycles between them.
     * Call this alongside the other paintbrush compat layers in
     * {@code Ev0sChiselPlugin.start()}.
     */
    public static void injectPaintbrushStates() {
        List<String> found = new ArrayList<>();
        for (String key : VOXEL_ART) {
            if (BlockTypeCache.exists(key)) found.add(key);
        }
        if (found.isEmpty()) return;

        String[] arr = found.toArray(new String[0]);

        // Build the shared Paintbrush.Data — all panels point to the same instance
        Paintbrush.Data data = new Paintbrush.Data();
        data.source        = "PixelHeroes";
        data.colorVariants = arr;

        int count = 0;
        for (String key : arr) {
            BlockType bt = BlockTypeCache.get(key);
            if (bt == null) continue;
            StateData existing = bt.getState();
            if (existing instanceof Paintbrush.Data) continue; // already handled
            if (ComboStateHelper.inject(bt, null, data, null)) count++;
        }

        if (count > 0) {
            LOGGER.atWarning().log("[PixelHeroesCompat] Injected paintbrush onto " + count
                    + " PixelHeroes panel blocks.");
        }
    }
}
