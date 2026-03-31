

package com.Ev0sMods.Ev0sChisel.Interactions;

import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.ComboState;
import com.Ev0sMods.Ev0sChisel.compat.CarpentryCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawCompat;
import com.Ev0sMods.Ev0sChisel.compat.MasonryCompat;
import com.Ev0sMods.Ev0sChisel.compat.StoneworksCompat;
import com.Ev0sMods.Ev0sChisel.compat.StatuesCompat;
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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
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
//import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

@SuppressWarnings("removal")
public class ChiselInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<ChiselInteraction> CODEC;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    protected void interactWithBlock(@NonNullDecl World world, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl Vector3i vector3i, @NonNullDecl CooldownHandler cooldownHandler) {
        if (interactionContext == null) {
            return;
        }
        BlockPosition contextTargetBlock = interactionContext.getTargetBlock();
        if (contextTargetBlock == null || (contextTargetBlock.x == 0 && contextTargetBlock.y == 0 && contextTargetBlock.z == 0)) {
            return;
        }

        // removed interaction trace log

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(contextTargetBlock.x, contextTargetBlock.z));
        if (chunk == null) return;

        // BlockState was removed in prerelease; rely on BlockType.getState() (StateData / components)
        //com.hypixel.hytale.server.core.universe.world.meta.BlockState blockState = null;
        com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType targetBlockType = chunk.getBlockType(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);

        // Detect if this is a right-click-like interaction (heuristic)
        boolean isRightClick = false;
        try {
            String it = interactionType != null ? interactionType.toString() : "";
            if (it != null) {
                String lit = it.toLowerCase(java.util.Locale.ROOT);
                if (lit.contains("right") || lit.contains("secondary") || lit.contains("activate")) isRightClick = true;
            }
        } catch (Throwable ignored) {}

        // ── Crouch + interact ───────────────────────────────────────────
        //   • On a chisel block → cycle rotation
        //   • On a non-chisel block → open the chisel table UI
        if (isCrouching(commandBuffer, interactionContext)) {
            // Only rotate chisel-like blocks: actual Chisel block, blocks with injected Chisel.Data,
            // or common derived keys (stairs/halfs/roofs).
            boolean rotated = false;
            try {
                BlockType blockType = chunk.getBlockType(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                String blockKey = blockType != null ? (String) blockType.getId() : null;
                // Determine chisel-like via injected BlockType state (compat) or derived key
                boolean isChiselLike = false;
                // Use outer isRightClick value (computed above)
                boolean isStatue = false;
                try {
                    String probeKey = blockType != null ? (String) blockType.getId() : null;
                    if (probeKey != null) {
                        String lk = probeKey.toLowerCase(java.util.Locale.ROOT);
                        if (lk.contains("ymmersive_statues") || lk.contains("statue")) isStatue = true;
                        if (!isStatue && com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.isAvailable()) {
                            try {
                                String mapped = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getMappedChiselTypeForStatue(probeKey);
                                if (mapped != null) isStatue = true;
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}

                if (!isChiselLike && blockType != null) {
                    StateData bs = null;
                    try { bs = blockType.getState(); } catch (Throwable ignored) {}
                    if (extractChiselData(bs) != null) {
                        // For compat-injected statues require crouch+right-click to rotate
                        if (isRightClick) isChiselLike = true;
                    } else if (blockKey != null) {
                        String lower = blockKey.toLowerCase(java.util.Locale.ROOT);
                        if (lower.endsWith("_stairs") || lower.endsWith("_stair") || lower.endsWith("_half") || lower.endsWith("_slab") || lower.contains("_roof") || lower.contains("_roofs")) {
                            isChiselLike = true;
                        }
                    }
                }

                if (!isChiselLike) {
                    // skipped: not a chisel-like block
                } else if (blockType == null) {
                    LOGGER.atWarning().log("[Chisel] Cannot rotate: blockType is null");
                } else {
                    VariantRotation vr = blockType.getVariantRotation();
                    if (vr == null || vr == VariantRotation.None) {
                        // block has no VariantRotation
                    } else {
                        RotationTuple[] validRotations = vr.getRotations();
                        if (validRotations == null || validRotations.length <= 1) {
                            // no multiple rotations available
                        } else {
                            int currentIdx = chunk.getRotationIndex(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                            int pos = 0;
                            for (int i = 0; i < validRotations.length; i++) { if (validRotations[i].index() == currentIdx) { pos = i; break; } }
                            int nextPos = (pos + 1) % validRotations.length;
                            RotationTuple next = validRotations[nextPos];
                            if (isStatue && isRightClick) {
                                // Rotate both bottom and top of a statue pillar
                                int bottomId = chunk.getBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                int bottomFiller = chunk.getFiller(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                int topId = chunk.getBlock(contextTargetBlock.x, contextTargetBlock.y + 1, contextTargetBlock.z);
                                int topFiller = chunk.getFiller(contextTargetBlock.x, contextTargetBlock.y + 1, contextTargetBlock.z);
                                try {
                                    chunk.setBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z, bottomId, blockType, next.index(), bottomFiller, 0);
                                    chunk.setBlock(contextTargetBlock.x, contextTargetBlock.y + 1, contextTargetBlock.z, topId, blockType, next.index(), topFiller, 0);
                                    rotated = true;
                                } catch (Throwable t) {
                                    // ignore and fall back to single-block rotation
                                }
                            } else {
                                int blockId = chunk.getBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                int filler  = chunk.getFiller(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
                                chunk.setBlock(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z, blockId, blockType, next.index(), filler, 0);
                                // rotated block
                                rotated = true;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.atWarning().log("[Chisel] Failed to rotate block: " + t.getMessage());
            }
            if (rotated) return; // only suppress UI when we actually rotated
        }

        // ── Get player references (shared by both UI paths) ────────────
        Ref<EntityStore> playerEnt;
        Store<EntityStore> store;
        PlayerRef playerRef;
        com.hypixel.hytale.server.core.entity.entities.Player player;
        try {
            playerEnt = interactionContext.getOwningEntity();
            store     = playerEnt.getStore();
            playerRef = store.getComponent(playerEnt, PlayerRef.getComponentType());
            player    = store.getComponent(playerEnt,
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerRef == null || player == null) return;
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] Failed to get player refs: " + t.getMessage());
            return;
        }

        Vector3i blockPos = new Vector3i(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);

        // ── Non-chisel block → open in Table mode ───────────────────────
        // Accept blocks that either have a per-block Chisel component or whose BlockType
        // was injected with a Chisel.Data state (compat-injected statue types).
        boolean hasChiselLikeState = false;
        try {
            Object comp = com.Ev0sMods.Ev0sChisel.compat.ComponentCompat.getBlockComponent(chunk, contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z, com.Ev0sMods.Ev0sChisel.Chisel.class);
            if (comp != null) hasChiselLikeState = true;
        } catch (Throwable ignored) {}
        try {
                if (targetBlockType != null) {
                String tbId = targetBlockType.getId() != null ? targetBlockType.getId().toString() : "<null>";
                StateData tstate = null;
                try { tstate = targetBlockType.getState(); } catch (Throwable t) { /* ignore */ }
                boolean inst = (extractChiselData(tstate) != null);
                if (!hasChiselLikeState && inst) hasChiselLikeState = true;
            } else {
                // targetBlockType is null
            }
        } catch (Throwable ignored) {}

        if (!hasChiselLikeState) {
            // If player right-clicked a statue, open Chisel UI showing mapped material
            String maybeKey = targetBlockType != null ? (String) targetBlockType.getId() : null;
            try {
                if (isRightClick && maybeKey != null && com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.isAvailable()) {
                    String mapped = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getMappedChiselTypeForStatue(maybeKey);
                    if (mapped != null) {
                        ChiselUIPage.openChisel(playerRef, store, world, blockPos, player,
                                new String[]{mapped}, new String[0], new String[0], new String[0]);
                        return;
                    }
                }
            } catch (Throwable ignored) {}

            // If not handled, attempt runtime injection then fallback to Table UI
            boolean attemptedRuntimeInject = false;
            try {
                if (maybeKey != null) {
                    String lk = maybeKey.toLowerCase(java.util.Locale.ROOT);
                    if (lk.contains("ymmersive_statues") || lk.startsWith("ymmersive_statues_")) {
                        attemptedRuntimeInject = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.ensureInjectedFor(maybeKey);
                    }
                }
            } catch (Throwable ignored) {}

            if (attemptedRuntimeInject) {
                // re-check state
                try {
                    StateData tstate2 = targetBlockType != null ? targetBlockType.getState() : null;
                    Chisel.Data injectedData2 = extractChiselData(tstate2);
                    if (injectedData2 != null) {
                        ChiselUIPage.openChisel(playerRef, store, world, blockPos, player,
                                safe(injectedData2.substitutions), safe(injectedData2.stairs), safe(injectedData2.halfSlabs), safe(injectedData2.roofing));
                        return;
                    }
                } catch (Throwable ignored) {}
            }

            ChiselUIPage.openTable(playerRef, store, world, blockPos, player);
            return;
        }

        // Get the block's own key for accurate type detection
        targetBlockType = chunk.getBlockType(contextTargetBlock.x, contextTargetBlock.y, contextTargetBlock.z);
        String blockKey = targetBlockType != null ? (String) targetBlockType.getId() : null;

        // If the BlockType was injected with a `Chisel.Data` state (compat-injected
        // statue BlockTypes), open the Chisel UI using that injected data.
        try {
            StateData tstate = targetBlockType != null ? targetBlockType.getState() : null;
            Chisel.Data injectedData = extractChiselData(tstate);
            if (injectedData != null) {
                ChiselUIPage.openChisel(playerRef, store, world, blockPos, player,
                        safe(injectedData.substitutions), safe(injectedData.stairs), safe(injectedData.halfSlabs), safe(injectedData.roofing));
                return;
            }
        } catch (Throwable ignored) {}

        // If there is per-block instance state it would be present as a BlockState in older APIs.
        // With components, prefer BlockType-injected `Chisel.Data` for metadata.
        StateData maybeState = targetBlockType != null ? targetBlockType.getState() : null;
        Chisel.Data chiselData = extractChiselData(maybeState);
        if (chiselData != null) {
            String[] subs     = chiselData.substitutions;
            String[] stairs   = chiselData.stairs;
            String[] halfs    = chiselData.halfSlabs;
            String[] roofs    = chiselData.roofing;

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
                if (empty(roofs))
                    roofs = com.Ev0sMods.Ev0sChisel.compat.VanillaCompat.deriveExistingWoodRoofing(subs);
            }

            // ── Open UI ─────────────────────────────────────────────────
            if (!empty(subs) || !empty(stairs) || !empty(halfs) || !empty(roofs)) {
                ChiselUIPage.openChisel(playerRef, store, world, blockPos, player,
                        safe(subs), safe(stairs), safe(halfs), safe(roofs));
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
    /** Extracts {@link Chisel.Data} from either a plain or combo state, or returns {@code null}. */
    private static Chisel.Data extractChiselData(StateData sd) {
        if (sd instanceof Chisel.Data d) return d;
        if (sd instanceof ComboState cs) return cs.chisel;
        return null;
    }

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
            boolean crouch = ms != null && ms.crouching;
            return crouch;
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] isCrouching failed: " + t.getMessage());
            return false;
        }
    }

    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) {
    }

    static {
        CODEC = BuilderCodec.builder(ChiselInteraction.class, ChiselInteraction::new, SimpleBlockInteraction.CODEC).build();
    }
}
