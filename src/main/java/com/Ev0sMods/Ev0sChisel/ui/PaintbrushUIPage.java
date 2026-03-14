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

import java.util.ArrayList;
import java.util.List;
import com.Ev0sMods.Ev0sChisel.compat.BlockTypeCache;
		
import com.Ev0sMods.Ev0sChisel.Chisel;
import com.Ev0sMods.Ev0sChisel.Paintbrush;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

/** Minimal paintbrush UI: displays color variants and applies selected variant to the targeted block. */
public final class PaintbrushUIPage {
    
	// Inventory item holder for table mode
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

	// Helper: collect block items from inventory
	private static void collectBlockItems(List<InvItem> dest, ItemContainer container, int section) {
		if (container == null) return;
		short capacity = container.getCapacity();
		for (short slot = 0; slot < capacity; slot++) {
			ItemStack stack = container.getItemStack(slot);
			if (stack == null || stack.isEmpty()) continue;
			String blockKey = stack.getItemId();
			if (blockKey != null && !blockKey.isEmpty())
				dest.add(new InvItem(stack.getItemId(), blockKey, stack.getQuantity(), slot, section));
		}
	}

	/** Opens paintbrush table mode for inventory conversion. */
	public static void openPaintbrushTable(PlayerRef playerRef, Store<EntityStore> store, World world, Vector3i blockPos, LivingEntity player, String[] variants) {
		Inventory inv = player.getInventory();
		if (inv == null) {
			openPaintbrush(playerRef, store, world, blockPos, player, variants);
			return;
		}
		List<InvItem> items = new ArrayList<>();
		collectBlockItems(items, inv.getHotbar(), 0);
		collectBlockItems(items, inv.getStorage(), 1);
		int dyeCount = getDyeBaseCount(inv);

		StringBuilder sb = new StringBuilder();
		sb.append("<p class=\"title-label\">Paintbrush Table</p>\n");
		sb.append("<p class=\"info-label\">Convert blocks in your inventory to color variants.\nEach conversion consumes 1 Dye_Base per item.</p>\n");
		sb.append(String.format("<div style=\"layout-mode: Left; vertical-align: middle; padding-top:6; padding-bottom:6;\">"
				+ "<span class=\"item-icon\" data-hyui-item-id=\"Dye_Base\" style=\"anchor-width:24;anchor-height:24;\"></span>"
				+ "<p class=\"input-count\">Dye Base: %d</p></div>\n", dyeCount));
		sb.append("<div class=\"separator\"></div>\n");

		// Inventory grid
		// Color Variants area (initially empty until a block is selected)
		sb.append("<div class=\"section-label\">Color Variants</div>\n");
		sb.append("<div class=\"btn-grid\">\n");
		sb.append("  <p class=\"info-label\">Select a block from the inventory below to view its color variants.</p>\n");
		sb.append("</div>\n");
		sb.append("<div class=\"separator\"></div>\n");

		// Inventory grid (bottom) – 9 columns x 4 rows
		int INV_COLUMNS = 9;
		int INV_ROWS = 4;
		int INV_PER_PAGE = INV_COLUMNS * INV_ROWS; // 36
		int showCount = Math.min(items.size(), INV_PER_PAGE);
		sb.append("<div class=\"section-label\">Inventory</div>\n");
		sb.append("<div class=\"btn-grid\">\n");
		for (int i = 0; i < showCount; i++) {
			if (i % INV_COLUMNS == 0) sb.append("  <div class=\"variant-row\">\n");
			InvItem item = items.get(i);
			sb.append(String.format("      <button id=\"inv_%d\" class=\"variant-btn\" data-hyui-tooltiptext=\"%s x%d\">", i, item.blockKey, item.count));
			sb.append(String.format("<span class=\"item-icon\" data-hyui-item-id=\"%s\"></span>", item.blockKey));
			sb.append("</button>\n");
			if (i % INV_COLUMNS == INV_COLUMNS - 1 || i == showCount - 1) sb.append("  </div>\n");
		}
		sb.append("</div>\n");

		String html = STYLE + String.format("<div style=\"layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;\">\n" +
			"  <div class=\"decorated-container\" data-hyui-title=\"Paintbrush Table\" style=\"anchor-width:660; margin-right: 40; vertical-align: middle;\">\n" +
			"    <div class=\"container-contents\" style=\"layout-mode: Top; padding-top: 8; padding-bottom: 8; padding-left: 12; padding-right: 12;\">%s</div>\n" +
			"  </div>\n" +
			"</div>", sb.toString());

		PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
				.fromHtml(html)
				.withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

		for (int i = 0; i < showCount; i++) {
			InvItem it = items.get(i);
			final String[] itemVars = resolveVariantsForBlock(it.blockKey);
			final InvItem fIt = it;
			builder.addEventListener("inv_" + i, CustomUIEventBindingType.Activating, (e, ctx) -> {
				openPaintbrushTableInput(playerRef, store, world, blockPos, player, itemVars.length > 0 ? itemVars : variants, fIt.blockKey, fIt.count, fIt.slot, fIt.section);
			});
		}

		final String[] variantsFinal = variants;
		// We intentionally do not render variant buttons in this view unless an
		// inventory item has been selected. Only register out_* listeners when
		// variant buttons are actually present.
		boolean renderedVariants = false; // openPaintbrushTable shows no variants initially
		if (renderedVariants) {
			for (int i = 0; i < variantsFinal.length; i++) {
				final int idx = i;
				builder.addEventListener("out_" + idx, CustomUIEventBindingType.Activating, (e, ctx) -> {
					String key = variantsFinal[idx];
					// Conversion logic handled in openPaintbrushTableInput
				});
				builder.addEventListener("out_" + idx, CustomUIEventBindingType.RightClicking, (e, ctx) -> {
					String key = variantsFinal[idx];
					// Conversion logic handled in openPaintbrushTableInput
				});
			}
		}

		builder.open(store);
	}

