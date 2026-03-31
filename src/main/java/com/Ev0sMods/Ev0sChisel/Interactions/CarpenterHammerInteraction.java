package com.Ev0sMods.Ev0sChisel.Interactions;

import com.Ev0sMods.Ev0sChisel.CarpenterHammer;
import com.Ev0sMods.Ev0sChisel.ComboState;
import com.Ev0sMods.Ev0sChisel.ui.CarpenterHammerUIPage;
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
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Locale;

/**
 * Server-side interaction for the Carpenter's Hammer.
 * <p>
 * Right-clicking a furniture block (one that carries a
 * {@link CarpenterHammer.Data} state) opens the
 * {@link CarpenterHammerUIPage} with the appropriate tab pre-selected.
 * The UI then lets the player replace the targeted block with any other
 * furniture variant in the same category group.
 */
@SuppressWarnings("removal")
public class CarpenterHammerInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<CarpenterHammerInteraction> CODEC;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ─────────────────────────────────────────────────────────────────────
    // Core interaction
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void interactWithBlock(
            @NonNullDecl World world,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
            @NonNullDecl InteractionType interactionType,
            @NonNullDecl InteractionContext interactionContext,
            @NullableDecl ItemStack itemStack,
            @NonNullDecl Vector3i vector3i,
            @NonNullDecl CooldownHandler cooldownHandler) {

        if (interactionContext == null) return;

        BlockPosition target = interactionContext.getTargetBlock();
        if (target == null || (target.x == 0 && target.y == 0 && target.z == 0)) return;

        WorldChunk chunk = world.getChunkIfInMemory(
                ChunkUtil.indexChunkFromBlock(target.x, target.z));
        if (chunk == null) return;

        // ── Resolve player ───────────────────────────────────────────────
        Ref<EntityStore>  playerEnt;
        Store<EntityStore> store;
        PlayerRef          playerRef;
        com.hypixel.hytale.server.core.entity.entities.Player player;
        try {
            playerEnt = interactionContext.getOwningEntity();
            store     = playerEnt.getStore();
            playerRef = store.getComponent(playerEnt, PlayerRef.getComponentType());
            player    = store.getComponent(playerEnt,
                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerRef == null || player == null) return;
        } catch (Throwable t) {
            LOGGER.atWarning().log("[CarpenterHammer] Failed to get player refs: " + t.getMessage());
            return;
        }

        Vector3i blockPos = new Vector3i(target.x, target.y, target.z);

        // ── Look up CarpenterHammer.Data on the targeted BlockType ───────
        BlockType targetBlockType = chunk.getBlockType(target.x, target.y, target.z);
        StateData state = targetBlockType != null ? targetBlockType.getState() : null;

        CarpenterHammer.Data hammerData = extractHammerData(state);
        if (hammerData != null) {
            CarpenterHammerUIPage.Tab defaultTab = detectDefaultTab(targetBlockType, hammerData);

            // Door stacking requirement: if the target block is a base material (not already a
            // door) and the hammer group contains door options, require 2 of the same block to
            // be stacked vertically (target + block directly above) before showing door options.
            String[] effectiveWindows = safe(hammerData.windows);
            if (targetBlockType != null && !isBlockDoor(targetBlockType) && containsDoors(effectiveWindows)) {
                BlockType above = chunk.getBlockType(target.x, target.y + 1, target.z);
                boolean stacked = above != null
                        && targetBlockType.getId() != null
                        && targetBlockType.getId().equals(above.getId());
                if (!stacked) {
                    effectiveWindows = stripDoors(effectiveWindows);
                }
            }

            CarpenterHammerUIPage.openHammer(
                    playerRef, store, world, blockPos, player,
                    safe(hammerData.chairs),
                    safe(hammerData.tables),
                    safe(hammerData.storage),
                    effectiveWindows,
                    safe(hammerData.lights),
                    defaultTab, 0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Determines which tab should be shown first based on the block key and
     * whatever category arrays are non-empty.
     */
    private static CarpenterHammerUIPage.Tab detectDefaultTab(
            BlockType bt, CarpenterHammer.Data data) {

        if (bt != null && bt.getId() != null) {
            String key = bt.getId().toString().toLowerCase(Locale.ROOT);

            if (key.contains("chair")  || key.contains("sofa")    ||
                key.contains("bench")  || key.contains("stool")   ||
                key.contains("seat")   || key.contains("couch"))
                return CarpenterHammerUIPage.Tab.CHAIR;

            if (key.contains("table")  || key.contains("desk")    ||
                key.contains("coffee"))
                return CarpenterHammerUIPage.Tab.TABLE;

            if (key.contains("kitchen") || key.contains("counter") ||
                key.contains("cupboard") || key.contains("shelf") ||
                key.contains("cabinet") || key.contains("sink")   ||
                key.contains("storage") || key.contains("chest"))
                return CarpenterHammerUIPage.Tab.STORAGE;

            if (key.contains("window") || key.contains("shutter") ||
                key.contains("pane"))
                return CarpenterHammerUIPage.Tab.WINDOW;

            if (key.contains("lantern") || key.contains("torch")  ||
                key.contains("candle")  || key.contains("lamp")   ||
                key.contains("brazier") || key.contains("light")  ||
                key.contains("glow"))
                return CarpenterHammerUIPage.Tab.LIGHT;
        }

        // Fallback: first non-empty category
        if (hasItems(data.chairs))  return CarpenterHammerUIPage.Tab.CHAIR;
        if (hasItems(data.tables))  return CarpenterHammerUIPage.Tab.TABLE;
        if (hasItems(data.storage)) return CarpenterHammerUIPage.Tab.STORAGE;
        if (hasItems(data.windows)) return CarpenterHammerUIPage.Tab.WINDOW;
        if (hasItems(data.lights))  return CarpenterHammerUIPage.Tab.LIGHT;
        return CarpenterHammerUIPage.Tab.CHAIR;
    }

    /** Returns {@code true} if the block's key contains {@code "_door"} (case-insensitive). */
    private static boolean isBlockDoor(BlockType bt) {
        if (bt == null || bt.getId() == null) return false;
        return bt.getId().toString().toLowerCase(Locale.ROOT).contains("_door");
    }

    /** Returns {@code true} if any key in {@code arr} is a door block. */
    private static boolean containsDoors(String[] arr) {
        if (arr == null) return false;
        for (String k : arr) if (k != null && k.toLowerCase(Locale.ROOT).contains("_door")) return true;
        return false;
    }

    /** Returns a copy of {@code arr} with all door keys removed. */
    private static String[] stripDoors(String[] arr) {
        if (arr == null) return new String[0];
        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (String k : arr) if (k != null && !k.toLowerCase(Locale.ROOT).contains("_door")) filtered.add(k);
        return filtered.toArray(new String[0]);
    }

    /** Extracts {@link CarpenterHammer.Data} from either a plain or combo state, or returns {@code null}. */
    private static CarpenterHammer.Data extractHammerData(StateData state) {
        if (state instanceof CarpenterHammer.Data d) return d;
        if (state instanceof ComboState cs) return cs.hammer;
        return null;
    }

    private static boolean hasItems(String[] arr) {
        return arr != null && arr.length > 0;
    }

    private static String[] safe(String[] arr) {
        return arr != null ? arr : new String[0];
    }

    @Override
    protected void simulateInteractWithBlock(
            @NonNullDecl InteractionType it,
            @NonNullDecl InteractionContext ic,
            @NullableDecl ItemStack is,
            @NonNullDecl World w,
            @NonNullDecl Vector3i v) { /* no simulation */ }

    // ─────────────────────────────────────────────────────────────────────
    // Static initialiser
    // ─────────────────────────────────────────────────────────────────────

    static {
        CODEC = BuilderCodec.builder(
                CarpenterHammerInteraction.class,
                CarpenterHammerInteraction::new,
                SimpleBlockInteraction.CODEC).build();
    }
}
