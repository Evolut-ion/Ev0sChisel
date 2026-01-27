//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel.Interactions;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Random;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
@SuppressWarnings("removal")
public class ChiselInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<ChiselInteraction> CODEC;
    public Random r = new Random();

    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl Vector3i vector3i, @NonNullDecl CooldownHandler cooldownHandler) {
        BlockPosition contextTargetBlock = interactionContext.getTargetBlock();

        assert contextTargetBlock != null;

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(contextTargetBlock.x, contextTargetBlock.z));

        assert chunk != null;

        BlockState var11 = chunk.getState(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
        if (var11 instanceof Chisel) {
            Chisel c = (Chisel)var11;
            Chisel cx = (Chisel)chunk.getState(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);

            for(int v = 0; v < c.data.substitutions.length; ++v) {
                ((HytaleLogger.Api)HytaleLogger.getLogger().atInfo()).log(c.data.substitutions[0]);
                ItemStack is = new ItemStack(c.data.substitutions[this.r.nextInt(c.data.substitutions.length)]);
                Item i = is.getItem();
                chunk.setBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z, i.getBlockId());
            }
        }

    }

    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) {
    }

    static {
        CODEC = BuilderCodec.builder(ChiselInteraction.class, ChiselInteraction::new, SimpleBlockInteraction.CODEC).build();
    }
}
