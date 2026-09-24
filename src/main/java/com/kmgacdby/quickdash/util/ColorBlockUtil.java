package com.kmgacdby.quickdash.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

/**
 * Matches wool / concrete / terracotta blocks (and the item forms of the same blocks)
 * to a {@link DyeColor}, per the module-2 spec:
 *  - {color}_wool, {color}_concrete, {color}_terracotta all map to that DyeColor
 *  - plain "terracotta" (the undyed block) is treated as equivalent to WHITE, since
 *    unlike wool/concrete there is no "white_terracotta"-named default block the
 *    player would intuitively expect — the user explicitly asked for the undyed
 *    terracotta/concrete case to be included.
 *  - concrete has no undyed vanilla block, so only the 16 colored concrete blocks apply.
 */
public final class ColorBlockUtil {
	private ColorBlockUtil() {
	}

	/** Returns the matching DyeColor for a held item, or null if it's not a colorable block we track. */
	public static DyeColor colorOfItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return null;
		Item item = stack.getItem();
		Block block = Block.getBlockFromItem(item);
		return colorOfBlock(block);
	}

	public static DyeColor colorOfBlock(Block block) {
		if (block == null) return null;
		Identifier id = Registries.BLOCK.getId(block);
		if (!id.getNamespace().equals("minecraft")) return null;
		String path = id.getPath();

		if (path.equals("terracotta")) {
			return DyeColor.WHITE;
		}

		for (DyeColor color : DyeColor.values()) {
			String prefix = color.getName(); // e.g. "white", "light_gray", "red", ...
			if (path.equals(prefix + "_wool")
					|| path.equals(prefix + "_concrete")
					|| path.equals(prefix + "_terracotta")) {
				return color;
			}
		}
		return null;
	}
}
