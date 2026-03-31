package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * A combined {@link StateData} that allows a single block to carry
 * {@link Chisel.Data}, {@link Paintbrush.Data}, and
 * {@link CarpenterHammer.Data} simultaneously.
 *
 * <p>Any of the three fields may be {@code null} when that tool has no
 * applicable behaviour for the block.  Each interaction class checks for
 * {@code ComboState} first and extracts its relevant sub-state, so no
 * existing injection logic needs to know which combinations will occur.
 *
 * <p>Compat classes should use
 * {@link com.Ev0sMods.Ev0sChisel.compat.ComboStateHelper} to build and inject
 * combo states safely (it handles upgrading a plain single-state to a combo
 * when a second tool is added to an already-annotated block).
 */
@SuppressWarnings("removal")
public class ComboState extends StateData {

    /** Chisel-tool data (pattern/design cycling). May be {@code null}. */
    public Chisel.Data chisel;

    /** Paintbrush-tool data (colour cycling). May be {@code null}. */
    public Paintbrush.Data paintbrush;

    /** Carpenter's Hammer data (furniture window/door cycling). May be {@code null}. */
    public CarpenterHammer.Data hammer;
}
