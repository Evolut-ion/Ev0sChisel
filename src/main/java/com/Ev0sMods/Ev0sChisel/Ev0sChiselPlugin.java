//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.Ev0sMods.Ev0sChisel.Interactions.CarpenterHammerInteraction;
import com.Ev0sMods.Ev0sChisel.Interactions.ChiselInteraction;
import com.Ev0sMods.Ev0sChisel.Interactions.PaintbrushInteraction;
import com.Ev0sMods.Ev0sChisel.compat.BreezeBlocksCompat;
import com.Ev0sMods.Ev0sChisel.compat.CarpentryCompat;
import com.Ev0sMods.Ev0sChisel.compat.ChippedCompat;
import com.Ev0sMods.Ev0sChisel.compat.CompatMerger;
import com.Ev0sMods.Ev0sChisel.compat.FemboyDelightCompat;
import com.Ev0sMods.Ev0sChisel.compat.FurnitureWindowCompat;
import com.Ev0sMods.Ev0sChisel.compat.GlassCompat;
import com.Ev0sMods.Ev0sChisel.compat.GuiFurnitureCompat;
import com.Ev0sMods.Ev0sChisel.compat.LabelsCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawWindowDoorCompat;
import com.Ev0sMods.Ev0sChisel.compat.MasonryCompat;
import com.Ev0sMods.Ev0sChisel.compat.NoCubeNeonCompat;
import com.Ev0sMods.Ev0sChisel.compat.OctaPanelCompat;
import com.Ev0sMods.Ev0sChisel.compat.PixelHeroesCompat;
import com.Ev0sMods.Ev0sChisel.compat.SerenalCompat;
import com.Ev0sMods.Ev0sChisel.compat.StatuesCompat;
import com.Ev0sMods.Ev0sChisel.compat.StoneworksCompat;
import com.Ev0sMods.Ev0sChisel.compat.TreeSourcesCompat;
import com.Ev0sMods.Ev0sChisel.compat.VanillaClothCompat;
import com.Ev0sMods.Ev0sChisel.compat.VanillaCompat;
import com.Ev0sMods.Ev0sChisel.compat.VanillaFurnitureCompat;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class Ev0sChiselPlugin extends JavaPlugin {
    private static Ev0sChiselPlugin instance;

    public Ev0sChiselPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.getLogger().at(Level.INFO).log("[ChiselPlugin] Plugin loaded!");
    }

    public static Ev0sChiselPlugin getInstance() {
        return instance;
    }

    protected void setup() {
        this.getLogger().at(Level.INFO).log("[ChiselPlugin] Plugin setup!");
        this.registerEvents();
        this.registerCommands();
        MasonryCompat.init();
        CarpentryCompat.init();
        NoCubeNeonCompat.init();
        StatuesCompat.init();
    }

    protected void start() {
        this.getLogger().at(Level.INFO).log("[ChiselPlugin] Plugin enabled!");
        // Serial compat initialization for faster, safer startup
        MasonryCompat.init();
        CarpentryCompat.init();
        StatuesCompat.init();
        StoneworksCompat.init();
        MacawCompat.init();
        CompatMerger.mergeAllCompatData();
        VanillaCompat.injectChiselStates();
        TreeSourcesCompat.injectChiselStates();
        ChippedCompat.init();
        GlassCompat.injectChiselStates();
        BreezeBlocksCompat.init();
        GuiFurnitureCompat.init();
        FurnitureWindowCompat.init();
        MacawWindowDoorCompat.init();
        FemboyDelightCompat.init();
        VanillaFurnitureCompat.init();
        SerenalCompat.init();
        
        injectDerivedBlockStates();
        NoCubeNeonCompat.init();
        // Ensure paintbrush compat layers are injected
        NoCubeNeonCompat.injectPaintbrushStates();
        VanillaClothCompat.injectPaintbrushStates();
        GlassCompat.injectPaintbrushStates();
        OctaPanelCompat.injectPaintbrushStates();
        PixelHeroesCompat.injectPaintbrushStates();
        FemboyDelightCompat.injectPaintbrushStates();
        // Labels compat (Yer's Labels + Boske's Chest Labels)
        LabelsCompat.init();
        LabelsCompat.injectChiselStates();
    }

    public void shutdown() {
        this.getLogger().at(Level.INFO).log("[ChiselPlugin] Plugin disabled!");
    }

    private void registerEvents() {
        // Register as components (component API in prerelease) with compatibility fallback
        try {
            // Try to register per-block component types with prerelease ComponentRegistry via reflection.
            com.Ev0sMods.Ev0sChisel.compat.ComponentCompat.registerComponent(Chisel.class, "Ev0sChisel", Chisel.CODEC);
            com.Ev0sMods.Ev0sChisel.compat.ComponentCompat.registerComponent(Paintbrush.class, "Ev0sPaintbrush", Paintbrush.CODEC);
            com.Ev0sMods.Ev0sChisel.compat.ComponentCompat.registerComponent(CarpenterHammer.class, "Ev0sCarpenterHammer", CarpenterHammer.CODEC);
        } catch (Throwable ignored) {}
        this.getCodecRegistry(Interaction.CODEC).register("ChiselInteraction", ChiselInteraction.class,  ChiselInteraction.CODEC );
        this.getCodecRegistry(Interaction.CODEC).register("PaintbrushInteraction", PaintbrushInteraction.class, PaintbrushInteraction.CODEC );
        this.getCodecRegistry(Interaction.CODEC).register("CarpenterHammerInteraction", CarpenterHammerInteraction.class, CarpenterHammerInteraction.CODEC);
    }

    private void registerCommands() {
    }

    // ─────────────────────────────────────────────────────────────────────
    // Inject Chisel.Data onto vanilla derived blocks (stairs, halfs, roofing)
    // so that clicking them with the chisel opens the variant picker.
    // ─────────────────────────────────────────────────────────────────────

    private void injectDerivedBlockStates() {
        // Collect all blocks that already have Chisel.Data
        // For each, look at their stairs/halfs/roofing arrays
        // Inject Chisel.Data onto any entry that doesn't already have it
        int injected = 0;
        int scanned  = 0;

        // Scan the vanilla rock types that MasonryCompat knows about
        String[] rockTypes = VanillaCompat.getRockTypes();

        for (String rockType : rockTypes) {
            try {
                String baseKey = "Rock_" + rockType;
                if (VanillaCompat.isMetalType(rockType)) {
                    String metalKey = "Metal_" + rockType;
                    if (com.Ev0sMods.Ev0sChisel.compat.BlockTypeCache.exists(metalKey)) {
                        baseKey = metalKey;
                    }
                }

                BlockType rockBt = com.Ev0sMods.Ev0sChisel.compat.BlockTypeCache.get(baseKey);
                if (rockBt == null) continue;
                StateData state = rockBt.getState();
                if (!(state instanceof Chisel.Data parentData)) continue;
                scanned++;

                // The parent block's full chisel arrays
                String[] subs   = parentData.substitutions;
                String[] stairs = parentData.stairs;
                String[] halfs  = parentData.halfSlabs;
                String[] roofs  = parentData.roofing;

                // Auto-derive stairs/halfs/roofing from block subs if arrays are empty
                if (empty(stairs) && !empty(subs)) stairs = MasonryCompat.deriveExistingVariants(subs, "_Stairs");
                if (empty(halfs) && !empty(subs))  halfs  = MasonryCompat.deriveExistingVariants(subs, "_Half");
                if (empty(roofs) && !empty(subs)) {
                    roofs = MasonryCompat.deriveExistingRoofing(subs);
                    if (empty(roofs))
                        roofs = VanillaCompat.deriveExistingWoodRoofing(subs);
                }

                // Merge compat contributions
                if (MasonryCompat.isAvailable()) {
                    String normType = rockType.toLowerCase(Locale.ROOT);
                    stairs = mergeArr(stairs, MasonryCompat.getStairVariants(normType));
                    halfs  = mergeArr(halfs,  MasonryCompat.getHalfVariants(normType));
                }

                // Inject onto derived blocks that lack Chisel.Data
                injected += injectOnArray(stairs, subs, stairs, halfs, roofs, rockType);
                injected += injectOnArray(halfs,  subs, stairs, halfs, roofs, rockType);
                injected += injectOnArray(roofs,  subs, stairs, halfs, roofs, rockType);

            } catch (Throwable t) {
                this.getLogger().at(Level.WARNING).log(
                        "[Chisel] Error injecting derived states for " + rockType + ": " + t.getMessage());
            }
        }

        this.getLogger().at(Level.INFO).log(
                "[Chisel] Scanned " + scanned + " rock types, injected Chisel state onto "
                        + injected + " derived blocks (stairs/halfs/roofing)");
    }

    /**
     * For each block key in {@code targets}, inject a {@link Chisel.Data}
     * if the block doesn't already have one.
     */
    private int injectOnArray(String[] targets, String[] subs, String[] stairs,
                              String[] halfs, String[] roofs, String source) {
        if (targets == null) return 0;
        int count = 0;
        for (String key : targets) {
            if (key == null) continue;
            try {
                BlockType bt = com.Ev0sMods.Ev0sChisel.compat.BlockTypeCache.get(key);
                if (bt == null) continue;
                StateData existing = bt.getState();
                if (existing instanceof Chisel.Data) continue; // already has it

                Chisel.Data data = new Chisel.Data();
                data.source        = source;
                data.substitutions = subs  != null ? subs  : new String[0];
                data.stairs        = stairs != null ? stairs : new String[0];
                data.halfSlabs     = halfs != null ? halfs : new String[0];
                data.roofing       = roofs != null ? roofs : new String[0];

                setField(StateData.class, data, "id", "Ev0sChisel");
                setField(BlockType.class, bt, "state", data);
                count++;
            } catch (Throwable t) {
                // silently skip blocks that can't be injected
            }
        }
        return count;
    }

    private static void setField(Class<?> clazz, Object target,
                                 String fieldName, Object value) throws Exception {
        com.Ev0sMods.Ev0sChisel.compat.ReflectionCache.setField(clazz, target, fieldName, value);
    }

    private static String[] mergeArr(String[] base, List<String> extra) {
        if (extra == null || extra.isEmpty()) return base;
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (base != null) Collections.addAll(set, base);
        set.addAll(extra);
        return set.toArray(new String[0]);
    }

    private static boolean empty(String[] arr) { return arr == null || arr.length == 0; }
}
