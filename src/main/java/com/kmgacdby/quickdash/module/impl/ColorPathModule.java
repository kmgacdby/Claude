package com.kmgacdby.quickdash.module.impl;

import com.kmgacdby.quickdash.module.Module;
import com.kmgacdby.quickdash.module.Setting;
import com.kmgacdby.quickdash.util.ColorBlockUtil;
import com.kmgacdby.quickdash.util.MovementOverride;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Continuously (while enabled): looks at the main-hand item, and if it's a wool /
 * concrete / terracotta block (or plain terracotta, treated as white), finds the
 * nearest matching-color block at the player's current feet Y level and forces the
 * player to sprint-jump straight to it, fully overriding look direction and movement
 * until arrival. Re-targets whenever the held item's color changes.
 */
public class ColorPathModule extends Module {
	private static final double ARRIVE_DISTANCE_SQ = 0.36; // ~0.6 block radius counts as "arrived"

	// No explicit default radius was given for this module (only the 128 cap was) —
	// defaulting to 64 as a reasonable middle ground; adjustable 8-128 in the details panel.
	private final Setting.DoubleValue radius = new Setting.DoubleValue(
			"搜索半径 (格)", 8, 128, 64, true);

	private DyeColor lastHeldColor;
	private BlockPos target;
	private boolean reachedCurrentTarget;

	public ColorPathModule() {
		super("同色方块寻路", "疾跑跳跃前往与手持羊毛/混凝土/陶瓦同色的最近方块");
		addSetting(radius);
	}

	@Override
	public void onDisable(MinecraftClient client) {
		stopMoving(client);
		target = null;
		lastHeldColor = null;
		reachedCurrentTarget = false;
	}

	@Override
	public void onTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		World world = client.world;
		if (player == null || world == null) {
			stopMoving(client);
			return;
		}

		ItemStack held = player.getMainHandStack();
		DyeColor heldColor = ColorBlockUtil.colorOfItem(held);

		if (heldColor == null) {
			// Not holding a colorable block (e.g. a sword) -> do not run.
			stopMoving(client);
			target = null;
			lastHeldColor = null;
			reachedCurrentTarget = false;
			return;
		}

		if (heldColor != lastHeldColor) {
			lastHeldColor = heldColor;
			reachedCurrentTarget = false;
			target = findNearest(world, player, heldColor);
		}

		if (target == null || reachedCurrentTarget) {
			stopMoving(client);
			return;
		}

		double targetX = target.getX() + 0.5;
		double targetZ = target.getZ() + 0.5;
		double dx = targetX - player.getX();
		double dz = targetZ - player.getZ();
		double distSq = dx * dx + dz * dz;

		if (distSq < ARRIVE_DISTANCE_SQ) {
			reachedCurrentTarget = true;
			stopMoving(client);
			return;
		}

		// MC yaw convention: 0 = +Z, 90 = -X, so yaw = atan2(-dx, dz).
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setBodyYaw(yaw);
		player.setSprinting(true);
		MovementOverride.set(client, true, false, false, false, true);
	}

	private void stopMoving(MinecraftClient client) {
		MovementOverride.releaseAll(client);
		if (client.player != null) {
			client.player.setSprinting(false);
		}
	}

	/**
	 * Scans the horizontal plane at the player's current feet Y for the nearest
	 * loaded block matching the target color, in a square of side (2*radius+1)
	 * (a 360-degree search). Ties keep whichever block iteration reaches first.
	 */
	private BlockPos findNearest(World world, ClientPlayerEntity player, DyeColor color) {
		int y = player.getBlockPos().getY();
		int r = radius.getInt();
		int px = player.getBlockPos().getX();
		int pz = player.getBlockPos().getZ();

		BlockPos best = null;
		double bestDistSq = Double.MAX_VALUE;

		BlockPos.Mutable cursor = new BlockPos.Mutable();
		for (int dz = -r; dz <= r; dz++) {
			for (int dx = -r; dx <= r; dx++) {
				double distSq = (double) dx * dx + (double) dz * dz;
				if (distSq > (double) r * r) continue;
				cursor.set(px + dx, y, pz + dz);
				if (!world.isChunkLoaded(cursor)) continue;
				DyeColor blockColor = ColorBlockUtil.colorOfBlock(world.getBlockState(cursor).getBlock());
				if (blockColor == color && distSq < bestDistSq) {
					bestDistSq = distSq;
					best = cursor.toImmutable();
				}
			}
		}
		return best;
	}
}