	// Helper: open table with input selected
	private static void openPaintbrushTableInput(PlayerRef playerRef, Store<EntityStore> store, World world, Vector3i blockPos, LivingEntity player, String[] variants, String inputKey, int inputCount, short inputSlot, int inputSection) {
		
        Inventory inv = player.getInventory();
		int dyeCount = getDyeBaseCount(inv);
		List<InvItem> items = new ArrayList<>();
		collectBlockItems(items, inv.getHotbar(), 0);
		collectBlockItems(items, inv.getStorage(), 1);
		StringBuilder sb = new StringBuilder();
		sb.append("<p class=\"title-label\">Paintbrush Table</p>\n");
		sb.append("<p class=\"info-label\">Convert blocks in your inventory to color variants.\nEach conversion consumes 1 Dye_Base per item.</p>\n");
		sb.append(String.format("<div style=\"layout-mode: Left; vertical-align: middle; padding-top:6; padding-bottom:6;\">"
				+ "<span class=\"item-icon\" data-hyui-item-id=\"Dye_Base\" style=\"anchor-width:24;anchor-height:24;\"></span>"
				+ "<p class=\"input-count\">Dye Base: %d</p></div>\n", dyeCount));
		sb.append("<div class=\"separator\"></div>\n");
		sb.append("<div class=\"section-label\">Input</div>\n");
		sb.append(String.format("<button id=\"clear_input\" class=\"variant-btn\" data-hyui-tooltiptext=\"Clear input\">"
				+ "<span class=\"item-icon\" data-hyui-item-id=\"%s\"></span></button>\n", inputKey));
		sb.append(String.format("<p class=\"input-count\">%s x%d</p>\n", inputKey, inputCount));
		sb.append("<div class=\"separator\"></div>\n");
		sb.append("<div class=\"section-label\">Color Variants</div>\n");
		sb.append("<div class=\"btn-grid\">\n");
		for (int i = 0; i < variants.length; i++) {
			if (i % 4 == 0) sb.append("  <div class=\"variant-row\">\n");
			String key = variants[i];
			sb.append(String.format("      <button id=\"out_%d\" class=\"variant-btn\" data-hyui-tooltiptext=\"%s\">", i, key));
			sb.append(String.format("<span class=\"item-icon\" data-hyui-item-id=\"%s\"></span>", key));
			sb.append("</button>\n");
			if (i % 4 == 3 || i == variants.length - 1) sb.append("  </div>\n");
		}
		sb.append("</div>\n");

		// Inventory grid (bottom) – 9 columns x 4 rows
		int INV_COLUMNS = 9;
		int INV_ROWS = 4;
		int INV_PER_PAGE = INV_COLUMNS * INV_ROWS; // 36
		int showCount = Math.min(items.size(), INV_PER_PAGE);
		sb.append("<div class=\"separator\"></div>\n");
		sb.append("<div class=\"section-label\">Inventory</div>\n");
		sb.append("<div class=\"btn-grid\">\n");
		for (int i = 0; i < showCount; i++) {
			if (i % INV_COLUMNS == 0) sb.append("  <div class=\"variant-row\">\n");
			InvItem item = items.get(i);
			sb.append(String.format("      <button id=\"inv_%d\" class=\"variant-btn\" data-hyui-tooltiptext=\"%s x%d\">", i, item.blockKey, item.count));
			sb.append(String.format("<span class=\"item-icon\" data-hyui-item-id=\"%s\"></span>", item.blockKey));
			sb.append("</button>\n");
			if (i % INV_COLUMNS == INV_COLUMNS - 1 || i == showCount - 1) sb.append("  </div>\n");
		}
		sb.append("</div>\n");

		String html = STYLE + String.format("<div style=\"layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;\">\n" +
				"  <div class=\"decorated-container\" data-hyui-title=\"Paintbrush Table\" style=\"anchor-width:660; margin-right: 40; vertical-align: middle;\">\n" +
				"    <div class=\"container-contents\" style=\"layout-mode: Top; padding-top: 8; padding-bottom: 8; padding-left: 12; padding-right: 12;\">%s</div>\n" +
				"  </div>\n" +
				"</div>", sb.toString());

		PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
				.fromHtml(html)
				.withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

		for (int i = 0; i < variants.length; i++) {
			final String key = variants[i];
			builder.addEventListener("out_" + i, CustomUIEventBindingType.Activating, (e, ctx) -> {
				int dyeAvailable = getDyeBaseCount(inv);
				int toConvert = Math.min(inputCount, dyeAvailable);
				if (toConvert > 0) {
					removeDyeBase(inv, toConvert);
					ItemContainer container = (inputSection == 0) ? inv.getHotbar() : inv.getStorage();
					if (container != null) {
						if (inputCount > toConvert) {
							container.setItemStackForSlot(inputSlot, new ItemStack(inputKey, inputCount - toConvert));
							inv.getCombinedHotbarFirst().addItemStack(new ItemStack(key, toConvert));
						} else {
							container.setItemStackForSlot(inputSlot, new ItemStack(key, toConvert));
						}
						inv.markChanged();
					}
				}
				openPaintbrushTableInput(playerRef, store, world, blockPos, player, variants, inputKey, inputCount - toConvert, inputSlot, inputSection);
			});
			builder.addEventListener("out_" + i, CustomUIEventBindingType.RightClicking, (e, ctx) -> {
				int dyeAvailable = getDyeBaseCount(inv);
				int half = (inputCount + 1) / 2;
				int toConvert = Math.min(half, dyeAvailable);
				if (toConvert > 0) {
					removeDyeBase(inv, toConvert);
					ItemContainer container = (inputSection == 0) ? inv.getHotbar() : inv.getStorage();
					if (container != null) {
						if (inputCount > toConvert) {
							container.setItemStackForSlot(inputSlot, new ItemStack(inputKey, inputCount - toConvert));
							inv.getCombinedHotbarFirst().addItemStack(new ItemStack(key, toConvert));
						} else {
							container.setItemStackForSlot(inputSlot, new ItemStack(key, toConvert));
						}
						inv.markChanged();
					}
				}
				openPaintbrushTableInput(playerRef, store, world, blockPos, player, variants, inputKey, inputCount - toConvert, inputSlot, inputSection);
			});
		}
		builder.addEventListener("clear_input", CustomUIEventBindingType.Activating, (e, ctx) -> {
			openPaintbrushTable(playerRef, store, world, blockPos, player, variants);
		});

		builder.open(store);
	}

