package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

@SuppressWarnings("removal")
public class Paintbrush extends BlockState {

    public String source;
    public String[] colorVariants;
    public Data data;

    public static final BuilderCodec<Paintbrush> CODEC = BuilderCodec.builder(Paintbrush.class, Paintbrush::new, BlockState.BASE_CODEC)
            .append(new KeyedCodec<>("Source", Codec.STRING, true), (i, v) -> i.source = v, i -> i.source).add()
            .append(new KeyedCodec<>("ColorVariants", Codec.STRING_ARRAY, true), (i, v) -> i.colorVariants = v, i -> i.colorVariants).add()
            .build();

    public boolean initialize(BlockType blockType) {
        if (super.initialize(blockType) && blockType.getState() instanceof Data data) {
            this.data = data;
            return true;
        } else {
            return false;
        }
    }

    public static final class Data extends StateData {
        public static final BuilderCodec<Paintbrush.Data> PAINTBRUSHCODEC;
        public String source;
        public String[] colorVariants;

        static {
            PAINTBRUSHCODEC = BuilderCodec.builder(Paintbrush.Data.class, Paintbrush.Data::new, StateData.DEFAULT_CODEC)
                    .append(new KeyedCodec<>("Source", Codec.STRING, true), (i, v) -> i.source = v, i -> i.source).add()
                    .append(new KeyedCodec<>("ColorVariants", Codec.STRING_ARRAY, true), (i, v) -> i.colorVariants = v, i -> i.colorVariants).add()
                    .build();
        }
    }
}
