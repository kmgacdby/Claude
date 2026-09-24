package com.kmgacdby.quickdash.module;

/**
 * A single adjustable value shown in a module's right-click "details" panel.
 */
public abstract class Setting {
	public final String name;

	protected Setting(String name) {
		this.name = name;
	}

	/** A numeric slider setting (double-backed, can be displayed as an int). */
	public static class DoubleValue extends Setting {
		public final double min;
		public final double max;
		public double value;
		/** If true, the GUI rounds/displays this as a whole number (e.g. ticks, blocks). */
		public final boolean isInteger;

		public DoubleValue(String name, double min, double max, double defaultValue, boolean isInteger) {
			super(name);
			this.min = min;
			this.max = max;
			this.value = defaultValue;
			this.isInteger = isInteger;
		}

		public void setFromSliderProgress(double progress01) {
			double raw = min + (max - min) * Math.max(0.0, Math.min(1.0, progress01));
			this.value = isInteger ? Math.round(raw) : Math.round(raw * 100.0) / 100.0;
		}

		public double getSliderProgress() {
			if (max == min) return 0.0;
			return (value - min) / (max - min);
		}

		public int getInt() {
			return (int) Math.round(value);
		}
	}

	/** A simple on/off checkbox setting. */
	public static class BoolValue extends Setting {
		public boolean value;

		public BoolValue(String name, boolean defaultValue) {
			super(name);
			this.value = defaultValue;
		}
	}
}