	// Helper: count Dye_Base in inventory
	private static int getDyeBaseCount(Inventory inv) {
		int count = 0;
		for (ItemContainer container : new ItemContainer[]{inv.getHotbar(), inv.getStorage()}) {
			if (container == null) continue;
			short cap = container.getCapacity();
			for (short s = 0; s < cap; s++) {
				ItemStack st = container.getItemStack(s);
				if (st != null && !st.isEmpty() && "Dye_Base".equals(st.getItemId()))
					count += st.getQuantity();
			}
		}
		return count;
	}

	// Helper: remove Dye_Base from inventory
	private static void removeDyeBase(Inventory inv, int amount) {
		for (ItemContainer container : new ItemContainer[]{inv.getHotbar(), inv.getStorage()}) {
			if (container == null) continue;
			short cap = container.getCapacity();
			for (short s = 0; s < cap && amount > 0; s++) {
				ItemStack st = container.getItemStack(s);
				if (st != null && !st.isEmpty() && "Dye_Base".equals(st.getItemId())) {
					int qty = st.getQuantity();
					int toRemove = Math.min(qty, amount);
					if (qty > toRemove) {
						container.setItemStackForSlot(s, new ItemStack("Dye_Base", qty - toRemove));
					} else {
						container.setItemStackForSlot(s, null);
					}

					amount -= toRemove;
				}
			}
		}
	}

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	// Resolve color variants for a given block key using Paintbrush or Chisel data (best-effort).
	private static String[] resolveVariantsForBlock(String blockKey) {
		try {
			BlockType bt = BlockTypeCache.get(blockKey);
			if (bt == null) return new String[0];
			if (bt.getState() instanceof Paintbrush.Data pData) {
				if (pData.colorVariants != null) return pData.colorVariants;
			}
			if (bt.getState() instanceof Chisel.Data cData) {
				if (cData.substitutions != null) return cData.substitutions;
			}
		} catch (Throwable ignored) { }
		return new String[0];
	}

