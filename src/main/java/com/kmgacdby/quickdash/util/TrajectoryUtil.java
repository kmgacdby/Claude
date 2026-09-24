package com.kmgacdby.quickdash.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a projectile's future path using vanilla arrow physics:
 *   velocity.y -= 0.05   (gravity, applied each tick)
 *   position   += velocity
 *   velocity   *= 0.99   (drag, applied each tick)
 * This matches net.minecraft.entity.projectile.PersistentProjectileEntity's motion.
 */
public final class TrajectoryUtil {
	private static final double GRAVITY = 0.05;
	private static final double DRAG = 0.99;

	private TrajectoryUtil() {
	}

	public static final class Prediction {
		public final List<Vec3d> points = new ArrayList<>();
		public Vec3d impactPoint;
		public int impactTick = -1;
		public Vec3d landingPoint;
	}

	public static Prediction predict(Vec3d startPos, Vec3d startVel, World world, Box targetBox, int maxTicks) {
		Prediction result = new Prediction();
		Vec3d pos = startPos;
		Vec3d vel = startVel;
		result.points.add(pos);

		for (int tick = 1; tick <= maxTicks; tick++) {
			Vec3d nextVel = new Vec3d(vel.x, vel.y - GRAVITY, vel.z).multiply(DRAG);
			Vec3d nextPos = pos.add(vel);

			if (targetBox != null && result.impactPoint == null) {
				boolean hits = targetBox.contains(pos) || targetBox.raycast(pos, nextPos).isPresent();
				if (hits) {
					result.impactPoint = nextPos;
					result.impactTick = tick;
				}
			}

			HitResult blockHit = world.raycast(new RaycastContext(
					pos, nextPos,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE,
					(Entity) null));
			result.points.add(nextPos);
			if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
				result.landingPoint = blockHit.getPos();
				break;
			}

			pos = nextPos;
			vel = nextVel;
			if (tick == maxTicks) {
				result.landingPoint = nextPos;
			}
		}
		return result;
	}
}
