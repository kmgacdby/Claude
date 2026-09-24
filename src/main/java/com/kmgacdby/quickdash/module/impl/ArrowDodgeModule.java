package com.kmgacdby.quickdash.module.impl;

import com.kmgacdby.quickdash.module.Module;
import com.kmgacdby.quickdash.module.Setting;
import com.kmgacdby.quickdash.util.MovementOverride;
import com.kmgacdby.quickdash.util.TrajectoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Continuously (while enabled) tracks nearby drawn bows / fired arrows within range,
 * predicts their parabolic path, and side-steps (strafe first, then forward/back,
 * crossed if needed) by the minimum distance needed to clear only the single nearest
 * arrow that is actually predicted to hit the player's hitbox. View/camera is left
 * fully under player control. Sneak/jump are intentionally not used for dodging.
 */
public class ArrowDodgeModule extends Module {
	private static final int SIM_MAX_TICKS = 200;
	// How far past "just clearing the hitbox" to move, so the dodge isn't a knife's-edge miss.
	private static final double CLEARANCE_MARGIN = 0.20;
	private static final double PLAYER_HALF_WIDTH = 0.3; // half of the 0.6-wide player hitbox
	private static final double ARROW_RADIUS = 0.25;
	private static final double REQUIRED_CLEARANCE = PLAYER_HALF_WIDTH + ARROW_RADIUS + CLEARANCE_MARGIN;

	// Default 32, adjustable up to 128, per spec.
	private final Setting.DoubleValue range = new Setting.DoubleValue(
			"检测范围 (格)", 8, 128, 32, true);
	private final Setting.BoolValue excludeSelf = new Setting.BoolValue(
			"排除自己射出的箭矢", true);

	private boolean dodging = false;
	private PersistentProjectileEntity currentThreat;
	private Vec3d dodgeStartPos;
	private double requiredShift;
	private boolean pressForward, pressBack, pressLeft, pressRight;

	public ArrowDodgeModule() {
		super("箭矢闪避", "预测来袭箭矢弹道，最小距离左右/前后躲避");
		addSetting(range);
		addSetting(excludeSelf);
	}

	@Override
	public void onDisable(MinecraftClient client) {
		stopDodge(client);
	}

	@Override
	public void onTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		World world = client.world;
		if (player == null || world == null) {
			stopDodge(client);
			return;
		}

		if (dodging) {
			if (currentThreat == null || currentThreat.isRemoved()) {
				stopDodge(client);
			} else {
				double movedSq = player.getPos().subtract(dodgeStartPos).horizontalLengthSquared();
				if (movedSq >= requiredShift * requiredShift) {
					stopDodge(client);
				} else {
					MovementOverride.set(client, pressForward, pressBack, pressLeft, pressRight, false);
				}
			}
			return;
		}

