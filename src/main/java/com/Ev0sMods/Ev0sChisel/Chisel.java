//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
@SuppressWarnings("removal")
public class Chisel extends BlockState {

    public String source;
    public String[] substitutions;
    public String[] stairs;
    public String[] halfSlabs;
    public String[] roofing;
    public Data data;

    public static final BuilderCodec<Chisel> CODEC = BuilderCodec.builder(Chisel.class, Chisel::new, BlockState.BASE_CODEC)
            .append(new KeyedCodec<>("Source", Codec.STRING, true), (i, v) -> i.source = v, i -> i.source).add()
            .append(new KeyedCodec<>("Substitutions", Codec.STRING_ARRAY, true), (i, v) -> i.substitutions = v, i -> i.substitutions).add()
            .append(new KeyedCodec<>("Stairs", Codec.STRING_ARRAY, true), (i, v) -> i.stairs = v, i -> i.stairs).add()
            .append(new KeyedCodec<>("HalfSlabs", Codec.STRING_ARRAY, true), (i, v) -> i.halfSlabs = v, i -> i.halfSlabs).add()
            .append(new KeyedCodec<>("Roofing", Codec.STRING_ARRAY, true), (i, v) -> i.roofing = v, i -> i.roofing).add()
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
        public static final BuilderCodec<Chisel.Data> CHISELCODEC;
        public String source;
        public String[] substitutions;
        public String[] stairs;
        public String[] halfSlabs;
        public String[] roofing;

        static {
            CHISELCODEC = BuilderCodec.builder(Chisel.Data.class, Chisel.Data::new, StateData.DEFAULT_CODEC)
                    .append(new KeyedCodec<>("Source", Codec.STRING, true), (i, v) -> i.source = v, i -> i.source).add()
                    .append(new KeyedCodec<>("Substitutions", Codec.STRING_ARRAY, true), (i, v) -> i.substitutions = v, i -> i.substitutions).add()
                    .append(new KeyedCodec<>("Stairs", Codec.STRING_ARRAY, true), (i, v) -> i.stairs = v, i -> i.stairs).add()
                    .append(new KeyedCodec<>("HalfSlabs", Codec.STRING_ARRAY, true), (i, v) -> i.halfSlabs = v, i -> i.halfSlabs).add()
                    .append(new KeyedCodec<>("Roofing", Codec.STRING_ARRAY, true), (i, v) -> i.roofing = v, i -> i.roofing).add()
                    .build();
        }
    }
}
