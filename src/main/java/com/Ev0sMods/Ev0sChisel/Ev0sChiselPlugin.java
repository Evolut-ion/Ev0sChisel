//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel;

import com.Ev0sMods.Ev0sChisel.Chisel.Data;
import com.Ev0sMods.Ev0sChisel.Interactions.ChiselInteraction;
import com.Ev0sMods.Ev0sChisel.compat.CarpentryCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawCompat;
import com.Ev0sMods.Ev0sChisel.compat.MasonryCompat;
import com.Ev0sMods.Ev0sChisel.compat.StoneworksCompat;
import com.Ev0sMods.Ev0sChisel.compat.VanillaCompat;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateRegistry;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class Ev0sChiselPlugin extends JavaPlugin {
    private static Ev0sChiselPlugin instance;

    public Ev0sChiselPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin loaded!");
    }

    public static Ev0sChiselPlugin getInstance() {
        return instance;
    }

    protected void setup() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin setup!");
        this.registerEvents();
        this.registerCommands();
        MasonryCompat.init();
        CarpentryCompat.init();
    }

    protected void start() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin enabled!");
        MasonryCompat.injectChiselStates();
        CarpentryCompat.injectChiselStates();
        StoneworksCompat.init();
        MacawCompat.init();
        injectDerivedBlockStates();
        VanillaCompat.injectChiselStates();
    }

    public void shutdown() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin disabled!");
    }

    private void registerEvents() {
        BlockStateRegistry blockStateRegistry = this.getBlockStateRegistry();
        blockStateRegistry.registerBlockState(Chisel.class, "Ev0sChisel", Chisel.CODEC, Chisel.Data.class, Chisel.Data.CHISELCODEC);
        this.getCodecRegistry(Interaction.CODEC).register("ChiselInteraction", ChiselInteraction.class,  ChiselInteraction.CODEC );
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
        String[] rockTypes = {
                "Aqua", "Ash", "Basalt", "Calcite", "Chalk", "Clay_Brick",
                "Crystal_Cyan", "Crystal_Green", "Crystal_Pink", "Crystal_Yellow",
                "Dirt", "Marble", "Sandstone", "Sandstone_Red", "Sandstone_White",
                "Snow", "Stone"
        };

        for (String rockType : rockTypes) {
            try {
                BlockType rockBt = BlockType.fromString("Rock_" + rockType);
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
                        "[Chisel] Error injecting derived states for Rock_" + rockType + ": " + t.getMessage());
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
                BlockType bt = BlockType.fromString(key);
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
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
