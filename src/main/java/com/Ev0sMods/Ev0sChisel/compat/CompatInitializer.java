package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Ev0sChiselPlugin;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Helper to speed up startup by preloading BlockType cache and running
 * compat init passes in parallel.
 */
public final class CompatInitializer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CompatInitializer() {}

    /**
     * Warm the BlockType cache with likely keys and initialize compat systems
     * in parallel.  This reduces serial registry probing time.
     *
     * @param threads number of threads to use for preloading and init
     */
    public static void warmupAndInit(int threads) {
        LOGGER.atInfo().log("[Chisel] CompatInitializer: starting warmup with " + threads + " threads");

        List<String> candidates = new ArrayList<>();

        // Rock types and common suffixes
        String[] rockTypes = VanillaCompat.getRockTypes();
        String[] rockSuffixes = {"", "_Cobble", "_Polished", "_Brick", "_Bricks", "_Tile", "_Tiles", "_Slab", "_Slabs", "_Cracked", "_Mossy", "_Chiseled", "_Smooth", "_Cut"};
        for (String rt : rockTypes) {
            for (String s : rockSuffixes) candidates.add("Rock_" + rt + s);
            // also bare type variants
            for (String s : rockSuffixes) candidates.add(rt + s);
        }

        // Wood types and common suffixes
        String[] woodTypes = VanillaCompat.getWoodTypes();
        String[] woodSuffixes = {"", "_Planks", "_Log", "_Stripped_Log", "_Bark", "_Post", "_Panel", "_Roof", "_Roof_Flat", "_Shingle"};
        for (String wt : woodTypes) {
            for (String s : woodSuffixes) candidates.add("Wood_" + wt + s);
        }

        // Macaw / Stoneworks / Common detection keys
        candidates.addAll(Arrays.asList(
                "Mcw_Paths_Rock_Stone_Brick_Dumble",
                "Mcw_Stairs_Stone_Classic_Stairs",
                "Cobblestones",
                "Rock_Stone"
        ));

        // Cloth / wool common keys
        String[] baseColors = {"Red","Blue","Green","Yellow","White","Black","Orange","Purple","Pink","Cyan","Magenta","Lime","Brown","Gray","Beige"};
        String[] clothRoofStyles = {"", "_Flat", "_Flap", "_Vertical"};
        for (String c : baseColors) {
            candidates.add("Cloth_Block_Wool_" + c);
            for (String s : clothRoofStyles) candidates.add("Cloth_Roof_" + c + s);
            candidates.add("Wood_Village_Wall_" + c + "_Full");
        }

        // Preload cache in parallel
        int loaded = BlockTypeCache.preload(candidates, Math.max(1, threads), 30);
        LOGGER.atInfo().log("[Chisel] CompatInitializer: preload completed, " + loaded + " candidates loaded into cache");

        // Run compat init passes in parallel (these are idempotent guards inside)
        ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, threads));
        ex.submit(() -> MasonryCompat.init());
        ex.submit(() -> CarpentryCompat.init());
        ex.submit(() -> StoneworksCompat.init());
        ex.submit(() -> MacawCompat.init());
        ex.submit(() -> NoCubeNeonCompat.init());

        ex.shutdown();
        try {
            boolean ok = ex.awaitTermination(60, TimeUnit.SECONDS);
            if (!ok) LOGGER.atInfo().log("[Chisel] CompatInitializer: compat init timed out after 60s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // After compat init, merge contributions
        CompatMerger.mergeAllCompatData();

        // Inject derived block states (same as previously done from plugin)
        try {
            Ev0sChiselPlugin.getInstance().getLogger().atInfo().log("[Chisel] CompatInitializer: injecting derived block states");
            Ev0sChiselPlugin.getInstance().getClass().getDeclaredMethod("injectDerivedBlockStates").invoke(Ev0sChiselPlugin.getInstance());
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] CompatInitializer: failed to call injectDerivedBlockStates: " + t.getMessage());
        }

        // Finally, inject paintbrush states for NoCube and vanilla cloth
        NoCubeNeonCompat.injectPaintbrushStates();
        VanillaClothCompat.injectPaintbrushStates();

        LOGGER.atInfo().log("[Chisel] CompatInitializer: warmup and init complete");
    }
}
