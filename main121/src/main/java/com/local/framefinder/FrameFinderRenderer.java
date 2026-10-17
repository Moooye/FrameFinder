package com.local.framefinder;

import com.local.framefinder.util.FrFndrRenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 世界级渲染（1.21.1 旧管线）：两种高亮样式（/sou style 切换）。
 * <p>
 * 时机 = {@link WorldRenderEvents#AFTER_TRANSLUCENT}（fabric-rendering-v1）：
 * 半透明方块之后、云层之前，回调里拿 matrixStack/vertexConsumers/camera，
 * matrixStack.translate(-camX,-camY,-camZ) 后用世界坐标绘制。
 * 方框描边走 RenderLayer.getLines() / 自建 ESP_LINES（穿墙），
 * 填充走 RenderLayer.getDebugFilledBox() / 自建 ESP_QUADS（穿墙）。
 */
public final class FrameFinderRenderer {

    private FrameFinderRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(FrameFinderRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        List<FrameFinderState.Hit> hits = FrameFinderState.hits;
        if (hits.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        boolean boxMode = "BOX".equals(FrFndrConfig.get().style);
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        double radius = Math.max(1.0, FrameFinderState.getRadius());

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        try {
            if (boxMode) {
                // 方框：半透明填充 + 描边，颜色随距离渐变（近绿远橙）
                for (FrameFinderState.Hit hit : hits) {
                    Box box = hit.frame.getBoundingBox();
                    FrFndrRenderUtils.drawSolidBox(matrices, consumers, box,
                        distanceColor(hit.distance, radius, 0x50), false);
                    FrFndrRenderUtils.drawOutlinedBox(matrices, consumers, box,
                        distanceColor(hit.distance, radius, 0xC8), false);
                }
            } else {
                // 连线：相机相对空间（矩阵已 -cam 补偿），起点=视线前方 10 格
                for (FrameFinderState.Hit hit : hits)
                    FrFndrRenderUtils.drawTracer(matrices, consumers,
                        hit.frame.getBoundingBox().getCenter(),
                        distanceColor(hit.distance, radius, 0xE0), false);
            }
        } finally {
            matrices.pop();
        }

        // 立即提交，保证本帧可见
        com.local.framefinder.util.FrFndrBufferSource.draw(consumers);
    }

    /** 距离渐变色：近绿(0x3DFF3D) 远橙(0xFFA500) */
    private static int distanceColor(double dist, double radius, int alpha) {
        double t = Math.max(0.0, Math.min(1.0, dist / radius));
        int r = (int) Math.round(0x3D + (0xFF - 0x3D) * t);
        int g = (int) Math.round(0xFF + (0xA5 - 0xFF) * t);
        int b = (int) Math.round(0x3D + (0x00 - 0x3D) * t);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
}
