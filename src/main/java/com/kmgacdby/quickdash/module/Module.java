package com.kmgacdby.quickdash.module;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every QuickDash feature module.
 *
 * Interaction contract (handled by the ClickGUI screen, not here):
 *  - left click on the module row  -> toggle {@link #enabled}
 *  - right click on the module row -> open this module's {@link #settings} panel
 *  - middle click on the module row -> start "recording" a new value for {@link #boundKey};
 *    the next physical key pressed becomes the bound key, ESC cancels the recording.
 *
 * boundKey is a GLFW key code (see org.lwjgl.glfw.GLFW), or -1 if unbound.
 * When bound, pressing that key toggles {@link #enabled} (edge-triggered, handled by
 * {@link ModuleManager#tick(MinecraftClient)}).
 */
public abstract class Module {
	public final String name;
	public final String description;
	protected final List<Setting> settings = new ArrayList<>();

	public boolean enabled = false;
	public int boundKey = -1;

	/** Internal: tracks previous physical state of boundKey for edge detection. */
	public boolean keyWasDown = false;

	protected Module(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public List<Setting> getSettings() {
		return settings;
	}

	protected void addSetting(Setting setting) {
		settings.add(setting);
	}

	/** Called once when the module transitions from disabled -> enabled. */
	public void onEnable(MinecraftClient client) {
	}

	/** Called once when the module transitions from enabled -> disabled (including mid-action cancel). */
	public void onDisable(MinecraftClient client) {
	}

	/** Called every client tick while {@link #enabled} is true. */
	public abstract void onTick(MinecraftClient client);
}
