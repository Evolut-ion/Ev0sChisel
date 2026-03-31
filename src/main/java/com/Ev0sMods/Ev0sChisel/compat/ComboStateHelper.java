package com.Ev0sMods.Ev0sChisel.compat;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.ComboState;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * Utility for injecting {@link ComboState} onto a {@link BlockType}.
 *
 * <p>When a block already carries one of the plain single-tool states
 * ({@link Chisel.Data}, {@link Paintbrush.Data}, {@link CarpenterHammer.Data}),
 * this helper "upgrades" it to a {@link ComboState} so the new tool data sits
 * alongside the existing data rather than replacing it.
 *
 * <p>Calling with a {@code null} sub-state argument leaves that slot unchanged.
 */
public final class ComboStateHelper {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ComboStateHelper() {}

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injects (or merges into) a {@link ComboState} on {@code bt}.
     *
     * <p>Pass non-{@code null} for each sub-state you want to set.
     * Existing sub-states already on the block are preserved.
     *
     * @param bt        target BlockType (must be non-null)
     * @param chisel    Chisel sub-state to set, or {@code null} to leave unchanged
     * @param paintbrush Paintbrush sub-state to set, or {@code null} to leave unchanged
     * @param hammer    CarpenterHammer sub-state to set, or {@code null} to leave unchanged
     * @return {@code true} if the block was successfully updated
     */
    public static boolean inject(BlockType bt,
                                  Chisel.Data chisel,
                                  Paintbrush.Data paintbrush,
                                  CarpenterHammer.Data hammer) {
        if (bt == null) return false;
        try {
            StateData existing = bt.getState();
            ComboState combo   = toCombo(existing);

            if (chisel    != null) combo.chisel    = chisel;
            if (paintbrush != null) combo.paintbrush = paintbrush;
            if (hammer     != null) combo.hammer     = hammer;

            ReflectionCache.setField(StateData.class, combo, "id", "Ev0sCombo");
            ReflectionCache.setField(BlockType.class, bt,    "state", combo);
            return true;
        } catch (Throwable t) {
            LOGGER.atWarning().log("[ComboStateHelper] inject failed for "
                    + bt.getId() + ": " + t.getMessage());
            return false;
        }
    }

    /**
     * Convenience overload that looks up the BlockType by key first.
     *
     * @return {@code true} if the key exists and was successfully updated
     */
    public static boolean inject(String key,
                                  Chisel.Data chisel,
                                  Paintbrush.Data paintbrush,
                                  CarpenterHammer.Data hammer) {
        BlockType bt = BlockTypeCache.get(key);
        if (bt == null) return false;
        return inject(bt, chisel, paintbrush, hammer);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Wraps or converts an existing {@link StateData} into a {@link ComboState},
     * copying over any single-tool data that is already present.
     */
    private static ComboState toCombo(StateData existing) {
        if (existing instanceof ComboState cs) return cs;

        ComboState combo = new ComboState();
        if (existing instanceof Chisel.Data d)          combo.chisel    = d;
        else if (existing instanceof Paintbrush.Data d) combo.paintbrush = d;
        else if (existing instanceof CarpenterHammer.Data d) combo.hammer = d;
        return combo;
    }
}
