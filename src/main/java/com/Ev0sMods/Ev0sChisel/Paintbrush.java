package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
//import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

@SuppressWarnings("removal")
public class Paintbrush {

    // Per-block-instance fields
    public String source;
    public String[] colorVariants;

    public Data data;

    public static final BuilderCodec<Paintbrush> CODEC = buildCodec();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BuilderCodec<Paintbrush> buildCodec() {
        try {
            return BuilderCodec.builder(Paintbrush.class, Paintbrush::new)
                    .append(new KeyedCodec<>("Source", Codec.STRING, true), (i, v) -> i.source = v, i -> i.source).add()
                    .append(new KeyedCodec<>("ColorVariants", Codec.STRING_ARRAY, true), (i, v) -> i.colorVariants = v, i -> i.colorVariants).add()
                    .build();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static class Data extends StateData {
        public String source;
        public String[] colorVariants;
    }
}
