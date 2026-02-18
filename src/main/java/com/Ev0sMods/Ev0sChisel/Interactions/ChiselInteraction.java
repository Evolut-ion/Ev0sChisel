//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Ev0sMods.Ev0sChisel.Interactions;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.compat.CarpentryCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawCompat;
import com.Ev0sMods.Ev0sChisel.compat.MasonryCompat;
import com.Ev0sMods.Ev0sChisel.compat.StoneworksCompat;
import com.Ev0sMods.Ev0sChisel.ui.ChiselUIPage;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

@SuppressWarnings("removal")
public class ChiselInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<ChiselInteraction> CODEC;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl Vector3i vector3i, @NonNullDecl CooldownHandler cooldownHandler) {
        BlockPosition contextTargetBlock = interactionContext.getTargetBlock();
        assert contextTargetBlock != null;

        ((HytaleLogger.Api) HytaleLogger.getLogger().atInfo()).log("[Chisel] interactWithBlock called at " + contextTargetBlock.x + "," + contextTargetBlock.y + "," + contextTargetBlock.z);

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(contextTargetBlock.x, contextTargetBlock.z));
        assert chunk != null;

        // ── Crouch + interact → cycle through full rotation index ────────
        if (isCrouching(commandBuffer, interactionContext)) {
            try {
                BlockType blockType = chunk.getBlockType(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                if (blockType != null) {
                    VariantRotation vr = blockType.getVariantRotation();
                    if (vr != null && vr != VariantRotation.None) {
                        RotationTuple[] validRotations = vr.getRotations();
                        if (validRotations != null && validRotations.length > 1) {
                            int currentIdx = chunk.getRotationIndex(
                                    contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);

                            // Find current position in the valid rotations array
                            int pos = 0;
                            for (int i = 0; i < validRotations.length; i++) {
                                if (validRotations[i].index() == currentIdx) {
                                    pos = i;
                                    break;
                                }
                            }

                            // Advance to the next valid rotation (wrapping around)
                            int nextPos = (pos + 1) % validRotations.length;
                            RotationTuple next = validRotations[nextPos];

                            int blockId = chunk.getBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                            int filler  = chunk.getFiller(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                            chunk.setBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z,
                                    blockId, blockType, next.index(), filler, 0);

                            String key = (String) blockType.getId();
                            LOGGER.atInfo().log("[Chisel] Rotated block " + key
                                    + " idx " + currentIdx + " → " + next.index()
                                    + " (yaw=" + next.yaw() + " pitch=" + next.pitch() + " roll=" + next.roll() + ")"
                                    + " [" + (nextPos + 1) + "/" + validRotations.length + "]");
                            return;
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.atWarning().log("[Chisel] Failed to rotate block: " + t.getMessage());
                t.printStackTrace();
            }
            return; // crouching but no rotation possible – do nothing
        }

        BlockState blockState = chunk.getState(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);

        // Get the block's own key for accurate type detection
        BlockType targetBlockType = chunk.getBlockType(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
        String blockKey = targetBlockType != null ? (String) targetBlockType.getId() : null;

        if (blockState instanceof Chisel chisel) {
            // ── Gather substitution arrays ──────────────────────────────
            String[] subs     = pick(chisel.substitutions, chisel.data != null ? chisel.data.substitutions : null);
            String[] stairs   = pick(chisel.stairs,        chisel.data != null ? chisel.data.stairs : null);
            String[] halfs    = pick(chisel.halfSlabs,     chisel.data != null ? chisel.data.halfSlabs : null);
            String[] roofs    = pick(chisel.roofing,       chisel.data != null ? chisel.data.roofing : null);

            // ── Detect rock type once (blockKey priority, shared by all) ─
            String detectedRockType = MasonryCompat.detectStoneType(blockKey, subs);
            if (detectedRockType == null) {
                detectedRockType = MacawCompat.detectRockType(blockKey, subs);
            }

            // ── Filter vanilla Rock_* entries to detected type only ──────
            //     Prevents cross-family entries (e.g. Rock_Stone in a
            //     Rock_Basalt block) from polluting the chisel UI.
            if (detectedRockType != null) {
                subs   = filterByRockType(subs,   detectedRockType);
                stairs = filterByRockType(stairs,  detectedRockType);
                halfs  = filterByRockType(halfs,   detectedRockType);
                roofs  = filterByRockType(roofs,   detectedRockType);
            }

            // ── Masonry merge (if Ymmersive is present) ─────────────────
            if (MasonryCompat.isAvailable() && detectedRockType != null) {
                subs   = merge(subs,   MasonryCompat.getVariants(detectedRockType));
                stairs = merge(stairs,  MasonryCompat.getStairVariants(detectedRockType));
                halfs  = merge(halfs,   MasonryCompat.getHalfVariants(detectedRockType));
            }

            // ── Stoneworks Expanded merge ────────────────────────────────
            if (StoneworksCompat.isAvailable() && "stone".equals(detectedRockType)) {
                subs = merge(subs, StoneworksCompat.getVariants());
            }

            // ── Macaw merge (Paths + Stairs) ────────────────────────────
            if ((MacawCompat.isPathsAvailable() || MacawCompat.isStairsAvailable()) && detectedRockType != null) {
                if (MacawCompat.isPathsAvailable()) {
                    subs   = merge(subs,   MacawCompat.getPathsBlocks(detectedRockType));
                    stairs = merge(stairs, MacawCompat.getPathsStairs(detectedRockType));
                    halfs  = merge(halfs,  MacawCompat.getPathsHalfs(detectedRockType));
                }
                if (MacawCompat.isStairsAvailable()) {
                    stairs = merge(stairs, MacawCompat.getMcwStairs(detectedRockType));
                }
            }

            // ── Carpentry merge (if Ymmersive Carpentry is present) ─────
            if (CarpentryCompat.isAvailable() && subs != null) {
                String woodType = CarpentryCompat.detectWoodType(blockKey, subs);
                if (woodType != null) {
                    subs   = merge(subs,   CarpentryCompat.getVariants(woodType));
                    stairs = merge(stairs,  CarpentryCompat.getStairVariants(woodType));
                    halfs  = merge(halfs,   CarpentryCompat.getHalfVariants(woodType));
                }
            }

            // ── Auto-derive stairs/halfs/roofing from block subs if empty
            if (empty(stairs) && !empty(subs)) {
                stairs = MasonryCompat.deriveExistingVariants(subs, "_Stairs");
            }
            if (empty(halfs) && !empty(subs)) {
                halfs = MasonryCompat.deriveExistingVariants(subs, "_Half");
            }
            if (empty(roofs) && !empty(subs)) {
                roofs = MasonryCompat.deriveExistingRoofing(subs);
            }

            // ── Open UI ─────────────────────────────────────────────────
            if (!empty(subs) || !empty(stairs) || !empty(halfs) || !empty(roofs)) {
                try {
                    Ref<EntityStore> playerEnt = interactionContext.getOwningEntity();
                    Store<EntityStore> store = playerEnt.getStore();
                    PlayerRef playerRef = store.getComponent(playerEnt, PlayerRef.getComponentType());

                    if (playerRef != null) {
                        Vector3i blockPos = new Vector3i(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                        ChiselUIPage.open(playerRef, store, blockPos, world,
                                safe(subs), safe(stairs), safe(halfs), safe(roofs));
                    }
                } catch (Throwable t) {
                    ((HytaleLogger.Api) HytaleLogger.getLogger().atWarning()).log("[Chisel] Failed to open UI: " + t.getMessage());
                    t.printStackTrace();
                }
            } else {
                ((HytaleLogger.Api) HytaleLogger.getLogger().atWarning()).log("[Chisel] No substitutions found on block");
            }
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    /** Pick the first non-empty array. */
    private static String[] pick(String[] primary, String[] fallback) {
        if (primary != null && primary.length > 0) return primary;
        return fallback;
    }

    /** Merge base array with a list, deduplicating, returning new array. */
    private static String[] merge(String[] base, java.util.List<String> extra) {
        if (extra == null || extra.isEmpty()) return base;
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (base != null) java.util.Collections.addAll(set, base);
        set.addAll(extra);
        return set.toArray(new String[0]);
    }

    private static boolean empty(String[] arr) { return arr == null || arr.length == 0; }
    private static String[] safe(String[] arr) { return arr != null ? arr : new String[0]; }

    /**
     * Filters an array of block keys so that only {@code Rock_} entries
     * matching the given rock type are kept.  Non-{@code Rock_} entries
     * (mod blocks, masonry, Macaw, etc.) are always retained.
     */
    private static String[] filterByRockType(String[] arr, String rockType) {
        if (arr == null || rockType == null) return arr;
        String matchPrefix = "rock_" + rockType.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (String s : arr) {
            if (s == null) continue;
            String lower = s.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("rock_")) {
                if (lower.equals(matchPrefix) || lower.startsWith(matchPrefix + "_")) {
                    filtered.add(s);
                }
                // else: different rock type → skip
            } else {
                filtered.add(s);
            }
        }
        return filtered.toArray(new String[0]);
    }

    /** Check if the owning player is crouching (ctrl / sneak). */
    private static boolean isCrouching(CommandBuffer<EntityStore> commandBuffer, InteractionContext ctx) {
        try {
            Ref<EntityStore> entityRef = ctx.getOwningEntity();
            if (entityRef == null) return false;
            MovementStatesComponent msc = commandBuffer.getComponent(entityRef, MovementStatesComponent.getComponentType());
            if (msc == null) return false;
            MovementStates ms = msc.getMovementStates();
            return ms != null && ms.crouching;
        } catch (Throwable t) {
            return false;
        }
    }

    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) {
    }

    static {
        CODEC = BuilderCodec.builder(ChiselInteraction.class, ChiselInteraction::new, SimpleBlockInteraction.CODEC).build();
    }
}
