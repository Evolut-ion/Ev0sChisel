package com.Ev0sMods.Ev0sChisel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import org.bson.BsonValue;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
@SuppressWarnings("removal")
public class ChiselCodec extends BlockState {
    public static final Codec<String[]> CHISEL = new ArrayCodec(Codec.STRING_ARRAY, Chisel[]::new) {
        @NullableDecl
        @Override
        public String[] decode(BsonValue bsonValue, ExtraInfo extraInfo) {
            return new String[0];
        }


        @NonNullDecl
        @Override
        public Schema toSchema(@NonNullDecl SchemaContext schemaContext) {
            return null;
        }
    };
}
