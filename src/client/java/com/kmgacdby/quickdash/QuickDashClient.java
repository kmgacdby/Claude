package com.kmgacdby.quickdash;

import com.kmgacdby.quickdash.gui.ClickGuiScreen;
import com.kmgacdby.quickdash.module.ModuleManager;
import com.kmgacdby.quickdash.util.RenderUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class QuickDashClient implements ClientModInitializer {

	// Fixed global hotkey that opens/closes the ClickGUI itself. Rebindable normally
	// through vanilla Controls settings, since it's a real registered KeyBinding.
	private static final KeyBinding OPEN_GUI_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.quickdash.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			"key.categories.quickdash"));

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (OPEN_GUI_KEY.wasPressed()) {
				if (client.currentScreen instanceof ClickGuiScreen) {
					client.setScreen(null);
				} else if (client.currentScreen == null) {
					client.setScreen(new ClickGuiScreen());
				}
			}

			if (client.player != null && client.world != null) {
				ModuleManager.INSTANCE.tick(client);
			}
		});

		WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
			List<List<Vec3d>> lines = ModuleManager.INSTANCE.arrowDodge.collectPreviewLines(net.minecraft.client.MinecraftClient.getInstance());
			for (List<Vec3d> line : lines) {
				RenderUtil.drawLineStrip(context, line, 1.0F, 0.35F, 0.1F, 0.9F);
			}
		});
	}
}
