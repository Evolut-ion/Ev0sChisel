package com.Ev0sMods.Ev0sChisel.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.compat.CarpentryCompat;
import com.Ev0sMods.Ev0sChisel.compat.LabelsCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawCompat;
import com.Ev0sMods.Ev0sChisel.compat.MasonryCompat;
import com.Ev0sMods.Ev0sChisel.compat.StoneworksCompat;
import com.Ev0sMods.Ev0sChisel.compat.VanillaCompat;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;

/**
 * Unified Chisel UI – combines in-world block chiselling and inventory-based
 * table conversion into a single page with two switchable modes.
 * <ul>
 *   <li><b>Chisel mode</b> – click a variant to replace the targeted block
 *       in the world.</li>
 *   <li><b>Table mode</b> – select a block from inventory, then convert it
 *       into chisel variants (left-click = all, right-click = half).</li>
 * </ul>
 */
public final class ChiselUIPage {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final java.util.concurrent.ConcurrentHashMap<String, ChiselVariants> CHISEL_VARIANTS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** The two sub-pages the player can switch between. */
    public enum Mode { CHISEL, TABLE }

    /** Output tab types inside the UI. */
    public enum Tab { BLOCKS, STAIRS, HALF_SLABS, ROOFING, STATUE, LABELS }

    /* Layout / paging constants */
    private static final int GRID_COLUMNS   = 4;
    private static final int ITEMS_PER_PAGE = GRID_COLUMNS * 3; // 3 rows
    private static final int INV_COLUMNS    = 8;
    private static final int INV_PER_PAGE   = INV_COLUMNS * 4;  // 4 rows

    // (Icon strings are now sourced dynamically from the first item of each variant array)

    /* Lightweight holder for inventory items presented in the table */
    private static final class InvItem {
        final Object itemId;
        final String blockKey;
        final int count;
        final short slot;
        final int section;

        InvItem(Object itemId, String blockKey, int count, short slot, int section) {
            this.itemId = itemId;
            this.blockKey = blockKey;
            this.count = count;
            this.slot = slot;
            this.section = section;
        }
    }

    /* Simple carrier for resolved chisel variant arrays */
    private static final class ChiselVariants {
        final String[] subs, stairs, halfs, roofs;
        ChiselVariants(String[] subs, String[] stairs, String[] halfs, String[] roofs) {
            this.subs = subs; this.stairs = stairs; this.halfs = halfs; this.roofs = roofs;
        }
    }

