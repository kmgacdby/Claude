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
		/** Sampled points of the flight path, from "now" up to landing/max range. */
		public final List<Vec3d> points = new ArrayList<>();
		/** Non-null if the path intersects the target box before landing. */
		public Vec3d impactPoint;
		/** Ticks from now until impact, or -1 if it never hits the target box. */
		public int impactTick = -1;
		/** Where the arrow lands / stops being simulated. */
		public Vec3d landingPoint;
	}

	/**
	 * @param startPos   current arrow position
	 * @param startVel   current arrow velocity (blocks/tick)
	 * @param world      world for block-collision checks
	 * @param targetBox  the hitbox to test for a hit (usually the player's bounding box); may be null to skip hit-testing
	 * @param maxTicks   safety cap on simulation length
	 */
	public static Prediction predict(Vec3d startPos, Vec3d startVel, World world, Box targetBox, int maxTicks) {
		Prediction result = new Prediction();
		Vec3d pos = startPos;
		Vec3d vel = startVel;
		result.points.add(pos);

		for (int tick = 1; tick <= maxTicks; tick++) {
			Vec3d nextVel = new Vec3d(vel.x, vel.y - GRAVITY, vel.z).multiply(DRAG);
			Vec3d nextPos = pos.add(vel);

			// Hit-test against the target box along this tick's segment.
			if (targetBox != null && result.impactPoint == null) {
				boolean hits = targetBox.contains(pos) || targetBox.raycast(pos, nextPos).isPresent();
				if (hits) {
					result.impactPoint = nextPos;
					result.impactTick = tick;
				}
			}

			// Stop simulating once the arrow would hit a solid block (its landing point).
			HitResult blockHit = world.raycast(new RaycastContext(
					pos, nextPos,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE,
					null));
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
