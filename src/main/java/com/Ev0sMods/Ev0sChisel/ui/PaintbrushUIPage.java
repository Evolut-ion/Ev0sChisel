package com.Ev0sMods.Ev0sChisel.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.LivingEntity;

import java.util.Locale;

/** Minimal paintbrush UI: displays color variants and applies selected variant to the targeted block. */
public final class PaintbrushUIPage {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void openPaintbrush(PlayerRef playerRef, Store<EntityStore> store, World world, Vector3i blockPos, LivingEntity player, String[] variants) {
        if (variants == null) variants = new String[0];
        final String[] variantList = variants;
        HytaleLogger.forEnclosingClass().atInfo().log("[PaintbrushUI] Building UI for variants: " + java.util.Arrays.toString(variants));

        // determine how many Dye_Base the player has
        int dyeCount = 0;
        try {
            Inventory inv = player.getInventory();
            if (inv != null) {
                ItemContainer hot = inv.getHotbar();
                ItemContainer stor = inv.getStorage();
                if (hot != null) {
                    short cap = hot.getCapacity();
                    for (short s = 0; s < cap; s++) {
                        ItemStack st = hot.getItemStack(s);
                        if (st != null && !st.isEmpty() && "Dye_Base".equals(st.getItemId()))
                            dyeCount += st.getQuantity();
                    }
                }
                if (stor != null) {
                    short cap = stor.getCapacity();
                    for (short s = 0; s < cap; s++) {
                        ItemStack st = stor.getItemStack(s);
                        if (st != null && !st.isEmpty() && "Dye_Base".equals(st.getItemId()))
                            dyeCount += st.getQuantity();
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.atWarning().log("[Paintbrush] Failed to read inventory for dye count: " + t.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"title-label\">Paintbrush</p>\n");
        sb.append("<p class=\"info-label\">Select a color variant to apply.</p>\n");
        sb.append(String.format("<div style=\"layout-mode: Left; vertical-align: middle; padding-top:6; padding-bottom:6;\">"
                + "<span class=\"item-icon\" data-hyui-item-id=\"Dye_Base\" style=\"anchor-width:24;anchor-height:24;\"></span>"
                + "<p class=\"input-count\">Dye Base: %d</p></div>\n", dyeCount));
        sb.append("<div class=\"separator\"></div>\n");
        sb.append("<div class=\"btn-grid\">\n");
        // create rows of up to 9 icons each so they render as a grid
        for (int i = 0; i < variants.length; i++) {
            if (i % 9 == 0) sb.append("  <div class=\"variant-row\">\n");
            String key = variants[i];
            sb.append(String.format("    <button id=\"vb_%d\" class=\"variant-btn\">", i));
            sb.append(String.format("<span class=\"item-icon\" data-hyui-item-id=\"%s\"></span>", key));
            sb.append("</button>\n");
            if (i % 9 == 8 || i == variants.length - 1) sb.append("  </div>\n");
        }
        sb.append("</div>\n");

        String html = STYLE + String.format("<div style=\"layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;\">\n" +
                "  <div class=\"decorated-container\" data-hyui-title=\"Paintbrush\" style=\"anchor-width:480; margin-right: 40; vertical-align: middle;\">\n" +
                "    <div class=\"container-contents\" style=\"layout-mode: Top; padding:12;\">%s</div>\n" +
                "  </div>\n" +
                "</div>", sb.toString());

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

        // register buttons
        HytaleLogger.forEnclosingClass().atInfo().log("[PaintbrushUI] Registering " + variants.length + " buttons");
        for (int i = 0; i < variants.length; i++) {
            final int idx = i;
            final String key = variants[i];
            builder.addEventListener("vb_" + i, CustomUIEventBindingType.Activating, (e, ctx) -> {
                try {
                    Inventory inv = player.getInventory();
                    if (inv == null) {
                        LOGGER.atInfo().log("[Paintbrush] Player inventory missing; not applying variant");
                        return;
                    }

                    // Find a slot that contains Dye_Base (do not consume yet)
                    ItemContainer foundContainer = null;
                    short foundSlot = -1;
                    int foundQty = 0;

                    ItemContainer hot = inv.getHotbar();
                    ItemContainer stor = inv.getStorage();
                    if (hot != null) {
                        short cap = hot.getCapacity();
                        for (short s = 0; s < cap; s++) {
                            ItemStack st = hot.getItemStack(s);
                            if (st != null && !st.isEmpty() && "Dye_Base".equals(st.getItemId())) {
                                foundContainer = hot; foundSlot = s; foundQty = st.getQuantity(); break;
                            }
                        }
                    }
                    if (foundContainer == null && stor != null) {
                        short cap = stor.getCapacity();
                        for (short s = 0; s < cap; s++) {
                            ItemStack st = stor.getItemStack(s);
                            if (st != null && !st.isEmpty() && "Dye_Base".equals(st.getItemId())) {
                                foundContainer = stor; foundSlot = s; foundQty = st.getQuantity(); break;
                            }
                        }
                    }

                    if (foundContainer == null) {
                        LOGGER.atInfo().log("[Paintbrush] Player lacks Dye_Base; not applying variant");
                        return;
                    }

                    // Ensure world chunk is loaded and apply the block first
                    WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
                    if (chunk == null) {
                        LOGGER.atInfo().log("[Paintbrush] Chunk not loaded; cannot apply variant");
                        return;
                    }

                    chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, key);

                    // Now consume one Dye_Base from the found slot
                    if (foundQty > 1) {
                        foundContainer.setItemStackForSlot(foundSlot, new ItemStack("Dye_Base", foundQty - 1));
                    } else {
                        foundContainer.setItemStackForSlot(foundSlot, null);
                    }
                    inv.markChanged();

                    LOGGER.atInfo().log("[Paintbrush] Applied variant " + key + " at " + blockPos + " (consumed Dye_Base)");

                    // refresh the UI so the dye count updates
                    try {
                        openPaintbrush(playerRef, store, world, blockPos, player, variantList);
                    } catch (Throwable t2) {
                        LOGGER.atWarning().log("[Paintbrush] Failed to refresh UI: " + t2.getMessage());
                    }
                } catch (Throwable t) {
                    LOGGER.atWarning().log("[Paintbrush] Failed to apply variant: " + t.getMessage());
                }
            });
        }

        builder.open(store);
    }

    public static void openTable(PlayerRef playerRef, Store<EntityStore> store, World world, Vector3i blockPos, LivingEntity player) {
        // Simple helper: open an empty Paintbrush page indicating no variants
        openPaintbrush(playerRef, store, world, blockPos, player, new String[0]);
    }

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
                .variant-row {
                    layout-mode: Center;
                    horizontal-align: center;
                    padding-top: 4;
                    padding-bottom: 4;
                }
                .variant-btn {
                    anchor-width: 32;
                    anchor-height: 32;
                    margin-left: 4;
                    margin-right: 4;
                    margin-top: 4;
                    margin-bottom: 4;
                    background-color: transparent;
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
}
