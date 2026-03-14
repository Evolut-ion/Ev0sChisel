package com.Ev0sMods.Ev0sChisel.Interactions;

import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.Ev0sMods.Ev0sChisel.ui.PaintbrushUIPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

@SuppressWarnings("removal")
public class PaintbrushInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<PaintbrushInteraction> CODEC;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> commandBuffer, @NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl Vector3i vector3i, @NonNullDecl CooldownHandler cooldownHandler) {
        if (interactionContext == null) {
            LOGGER.atInfo().log("[Paintbrush] Ignored interaction: missing context");
            return;
        }
        BlockPosition contextTargetBlock = interactionContext.getTargetBlock();
        if (contextTargetBlock == null || (contextTargetBlock.x == 0 && contextTargetBlock.y == 0 && contextTargetBlock.z == 0)) {
            LOGGER.atInfo().log("[Paintbrush] Ignored interaction at (0,0,0) or null target block");
            return;
        }

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(contextTargetBlock.x, contextTargetBlock.z));
        if (chunk == null) return;

        BlockState blockState = chunk.getState(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);

        // Crouch + interact: rotate if block supports rotations (same behavior as Chisel)
        if (isCrouching(commandBuffer, interactionContext)) {
            if (blockState instanceof Paintbrush) {
                try {
                    BlockType blockType = chunk.getBlockType(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                    if (blockType != null) {
                        VariantRotation vr = blockType.getVariantRotation();
                        if (vr != null && vr != VariantRotation.None) {
                            RotationTuple[] validRotations = vr.getRotations();
                            if (validRotations != null && validRotations.length > 1) {
                                int currentIdx = chunk.getRotationIndex(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                int pos = 0;
                                for (int i = 0; i < validRotations.length; i++) {
                                    if (validRotations[i].index() == currentIdx) { pos = i; break; }
                                }
                                int nextPos = (pos + 1) % validRotations.length;
                                RotationTuple next = validRotations[nextPos];
                                int blockId = chunk.getBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                int filler  = chunk.getFiller(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                chunk.setBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z, blockId, blockType, next.index(), filler, 0);
                                LOGGER.atInfo().log("[Paintbrush] Rotated block idx " + currentIdx + " → " + next.index());
                            }
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.atWarning().log("[Paintbrush] Failed to rotate block: " + t.getMessage());
                }
            }
            return; // crouch always returns
        }

        // Get player refs
        Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> playerEnt;
        Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store;
        PlayerRef playerRef;
        com.hypixel.hytale.server.core.entity.entities.Player player;
        try {
            playerEnt = interactionContext.getOwningEntity();
            store     = playerEnt.getStore();
            playerRef = store.getComponent(playerEnt, PlayerRef.getComponentType());
            player    = store.getComponent(playerEnt, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerRef == null || player == null) return;
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Paintbrush] Failed to get player refs: " + t.getMessage());
            return;
        }

        if (!(blockState instanceof Paintbrush)) {
            // For simplicity, open paintbrush UI in table mode (no input handling implemented)
            PaintbrushUIPage.openTable(playerRef, store, world, new Vector3i(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z), player);
            return;
        }

        if (blockState instanceof Paintbrush pb) {
            String[] variants = pick(pb.colorVariants, pb.data != null ? pb.data.colorVariants : null);
            LOGGER.atInfo().log("[Paintbrush] Resolved variants for block: " + java.util.Arrays.toString(variants));

            if (variants != null && variants.length > 0) {
                PaintbrushUIPage.openPaintbrush(playerRef, store, world, new Vector3i(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z), player, variants);
            } else {
                // Attempt to discover variants from user mods folder (best-effort)
                java.util.List<String> discovered = discoverUserModVariants();
                LOGGER.atInfo().log("[Paintbrush] Discovered fallback variants: " + discovered);
                if (discovered != null && !discovered.isEmpty()) {
                    String[] arr = discovered.toArray(new String[0]);
                    PaintbrushUIPage.openPaintbrush(playerRef, store, world, new Vector3i(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z), player, arr);
                } else {
                    LOGGER.atWarning().log("[Paintbrush] No color variants found for block");
                }
            }
        }
    }

    private static String[] pick(String[] primary, String[] fallback) {
        if (primary != null && primary.length > 0) return primary;
        return fallback;
    }

    private static boolean isCrouching(CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> commandBuffer, InteractionContext ctx) {
        try {
            Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> entityRef = ctx.getOwningEntity();
            if (entityRef == null) return false;
            MovementStatesComponent msc = commandBuffer.getComponent(entityRef, MovementStatesComponent.getComponentType());
            if (msc == null) return false;
            MovementStates ms = msc.getMovementStates();
            return ms != null && ms.crouching;
        } catch (Throwable t) { return false; }
    }

    /** Best-effort: scan user's Hytale mods folder for file names to use as variants. */
    private static java.util.List<String> discoverUserModVariants() {
        try {
            String home = System.getProperty("user.home");
            if (home == null) return java.util.Collections.emptyList();
            java.io.File modsDir = new java.io.File(home, "AppData\\Roaming\\Hytale\\UserData\\Mods");
            if (!modsDir.exists() || !modsDir.isDirectory()) return java.util.Collections.emptyList();
            java.util.List<String> out = new java.util.ArrayList<>();
            for (java.io.File mod : modsDir.listFiles(java.io.File::isDirectory)) {
                // collect files beneath mod root (names only)
                java.io.File[] children = mod.listFiles();
                if (children == null) continue;
                for (java.io.File f : children) {
                    String name = f.getName();
                    int dot = name.lastIndexOf('.');
                    if (dot > 0) name = name.substring(0, dot);
                    // produce a mod-prefixed key to avoid collisions
                    out.add(mod.getName().toLowerCase(java.util.Locale.ROOT) + ":" + name);
                }
            }
            return out;
        } catch (Throwable t) {
            return java.util.Collections.emptyList();
        }
    }

    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) { }

    static { CODEC = BuilderCodec.builder(PaintbrushInteraction.class, PaintbrushInteraction::new, SimpleBlockInteraction.CODEC).build(); }
}
