package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;

/**
 * State-data carrier for the Carpenter's Hammer.
 * <p>
 * Works identically to {@link Chisel} / {@link Paintbrush}: a
 * {@link CarpenterHammer.Data} is injected onto furniture {@code BlockType}s
 * at plugin startup so the hammer interaction can discover all related items.
 * <p>
 * Five furniture categories are supported, each with its own tab in the UI:
 * <ul>
 *   <li><b>chairs</b>  – seats, benches, sofas</li>
 *   <li><b>tables</b>  – dining tables, coffee tables, desks</li>
 *   <li><b>storage</b> – counters, cupboards, shelves, sinks</li>
 *   <li><b>windows</b> – window panes, shutters</li>
 *   <li><b>lights</b>  – lanterns, torches, candles, lamps</li>
 * </ul>
 */
@SuppressWarnings("removal")
public class CarpenterHammer {

    // Per-block-instance fields (reserved for future in-world instance encoding)
    public String  source;
    public String[] chairs;
    public String[] tables;
    public String[] storage;
    public String[] windows;
    public String[] lights;

    public Data data;

    public static final BuilderCodec<CarpenterHammer> CODEC = buildCodec();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BuilderCodec<CarpenterHammer> buildCodec() {
        try {
            return BuilderCodec.builder(CarpenterHammer.class, CarpenterHammer::new)
                    .append(new KeyedCodec<>("Source",  Codec.STRING,       true), (i, v) -> i.source  = v, i -> i.source).add()
                    .append(new KeyedCodec<>("Chairs",  Codec.STRING_ARRAY, true), (i, v) -> i.chairs  = v, i -> i.chairs).add()
                    .append(new KeyedCodec<>("Tables",  Codec.STRING_ARRAY, true), (i, v) -> i.tables  = v, i -> i.tables).add()
                    .append(new KeyedCodec<>("Storage", Codec.STRING_ARRAY, true), (i, v) -> i.storage = v, i -> i.storage).add()
                    .append(new KeyedCodec<>("Windows", Codec.STRING_ARRAY, true), (i, v) -> i.windows = v, i -> i.windows).add()
                    .append(new KeyedCodec<>("Lights",  Codec.STRING_ARRAY, true), (i, v) -> i.lights  = v, i -> i.lights).add()
                    .build();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * BlockType-level shared state, injected via reflection (same pattern as
     * {@link Chisel.Data} and {@link Paintbrush.Data}).
     */
    public static class Data extends StateData {
        public String  source;
        public String[] chairs;
        public String[] tables;
        public String[] storage;
        public String[] windows;
        public String[] lights;
    }
}