	public static void openPaintbrush(PlayerRef playerRef, Store<EntityStore> store, World world, Vector3i blockPos, LivingEntity player, String[] variants) {
		if (variants == null){ variants = new String[0]; }
		final String[] variantList = variants;
		LOGGER.atInfo().log("[PaintbrushUI] Building UI for variants: " + java.util.Arrays.toString(variants));

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
		sb.append("<div class=\"mode-row\">\n");
		sb.append("  <button id=\"pb_table_btn\" class=\"mode-btn\">Table Mode</button>\n");
		sb.append("</div>\n");
		sb.append("<div class=\"btn-grid\">\n");
		for (int i = 0; i < variants.length; i++) {
			if (i % 4 == 0) sb.append("  <div class=\"variant-row\">\n");
			String key = variants[i];
			sb.append(String.format("      <button id=\"vb_%d\" class=\"variant-btn\">", i));
			sb.append(String.format("<span class=\"item-icon\" data-hyui-item-id=\"%s\"></span>", key));
			sb.append("</button>\n");
			if (i % 4 == 3 || i == variants.length - 1) sb.append("  </div>\n");
		}
		sb.append("</div>\n");

		String html = STYLE + String.format("<div style=\"layout-mode: Right; anchor-width: 100%%; anchor-height: 100%%;\">\n" +
			"  <div class=\"decorated-container\" data-hyui-title=\"Paintbrush\" style=\"anchor-width:660; margin-right: 40; vertical-align: middle;\">\n" +
			"    <div class=\"container-contents\" style=\"layout-mode: Top; padding-top: 8; padding-bottom: 8; padding-left: 12; padding-right: 12;\">%s</div>\n" +
			"  </div>\n" +
			"</div>", sb.toString());

		PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
				.fromHtml(html)
				.withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);

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
					WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
					if (chunk == null) {
						LOGGER.atInfo().log("[Paintbrush] Chunk not loaded; cannot apply variant");
						return;
					}
					chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, key);
					if (foundQty > 1) {
						foundContainer.setItemStackForSlot(foundSlot, new ItemStack("Dye_Base", foundQty - 1));
					} else {
						foundContainer.setItemStackForSlot(foundSlot, null);
					}
					inv.markChanged();
					LOGGER.atInfo().log("[Paintbrush] Applied variant " + key + " at " + blockPos + " (consumed Dye_Base)");
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
		builder.addEventListener("pb_table_btn", CustomUIEventBindingType.Activating, (e, ctx) -> {
			openPaintbrushTable(playerRef, store, world, blockPos, player, variantList);
		});
		builder.open(store);
	}

	public static void openTable(PlayerRef playerRef, Store<EntityStore> store, World world, Vector3i blockPos, LivingEntity player) {
		String[] allVariants = getAllPaintbrushColorVariants();
		openPaintbrushTable(playerRef, store, world, blockPos, player, allVariants);
	}

	/** Returns all paintbrush color variants for table mode. */
	private static String[] getAllPaintbrushColorVariants() {
		return new String[] {
			"Cloth_Block_Wool_White",
			"Cloth_Block_Wool_LightGray",
			"Cloth_Block_Wool_Gray",
			"Cloth_Block_Wool_Black",
			"Cloth_Block_Wool_Red",
			"Cloth_Block_Wool_Orange",
			"Cloth_Block_Wool_Yellow",
			"Cloth_Block_Wool_Lime",
			"Cloth_Block_Wool_Green",
			"Cloth_Block_Wool_Cyan",
			"Cloth_Block_Wool_LightBlue",
			"Cloth_Block_Wool_Blue",
			"Cloth_Block_Wool_Purple",
			"Cloth_Block_Wool_Magenta",
			"Cloth_Block_Wool_Pink",
			"Cloth_Block_Wool_Brown"
		};
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
					anchor-width: 40;
					anchor-height: 40;
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
					anchor-width: 52;
					anchor-height: 52;
					margin-left: 6;
					margin-right: 6;
					margin-top: 6;
					margin-bottom: 6;
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
