package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.Ev0sMods.Ev0sChisel.Ev0sChiselPlugin;
import com.hypixel.hytale.logger.HytaleLogger;

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
        // removed CompatInitializer start info log

        List<String> candidates = new ArrayList<>();

        // Rock types and common suffixes
        String[] rockTypes = VanillaCompat.getRockTypes();
        String[] rockSuffixes = VanillaCompat.getRockNaturalSuffixes();
        for (String rt : rockTypes) {
            for (String s : rockSuffixes) candidates.add("Rock_" + rt + s);
            if (VanillaCompat.isMetalType(rt)) {
                for (String s : rockSuffixes) candidates.add("Metal_" + rt + s);
            }
            // also bare type variants
            for (String s : rockSuffixes) candidates.add(rt + s);
        }

        // Wood types and common suffixes
        String[] woodTypes = VanillaCompat.getWoodTypes();
        String[] woodSuffixes = VanillaCompat.getVanillaWoodSuffixes();
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
        for (String c : VanillaClothCompat.getAllColors()) {
            for (String suffix : VanillaClothCompat.getWoolSuffixes()) {
                candidates.add("Cloth_Block_Wool_" + c + suffix);
            }
            for (String s : VanillaClothCompat.getClassicRoofStyles()) {
                candidates.add("Cloth_Roof_" + c + s);
            }
            for (String s : VanillaClothCompat.getModernRoofSuffixes()) {
                candidates.add("Cloth_Modern_" + c + s);
            }
            candidates.add("Wood_Village_Wall_" + c + "_Full");
        }

        // Preload cache in parallel
        int loaded = BlockTypeCache.preload(candidates, Math.max(1, threads), 30);
        // removed preload completed info log

        // Run compat init passes in parallel (these are idempotent guards inside)
        ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, threads));
        ex.submit(() -> MasonryCompat.init());
        ex.submit(() -> CarpentryCompat.init());
        ex.submit(() -> StatuesCompat.init());
        ex.submit(() -> StoneworksCompat.init());
        ex.submit(() -> MacawCompat.init());
        ex.submit(() -> NoCubeNeonCompat.init());

        ex.shutdown();
        try {
            boolean ok = ex.awaitTermination(60, TimeUnit.SECONDS);
            if (!ok) {
                // removed compat init timeout info log
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // After compat init, merge contributions
        CompatMerger.mergeAllCompatData();

        // Inject statue chisel states (so statues can be chiseled back into their material)
        try {
            StatuesCompat.injectChiselStates();
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] Failed to inject statue chisel states: " + t.getMessage());
        }

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

        // removed warmup and init complete info log
    }
}