    public static void open(PlayerRef playerRef,
                            Store<EntityStore> store,
                            World world,
                            Vector3i blockPos,
                            LivingEntity player,
                            String[] chiselSubs,
                            String[] chiselStairs,
                            String[] chiselHalfs,
                            String[] chiselRoofs,
                            String inputKey,
                            int inputCount,
                            short inputSlot,
                            int inputSection,
                            Mode mode,
                            Tab tab,
                            int outputPage,
                            int invPage) {

        boolean hasChiselData = !empty(chiselSubs) || !empty(chiselStairs)
                || !empty(chiselHalfs) || !empty(chiselRoofs);

        // ── Resolve output variant arrays based on mode ─────────────
            // Cache the chunk once to avoid repeated world lookups in event handlers
            final WorldChunk cachedChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
            // ── Statue detection (two-block pillar of same key/variants) ──
            String[] statueVariants = resolveStatueVariants(cachedChunk, blockPos);
            boolean hasStatues = statueVariants != null && statueVariants.length > 0;
        String[] outSubs, outStairs, outHalfs, outRoofs;
        if (mode == Mode.CHISEL) {
            outSubs   = chiselSubs;
            outStairs = chiselStairs;
            outHalfs  = chiselHalfs;
            outRoofs  = chiselRoofs;
        } else {
            outSubs = null; outStairs = null; outHalfs = null; outRoofs = null;
            if (inputKey != null) {
                ChiselVariants v = resolveChiselVariants(inputKey);
                if (v != null) {
                    outSubs = v.subs; outStairs = v.stairs;
                    outHalfs = v.halfs; outRoofs = v.roofs;
                }
            }
        }

        // If we detected a two-block statue pillar, prefer showing the
        // statue's mapped base chisel material in the BLOCKS tab so the
        // user can convert a statue back into its material blocks.
        if (hasStatues && (tab == Tab.BLOCKS)) {
            boolean hasAny = (outSubs != null && outSubs.length > 0);
            if (!hasAny) {
                java.util.LinkedHashSet<String> mapped = new java.util.LinkedHashSet<>();
                for (String s : statueVariants) {
                    try {
                        String m = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getMappedChiselTypeForStatue(s);
                        if (m != null && !m.isEmpty()) mapped.add(m);
                    } catch (Throwable ignored) {}
                }
                if (!mapped.isEmpty()) {
                    outSubs = mapped.toArray(new String[0]);
                }
            }
        }

        // Detect whether the current substitutions are label variants (not regular material blocks).
        // When true, we surface a dedicated "Labels" tab instead of the generic "Blocks" tab.
        boolean hasLabels = LabelsCompat.isAvailable()
                && len(outSubs) > 0
                && LabelsCompat.isLabelKey(first(outSubs));
        boolean hasBlocks = !hasLabels && len(outSubs) > 0;
        boolean hasStairs = len(outStairs) > 0;
        boolean hasHalfs  = len(outHalfs) > 0;
        boolean hasRoofs  = len(outRoofs) > 0;

        // If opening with BLOCKS active but this is actually a labels block, redirect to LABELS tab
        final Tab activeTab = (hasLabels && tab == Tab.BLOCKS) ? Tab.LABELS : tab;

        String[] allOutputs = switch (activeTab) {
            case BLOCKS     -> safe(outSubs);
            case STAIRS     -> safe(outStairs);
            case HALF_SLABS -> safe(outHalfs);
            case ROOFING    -> safe(outRoofs);
            case STATUE     -> safe(statueVariants);
            case LABELS     -> safe(outSubs);
        };

        // ── Paginate output grid ────────────────────────────────────
        int totalOut   = allOutputs.length;
        int totalOutPg = Math.max(1, (totalOut + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int curOutPg   = Math.max(0, Math.min(outputPage, totalOutPg - 1));
        int outStart   = curOutPg * ITEMS_PER_PAGE;
        int outEnd     = Math.min(outStart + ITEMS_PER_PAGE, totalOut);
        String[] pgOut = new String[outEnd - outStart];
        if (pgOut.length > 0)
            System.arraycopy(allOutputs, outStart, pgOut, 0, pgOut.length);

        // ── Paginate inventory (TABLE mode only) ────────────────────
        List<InvItem> pgInv = Collections.emptyList();
        int invStart = 0, curInvPg = 0, totalInvPg = 1;
        if (mode == Mode.TABLE) {
            List<InvItem> fullInv = readPlayerInventory(player);
            totalInvPg = Math.max(1, (fullInv.size() + INV_PER_PAGE - 1) / INV_PER_PAGE);
            curInvPg   = Math.max(0, Math.min(invPage, totalInvPg - 1));
            invStart   = curInvPg * INV_PER_PAGE;
            int invEnd = Math.min(invStart + INV_PER_PAGE, fullInv.size());
            pgInv      = fullInv.subList(invStart, invEnd);
        }

        // ── Build HTML ──────────────────────────────────────────────
        String html;
        String iconBlocks = first(outSubs);
        String iconStairs = first(outStairs);
        String iconHalfs  = first(outHalfs);
        String iconRoofs  = first(outRoofs);

        if (mode == Mode.CHISEL) {
            html = buildChiselHtml(pgOut, outStart, activeTab,
                hasBlocks, iconBlocks, hasStairs, iconStairs,
                hasHalfs, iconHalfs, hasRoofs, iconRoofs,
                hasStatues, first(statueVariants), hasLabels, iconBlocks,
                hasChiselData, curOutPg, totalOutPg);
        } else {
            html = buildTableHtml(pgInv, invStart, pgOut, outStart,
                inputKey, inputCount, activeTab,
                hasBlocks, iconBlocks, hasStairs, iconStairs,
                hasHalfs, iconHalfs, hasRoofs, iconRoofs,
                hasStatues, first(statueVariants), hasLabels, iconBlocks,
                hasChiselData, curOutPg, totalOutPg,
                curInvPg, totalInvPg);
        }

        // ── Create page & register events ───────────────────────────
        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

        // Capture for lambdas
        final int fCurInvPg = curInvPg;
        final int fCurOutPg = curOutPg;
        final int fInvStart = invStart;

        // ── Mode toggle handlers ────────────────────────────────────
        if (mode != Mode.CHISEL && hasChiselData) {
            builder.addEventListener("mode_chisel", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            null, 0, (short) -1, -1,
                            Mode.CHISEL, Tab.BLOCKS, 0, 0));
        }
        if (mode != Mode.TABLE) {
            builder.addEventListener("mode_table", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            null, 0, (short) -1, -1,
                            Mode.TABLE, Tab.BLOCKS, 0, 0));
        }

        // (Removed explicit switch button listeners; mode-row handles switching)

        // ── Tab handlers ────────────────────────────────────────────
        if (hasBlocks && activeTab != Tab.BLOCKS)
            builder.addEventListener("tab_blocks", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, Tab.BLOCKS, 0, fCurInvPg));
        if (hasStairs && activeTab != Tab.STAIRS)
            builder.addEventListener("tab_stairs", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, Tab.STAIRS, 0, fCurInvPg));
        if (hasHalfs && activeTab != Tab.HALF_SLABS)
            builder.addEventListener("tab_halfslabs", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, Tab.HALF_SLABS, 0, fCurInvPg));
        if (hasRoofs && activeTab != Tab.ROOFING)
            builder.addEventListener("tab_roofing", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, Tab.ROOFING, 0, fCurInvPg));
        if (hasStatues && activeTab != Tab.STATUE) {
            boolean tabRendered = (mode == Mode.CHISEL)
                    || (mode == Mode.TABLE && inputKey != null
                        && (hasBlocks || hasStairs || hasHalfs || hasRoofs || hasStatues));
            if (tabRendered) {
                builder.addEventListener("tab_statues", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                        chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                        inputKey, inputCount, inputSlot, inputSection,
                        mode, Tab.STATUE, 0, fCurInvPg));
            }
        }
        if (hasLabels && activeTab != Tab.LABELS)
            builder.addEventListener("tab_labels", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, Tab.LABELS, 0, fCurInvPg));

        // ── Output pagination ───────────────────────────────────────
        if (curOutPg > 0)
            builder.addEventListener("out_prev", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, activeTab, fCurOutPg - 1, fCurInvPg));
        if (curOutPg < totalOutPg - 1)
            builder.addEventListener("out_next", CustomUIEventBindingType.Activating,
                    (i, c) -> open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            inputKey, inputCount, inputSlot, inputSection,
                            mode, activeTab, fCurOutPg + 1, fCurInvPg));

        // ── Mode-specific event handlers ────────────────────────────
        if (mode == Mode.CHISEL) {
            // Click output → replace block in world
            for (int i = 0; i < pgOut.length; i++) {
                final String blockKey = pgOut[i];
                final String btnId = "out_" + (outStart + i);
                builder.addEventListener(btnId, CustomUIEventBindingType.Activating,
                        (ignored, ctx) -> {
                            try {
                                WorldChunk chunk = cachedChunk;
                                if (chunk != null) {
                            // STATUE tab replaces a two-block pillar (bottom+top)
                            if (activeTab == Tab.STATUE) {
                                // STATUE tab: place the statue key (user expects statue placement)
                                try {
                                    chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                                    chunk.setBlock(blockPos.x, blockPos.y + 1, blockPos.z, blockKey);
                                    LOGGER.atWarning().log("[Chisel] STATUE tab placed statue key: " + blockKey);
                                } catch (Throwable ex) {
                                    try {
                                        chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                                        chunk.setBlock(blockPos.x, blockPos.y + 1, blockPos.z, blockKey);
                                    } catch (Throwable ignoredEx) {}
                                }
                            } else {
                                // BLOCKS tab: if the chosen key is actually a Ymmersive statue,
                                // map it back to its material (e.g., Rock_Marble) and replace
                                // the two-block pillar with two stacked material blocks.
                                try {
                                    boolean handled = false;
                                    if (com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.isAvailable()) {
                                        String mapped = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getMappedChiselTypeForStatue(blockKey);
                                        if (mapped != null && mapped.toLowerCase(java.util.Locale.ROOT).startsWith("rock_")) {
                                            // place material blocks on bottom+top and verify
                                            chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, mapped);
                                            chunk.setBlock(blockPos.x, blockPos.y + 1, blockPos.z, mapped);
                                            try {
                                                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType bBottom2 = chunk.getBlockType(blockPos.x, blockPos.y, blockPos.z);
                                                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType bTop2 = chunk.getBlockType(blockPos.x, blockPos.y + 1, blockPos.z);
                                                String bottomId2 = bBottom2 != null ? bBottom2.getId().toString() : "<null>";
                                                String topId2 = bTop2 != null ? bTop2.getId().toString() : "<null>";
                                                if (!mapped.equals(bottomId2)) chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, mapped);
                                                if (!mapped.equals(topId2)) chunk.setBlock(blockPos.x, blockPos.y + 1, blockPos.z, mapped);
                                            } catch (Throwable v) {
                                                LOGGER.atWarning().log("[Chisel] BLOCKS tab: verification failed: " + v.getMessage());
                                            }
                                            handled = true;
                                            LOGGER.atWarning().log("[Chisel] BLOCKS tab replacement attempted: key=" + blockKey + " mapped=" + mapped);
                                        }
                                    }
                                    if (!handled) {
                                        if (hasStatues) {
                                            // Under statue conditions, place a two-block pillar (bottom+top)
                                            chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                                            chunk.setBlock(blockPos.x, blockPos.y + 1, blockPos.z, blockKey);
                                            LOGGER.atWarning().log("[Chisel] BLOCKS tab placed pillar fallback: " + blockKey);
                                        } else {
                                            chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                                            LOGGER.atWarning().log("[Chisel] BLOCKS tab placed key fallback: " + blockKey);
                                        }
                                    }
                                } catch (Throwable ex) {
                                    try { chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey); } catch (Throwable ignoredEx) {}
                                }
                            }
                            }
                                // removed info log for setBlock action
                        }
                     catch (Throwable t) {
                        LOGGER.atWarning().log("[Chisel] Failed to set block: "
                                + t.getMessage());
                     }}
                );
            }
            
        } else {
            // ── Inventory item clicks → select as input ─────────────
            for (int i = 0; i < pgInv.size(); i++) {
                final InvItem item = pgInv.get(i);
                final String btnId = "inv_" + (fInvStart + i);
                builder.addEventListener(btnId, CustomUIEventBindingType.Activating,
                        (ignored, ctx) -> open(playerRef, store, world, blockPos, player,
                                chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                                item.blockKey, item.count, item.slot, item.section,
                                Mode.TABLE, Tab.BLOCKS, 0, fCurInvPg));
            }

            // ── Inventory pagination ────────────────────────────────
            if (curInvPg > 0)
                builder.addEventListener("inv_prev", CustomUIEventBindingType.Activating,
                        (i, c) -> open(playerRef, store, world, blockPos, player,
                                chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                                inputKey, inputCount, inputSlot, inputSection,
                                Mode.TABLE, activeTab, fCurOutPg, fCurInvPg - 1));
            if (curInvPg < totalInvPg - 1)
                builder.addEventListener("inv_next", CustomUIEventBindingType.Activating,
                        (i, c) -> open(playerRef, store, world, blockPos, player,
                                chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                                inputKey, inputCount, inputSlot, inputSection,
                                Mode.TABLE, activeTab, fCurOutPg, fCurInvPg + 1));

            // ── Output clicks: left = convert all, right = half ─────
            for (int i = 0; i < pgOut.length; i++) {
                final String outputKey = pgOut[i];
                final String btnId = "out_" + (outStart + i);

                builder.addEventListener(btnId, CustomUIEventBindingType.Activating,
                        (ignored, ctx) -> {
                    if (inputKey != null && inputCount > 0)
                        convertItems(player, inputSlot, inputSection,
                                inputKey, inputCount, outputKey, inputCount);
                    open(playerRef, store, world, blockPos, player,
                            chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                            null, 0, (short) -1, -1,
                            Mode.TABLE, Tab.BLOCKS, 0, fCurInvPg);
                });

                builder.addEventListener(btnId, CustomUIEventBindingType.RightClicking,
                        (ignored, ctx) -> {
                    if (inputKey != null && inputCount > 0) {
                        int half = (inputCount + 1) / 2;
                        int rem  = inputCount - half;
                        convertItems(player, inputSlot, inputSection,
                                inputKey, inputCount, outputKey, half);
                        if (rem > 0)
                            open(playerRef, store, world, blockPos, player,
                                    chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                                    inputKey, rem, inputSlot, inputSection,
                                    Mode.TABLE, activeTab, fCurOutPg, fCurInvPg);
                        else
                            open(playerRef, store, world, blockPos, player,
                                    chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                                    null, 0, (short) -1, -1,
                                    Mode.TABLE, Tab.BLOCKS, 0, fCurInvPg);
                    }
                });
            }

            // ── Clear-input button ──────────────────────────────────
            if (inputKey != null)
                builder.addEventListener("clear_input", CustomUIEventBindingType.Activating,
                        (i, c) -> open(playerRef, store, world, blockPos, player,
                                chiselSubs, chiselStairs, chiselHalfs, chiselRoofs,
                                null, 0, (short) -1, -1,
                                Mode.TABLE, Tab.BLOCKS, 0, fCurInvPg));
        }

        builder.open(store);
        // removed mode open info log
    }

    /** Opens in Chisel mode (in-world block replacement). */
    public static void openChisel(PlayerRef playerRef,
                                  Store<EntityStore> store,
                                  World world,
                                  Vector3i blockPos,
                                  LivingEntity player,
                                  String[] subs,
                                  String[] stairs,
                                  String[] halfs,
                                  String[] roofs) {
        ChiselVariants resolved = null;
        try {
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
            if (chunk != null) {
                BlockType blockType = chunk.getBlockType(blockPos.x, blockPos.y, blockPos.z);
                String blockKey = blockType != null && blockType.getId() != null ? String.valueOf(blockType.getId()) : null;
                resolved = resolveChiselVariants(blockKey);
            }
        } catch (Throwable ignored) {}

        ChiselVariants normalized = resolved != null
                ? resolved
                : normalizeVariantBuckets(subs, stairs, halfs, roofs);

        open(playerRef, store, world, blockPos, player,
                normalized.subs, normalized.stairs, normalized.halfs, normalized.roofs,
                null, 0, (short) -1, -1,
                Mode.CHISEL, Tab.BLOCKS, 0, 0);
    }

    /** Opens in Table mode (inventory conversion). */
    public static void openTable(PlayerRef playerRef,
                                 Store<EntityStore> store,
                                 World world,
                                 Vector3i blockPos,
                                 LivingEntity player) {
        open(playerRef, store, world, blockPos, player,
                null, null, null, null,
                null, 0, (short) -1, -1,
                Mode.TABLE, Tab.BLOCKS, 0, 0);
    }

    // ═════════════════════════════════════════════════════════════════
    // HTML builders
    // ═════════════════════════════════════════════════════════════════

    // ── Chisel mode layout ──────────────────────────────────────────

    private static String buildChiselHtml(String[] pageOut, int outStart,
                                          Tab activeTab,
                                          boolean hasBlocks, String iconBlocks,
                                          boolean hasStairs, String iconStairs,
                                          boolean hasHalfs,  String iconHalfs,
                                          boolean hasRoofs,  String iconRoofs,
                                          boolean hasStatues, String iconStatues,
                                          boolean hasLabels, String iconLabels,
                                          boolean hasChiselData,
                                          int curOutPg, int totalOutPg) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildModeToggle(Mode.CHISEL, hasChiselData));
        sb.append("<div class=\"separator\"></div>\n");

        sb.append("<p class=\"title-label\">Choose a Block</p>\n");
        sb.append("<p class=\"info-label\">Select a variant to chisel this block into.</p>\n");

        sb.append(buildTabRow(hasBlocks, iconBlocks, hasStairs, iconStairs,
            hasHalfs, iconHalfs, hasRoofs, iconRoofs, hasStatues, iconStatues,
            hasLabels, iconLabels, activeTab));
        sb.append("<div class=\"separator\"></div>\n");

        sb.append("<div class=\"btn-grid\">\n");
        sb.append(buildGrid(pageOut, outStart));
        sb.append("</div>\n");

        sb.append(buildPager(curOutPg, totalOutPg, "out_prev", "out_next"));

        return STYLE + String.format("""
                <div style="layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;">
                    <div class="decorated-container" data-hyui-title="Chisel"
                             style="anchor-width: 660; margin-right: 40; vertical-align: middle;">
                        <div class="container-contents" style="layout-mode: Top; padding-top: 12; padding-bottom: 12; padding-left: 24; padding-right: 24;">
                %s
                        </div>
                    </div>
                </div>
                """, sb.toString());
    }

    // ── Table mode layout ───────────────────────────────────────────

    private static String buildTableHtml(List<InvItem> pageInv, int invStartIdx,
                                         String[] pageOut, int outStart,
                                         String inputKey, int inputCount,
                                         Tab activeTab,
                                         boolean hasBlocks, String iconBlocks,
                                         boolean hasStairs, String iconStairs,
                                         boolean hasHalfs,  String iconHalfs,
                                         boolean hasRoofs,  String iconRoofs,
                                         boolean hasStatues, String iconStatues,
                                         boolean hasLabels, String iconLabels,
                                         boolean hasChiselData,
                                         int curOutPg, int totalOutPg,
                                         int curInvPg, int totalInvPg) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildModeToggle(Mode.TABLE, hasChiselData));
        sb.append("<div class=\"separator\"></div>\n");

        // Single-column layout: Output on top, Inventory at bottom so the
        // inventory panel naturally grows with content and sits beneath
        // the output grid on narrow/short windows.
        sb.append("<div style=\"layout-mode: Top; anchor-width: 100%; padding-top: 4; padding-bottom: 4; padding-left: 6; padding-right: 6;\">\n");

        // ── Output panel (full width) ───────────────────────────
        sb.append("  <div style=\"layout-mode: Top; anchor-width: 100%; padding-top: 4; padding-bottom: 4; padding-left: 6; padding-right: 6;\">\n");
        sb.append("    <p class=\"section-label\">Input</p>\n");
        sb.append("    <div style=\"layout-mode: Left; horizontal-align: center; padding-top: 6; padding-bottom: 6;\">\n");
        if (inputKey != null) {
            String inputName = prettifyBlockKey(inputKey);
            sb.append(String.format(
                    "      <button id=\"clear_input\" data-hyui-tooltiptext=\"Click to clear\" "
                  + "style=\"anchor-width: 56; anchor-height: 56; padding: 6; margin-top: 4; margin-bottom: 4; margin-left: 4; margin-right: 4;\">"
                  + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" "
                  + "style=\"anchor-width: 40; anchor-height: 40;\"></span>"
                  + "</button>\n", inputKey));
            sb.append(String.format(
                    "      <p class=\"input-count\">%s  x%d</p>\n", inputName, inputCount));
        } else {
            sb.append("      <div class=\"empty-slot\"></div>\n");
            sb.append("      <p class=\"info-label\">Select an item from inventory</p>\n");
        }
        sb.append("    </div>\n");
        sb.append("    <div class=\"separator\"></div>\n");

        if (inputKey != null && (hasBlocks || hasStairs || hasHalfs || hasRoofs || hasStatues || hasLabels))
            sb.append(buildTabRow(hasBlocks, iconBlocks, hasStairs, iconStairs,
                hasHalfs, iconHalfs, hasRoofs, iconRoofs, hasStatues, iconStatues,
                hasLabels, iconLabels, activeTab));

        sb.append("    <div class=\"btn-grid\">\n");
        if (pageOut.length > 0) {
            sb.append(buildGrid(pageOut, outStart));
        } else if (inputKey != null) {
            sb.append("      <p class=\"info-label\">No chisel variants available for this block.</p>\n");
        } else {
            sb.append("      <p class=\"info-label\">Place a block in the input slot to see variants.</p>\n");
        }
        sb.append("    </div>\n");

        sb.append(buildPager(curOutPg, totalOutPg, "out_prev", "out_next"));

        if (inputKey != null && pageOut.length > 0)
            sb.append("    <p class=\"hint-label\">Left-click: convert all  |  Right-click: convert half</p>\n");

        sb.append("  </div>\n");

        // ── Inventory panel (bottom, full width) ─────────────────
        sb.append("  <div style=\"layout-mode: Top; anchor-width: 100%; padding-top: 8; padding-bottom: 8; padding-left: 6; padding-right: 6; margin-top: 8;\">\n");
        sb.append("    <p class=\"section-label\">Inventory</p>\n");
        sb.append("    <div class=\"separator\"></div>\n");

        for (int i = 0; i < pageInv.size(); i++) {
            if (i % INV_COLUMNS == 0)
                sb.append("    <div style=\"layout-mode: Left; horizontal-align: center; padding-top: 2; padding-bottom: 2;\">\n");
            InvItem item = pageInv.get(i);
            String name = prettifyBlockKey(item.blockKey);
            sb.append(String.format(
                    "      <button id=\"inv_%d\" data-hyui-tooltiptext=\"%s x%d\" "
                  + "style=\"anchor-width: 42; anchor-height: 42; padding: 4; margin-top: 2; margin-bottom: 2; margin-left: 2; margin-right: 2;\">"
                  + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" "
                  + "style=\"anchor-width: 32; anchor-height: 32;\"></span>"
                  + "</button>\n",
                    invStartIdx + i, name, item.count, item.blockKey));
            if (i % INV_COLUMNS == INV_COLUMNS - 1 || i == pageInv.size() - 1)
                sb.append("    </div>\n");
        }
        if (pageInv.isEmpty())
            sb.append("    <p class=\"info-label\">No block items in inventory.</p>\n");

        sb.append(buildPager(curInvPg, totalInvPg, "inv_prev", "inv_next"));
        sb.append("  </div>\n");

        sb.append("</div>\n");

        return STYLE + String.format("""
                <div style="layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;">
                    <div class="decorated-container" data-hyui-title="Chisel"
                        style="anchor-width: 660; margin-right: 40; vertical-align: middle;">
                        <div class="container-contents" style="layout-mode: Top; padding-top: 8; padding-bottom: 8; padding-left: 12; padding-right: 12;">
                %s
                        </div>
                    </div>
                </div>
                """, sb.toString());
    }

    // ── Shared HTML helpers ─────────────────────────────────────────

    private static String buildModeToggle(Mode active, boolean hasChiselData) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"mode-row\">\n");
        String chiselCls = hasChiselData ? (active == Mode.CHISEL ? "mode-btn-active" : "mode-btn") : "mode-btn-disabled";
        sb.append(String.format("  <button id=\"mode_chisel\" class=\"%s\">Chisel</button>\n", chiselCls));
        sb.append(String.format("  <button id=\"mode_table\" class=\"%s\">Table</button>\n", active == Mode.TABLE ? "mode-btn-active" : "mode-btn"));
        sb.append("</div>\n");
        return sb.toString();
    }

    private static String buildTabRow(boolean hasBlocks, String iconBlocks,
                                      boolean hasStairs, String iconStairs,
                                      boolean hasHalfs,  String iconHalfs,
                                      boolean hasRoofs,  String iconRoofs,
                                      boolean hasStatues, String iconStatues,
                                      boolean hasLabels, String iconLabels,
                                      Tab activeTab) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"tab-row\">\n");
        if (hasBlocks) appendIconTab(sb, "tab_blocks",    iconBlocks, "Blocks",     activeTab == Tab.BLOCKS);
        if (hasStairs) appendIconTab(sb, "tab_stairs",    iconStairs, "Stairs",     activeTab == Tab.STAIRS);
        if (hasHalfs)  appendIconTab(sb, "tab_halfslabs", iconHalfs,  "Half Slabs", activeTab == Tab.HALF_SLABS);
        if (hasRoofs)  appendIconTab(sb, "tab_roofing",   iconRoofs,  "Roofing",    activeTab == Tab.ROOFING);
        if (hasStatues) appendIconTab(sb, "tab_statues", iconStatues, "Statues", activeTab == Tab.STATUE);
        if (hasLabels) appendIconTab(sb, "tab_labels", iconLabels, "Labels", activeTab == Tab.LABELS);
        sb.append("</div>\n");
        return sb.toString();
    }

    private static String buildGrid(String[] items, int startIdx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i % GRID_COLUMNS == 0)
                sb.append("<div style=\"layout-mode: Left; horizontal-align: center; padding-top: 2; padding-bottom: 2;\">\n");
            String name = prettifyBlockKey(items[i]);
            sb.append(String.format(
                    "  <button id=\"out_%d\" data-hyui-tooltiptext=\"%s\" "
                  + "style=\"anchor-width: 52; anchor-height: 52; padding: 6; margin-top: 4; margin-bottom: 4; margin-left: 4; margin-right: 4;\">"
                  + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" "
                  + "style=\"anchor-width: 40; anchor-height: 40;\"></span>"
                  + "</button>\n",
                    startIdx + i, name, items[i]));
            if (i % GRID_COLUMNS == GRID_COLUMNS - 1 || i == items.length - 1)
                sb.append("</div>\n");
        }
        return sb.toString();
    }

    private static String buildPager(int cur, int total,
                                     String prevId, String nextId) {
        if (total <= 1) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"page-row\">\n");
        sb.append(cur > 0
                ? String.format("  <button id=\"%s\" class=\"page-btn\">&lt;</button>\n", prevId)
                : "  <button class=\"page-btn-disabled\">&lt;</button>\n");
        sb.append(String.format("  <p class=\"page-label\">%d / %d</p>\n", cur + 1, total));
        sb.append(cur < total - 1
                ? String.format("  <button id=\"%s\" class=\"page-btn\">&gt;</button>\n", nextId)
                : "  <button class=\"page-btn-disabled\">&gt;</button>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    private static void appendIconTab(StringBuilder sb, String id,
                                      String iconBlockId, String tooltip,
                                      boolean active) {
        String bgStyle = active ? " background-color: #ffffff(0.12);" : "";
        String baseStyle = "anchor-width: 52; anchor-height: 52; padding: 4; margin-left: 4; margin-right: 4;"
                + bgStyle;
        if (iconBlockId != null && !iconBlockId.isEmpty()) {
            sb.append(String.format(
                    "  <button id=\"%s\" style=\"%s\" data-hyui-tooltiptext=\"%s\">"
                  + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" "
                  + "style=\"anchor-width: 40; anchor-height: 40;\"></span>"
                  + "</button>\n",
                    id, baseStyle, tooltip, iconBlockId));
        } else {
            sb.append(String.format(
                    "  <button id=\"%s\" style=\"%s\" data-hyui-tooltiptext=\"%s\">%s</button>\n",
                    id, baseStyle, tooltip, tooltip.split(" ")[0]));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Inventory reading
    // ═════════════════════════════════════════════════════════════════

    private static List<InvItem> readPlayerInventory(LivingEntity player) {
        List<InvItem> items = new ArrayList<>();
        try {
            Inventory inv = player.getInventory();
            if (inv == null) {
                LOGGER.atWarning().log("[Chisel] Player inventory is null");
                return items;
            }
                collectBlockItems(items, inv.getHotbar(), 0);
                collectBlockItems(items, inv.getStorage(), 1);
                // removed info log for inventory read
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] Error reading inventory: "
                    + t.getMessage());
            t.printStackTrace();
        }
        return items;
    }

    private static void collectBlockItems(List<InvItem> dest,
                                          ItemContainer container,
                                          int section) {
        if (container == null) return;
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            String blockKey = stack.getBlockKey();
            if (blockKey != null && !blockKey.isEmpty())
                dest.add(new InvItem(stack.getItemId(), blockKey,
                        stack.getQuantity(), slot, section));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Inventory manipulation – conversion
    // ═════════════════════════════════════════════════════════════════

    private static void convertItems(LivingEntity player,
                                     short inputSlot, int inputSection,
                                     String inputKey, int inputCount,
                                     String outputKey, int convertCount) {
        try {
            Inventory inv = player.getInventory();
            if (inv == null) return;
            ItemContainer container = (inputSection == 0)
                    ? inv.getHotbar() : inv.getStorage();
            if (container == null) return;

            int remaining = inputCount - convertCount;
            if (remaining <= 0) {
                container.setItemStackForSlot(inputSlot,
                        new ItemStack(outputKey, inputCount));
            } else {
                container.setItemStackForSlot(inputSlot,
                        new ItemStack(inputKey, remaining));
                inv.getCombinedHotbarFirst().addItemStack(
                        new ItemStack(outputKey, convertCount));
            }
                            markInventoryChanged(inv);
                // removed conversion info log
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] Conversion failed: "
                    + t.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Chisel variant resolution
    // ═════════════════════════════════════════════════════════════════

    private static ChiselVariants resolveChiselVariants(String blockKey) {
        if (blockKey == null) return null;
        ChiselVariants cached = CHISEL_VARIANTS_CACHE.get(blockKey);
        if (cached != null) return cached;

        BlockType bt = com.Ev0sMods.Ev0sChisel.compat.BlockTypeCache.get(blockKey);
        if (bt == null) return null;
        StateData state = bt.getState();
        if (!(state instanceof Chisel.Data chiselData)) return null;

        String[] subs   = chiselData.substitutions;
        String[] stairs = chiselData.stairs;
        String[] halfs  = chiselData.halfSlabs;
        String[] roofs  = chiselData.roofing;

        String detectedRockType = MasonryCompat.detectStoneType(blockKey, subs);
        if (detectedRockType == null)
            detectedRockType = MacawCompat.detectRockType(blockKey, subs);

        if (detectedRockType != null) {
            subs = merge(subs, discoverVanillaRockBlocksForUi(canonicalType(detectedRockType, VanillaCompat.getRockTypes())));
            subs   = filterByRockType(subs,   detectedRockType);
            stairs = filterByRockType(stairs,  detectedRockType);
            halfs  = filterByRockType(halfs,   detectedRockType);
            roofs  = filterByRockType(roofs,   detectedRockType);
        }

        if (MasonryCompat.isAvailable() && detectedRockType != null) {
            subs   = merge(subs,  MasonryCompat.getVariants(detectedRockType));
            stairs = merge(stairs, MasonryCompat.getStairVariants(detectedRockType));
            halfs  = merge(halfs,  MasonryCompat.getHalfVariants(detectedRockType));
        }
        if (StoneworksCompat.isAvailable() && "stone".equals(detectedRockType))
            subs = merge(subs, StoneworksCompat.getVariants());

        if ((MacawCompat.isPathsAvailable() || MacawCompat.isStairsAvailable())
                && detectedRockType != null) {
            if (MacawCompat.isPathsAvailable()) {
                subs   = merge(subs,   MacawCompat.getPathsBlocks(detectedRockType));
                stairs = merge(stairs, MacawCompat.getPathsStairs(detectedRockType));
                halfs  = merge(halfs,  MacawCompat.getPathsHalfs(detectedRockType));
            }
            if (MacawCompat.isStairsAvailable())
                stairs = merge(stairs, MacawCompat.getMcwStairs(detectedRockType));
        }

        if (CarpentryCompat.isAvailable() && subs != null) {
            String woodType = CarpentryCompat.detectWoodType(blockKey, subs);
            if (woodType != null) {
                subs   = merge(subs,   discoverVanillaWoodBlocksForUi(canonicalType(woodType, VanillaCompat.getWoodTypes())));
                subs   = merge(subs,   CarpentryCompat.getVariants(woodType));
                stairs = merge(stairs, CarpentryCompat.getStairVariants(woodType));
                halfs  = merge(halfs,  CarpentryCompat.getHalfVariants(woodType));
            }
        }

        if (!empty(subs)) {
            stairs = merge(stairs, Arrays.asList(safe(MasonryCompat.deriveExistingVariants(subs, "_Stairs"))));
            halfs  = merge(halfs, Arrays.asList(safe(MasonryCompat.deriveExistingVariants(subs, "_Half"))));
            roofs  = merge(roofs, Arrays.asList(safe(MasonryCompat.deriveExistingRoofing(subs))));
            roofs  = merge(roofs, Arrays.asList(VanillaCompat.deriveExistingWoodRoofing(subs)));
        }

        ChiselVariants result = normalizeVariantBuckets(subs, stairs, halfs, roofs);

        if (empty(result.subs) && empty(result.stairs) && empty(result.halfs) && empty(result.roofs))
            return null;

        CHISEL_VARIANTS_CACHE.put(blockKey, result);
        return result;
    }

    // ── Statue detection (two-block pillar) ───────────────────────
    private static String[] resolveStatueVariants(World world, Vector3i blockPos) {
        try {
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
            return resolveStatueVariants(chunk, blockPos);
        } catch (Throwable t) {
            return new String[0];
        }
    }

    // Chunk-based resolver avoids repeated world lookups; prefer calling this
    private static String[] resolveStatueVariants(WorldChunk chunk, Vector3i blockPos) {
        try {
            if (chunk == null) return new String[0];
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType bottom = chunk.getBlockType(blockPos.x, blockPos.y, blockPos.z);
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType top = chunk.getBlockType(blockPos.x, blockPos.y + 1, blockPos.z);
            if (bottom == null || top == null) return new String[0];
            String bottomKey = (String) bottom.getId();
            String topKey = (String) top.getId();
            if (bottomKey == null || topKey == null) return new String[0];
            if (!sameVariant(bottomKey, topKey)) return new String[0];

            // Candidate keys to test (base + chisel substitutions)
            java.util.Set<String> candidates = new java.util.LinkedHashSet<>();
            candidates.add(bottomKey);
            ChiselVariants cv = resolveChiselVariants(bottomKey);
            if (cv != null && cv.subs != null) java.util.Collections.addAll(candidates, cv.subs);

            // Material -> furniture mapping (from user's spec)
            java.util.Map<String, String[]> mat = new java.util.LinkedHashMap<>();
            mat.put("rock_stone", new String[]{"Furniture_Ancient_Statue", "Furniture_Human_Ruins_Statue_Broken"});
            mat.put("any_wood", new String[]{"Furniture_Kweebec_Statue", "Furniture_Temple_Emerald_Statue"});
            mat.put("rock_shale", new String[]{"Furniture_Temple_Dark_Statue_Gaia", "Furniture_Temple_Dark_Statue"});
            mat.put("rock_chalk", new String[]{"Furniture_Temple_Light_Statue"});
            mat.put("rock_gold", new String[]{"Furniture_Temple_Scarak_Statue"});
            mat.put("white_sandstone", new String[]{"Furniture_Temple_Wind_Statue_Gaia", "Furniture_Temple_Wind_Statue"});

            java.util.List<String> results = new java.util.ArrayList<>();
            for (String candidate : candidates) {
                String lower = candidate.toLowerCase(java.util.Locale.ROOT);
                for (java.util.Map.Entry<String, String[]> e : mat.entrySet()) {
                    String key = e.getKey();
                    boolean match = false;
                    if ("any_wood".equals(key)) {
                        if (lower.startsWith("wood_")) match = true;
                    } else if ("white_sandstone".equals(key)) {
                        if (lower.contains("sandstone") && lower.contains("white")) match = true;
                    } else if (key.startsWith("rock_")) {
                        String rockType = key.substring("rock_".length());
                        if (lower.contains(rockType)) match = true;
                    }
                    if (match) {
                        for (String f : e.getValue()) if (!results.contains(f)) results.add(f);
                    }
                }
            }

            // Append Ymmersive Statues candidates via compat helper
            if (com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.isAvailable()) {
                // Determine the desired chisel type for this pillar (e.g., Rock_Marble, any_wood)
                String desiredChiselType = null;
                try {
                    ChiselVariants baseCv = resolveChiselVariants(bottomKey);
                    String[] baseSubs = (baseCv != null) ? baseCv.subs : null;
                    String detectedRockType = null;
                    if (MasonryCompat.isAvailable()) detectedRockType = MasonryCompat.detectStoneType(bottomKey, baseSubs);
                    if (detectedRockType == null) detectedRockType = MacawCompat.detectRockType(bottomKey, baseSubs);
                    if (detectedRockType != null) {
                        desiredChiselType = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.mapBlockMaterialToChisel(detectedRockType);
                    } else if (CarpentryCompat.isAvailable()) {
                        String woodType = CarpentryCompat.detectWoodType(bottomKey, baseSubs);
                        if (woodType != null) desiredChiselType = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.mapBlockMaterialToChisel("wood");
                    } else if (bottomKey.toLowerCase(java.util.Locale.ROOT).contains("mossy")) {
                        desiredChiselType = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.mapBlockMaterialToChisel("mossy");
                    }

                    java.util.List<String> filtered = new java.util.ArrayList<>();
                    for (String candidate : candidates) {
                        ChiselVariants cv2 = resolveChiselVariants(candidate);
                        String[] subsArr = (cv2 != null) ? cv2.subs : null;
                        java.util.List<String> s = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getCandidatesFor(candidate, subsArr);
                        for (String k : s) {
                            String mapped = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getMappedChiselTypeForStatue(k);
                            if (desiredChiselType == null) {
                                if (!results.contains(k)) results.add(k);
                            } else {
                                if (mapped != null && mapped.equalsIgnoreCase(desiredChiselType)) {
                                    if (!filtered.contains(k)) filtered.add(k);
                                }
                            }
                        }
                    }
                    if (desiredChiselType != null) {
                        for (String k : filtered) if (!results.contains(k)) results.add(k);
                    }
                } catch (Throwable t) {
                    // On any failure, fall back to the old behavior (add all candidates)
                    for (String candidate : candidates) {
                        ChiselVariants cv2 = resolveChiselVariants(candidate);
                        String[] subsArr = (cv2 != null) ? cv2.subs : null;
                        java.util.List<String> s = com.Ev0sMods.Ev0sChisel.compat.StatuesCompat.getCandidatesFor(candidate, subsArr);
                        for (String k : s) if (!results.contains(k)) results.add(k);
                    }
                }
            }

            // Debug: log statue candidate resolution for troubleshooting material/state issues
            // removed statue candidate resolution info log

            return results.toArray(new String[0]);
        } catch (Throwable t) {
            return new String[0];
        }
    }

    private static boolean sameVariant(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        ChiselVariants va = resolveChiselVariants(a);
        if (va != null && va.subs != null) {
            for (String s : va.subs) if (b.equals(s)) return true;
        }
        ChiselVariants vb = resolveChiselVariants(b);
        if (vb != null && vb.subs != null) {
            for (String s : vb.subs) if (a.equals(s)) return true;
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════
    // CSS
    // ═════════════════════════════════════════════════════════════════

    private static final String STYLE = """
            <style>
                .title-label {
                    font-weight: bold;
                    color: #bdcbd3;
                    font-size: 18;
                    padding-top: 8;
                    padding-bottom: 6;
                }
                .section-label {
                    font-weight: bold;
                    color: #bdcbd3;
                    font-size: 16;
                    padding-top: 4;
                    padding-bottom: 4;
                }
                .info-label {
                    padding-top: 4;
                    padding-bottom: 4;
                    color: #a0b8c8;
                    font-size: 12;
                }
                .hint-label {
                    color: #7a9aaa;
                    font-size: 11;
                    padding-top: 6;
                    horizontal-align: center;
                }
                .input-count {
                    color: #bdcbd3;
                    font-size: 15;
                    font-weight: bold;
                    vertical-align: middle;
                    padding-left: 8;
                    padding-right: 8;
                }
                .separator {
                    layout-mode: Full;
                    anchor-height: 1;
                    background-color: #ffffff(0.15);
                    margin-top: 4;
                    margin-bottom: 4;
                }
                .vert-separator {
                    anchor-width: 1;
                    layout-mode: Full;
                    background-color: #ffffff(0.15);
                    margin-left: 6;
                    margin-right: 6;
                }
                .empty-slot {
                    anchor-width: 56;
                    anchor-height: 56;
                    margin-top: 4;
                    margin-right: 4;
                    margin-bottom: 4;
                    margin-left: 4;
                    background-color: #ffffff(0.06);
                }
                .mode-row {
                    layout-mode: Left;
                    horizontal-align: center;
                    padding-top: 6;
                    padding-bottom: 4;
                }
                .mode-btn {
                    anchor-width: 100;
                    anchor-height: 32;
                    font-size: 14;
                    font-weight: bold;
                    color: #a0b8c8;
                    margin-left: 6;
                    margin-right: 6;
                }
                .mode-btn-active {
                    anchor-width: 100;
                    anchor-height: 32;
                    font-size: 14;
                    font-weight: bold;
                    color: #bdcbd3;
                    margin-left: 6;
                    margin-right: 6;
                    background-color: #ffffff(0.12);
                }
                .mode-btn-disabled {
                    anchor-width: 100;
                    anchor-height: 32;
                    font-size: 14;
                    color: #555555;
                    margin-left: 6;
                    margin-right: 6;
                }
                .tab-row {
                    layout-mode: Left;
                    horizontal-align: center;
                    padding-top: 4;
                    padding-bottom: 2;
                }
                .item-icon {
                    anchor-width: 36;
                    anchor-height: 36;
                }
                .btn-grid {
                    layout-mode: Top;
                    padding-top: 4;
                    padding-bottom: 4;
                    horizontal-align: center;
                }
                .page-row {
                    layout-mode: Left;
                    horizontal-align: center;
                    padding-top: 4;
                    padding-bottom: 4;
                }
                .page-btn {
                    anchor-width: 30;
                    anchor-height: 26;
                    font-size: 14;
                    font-weight: bold;
                    color: #a0b8c8;
                    margin-left: 6;
                    margin-right: 6;
                }
                .page-btn-disabled {
                    anchor-width: 30;
                    anchor-height: 26;
                    font-size: 14;
                    color: #555555;
                    margin-left: 6;
                    margin-right: 6;
                }
                .page-label {
                    color: #bdcbd3;
                    font-size: 13;
                    padding-left: 8;
                    padding-right: 8;
                    vertical-align: middle;
                }
                .mode-switch {
                    anchor-width: 120;
                    anchor-height: 28;
                    font-size: 12;
                    color: #a0b8c8;
                    margin-top: 4;
                    margin-bottom: 4;
                    margin-left: 6;
                    margin-right: 6;
                    padding-top: 2;
                    padding-bottom: 2;
                    padding-left: 6;
                    padding-right: 6;
                }
            </style>
            """;

    // ═════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════

    private static String prettifyBlockKey(String key) {
        if (key == null || key.isEmpty()) return "Unknown";
        int c = key.indexOf(':');
        String local = (c >= 0) ? key.substring(c + 1) : key;
        return local.replace('_', ' ');
    }

    private static String canonicalType(String type, String[] knownTypes) {
        if (type == null || knownTypes == null) return type;
        for (String knownType : knownTypes) {
            if (knownType.equalsIgnoreCase(type)) {
                return knownType;
            }
        }
        return type;
    }

    private static List<String> discoverVanillaRockBlocksForUi(String rockType) {
        if (rockType == null || rockType.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (String suffix : VanillaCompat.getRockNaturalSuffixes()) {
            addIfExists(found, "Rock_" + rockType + suffix);
            if (VanillaCompat.isMetalType(rockType)) {
                addIfExists(found, "Metal_" + rockType + suffix);
            }
            if (!suffix.isEmpty()) {
                addIfExists(found, rockType + suffix);
            }
        }
        return new ArrayList<>(found);
    }

    private static List<String> discoverVanillaWoodBlocksForUi(String woodType) {
        if (woodType == null || woodType.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (String suffix : VanillaCompat.getVanillaWoodSuffixes()) {
            if (VanillaCompat.isWoodRoofSuffix(suffix)) {
                continue;
            }
            addIfExists(found, "Wood_" + woodType + suffix);
        }
        return new ArrayList<>(found);
    }

    private static ChiselVariants normalizeVariantBuckets(String[] subs,
                                                          String[] stairs,
                                                          String[] halfs,
                                                          String[] roofs) {
        LinkedHashSet<String> stairSet = new LinkedHashSet<>();
        LinkedHashSet<String> halfSet = new LinkedHashSet<>();
        LinkedHashSet<String> roofSet = new LinkedHashSet<>();
        LinkedHashSet<String> blockSet = new LinkedHashSet<>();

        addAll(stairSet, stairs);
        addAll(halfSet, halfs);
        addAll(roofSet, roofs);

        if (subs != null) {
            for (String key : subs) {
                if (key == null || key.isEmpty()) continue;
                if (isRoofVariant(key)) {
                    roofSet.add(key);
                } else if (isHalfVariant(key)) {
                    halfSet.add(key);
                } else if (isStairVariant(key)) {
                    stairSet.add(key);
                } else {
                    blockSet.add(key);
                }
            }
        }

        removeAll(blockSet, stairSet);
        removeAll(blockSet, halfSet);
        removeAll(blockSet, roofSet);

        return new ChiselVariants(
                blockSet.toArray(new String[0]),
                stairSet.toArray(new String[0]),
                halfSet.toArray(new String[0]),
                roofSet.toArray(new String[0]));
    }

    private static String[]  safe(String[] a)  { return a != null ? a : new String[0]; }
    private static int       len(String[] a)   { return a != null ? a.length : 0; }
    private static boolean   empty(String[] a) { return a == null || a.length == 0; }
    private static String    first(String[] a) { return (a != null && a.length > 0) ? a[0] : null; }

    private static boolean isStairVariant(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("_stairs")
                || lower.contains("_stairs_")
                || lower.startsWith("mcw_stairs_");
    }

    private static boolean isHalfVariant(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("_half")
                || lower.endsWith("_slab")
                || lower.endsWith("_slabs")
                || lower.contains("_half_");
    }

    private static boolean isRoofVariant(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("_roof")
                || lower.contains("_shingle");
    }

    private static void addIfExists(Set<String> dest, String key) {
        if (key != null && !key.isEmpty()
                && com.Ev0sMods.Ev0sChisel.compat.BlockTypeCache.exists(key)) {
            dest.add(key);
        }
    }

    private static void addAll(Set<String> dest, String[] arr) {
        if (arr == null) return;
        for (String key : arr) {
            if (key != null && !key.isEmpty()) {
                dest.add(key);
            }
        }
    }

    private static void removeAll(Set<String> dest, Set<String> toRemove) {
        for (String key : toRemove) {
            dest.remove(key);
        }
    }

    private static String[] filterByRockType(String[] arr, String rockType) {
        if (arr == null || rockType == null) return arr;
        String matchPrefix = "rock_" + rockType.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String s : arr) {
            if (s == null) continue; // Ignore missing/null keys
            String lower = s.toLowerCase(Locale.ROOT);
            if (lower.startsWith("rock_")) {
                if (lower.equals(matchPrefix)
                        || lower.startsWith(matchPrefix + "_"))
                    filtered.add(s);
            } else {
                filtered.add(s);
            }
        }
        return filtered.toArray(new String[0]);
    }

    private static String[] merge(String[] base, List<String> extra) {
        if (extra == null || extra.isEmpty()) return base;
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (base != null) Collections.addAll(set, base);
        set.addAll(extra);
        return set.toArray(new String[0]);
    }

    // Best-effort: mark inventory/store as changed using reflection (replacement for HytaleCompat.markInventoryChanged)
    private static void markInventoryChanged(Inventory inv) {
        if (inv == null) return;
        try {
            java.lang.Class<?> c = inv.getClass();
            for (java.lang.reflect.Method m : c.getMethods()) {
                String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                if ((n.contains("mark") || n.contains("notify") || n.contains("changed")) && m.getParameterCount() == 0) {
                    try { m.invoke(inv); return; } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }
}
