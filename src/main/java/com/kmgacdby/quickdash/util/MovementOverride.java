package com.kmgacdby.quickdash.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/**
 * Forces the vanilla movement key bindings into a pressed/released state so that
 * normal player physics (sprint-jump, strafing, etc.) drive the actual movement,
 * exactly as if the player were holding those keys themselves.
 *
 * Modules MUST call {@link #releaseAll(MinecraftClient)} whenever they stop moving
 * the player (arrival, disable, cancel) so a real key the player is holding isn't
 * left stuck down and so unrelated modules don't fight over the same keys.
 */
public final class MovementOverride {
	private MovementOverride() {
	}

	public static void set(MinecraftClient client, boolean forward, boolean back, boolean left, boolean right, boolean jump) {
		press(client.options.forwardKey, forward);
		press(client.options.backKey, back);
		press(client.options.leftKey, left);
		press(client.options.rightKey, right);
		press(client.options.jumpKey, jump);
	}

	public static void releaseAll(MinecraftClient client) {
		press(client.options.forwardKey, false);
		press(client.options.backKey, false);
		press(client.options.leftKey, false);
		press(client.options.rightKey, false);
		press(client.options.jumpKey, false);
	}

	private static void press(KeyBinding key, boolean down) {
		KeyBinding.setKeyPressed(key.getDefaultKey(), down);
	}
}
