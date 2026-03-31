package com.Ev0sMods.Ev0sChisel.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Vanilla compat for the Paintbrush covering Hytale's cloth/wool blocks.
 *
 * <h3>Two distinct groups:</h3>
 * <ul>
 *   <li><b>Wool group</b> – all {@code Cloth_Block_Wool_{Color}} blocks.
 *       Any wool block can be painted to any other wool color.</li>
 *   <li><b>Cloth roof group</b> – every variant of
 *       {@code Cloth_Roof_{Color}}, {@code Cloth_Roof_{Color}_Flat},
 *       {@code Cloth_Roof_{Color}_Flap}, and
 *       {@code Cloth_Roof_{Color}_Vertical}.
 *       All roof styles and colors are cross-selectable with each other.</li>
 * </ul>
 *
 * Colors are probed at runtime.  For each base color the probe checks both
 * orderings: {@code Light_{Color}} / {@code Dark_{Color}} (prefix form) and
 * {@code {Color}_Light} / {@code {Color}_Dark} (suffix form), so the compat
 * works regardless of which convention the asset pack uses.
 */
public final class VanillaClothCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ── Base color names ────────────────────────────────────────────────
    private static final String[] BASE_COLORS = {
            "Red", "Blue", "Green", "Yellow", "White", "Black",
            "Orange", "Purple", "Pink", "Cyan", "Magenta",
            "Lime", "Brown", "Gray", "Beige", "Cream", "Ivory",
            "Violet", "Indigo", "Teal", "Maroon", "Navy", "Olive",
            "Aqua", "Rose", "Coral", "Peach", "Salmon", "Crimson",
            "Amber", "Gold", "Silver", "Tan", "Khaki"
    };

    /**
     * ALL_COLORS contains every color token to probe, in priority order:
     * <ol>
     *   <li>Base colors (e.g. {@code Red})</li>
     *   <li>Suffix shades – color-then-modifier (e.g. {@code Red_Light}, {@code Red_Dark})</li>
     *   <li>Prefix shades – modifier-then-color (e.g. {@code Light_Red}, {@code Dark_Red})</li>
     * </ol>
     * Both orderings are included so blocks are discovered regardless of the
     * naming convention used in the asset pack.
     */
    private static final String[] ALL_COLORS;
    static {
        List<String> all = new ArrayList<>();
        // Base
        Collections.addAll(all, BASE_COLORS);
        // Suffix form: Color_Light / Color_Dark  (the more common Hytale convention)
        for (String c : BASE_COLORS) { all.add(c + "_Light"); all.add(c + "_Dark"); }
        // Prefix form: Light_Color / Dark_Color  (also checked as fallback)
        for (String c : BASE_COLORS) { all.add("Light_" + c); all.add("Dark_" + c); }
        for (String c : BASE_COLORS) { all.add("Light" + c); all.add("Dark" + c); }
        ALL_COLORS = all.toArray(new String[0]);
    }

    // ── Mcw Carpets ─────────────────────────────────────────────────────
    /** Carpet forms where color comes first: Mcw_Carpets_{Color}_{suffix} */
    private static final String[] MCW_CARPET_COLOR_SUFFIXES = {
            "Carpet_Block",
            "Carpet_Cloth",
            "Carpet_Cloth_Light",
            "Carpet_Cloth_Slab",
            "Carpet_Cloth_Light_Slab"
    };

    /** Carpet forms where a shape prefix comes before the color: Mcw_Carpets_{prefix}_{Color}_Carpet */
    private static final String[] MCW_CARPET_PREFIXES = {
            "Rectangle",
            "Small",
            "Small_Square"
    };

    // Roof style suffixes (empty string = plain Cloth_Roof_{Color})
    private static final String[] ROOF_STYLES = {
            "", "_Flat", "_Flap", "_Vertical"
    };

    private static final String[] WOOL_SUFFIXES = {
            "", "_Stairs", "_Slab", "_Half"
    };

    private static final String[] MODERN_ROOF_SUFFIXES = {
            "_Roof",
            "_Roof_Flat",
            "_Roof_Shallow",
            "_Roof_Steep",
            "_Roof_Vertical",
            "_Roof_Vertical_Flap"
    };

    private VanillaClothCompat() {}

    // ─────────────────────────────────────────────────────────────────────
    // Extra cloth keys registered by other compat passes (e.g. SerenalCompat)
    // before injectPaintbrushStates() runs.  All registered keys are merged
    // into the unified allCloth group so cross-mod painting works seamlessly.
    // ─────────────────────────────────────────────────────────────────────

    private static final List<String> EXTRA_CLOTH_KEYS = new ArrayList<>();

    /**
     * Registers additional block keys to be included in the unified
     * {@code Cloth_All} paintbrush group.  Must be called before
     * {@link #injectPaintbrushStates()}.
     *
     * @param keys block-type identifiers to include (only existing blocks
     *             should be passed; non-existent ones are silently ignored
     *             by {@link #injectGroup})
     */
    public static void registerExtraClothKeys(List<String> keys) {
        if (keys != null) EXTRA_CLOTH_KEYS.addAll(keys);
    }

    public static String[] getAllColors() {
        return ALL_COLORS;
    }

    public static String[] getClassicRoofStyles() {
        return ROOF_STYLES;
    }

    public static String[] getWoolSuffixes() {
        return WOOL_SUFFIXES;
    }

    public static String[] getModernRoofSuffixes() {
        return MODERN_ROOF_SUFFIXES;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Discovers existing cloth blocks and injects {@link Paintbrush.Data}
     * onto them so the Paintbrush UI can switch between color variants.
     * <p>
     * Call this from {@code Ev0sChiselPlugin.start()} after all other
     * compat passes.
     */
    public static void injectPaintbrushStates() {
        // ── Unified wool / cloth / carpet group ───────────────────────────
        // All wool, cloth-roof, modern-cloth, and all Mcw carpet variants are
        // merged into one array so pressing the paintbrush on any of them
        // shows the full cross-family colour picker.
        List<String> allCloth = new ArrayList<>();
        addAll(allCloth, discoverWoolVariants());
        addAll(allCloth, discoverRoofVariants());
        addAll(allCloth, discoverModernRoofVariants());
        for (String suffix : MCW_CARPET_COLOR_SUFFIXES)
            addAll(allCloth, discoverMcwCarpetBySuffix(suffix));
        for (String prefix : MCW_CARPET_PREFIXES)
            addAll(allCloth, discoverMcwCarpetByPrefix(prefix));
        // Extra keys registered by other compat passes (e.g. Serenal cloth shapes)
        if (!EXTRA_CLOTH_KEYS.isEmpty())
            addAll(allCloth, EXTRA_CLOTH_KEYS.toArray(new String[0]));

        int total = 0;
        if (!allCloth.isEmpty())
            total += injectGroup(allCloth.toArray(new String[0]), "Cloth_All");

        // ── Village wall stays its own group (wood, not cloth) ────────────
        total += injectGroup(discoverVillageWallVariants(), "Wood_Village_Wall");

        // injected Paintbrush.Data summary (info log removed)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Discovery
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Probes {@code Cloth_Block_Wool_{Color}} for every known color and
     * returns an array of the keys that actually exist in the asset registry.
     */
    /**
     * Probes {@code Wood_Village_Wall_{Color}_Full} for every known color and
     * returns an array of the keys that actually exist in the asset registry.
     */
    private static String[] discoverVillageWallVariants() {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            String key = "Wood_Village_Wall_" + color + "_Full";
            if (exists(key)) found.add(key);
        }
        if (found.isEmpty()) {
            // no village wall blocks found (info log removed)
        }
        return found.toArray(new String[0]);
    }
    private static String[] discoverModernRoofVariants() {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            for (String suffix : MODERN_ROOF_SUFFIXES) {
                String key = "Cloth_Modern_" + color + suffix;
                if (exists(key)) found.add(key);
            }
        }
        if (found.isEmpty()) {
            // no modern roof blocks found (info log removed)
        }
        return found.toArray(new String[0]);
    }

    private static String[] discoverWoolVariants() {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            for (String suffix : WOOL_SUFFIXES) {
                String key = "Cloth_Block_Wool_" + color + suffix;
                if (exists(key)) found.add(key);
            }
        }
        if (found.isEmpty()) {
            // no wool blocks found (info log removed)
        }
        return found.toArray(new String[0]);
    }

    /**
     * Probes all combinations of {@code Cloth_Roof_{Color}{Style}} and
     * returns every key that exists.  All styles (plain, Flat, Flap,
     * Vertical) and all colors are combined into one flat array so the
     * whole cloth-roof family is cross-selectable.
     */
    private static String[] discoverMcwCarpetBySuffix(String suffix) {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            String key = "Mcw_Carpets_" + color + "_" + suffix;
            if (exists(key)) found.add(key);
        }
        return found.toArray(new String[0]);
    }

    private static String[] discoverMcwCarpetByPrefix(String prefix) {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            String key = "Mcw_Carpets_" + prefix + "_" + color + "_Carpet";
            if (exists(key)) found.add(key);
        }
        return found.toArray(new String[0]);
    }

    private static String[] discoverRoofVariants() {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            for (String style : ROOF_STYLES) {
                String key = "Cloth_Roof_" + color + style;
                if (exists(key)) found.add(key);
            }
        }
        if (found.isEmpty()) {
            // no cloth roof blocks found (info log removed)
        }
        return found.toArray(new String[0]);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Injection
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injects a {@link Paintbrush.Data} state onto every block in
     * {@code keys}, using the full {@code keys} array as the
     * {@code colorVariants} list so every member can be painted to any
     * other member.
     *
     * @param keys   array of BlockType identifiers that form one paint group
     * @param source human-readable label stored on the state (e.g. "Cloth_Block_Wool")
     * @return number of blocks that were actually injected
     */
    private static int injectGroup(String[] keys, String source) {
        if (keys == null || keys.length == 0) return 0;

        int injected = 0;
        for (String key : keys) {
                try {
                    BlockType bt = BlockTypeCache.get(key);
                    if (bt == null) continue;

                StateData existing = bt.getState();
                if (existing instanceof Paintbrush.Data) continue; // already handled

                Paintbrush.Data data = new Paintbrush.Data();
                data.source       = source;
                data.colorVariants = keys;

                setField(StateData.class, data, "id", "Ev0sPaintbrush");
                setField(BlockType.class,  bt,  "state", data);
                injected++;
            } catch (Throwable t) {
                LOGGER.atWarning().log("[Paintbrush] VanillaClothCompat: failed to inject "
                        + key + ": " + t.getMessage());
            }
        }
        return injected;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static void addAll(List<String> list, String[] arr) {
        if (arr != null) Collections.addAll(list, arr);
    }

    private static boolean exists(String key) {
        try {
            return BlockTypeCache.exists(key);
        } catch (Exception e) {
            return false;
        }
    }

    private static void setField(Class<?> clazz, Object target,
                                  String fieldName, Object value) throws Exception {
        ReflectionCache.setField(clazz, target, fieldName, value);
    }
}
