package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Chisel compat for <b>TreeSources</b> mod ({@code Wood_{TreeType}_*}).
 *
 * <p>Groups all planks, decorative, ornate, and structural trunk variants
 * of each tree type into one {@link Chisel.Data} group, with stair and
 * half-slab arrays populated so the chisel UI shows the full variant picker.
 *
 * <h3>Supported tree types (buildable planks exist)</h3>
 * Coalc_Oak, Crystalder, Featherleaf, Gemnut, Ironwood, Leatherbark,
 * Void_Apple, Woolow
 *
 * <h3>Purely decorative / trunk-only trees (not grouped)</h3>
 * Dirtch, Essence, CyanCrystalder + seasonal trunks (Autumn/Spring/Summer/Winter)
 * have only logs, trunks, and leaves — no planks — so they are left to
 * VanillaCompat's discovery pass via its VANILLA_WOOD_SUFFIXES.
 */
public final class TreeSourcesCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private TreeSourcesCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Tree types that have planks / building blocks
    // ─────────────────────────────────────────────────────────────────────

    private static final String[] TREE_TYPES = {
            "Coalc_Oak", "Crystalder", "Featherleaf", "Gemnut",
            "Ironwood", "Leatherbark", "Void_Apple", "Woolow"
    };

    // ─────────────────────────────────────────────────────────────────────
    // Suffix lists — probed as "Wood_{type}{suffix}"
    // ─────────────────────────────────────────────────────────────────────

    /** Full-block (substitution) suffixes — planks, decorative, ornate, trunks. */
    private static final String[] BLOCK_SUFFIXES = {
            "_Planks", "_Plank",              // Leatherbark uses "_Plank" (singular)
            "_Decorative", "_Ornate",
            "_Trunk", "_Trunk_Full", "_Trunk_Half",
            "_Roots",
            "_Branch_Corner", "_Branch_Long", "_Branch_Short"
    };

    /** Stair suffixes — derived from building blocks. */
    private static final String[] STAIR_SUFFIXES = {
            "_Planks_Stairs", "_Decorative_Stairs", "_Ornate_Stairs",
            "_Trunk_Stairs"
    };

    /**
     * Half-slab suffixes — most trees use {@code _Half_Decorative / _Half_Ornate},
     * Leatherbark uses {@code _Decorative_Half / _Ornate_Half}.
     */
    private static final String[] HALF_SUFFIXES = {
            "_Half",
            "_Half_Decorative", "_Half_Ornate",     // standard (most types)
            "_Decorative_Half", "_Ornate_Half"       // Leatherbark-style
    };

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    public static void injectChiselStates() {
        int total = 0;
        for (String treeType : TREE_TYPES) {
            String prefix = "Wood_" + treeType;

            // Collect base blocks (substitutions)
            List<String> blocks = new ArrayList<>();
            for (String s : BLOCK_SUFFIXES) {
                String k = prefix + s;
                if (exists(k)) blocks.add(k);
            }
            // Stairs
            List<String> stairs = new ArrayList<>();
            for (String s : STAIR_SUFFIXES) {
                String k = prefix + s;
                if (exists(k)) stairs.add(k);
            }
            // Halfs
            List<String> halfs = new ArrayList<>();
            for (String s : HALF_SUFFIXES) {
                String k = prefix + s;
                if (exists(k)) halfs.add(k);
            }

            if (blocks.isEmpty() && stairs.isEmpty() && halfs.isEmpty()) continue;

            // All blocks form the substitution set; stairs/halfs are also listed there
            // so the picker shows every variant of the tree
            Set<String> allSet = new LinkedHashSet<>(blocks);
            allSet.addAll(stairs);
            allSet.addAll(halfs);
            String[] allSubs = allSet.toArray(new String[0]);
            String[] stairsArr = stairs.toArray(new String[0]);
            String[] halfsArr  = halfs.toArray(new String[0]);
            String[] roofing   = new String[0];

            total += injectFamily(new ArrayList<>(allSet), allSubs, stairsArr, halfsArr, roofing, prefix);
        }
        if (total > 0)
            LOGGER.atWarning().log("[TreeSourcesCompat] Injected chisel data onto " + total + " blocks.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Injection helper
    // ─────────────────────────────────────────────────────────────────────

    private static int injectFamily(List<String> keys,
                                    String[] substitutions,
                                    String[] stairs,
                                    String[] halfSlabs,
                                    String[] roofing,
                                    String source) {
        int count = 0;
        for (String key : keys) {
            try {
                BlockType bt = BlockTypeCache.get(key);
                if (bt == null) continue;

                StateData existing = bt.getState();

                if (existing instanceof Chisel.Data existingData) {
                    // Merge new arrays into any pre-existing chisel data
                    boolean changed = false;
                    String[] merged = mergeUnique(existingData.substitutions, substitutions);
                    if (!java.util.Arrays.equals(existingData.substitutions, merged)) {
                        existingData.substitutions = merged; changed = true;
                    }
                    String[] ms = mergeUnique(existingData.stairs, stairs);
                    if (!java.util.Arrays.equals(existingData.stairs, ms)) {
                        existingData.stairs = ms; changed = true;
                    }
                    String[] mh = mergeUnique(existingData.halfSlabs, halfSlabs);
                    if (!java.util.Arrays.equals(existingData.halfSlabs, mh)) {
                        existingData.halfSlabs = mh; changed = true;
                    }
                    if (changed) count++;
                    continue;
                }

                Chisel.Data data = new Chisel.Data();
                data.source        = source;
                data.substitutions = substitutions;
                data.stairs        = stairs;
                data.halfSlabs     = halfSlabs;
                data.roofing       = roofing;

                ReflectionCache.setField(StateData.class, data, "id", "Ev0sChisel");
                ReflectionCache.setField(BlockType.class, bt,   "state", data);
                count++;
            } catch (Exception e) {
                LOGGER.atWarning().log("[TreeSourcesCompat] inject failed for "
                        + key + ": " + e.getMessage());
            }
        }
        return count;
    }

    private static String[] mergeUnique(String[] first, String[] second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first  != null) for (String s : first)  if (s != null) merged.add(s);
        if (second != null) for (String s : second) if (s != null) merged.add(s);
        return merged.toArray(new String[0]);
    }

    private static boolean exists(String key) {
        try { return BlockTypeCache.exists(key); }
        catch (Exception e) { return false; }
    }
}
