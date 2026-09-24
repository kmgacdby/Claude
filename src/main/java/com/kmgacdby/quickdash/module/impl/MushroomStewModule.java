package com.kmgacdby.quickdash.module.impl;

import com.kmgacdby.quickdash.module.Module;
import com.kmgacdby.quickdash.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * When the player's own survival-inventory screen is open, periodically moves one
 * mushroom stew from the main inventory into the first empty hotbar slot, using the
 * same "swap into hotbar slot N" action as pressing a 1-9 number key while hovering
 * a slot (one server packet per move, exactly like a manual quick-swap).
 *
 * We check for {@link InventoryScreen} specifically (the survival "e" screen's class),
 * not any particular keybind, so it also fires if the player opens it some other way.
 */
public class MushroomStewModule extends Module {
	// Slot ids inside the survival InventoryScreen's ScreenHandler:
	// 0 = crafting result, 1-4 = crafting grid, 5-8 = armor, 9-35 = main inventory,
	// 36-44 = hotbar (0-8), 45 = offhand. This layout is fixed by PlayerScreenHandler.
	private static final int MAIN_INV_START = 9;
	private static final int MAIN_INV_END = 35; // inclusive
	private static final int HOTBAR_START = 36; // hotbar slot 0

	private final Setting.DoubleValue interval = new Setting.DoubleValue(
			"移动间隔 (tick)", 1, 20, 2, true);

	private int tickCounter = 0;

	public MushroomStewModule() {
		super("蘑菇煲自动收纳", "打开背包时自动把蘑菇煲搬到快捷栏空位");
		addSetting(interval);
	}

	@Override
	public void onDisable(MinecraftClient client) {
		tickCounter = 0;
	}

	@Override
	public void onTick(MinecraftClient client) {
		if (client.player == null) return;
		if (!(client.currentScreen instanceof InventoryScreen inventoryScreen)) {
			tickCounter = 0;
			return;
		}

		tickCounter++;
		if (tickCounter < interval.getInt()) return;
		tickCounter = 0;

		var handler = inventoryScreen.getScreenHandler();

		// Find the first empty hotbar slot; if there is none, do nothing this pass.
		int targetHotbarButton = -1;
		for (int hotbarIndex = 0; hotbarIndex < 9; hotbarIndex++) {
			Slot slot = handler.getSlot(HOTBAR_START + hotbarIndex);
			if (slot.getStack().isEmpty()) {
				targetHotbarButton = hotbarIndex;
				break;
			}
		}
		if (targetHotbarButton == -1) return;

		// Find the first mushroom stew sitting in the main inventory (hotbar/offhand excluded).
		int sourceSlotId = -1;
		for (int slotId = MAIN_INV_START; slotId <= MAIN_INV_END; slotId++) {
			ItemStack stack = handler.getSlot(slotId).getStack();
			if (!stack.isEmpty() && stack.isOf(Items.MUSHROOM_STEW)) {
				sourceSlotId = slotId;
				break;
			}
		}
		if (sourceSlotId == -1) return;

		// SWAP with a hotbar "button" performs the same single-packet action as
		// hovering the source slot and pressing the corresponding 1-9 number key.
		client.interactionManager.clickSlot(
				handler.syncId, sourceSlotId, targetHotbarButton,
				SlotActionType.SWAP, client.player);
	}
}
