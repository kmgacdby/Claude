package com.kmgacdby.quickdash.module;

import com.kmgacdby.quickdash.module.impl.ArrowDodgeModule;
import com.kmgacdby.quickdash.module.impl.ColorPathModule;
import com.kmgacdby.quickdash.module.impl.MushroomStewModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
	public static final ModuleManager INSTANCE = new ModuleManager();

	private final List<Module> modules = new ArrayList<>();

	public final MushroomStewModule mushroomStew = new MushroomStewModule();
	public final ColorPathModule colorPath = new ColorPathModule();
	public final ArrowDodgeModule arrowDodge = new ArrowDodgeModule();

	private ModuleManager() {
		modules.add(mushroomStew);
		modules.add(colorPath);
		modules.add(arrowDodge);
	}

	public List<Module> getModules() {
		return modules;
	}

	public void setEnabled(Module module, boolean enabled, MinecraftClient client) {
		if (module.enabled == enabled) return;
		module.enabled = enabled;
		if (enabled) {
			module.onEnable(client);
		} else {
			module.onDisable(client);
		}
	}

	public void toggle(Module module, MinecraftClient client) {
		setEnabled(module, !module.enabled, client);
	}

	/** Must be called once per client tick (see QuickDashClient). */
	public void tick(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();

		for (Module module : modules) {
			// Edge-triggered hotkey polling: a module's bound key toggles it on/off,
			// and (per spec) pressing it again mid-action cancels/stops that module.
			if (module.boundKey != -1) {
				boolean down = InputUtil.isKeyPressed(windowHandle, module.boundKey);
				if (down && !module.keyWasDown) {
					toggle(module, client);
				}
				module.keyWasDown = down;
			}

			if (module.enabled) {
				module.onTick(client);
			}
		}
	}
}
