package com.kmgacdby.quickdash.gui;

import com.kmgacdby.quickdash.module.Module;
import com.kmgacdby.quickdash.module.ModuleManager;
import com.kmgacdby.quickdash.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
	private static final int LIST_X = 20;
	private static final int LIST_Y = 24;
	private static final int LIST_WIDTH = 170;
	private static final int ROW_HEIGHT = 20;

	private static final int SETTINGS_X = LIST_X + LIST_WIDTH + 12;
	private static final int SETTINGS_Y = LIST_Y;
	private static final int SETTINGS_WIDTH = 220;
	private static final int DOUBLE_ROW_HEIGHT = 30;
	private static final int BOOL_ROW_HEIGHT = 20;

	private final List<Module> modules = ModuleManager.INSTANCE.getModules();

	/** Module currently shown in the right-hand details panel (opened via right-click). */
	private Module selectedModule;
	/** Module waiting for the next physical key press to become its bound hotkey (middle-click). */
	private Module recordingModule;

	/** The DoubleValue slider currently being dragged, if any. */
	private Setting.DoubleValue draggingSlider;

	public ClickGuiScreen() {
		super(Text.literal("QuickDash"));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false; // ESC is used to cancel keybind recording; handled manually otherwise.
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(LIST_X - 4, LIST_Y - 16, LIST_X + LIST_WIDTH + 4, LIST_Y + modules.size() * ROW_HEIGHT + 4, 0xB0101014);
		context.drawText(textRenderer, "QuickDash", LIST_X, LIST_Y - 13, 0xFFFFFFFF, true);

		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			int rowY = LIST_Y + i * ROW_HEIGHT;
			boolean hovered = mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

			int bg = module.enabled ? 0xFF3A6FD8 : (hovered ? 0xFF303038 : 0xFF202024);
			context.fill(LIST_X, rowY, LIST_X + LIST_WIDTH, rowY + ROW_HEIGHT - 2, bg);

			int textColor = module.enabled ? 0xFFFFFFFF : 0xFFC8C8C8;
			context.drawText(textRenderer, module.name, LIST_X + 6, rowY + 6, textColor, false);

			String keyLabel = module == recordingModule
					? "..."
					: (module.boundKey == -1 ? "-" : InputUtil.fromKeyCode(module.boundKey, 0).getLocalizedText().getString());
			int keyWidth = textRenderer.getWidth(keyLabel);
			context.drawText(textRenderer, keyLabel, LIST_X + LIST_WIDTH - keyWidth - 6, rowY + 6, 0xFFAAAAAA, false);
		}

		if (selectedModule != null) {
			renderSettingsPanel(context, selectedModule);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	private void renderSettingsPanel(DrawContext context, Module module) {
		List<Setting> settings = module.getSettings();
		int totalHeight = layoutHeight(settings);
		context.fill(SETTINGS_X - 4, SETTINGS_Y - 16, SETTINGS_X + SETTINGS_WIDTH + 4, SETTINGS_Y + totalHeight + 4, 0xB0101014);
		context.drawText(textRenderer, module.name + " 设置", SETTINGS_X, SETTINGS_Y - 13, 0xFFFFFFFF, true);

		int y = SETTINGS_Y;
		for (Setting setting : settings) {
			if (setting instanceof Setting.DoubleValue dv) {
				context.drawText(textRenderer, dv.name + ": " + (dv.isInteger ? dv.getInt() : dv.value), SETTINGS_X, y, 0xFFDDDDDD, false);
				int trackY = y + 14;
				context.fill(SETTINGS_X, trackY, SETTINGS_X + SETTINGS_WIDTH - 10, trackY + 4, 0xFF3A3A40);
				int handleX = SETTINGS_X + (int) (dv.getSliderProgress() * (SETTINGS_WIDTH - 10));
				context.fill(handleX - 2, trackY - 2, handleX + 2, trackY + 6, 0xFF6FA8FF);
				y += DOUBLE_ROW_HEIGHT;
			} else if (setting instanceof Setting.BoolValue bv) {
				context.fill(SETTINGS_X, y + 2, SETTINGS_X + 12, y + 14, bv.value ? 0xFF3A6FD8 : 0xFF3A3A40);
				context.drawText(textRenderer, bv.name, SETTINGS_X + 18, y + 4, 0xFFDDDDDD, false);
				y += BOOL_ROW_HEIGHT;
			}
		}
	}

	private int layoutHeight(List<Setting> settings) {
		int h = 0;
		for (Setting s : settings) {
			h += (s instanceof Setting.DoubleValue) ? DOUBLE_ROW_HEIGHT : BOOL_ROW_HEIGHT;
		}
		return h;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Any mouse click cancels an in-progress keybind recording (recording only accepts keyboard input).
		if (recordingModule != null) {
			recordingModule = null;
		}

		// Module list rows.
		for (int i = 0; i < modules.size(); i++) {
			int rowY = LIST_Y + i * ROW_HEIGHT;
			if (mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 2) {
				Module module = modules.get(i);
				if (button == 0) {
					ModuleManager.INSTANCE.toggle(module, client);
				} else if (button == 1) {
					selectedModule = (selectedModule == module) ? null : module;
				} else if (button == 2) {
					recordingModule = module;
				}
				return true;
			}
		}

		// Settings panel widgets.
		if (selectedModule != null && button == 0) {
			int y = SETTINGS_Y;
			for (Setting setting : selectedModule.getSettings()) {
				if (setting instanceof Setting.DoubleValue dv) {
					int trackY = y + 14;
					if (mouseX >= SETTINGS_X - 4 && mouseX <= SETTINGS_X + SETTINGS_WIDTH - 6 && mouseY >= trackY - 4 && mouseY <= trackY + 8) {
						draggingSlider = dv;
						applySliderDrag(dv, mouseX);
						return true;
					}
					y += DOUBLE_ROW_HEIGHT;
				} else if (setting instanceof Setting.BoolValue bv) {
					if (mouseX >= SETTINGS_X && mouseX <= SETTINGS_X + 12 && mouseY >= y + 2 && mouseY <= y + 14) {
						bv.value = !bv.value;
						return true;
					}
					y += BOOL_ROW_HEIGHT;
				}
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (draggingSlider != null) {
			applySliderDrag(draggingSlider, mouseX);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingSlider = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void applySliderDrag(Setting.DoubleValue dv, double mouseX) {
		double progress = (mouseX - SETTINGS_X) / (double) (SETTINGS_WIDTH - 10);
		dv.setFromSliderProgress(progress);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (recordingModule != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				recordingModule = null;
				return true;
			}
			recordingModule.boundKey = keyCode;
			// Avoid the same physical press instantly re-triggering the toggle this frame.
			recordingModule.keyWasDown = true;
			recordingModule = null;
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
