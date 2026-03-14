package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

import java.lang.reflect.Field;
import java.util.*;

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
        ALL_COLORS = all.toArray(new String[0]);
    }

    // Roof style suffixes (empty string = plain Cloth_Roof_{Color})
    private static final String[] ROOF_STYLES = {
            "", "_Flat", "_Flap", "_Vertical"
    };

    private VanillaClothCompat() {}

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
        String[] woolVariants        = discoverWoolVariants();
        String[] roofVariants        = discoverRoofVariants();
        String[] villageWallVariants = discoverVillageWallVariants();

        int total = 0;
        total += injectGroup(woolVariants,        "Cloth_Block_Wool");
        total += injectGroup(roofVariants,        "Cloth_Roof");
        total += injectGroup(villageWallVariants, "Wood_Village_Wall");

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

    private static String[] discoverWoolVariants() {
        List<String> found = new ArrayList<>();
        for (String color : ALL_COLORS) {
            String key = "Cloth_Block_Wool_" + color;
            if (exists(key)) found.add(key);
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
