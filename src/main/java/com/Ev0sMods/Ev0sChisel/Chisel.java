package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
@SuppressWarnings("removal")
public class Chisel extends BlockState {
    public String[] substitutions;
    public String sourceBlockId;
    public Data data;
    public static final BuilderCodec<Chisel> CODEC = BuilderCodec.builder(Chisel.class,Chisel::new).append(new KeyedCodec<>("Source", Codec.STRING, true),(o, v) -> o.sourceBlockId = v, o -> o.sourceBlockId ).add().append(new KeyedCodec<>("Substitutions", Codec.STRING_ARRAY, true),(o, v) -> o.substitutions = v, o -> o.substitutions).add().build();
    public Chisel(){


    }
    public boolean initialize(BlockType blockType) {
        if (super.initialize(blockType) && blockType.getState() instanceof Data data) {
            this.data = data;
            return true;
        } else{return false;}
    }
    public static final class Data extends StateData {
        public static final BuilderCodec<Data> CHISELCODEC = BuilderCodec.<Data>builder(Data.class, Data::new, StateData.DEFAULT_CODEC).append(new KeyedCodec<>("SourceBlock", Codec.STRING), (o, v) -> o.source = v, o -> o.source).add().append(new KeyedCodec<>("Substitutions", Codec.STRING_ARRAY, true),(o, v) -> o.substitutions = v, o -> o.substitutions).add().build();
        public String source;
        public String[] substitutions;

    }
}
