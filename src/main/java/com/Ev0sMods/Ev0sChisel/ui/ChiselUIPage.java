package com.Ev0sMods.Ev0sChisel.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Chisel UI page – shows available substitution blocks as icon buttons
 * organised into four tabs: Blocks, Stairs, Half Slabs, Roofing.
 * Supports pagination when the number of items exceeds one page.
 */
public final class ChiselUIPage {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** The four categories the UI can display. */
    public enum Tab { BLOCKS, STAIRS, HALF_SLABS, ROOFING }

    private ChiselUIPage() {} // utility class

    // ─────────────────────────────────────────────────────────────────────
    // Grid / pagination constants
    // ─────────────────────────────────────────────────────────────────────

    private static final int GRID_COLUMNS  = 5;
    private static final int GRID_ROWS     = 5;
    private static final int ITEMS_PER_PAGE = GRID_COLUMNS * GRID_ROWS; // 25

    // ─────────────────────────────────────────────────────────────────────
    // Representative block icons for each tab
    // ─────────────────────────────────────────────────────────────────────

    private static final String ICON_BLOCKS    = "Rock_Stone";
    private static final String ICON_STAIRS    = "Rock_Stone_Brick_Stairs";
    private static final String ICON_HALFSLABS = "Rock_Stone_Brick_Half";
    private static final String ICON_ROOFING   = "Rock_Stone_Brick_Roof";

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Opens (or re-opens) the chisel UI for the given player, showing the
     * items from the selected tab at the given page.
     *
     * @param playerRef     the player who is using the chisel
     * @param store         entity store (needed by PageBuilder)
     * @param blockPos      the world position of the block being chiselled
     * @param world         the world instance (used to write the new block)
     * @param substitutions full-block substitution keys (may be empty/null)
     * @param stairs        stair block keys (may be empty/null)
     * @param halfSlabs     half-slab block keys (may be empty/null)
     * @param roofing       roofing block keys (may be empty/null)
     * @param activeTab     the tab to display
     * @param page          zero-based page index
     */
    public static void open(PlayerRef playerRef,
                            Store<EntityStore> store,
                            Vector3i blockPos,
                            World world,
                            String[] substitutions,
                            String[] stairs,
                            String[] halfSlabs,
                            String[] roofing,
                            Tab activeTab,
                            int page) {

        // Pick the items for the active tab
        String[] allItems = switch (activeTab) {
            case BLOCKS    -> safe(substitutions);
            case STAIRS    -> safe(stairs);
            case HALF_SLABS -> safe(halfSlabs);
            case ROOFING   -> safe(roofing);
        };

        // ── Pagination ──────────────────────────────────────────────────
        int totalItems = allItems.length;
        int totalPages = Math.max(1, (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx   = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);

        // Slice the items for the current page
        String[] pageItems = new String[endIdx - startIdx];
        System.arraycopy(allItems, startIdx, pageItems, 0, pageItems.length);

        boolean hasBlocks = len(substitutions) > 0;
        boolean hasStairs = len(stairs) > 0;
        boolean hasHalfs  = len(halfSlabs) > 0;
        boolean hasRoofs  = len(roofing) > 0;

        String html = buildHtml(pageItems, startIdx, activeTab,
                hasBlocks, hasStairs, hasHalfs, hasRoofs,
                currentPage, totalPages);

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

        // ── Tab button handlers (re-open with page 0) ───────────────────
        if (hasBlocks && activeTab != Tab.BLOCKS) {
            builder.addEventListener("tab_blocks", CustomUIEventBindingType.Activating, (ignored, ctx) ->
                    open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, Tab.BLOCKS, 0));
        }
        if (hasStairs && activeTab != Tab.STAIRS) {
            builder.addEventListener("tab_stairs", CustomUIEventBindingType.Activating, (ignored, ctx) ->
                    open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, Tab.STAIRS, 0));
        }
        if (hasHalfs && activeTab != Tab.HALF_SLABS) {
            builder.addEventListener("tab_halfslabs", CustomUIEventBindingType.Activating, (ignored, ctx) ->
                    open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, Tab.HALF_SLABS, 0));
        }
        if (hasRoofs && activeTab != Tab.ROOFING) {
            builder.addEventListener("tab_roofing", CustomUIEventBindingType.Activating, (ignored, ctx) ->
                    open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, Tab.ROOFING, 0));
        }

        // ── Pagination handlers ──────────────────────────────────────────
        if (currentPage > 0) {
            builder.addEventListener("page_prev", CustomUIEventBindingType.Activating, (ignored, ctx) ->
                    open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, activeTab, currentPage - 1));
        }
        if (currentPage < totalPages - 1) {
            builder.addEventListener("page_next", CustomUIEventBindingType.Activating, (ignored, ctx) ->
                    open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, activeTab, currentPage + 1));
        }

        // ── Item click handlers ──────────────────────────────────────────
        for (int i = 0; i < pageItems.length; i++) {
            final String blockKey = pageItems[i];
            final String btnId = "sub_" + (startIdx + i);

            builder.addEventListener(btnId, CustomUIEventBindingType.Activating, (ignored, ctx) -> {
                try {
                    WorldChunk chunk = world.getChunkIfInMemory(
                            ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
                    if (chunk != null) {
                        chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                        LOGGER.atInfo().log("ChiselUI: set block at " + blockPos + " to " + blockKey);
                    }
                } catch (Throwable t) {
                    LOGGER.atWarning().log("ChiselUI: failed to set block: " + t.getMessage());
                }
            });
        }

        builder.open(store);
        LOGGER.atInfo().log("ChiselUI: opened tab=" + activeTab + " page=" + (currentPage + 1) + "/" + totalPages + " with " + pageItems.length + " items");
    }

    /** Convenience overload – defaults to the BLOCKS tab, page 0. */
    public static void open(PlayerRef playerRef,
                            Store<EntityStore> store,
                            Vector3i blockPos,
                            World world,
                            String[] substitutions,
                            String[] stairs,
                            String[] halfSlabs,
                            String[] roofing,
                            Tab activeTab) {
        open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, activeTab, 0);
    }

    /** Convenience overload – defaults to the BLOCKS tab, page 0. */
    public static void open(PlayerRef playerRef,
                            Store<EntityStore> store,
                            Vector3i blockPos,
                            World world,
                            String[] substitutions,
                            String[] stairs,
                            String[] halfSlabs,
                            String[] roofing) {
        open(playerRef, store, blockPos, world, substitutions, stairs, halfSlabs, roofing, Tab.BLOCKS, 0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML layout
    // ─────────────────────────────────────────────────────────────────────

    private static String buildHtml(String[] pageItems, int startIdx, Tab activeTab,
                                    boolean hasBlocks, boolean hasStairs,
                                    boolean hasHalfs, boolean hasRoofs,
                                    int currentPage, int totalPages) {

        // ── Tab button row (icon-based) ─────────────────────────────────
        StringBuilder tabs = new StringBuilder();
        tabs.append("<div class=\"tab-row\">\n");
        if (hasBlocks) appendIconTab(tabs, "tab_blocks", ICON_BLOCKS, "Blocks", activeTab == Tab.BLOCKS);
        if (hasStairs) appendIconTab(tabs, "tab_stairs", ICON_STAIRS, "Stairs", activeTab == Tab.STAIRS);
        if (hasHalfs)  appendIconTab(tabs, "tab_halfslabs", ICON_HALFSLABS, "Half Slabs", activeTab == Tab.HALF_SLABS);
        if (hasRoofs)  appendIconTab(tabs, "tab_roofing", ICON_ROOFING, "Roofing", activeTab == Tab.ROOFING);
        tabs.append("</div>\n");

        // ── Icon grid (current page only) ───────────────────────────────
        StringBuilder grid = new StringBuilder();
        for (int i = 0; i < pageItems.length; i++) {
            if (i % GRID_COLUMNS == 0) {
                grid.append("<div style=\"layout-mode: Left; horizontal-alignment: Center; padding: 2 0;\">\n");
            }
            String displayName = prettifyBlockKey(pageItems[i]);
            grid.append(String.format(
                    "<button id=\"sub_%d\" "
                  + "data-hyui-tooltiptext=\"%s\" "
                  + "style=\"anchor-width: 52; anchor-height: 52; padding: 6; margin: 4;\">"
                  + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" style=\"anchor-width: 40; anchor-height: 40;\"></span>"
                  + "</button>\n",
                    startIdx + i, displayName, pageItems[i]));
            if (i % GRID_COLUMNS == GRID_COLUMNS - 1 || i == pageItems.length - 1) {
                grid.append("</div>\n");
            }
        }

        // ── Pagination bar ──────────────────────────────────────────────
        StringBuilder pager = new StringBuilder();
        if (totalPages > 1) {
            pager.append("<div class=\"page-row\">\n");
            if (currentPage > 0) {
                pager.append("  <button id=\"page_prev\" class=\"page-btn\">&lt;</button>\n");
            } else {
                pager.append("  <button class=\"page-btn-disabled\">&lt;</button>\n");
            }
            pager.append(String.format(
                    "  <p class=\"page-label\">%d / %d</p>\n", currentPage + 1, totalPages));
            if (currentPage < totalPages - 1) {
                pager.append("  <button id=\"page_next\" class=\"page-btn\">&gt;</button>\n");
            } else {
                pager.append("  <button class=\"page-btn-disabled\">&gt;</button>\n");
            }
            pager.append("</div>\n");
        }

        return """
                <style>
                    .title-label {
                        font-weight: bold;
                        color: #bdcbd3;
                        font-size: 18;
                        padding-top: 8;
                        padding-bottom: 6;
                    }
                    .info-label {
                        padding-top: 2;
                        padding-bottom: 6;
                        color: #a0b8c8;
                        font-size: 13;
                    }
                    .separator {
                        layout-mode: Full;
                        anchor-height: 1;
                        background-color: #ffffff(0.15);
                    }
                    .tab-row {
                        layout-mode: Left;
                        horizontal-alignment: Center;
                        padding: 6 0 2 0;
                    }
                    .tab-btn {
                        anchor-width: 48;
                        anchor-height: 48;
                        padding: 4;
                        margin: 2 4;
                    }
                    .tab-btn-active {
                        anchor-width: 48;
                        anchor-height: 48;
                        padding: 4;
                        margin: 2 4;
                        background-color: #ffffff(0.12);
                    }
                    .btn-grid {
                        layout-mode: Top;
                        padding-top: 8;
                        padding-bottom: 4;
                        horizontal-alignment: Center;
                    }
                    .page-row {
                        layout-mode: Left;
                        horizontal-alignment: Center;
                        padding: 4 0;
                    }
                    .page-btn {
                        anchor-width: 30;
                        anchor-height: 26;
                        font-size: 14;
                        font-weight: bold;
                        color: #a0b8c8;
                        margin: 0 6;
                    }
                    .page-btn-disabled {
                        anchor-width: 30;
                        anchor-height: 26;
                        font-size: 14;
                        color: #555555;
                        margin: 0 6;
                    }
                    .page-label {
                        color: #bdcbd3;
                        font-size: 13;
                        padding: 0 8;
                        vertical-alignment: Center;
                    }
                </style>
                <div style="layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;">
                    <div class="decorated-container" data-hyui-title="Chisel" style="anchor-width: 400; anchor-height: 600; margin-right: 40; vertical-alignment: Center;">
                        <div class="container-contents" style="layout-mode: Top; padding: 12 24;">

                            <p class="title-label">Choose a Block</p>
                            <p class="info-label">Select a variant to chisel this block into.</p>

                %s

                            <div class="separator"></div>

                            <div class="btn-grid">
                %s
                            </div>

                %s

                        </div>
                    </div>
                </div>
                """.formatted(tabs.toString(), grid.toString(), pager.toString());
    }

    /** Appends an icon-based tab button using a block item icon. */
    private static void appendIconTab(StringBuilder sb, String id, String iconBlockId,
                                       String tooltip, boolean active) {
        String cls = active ? "tab-btn-active" : "tab-btn";
        sb.append(String.format(
                "  <button id=\"%s\" class=\"%s\" data-hyui-tooltiptext=\"%s\">"
              + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" style=\"anchor-width: 36; anchor-height: 36;\"></span>"
              + "</button>\n",
                id, cls, tooltip, iconBlockId));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static String prettifyBlockKey(String key) {
        if (key == null || key.isEmpty()) return "Unknown";
        int colonIdx = key.indexOf(':');
        String local = (colonIdx >= 0) ? key.substring(colonIdx + 1) : key;
        return local.replace('_', ' ');
    }

    private static String[] safe(String[] arr) { return arr != null ? arr : new String[0]; }
    private static int len(String[] arr) { return arr != null ? arr.length : 0; }
}
