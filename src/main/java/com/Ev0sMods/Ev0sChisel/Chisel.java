//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
//import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

@SuppressWarnings("removal")
public class Chisel {

    // Per-block-instance fields (used when the block in-world is a Chisel)
    public String source;
    public String[] substitutions;
    public String[] stairs;
    public String[] halfSlabs;
    public String[] roofing;

    // Optional BlockType-level shared data for compat-injected blocks
    public Data data;

    public static final BuilderCodec<Chisel> CODEC = buildCodec();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BuilderCodec<Chisel> buildCodec() {
        try {
            return BuilderCodec.builder(Chisel.class, Chisel::new)
                    .append(new KeyedCodec<>("Source", Codec.STRING, true), (i, v) -> i.source = v, i -> i.source).add()
                    .append(new KeyedCodec<>("Substitutions", Codec.STRING_ARRAY, true), (i, v) -> i.substitutions = v, i -> i.substitutions).add()
                    .append(new KeyedCodec<>("Stairs", Codec.STRING_ARRAY, true), (i, v) -> i.stairs = v, i -> i.stairs).add()
                    .append(new KeyedCodec<>("HalfSlabs", Codec.STRING_ARRAY, true), (i, v) -> i.halfSlabs = v, i -> i.halfSlabs).add()
                    .append(new KeyedCodec<>("Roofing", Codec.STRING_ARRAY, true), (i, v) -> i.roofing = v, i -> i.roofing).add()
                    .build();
        } catch (Throwable ignored) {
            return null;
        }
    }

    // Nested StateData class used when attaching chisel metadata to BlockType.state
    public static class Data extends StateData {
        public String source;
        public String[] substitutions;
        public String[] stairs;
        public String[] halfSlabs;
        public String[] roofing;
    }
}
