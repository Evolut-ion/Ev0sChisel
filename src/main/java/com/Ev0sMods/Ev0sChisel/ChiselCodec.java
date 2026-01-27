package com.Ev0sMods.Ev0sChisel;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

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
    public static final Codec<String[]> CHISEL;

    static {
        CHISEL = new ArrayCodec(Codec.STRING_ARRAY, (x$0) -> new Chisel[x$0]) {
            @NullableDecl
            public String[] decode(BsonValue bsonValue, ExtraInfo extraInfo) {
                return new String[0];
            }

            @NonNullDecl
            public Schema toSchema(@NonNullDecl SchemaContext schemaContext) {
                return null;
            }
        };
    }
}
