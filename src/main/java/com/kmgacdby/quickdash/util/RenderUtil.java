package com.kmgacdby.quickdash.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferRenderer;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormats;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Minimal immediate-mode line-strip renderer, used to draw predicted arrow paths. */
public final class RenderUtil {
	private RenderUtil() {
	}

	public static void drawLineStrip(WorldRenderContext context, List<Vec3d> points, float r, float g, float b, float a) {
		if (points.size() < 2) return;

		Vec3d camera = context.camera().getPos();
		MatrixStack matrices = context.matrixStack();
		matrices.push();
		matrices.translate(-camera.x, -camera.y, -camera.z);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.lineWidth(2.0F);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
		var matrix = matrices.peek().getPositionMatrix();
		for (Vec3d p : points) {
			buffer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a);
		}
		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		matrices.pop();
	}
}