		PersistentProjectileEntity threat = findNearestHitBoundArrow(client, player, world);
		if (threat != null) {
			startDodge(client, player, threat);
		}
	}

	private void startDodge(MinecraftClient client, ClientPlayerEntity player, PersistentProjectileEntity arrow) {
		Vec3d vel = arrow.getVelocity();
		Vec3d flatVel = new Vec3d(vel.x, 0, vel.z);
		if (flatVel.lengthSquared() < 1.0E-6) return; // near-vertical shot, nothing sane to strafe from
		flatVel = flatVel.normalize();

		// Perpendicular to the arrow's flight direction, in the horizontal plane.
		// Fixed convention: 90 degrees clockwise from the arrow's travel direction.
		Vec3d escapeDir = new Vec3d(flatVel.z, 0, -flatVel.x);

		double yawRad = Math.toRadians(player.getYaw());
		Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
		Vec3d right = new Vec3d(Math.cos(yawRad), 0, Math.sin(yawRad));

		double dotRight = escapeDir.dotProduct(right);
		double dotForward = escapeDir.dotProduct(forward);

		pressRight = dotRight > 0.15;
		pressLeft = dotRight < -0.15;
		// Strafe is the primary dodge; only add forward/back when the escape
		// direction has a substantial forward/back component too (crossed dodge).
		pressForward = dotForward > 0.5;
		pressBack = dotForward < -0.5;
		// Guarantee at least some movement if everything was near the deadzone.
		if (!pressRight && !pressLeft && !pressForward && !pressBack) {
			if (dotRight >= 0) pressRight = true; else pressLeft = true;
		}

		dodging = true;
		currentThreat = arrow;
		dodgeStartPos = player.getPos();
		requiredShift = REQUIRED_CLEARANCE;
	}

	private void stopDodge(MinecraftClient client) {
		MovementOverride.releaseAll(client);
		dodging = false;
		currentThreat = null;
		dodgeStartPos = null;
		pressForward = pressBack = pressLeft = pressRight = false;
	}

	/** Among arrows in range whose predicted path hits the player, returns the nearest one (by current distance). */
	private PersistentProjectileEntity findNearestHitBoundArrow(MinecraftClient client, ClientPlayerEntity player, World world) {
		double r = range.value;
		Box searchBox = player.getBoundingBox().expand(r);
		List<PersistentProjectileEntity> arrows = world.getEntitiesByClass(
				PersistentProjectileEntity.class, searchBox, a -> true);

		PersistentProjectileEntity best = null;
		double bestDistSq = Double.MAX_VALUE;

		for (PersistentProjectileEntity arrow : arrows) {
			if (excludeSelf.value && arrow.getOwner() == player) continue;
			if (arrow.isRemoved()) continue;
			if (arrow.getVelocity().lengthSquared() < 1.0E-4) continue; // stuck/landed arrows have ~no velocity
			if (arrow.squaredDistanceTo(player) > r * r) continue;

			Box targetBox = player.getBoundingBox().expand(0.05);
			TrajectoryUtil.Prediction prediction = TrajectoryUtil.predict(
					arrow.getPos(), arrow.getVelocity(), world, targetBox, SIM_MAX_TICKS);

			if (prediction.impactPoint != null) {
				double distSq = arrow.squaredDistanceTo(player);
				if (distSq < bestDistSq) {
					bestDistSq = distSq;
					best = arrow;
				}
			}
		}
		return best;
	}

	/**
	 * Data for the ClickGUI/world renderer: one polyline per tracked bow-draw or in-flight
	 * arrow within range. Computed on demand each render frame (cheap enough at this scale).
	 */
	public List<List<Vec3d>> collectPreviewLines(MinecraftClient client) {
		List<List<Vec3d>> lines = new ArrayList<>();
		ClientPlayerEntity player = client.player;
		World world = client.world;
		if (player == null || world == null) return lines;

		double r = range.value;
		Box searchBox = player.getBoundingBox().expand(r);

		// Entities currently drawing a bow: live preview using their current pull strength.
		List<LivingEntity> drawers = world.getEntitiesByClass(LivingEntity.class, searchBox,
				e -> e != player && e.isUsingItem() && e.getActiveItem().getItem() instanceof BowItem);
		for (LivingEntity drawer : drawers) {
			int useTicks = drawer.getItemUseTime();
			float pull = getPullProgress(useTicks);
			if (pull <= 0.01F) continue;
			Vec3d dir = drawer.getRotationVector();
			Vec3d startPos = drawer.getEyePos();
			Vec3d vel = dir.multiply(pull * 3.0);
			TrajectoryUtil.Prediction prediction = TrajectoryUtil.predict(startPos, vel, world, null, 60);
			lines.add(prediction.points);
		}

		// Arrows already in flight within range.
		List<PersistentProjectileEntity> arrows = world.getEntitiesByClass(
				PersistentProjectileEntity.class, searchBox, a -> true);
		for (PersistentProjectileEntity arrow : arrows) {
			if (excludeSelf.value && arrow.getOwner() == player) continue;
			if (arrow.isRemoved() || arrow.getVelocity().lengthSquared() < 1.0E-4) continue;
			TrajectoryUtil.Prediction prediction = TrajectoryUtil.predict(
					arrow.getPos(), arrow.getVelocity(), world, null, SIM_MAX_TICKS);
			lines.add(prediction.points);
		}
		return lines;
	}

	// Mirrors net.minecraft.item.BowItem#getPullProgress (private in vanilla).
	private static float getPullProgress(int useTicks) {
		float f = (float) useTicks / 20.0F;
		f = (f * f + f * 2.0F) / 3.0F;
		if (f > 1.0F) f = 1.0F;
		return f;
	}
}
