package com.Ev0sMods.Ev0sChisel.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * UI page for the Carpenter's Hammer.
 * <p>
 * Shows five furniture category tabs (Chair, Table, Storage, Window, Light).
 * Clicking a variant in the active tab replaces the targeted block in-world.
 * Pagination is supported when a tab has more than {@value #ITEMS_PER_PAGE} items.
 */
public final class CarpenterHammerUIPage {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** The five furniture tabs. */
    public enum Tab { CHAIR, TABLE, STORAGE, WINDOW, LIGHT }

    private static final int GRID_COLUMNS   = 4;
    private static final int ITEMS_PER_PAGE = GRID_COLUMNS * 3; // 12 per page

    private static final String STYLE = """
            <style>
                .title-label {
                    font-weight: bold;
                    color: #bdcbd3;
                    font-size: 18;
                    padding-top: 8;
                    padding-bottom: 6;
                }
                .info-label {
                    padding-top: 4;
                    padding-bottom: 4;
                    color: #a0b8c8;
                    font-size: 12;
                }
                .separator {
                    layout-mode: Full;
                    anchor-height: 1;
                    background-color: #ffffff(0.15);
                    margin-top: 4;
                    margin-bottom: 4;
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
            </style>
            """;

    private CarpenterHammerUIPage() {}

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Opens (or re-opens) the Carpenter's Hammer UI for {@code playerRef}.
     *
     * @param chairs   block keys for the Chair tab
     * @param tables   block keys for the Table tab
     * @param storage  block keys for the Storage tab
     * @param windows  block keys for the Window tab
     * @param lights   block keys for the Light tab
     * @param activeTab the tab to show first
     * @param outputPage 0-based page index for the active tab's grid
     */
    public static void openHammer(
            PlayerRef playerRef,
            Store<EntityStore> store,
            World world,
            Vector3i blockPos,
            LivingEntity player,
            String[] chairs,
            String[] tables,
            String[] storage,
            String[] windows,
            String[] lights,
            Tab activeTab,
            int outputPage) {

        // Cache chunk once for event handlers
        final WorldChunk cachedChunk = world.getChunkIfInMemory(
                ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));

        // ── Resolve active-tab items ──────────────────────────────────────
        String[] allItems = switch (activeTab) {
            case CHAIR   -> safe(chairs);
            case TABLE   -> safe(tables);
            case STORAGE -> safe(storage);
            case WINDOW  -> safe(windows);
            case LIGHT   -> safe(lights);
        };

        int total       = allItems.length;
        int totalPages  = Math.max(1, (total + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int curPage     = Math.max(0, Math.min(outputPage, totalPages - 1));
        int start       = curPage * ITEMS_PER_PAGE;
        int end         = Math.min(start + ITEMS_PER_PAGE, total);
        String[] pgOut  = new String[end - start];
        if (pgOut.length > 0)
            System.arraycopy(allItems, start, pgOut, 0, pgOut.length);

        // Tab presence flags
        boolean hasChairs   = hasItems(chairs);
        boolean hasTables   = hasItems(tables);
        boolean hasStorage  = hasItems(storage);
        boolean hasWindows  = hasItems(windows);
        boolean hasLights   = hasItems(lights);

        // ── Build HTML ────────────────────────────────────────────────────
        String html = buildHtml(
                pgOut, start, activeTab,
                hasChairs,  first(chairs),
                hasTables,  first(tables),
                hasStorage, first(storage),
                hasWindows, first(windows),
                hasLights,  first(lights),
                curPage, totalPages);

        // ── Assemble page ─────────────────────────────────────────────────
        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

        final int fPage = curPage;

        // Tab switch handlers
        if (hasChairs && activeTab != Tab.CHAIR)
            builder.addEventListener("tab_chair", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, Tab.CHAIR, 0));
        if (hasTables && activeTab != Tab.TABLE)
            builder.addEventListener("tab_table", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, Tab.TABLE, 0));
        if (hasStorage && activeTab != Tab.STORAGE)
            builder.addEventListener("tab_storage", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, Tab.STORAGE, 0));
        if (hasWindows && activeTab != Tab.WINDOW)
            builder.addEventListener("tab_window", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, Tab.WINDOW, 0));
        if (hasLights && activeTab != Tab.LIGHT)
            builder.addEventListener("tab_light", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, Tab.LIGHT, 0));

        // Pagination handlers
        if (curPage > 0)
            builder.addEventListener("out_prev", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, activeTab, fPage - 1));
        if (curPage < totalPages - 1)
            builder.addEventListener("out_next", CustomUIEventBindingType.Activating,
                    (i, c) -> openHammer(playerRef, store, world, blockPos, player,
                            chairs, tables, storage, windows, lights, activeTab, fPage + 1));

        // Output item click → replace targeted block
        for (int i = 0; i < pgOut.length; i++) {
            final String blockKey = pgOut[i];
            final String btnId    = "out_" + (start + i);
            builder.addEventListener(btnId, CustomUIEventBindingType.Activating,
                    (ignored, ctx) -> {
                        try {
                            if (cachedChunk != null)
                                cachedChunk.setBlock(blockPos.x, blockPos.y, blockPos.z, blockKey);
                        } catch (Throwable t) {
                            LOGGER.atWarning().log("[CarpenterHammer] Failed to set block: " + t.getMessage());
                        }
                    });
        }

        builder.open(store);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML builder
    // ─────────────────────────────────────────────────────────────────────

    private static String buildHtml(
            String[] pageOut, int outStart,
            Tab activeTab,
            boolean hasChairs,  String iconChair,
            boolean hasTables,  String iconTable,
            boolean hasStorage, String iconStorage,
            boolean hasWindows, String iconWindow,
            boolean hasLights,  String iconLight,
            int curPage, int totalPages) {

        StringBuilder sb = new StringBuilder(4096);

        sb.append("<p class=\"title-label\">Carpenter's Hammer</p>\n");
        sb.append("<p class=\"info-label\">Select a variant to replace this block.</p>\n");

        // ── Tab bar (icon buttons, same pattern as ChiselUIPage) ─────────
        sb.append("<div class=\"tab-row\">\n");
        if (hasChairs)  appendIconTab(sb, "tab_chair",   iconChair,   "Chair",   activeTab == Tab.CHAIR);
        if (hasTables)  appendIconTab(sb, "tab_table",   iconTable,   "Table",   activeTab == Tab.TABLE);
        if (hasStorage) appendIconTab(sb, "tab_storage", iconStorage, "Storage", activeTab == Tab.STORAGE);
        if (hasWindows) appendIconTab(sb, "tab_window",  iconWindow,  "Window",  activeTab == Tab.WINDOW);
        if (hasLights)  appendIconTab(sb, "tab_light",   iconLight,   "Light",   activeTab == Tab.LIGHT);
        sb.append("</div>\n");
        sb.append("<div class=\"separator\"></div>\n");

        // ── Item grid ─────────────────────────────────────────────────────
        if (pageOut.length == 0) {
            sb.append("<p class=\"info-label\">No furniture found for this category.</p>\n");
        } else {
            sb.append("<div class=\"btn-grid\">\n");
            for (int i = 0; i < pageOut.length; i++) {
                if (i % GRID_COLUMNS == 0)
                    sb.append("  <div style=\"layout-mode: Left; horizontal-align: center; padding-top: 2; padding-bottom: 2;\">\n");
                String key  = pageOut[i];
                String name = friendlyName(key);
                sb.append(String.format(
                        "    <button id=\"out_%d\" data-hyui-tooltiptext=\"%s\" "
                        + "style=\"anchor-width: 52; anchor-height: 52; padding: 6; margin-top: 4; margin-bottom: 4; margin-left: 4; margin-right: 4;\">"
                        + "<span class=\"item-icon\" data-hyui-item-id=\"%s\" "
                        + "style=\"anchor-width: 40; anchor-height: 40;\"></span>"
                        + "</button>\n",
                        outStart + i, name, key));
                if (i % GRID_COLUMNS == GRID_COLUMNS - 1 || i == pageOut.length - 1)
                    sb.append("  </div>\n");
            }
            sb.append("</div>\n");
        }

        // ── Pagination controls ───────────────────────────────────────────
        if (totalPages > 1) {
            sb.append("<div class=\"page-row\">\n");
            sb.append(curPage > 0
                    ? "  <button id=\"out_prev\" class=\"page-btn\">&lt;</button>\n"
                    : "  <button class=\"page-btn-disabled\">&lt;</button>\n");
            sb.append(String.format("  <p class=\"page-label\">%d / %d</p>\n", curPage + 1, totalPages));
            sb.append(curPage < totalPages - 1
                    ? "  <button id=\"out_next\" class=\"page-btn\">&gt;</button>\n"
                    : "  <button class=\"page-btn-disabled\">&gt;</button>\n");
            sb.append("</div>\n");
        }

        return STYLE + String.format("""
                <div style="layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;">
                    <div class="decorated-container" data-hyui-title="Carpenter's Hammer"
                             style="anchor-width: 660; margin-right: 40; vertical-align: middle;">
                        <div class="container-contents" style="layout-mode: Top; padding-top: 12; padding-bottom: 12; padding-left: 24; padding-right: 24;">
                %s
                        </div>
                    </div>
                </div>
                """, sb.toString());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Appends a single icon-based tab button. Active tab gets a highlight
     * background; the {@code id} is used by HyUI event listeners.
     */
    private static void appendIconTab(StringBuilder sb, String id,
                                       String iconBlockId, String tooltip,
                                       boolean active) {
        String bgStyle = active ? " background-color: #ffffff(0.12);" : "";
        String baseStyle = "anchor-width: 52; anchor-height: 52; padding: 4; margin-left: 4; margin-right: 4;" + bgStyle;
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
                    id, baseStyle, tooltip, tooltip));
        }
    }

    /**
     * Converts a block key like {@code Gui_LogChair} to a readable name
     * by stripping common mod prefixes and replacing underscores with spaces.
     */
    private static String friendlyName(String key) {
        if (key == null || key.isEmpty()) return key;
        String s = key.replaceFirst("^[A-Za-z]+_", "");
        return s.replace('_', ' ').trim();
    }

    private static boolean hasItems(String[] arr) {
        return arr != null && arr.length > 0;
    }

    private static String first(String[] arr) {
        return (arr != null && arr.length > 0) ? arr[0] : null;
    }

    private static String[] safe(String[] arr) {
        return arr != null ? arr : new String[0];
    }
}
