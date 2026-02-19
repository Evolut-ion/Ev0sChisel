package com.Ev0sMods.Ev0sChisel.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.compat.CarpentryCompat;
import com.Ev0sMods.Ev0sChisel.compat.MacawCompat;
import com.Ev0sMods.Ev0sChisel.compat.MasonryCompat;
import com.Ev0sMods.Ev0sChisel.compat.StoneworksCompat;
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

    /** The two sub-pages the player can switch between. */
    public enum Mode { CHISEL, TABLE }

    /** Output tab types inside the UI. */
    public enum Tab { BLOCKS, STAIRS, HALF_SLABS, ROOFING }

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
                            Tab activeTab,
                            int outputPage,
                            int invPage) {

        boolean hasChiselData = !empty(chiselSubs) || !empty(chiselStairs)
                || !empty(chiselHalfs) || !empty(chiselRoofs);

        // ── Resolve output variant arrays based on mode ─────────────
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

        boolean hasBlocks = len(outSubs) > 0;
        boolean hasStairs = len(outStairs) > 0;
        boolean hasHalfs  = len(outHalfs) > 0;
        boolean hasRoofs  = len(outRoofs) > 0;

        String[] allOutputs = switch (activeTab) {
            case BLOCKS     -> safe(outSubs);
            case STAIRS     -> safe(outStairs);
            case HALF_SLABS -> safe(outHalfs);
            case ROOFING    -> safe(outRoofs);
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
                    hasChiselData, curOutPg, totalOutPg);
        } else {
            html = buildTableHtml(pgInv, invStart, pgOut, outStart,
                    inputKey, inputCount, activeTab,
                    hasBlocks, iconBlocks, hasStairs, iconStairs,
                    hasHalfs, iconHalfs, hasRoofs, iconRoofs,
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
                        WorldChunk chunk = world.getChunkIfInMemory(
                                ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
                        if (chunk != null) {
                            chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                            LOGGER.atInfo().log("[Chisel] Set block at "
                                    + blockPos + " to " + blockKey);
                        }
                    } catch (Throwable t) {
                        LOGGER.atWarning().log("[Chisel] Failed to set block: "
                                + t.getMessage());
                    }
                });
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
        LOGGER.atInfo().log("[Chisel] mode=" + mode + " tab=" + activeTab
                + " outPg=" + (curOutPg + 1) + "/" + totalOutPg
                + (mode == Mode.TABLE
                    ? " invPg=" + (curInvPg + 1) + "/" + totalInvPg
                    : ""));
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
        open(playerRef, store, world, blockPos, player,
                subs, stairs, halfs, roofs,
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
                                          boolean hasChiselData,
                                          int curOutPg, int totalOutPg) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildModeToggle(Mode.CHISEL, hasChiselData));
        sb.append("<div class=\"separator\"></div>\n");

        sb.append("<p class=\"title-label\">Choose a Block</p>\n");
        sb.append("<p class=\"info-label\">Select a variant to chisel this block into.</p>\n");

        sb.append(buildTabRow(hasBlocks, iconBlocks, hasStairs, iconStairs,
                hasHalfs, iconHalfs, hasRoofs, iconRoofs, activeTab));
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

        if (inputKey != null && (hasBlocks || hasStairs || hasHalfs || hasRoofs))
            sb.append(buildTabRow(hasBlocks, iconBlocks, hasStairs, iconStairs,
                    hasHalfs, iconHalfs, hasRoofs, iconRoofs, activeTab));

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
                                      Tab activeTab) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"tab-row\">\n");
        if (hasBlocks) appendIconTab(sb, "tab_blocks",    iconBlocks, "Blocks",     activeTab == Tab.BLOCKS);
        if (hasStairs) appendIconTab(sb, "tab_stairs",    iconStairs, "Stairs",     activeTab == Tab.STAIRS);
        if (hasHalfs)  appendIconTab(sb, "tab_halfslabs", iconHalfs,  "Half Slabs", activeTab == Tab.HALF_SLABS);
        if (hasRoofs)  appendIconTab(sb, "tab_roofing",   iconRoofs,  "Roofing",    activeTab == Tab.ROOFING);
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
            LOGGER.atInfo().log("[Chisel] Read " + items.size()
                    + " block items from inventory");
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
            inv.markChanged();
            LOGGER.atInfo().log("[Chisel] Converted " + convertCount + "x "
                    + inputKey + " → " + outputKey
                    + " (remaining=" + remaining + ")");
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Chisel] Conversion failed: "
                    + t.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Chisel variant resolution
    // ═════════════════════════════════════════════════════════════════

    private static ChiselVariants resolveChiselVariants(String blockKey) {
        BlockType bt = BlockType.fromString(blockKey);
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
                subs   = merge(subs,   CarpentryCompat.getVariants(woodType));
                stairs = merge(stairs, CarpentryCompat.getStairVariants(woodType));
                halfs  = merge(halfs,  CarpentryCompat.getHalfVariants(woodType));
            }
        }

        if (empty(stairs) && !empty(subs))
            stairs = MasonryCompat.deriveExistingVariants(subs, "_Stairs");
        if (empty(halfs) && !empty(subs))
            halfs  = MasonryCompat.deriveExistingVariants(subs, "_Half");
        if (empty(roofs) && !empty(subs)) {
            roofs = MasonryCompat.deriveExistingRoofing(subs);
            // For wood blocks the standard derivation misses shingle variants;
            // merge in the wood-aware derivation as well.
            if (empty(roofs))
                roofs = com.Ev0sMods.Ev0sChisel.compat.VanillaCompat.deriveExistingWoodRoofing(subs);
        }

        if (empty(subs) && empty(stairs) && empty(halfs) && empty(roofs))
            return null;

        return new ChiselVariants(safe(subs), safe(stairs),
                safe(halfs), safe(roofs));
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

    private static String[]  safe(String[] a)  { return a != null ? a : new String[0]; }
    private static int       len(String[] a)   { return a != null ? a.length : 0; }
    private static boolean   empty(String[] a) { return a == null || a.length == 0; }
    private static String    first(String[] a) { return (a != null && a.length > 0) ? a[0] : null; }

    private static String[] filterByRockType(String[] arr, String rockType) {
        if (arr == null || rockType == null) return arr;
        String matchPrefix = "rock_" + rockType.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String s : arr) {
            if (s == null) continue;
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
}
