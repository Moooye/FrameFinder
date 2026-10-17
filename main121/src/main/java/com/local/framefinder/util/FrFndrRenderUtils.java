package com.local.framefinder.util;

import com.local.framefinder.FrFndrRenderLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.joml.Vector3f;

import java.util.List;

/**
 * 1.21.1 旧管线绘制工具（移植自 Wurst 7 GPL 源码 RenderUtils，老版画法）：
 * <ul>
 *   <li>方框描边：12 条边 24 顶点，LINES 顶点格式（normal = 边方向）；</li>
 *   <li>方框填充：6 个面 24 顶点，POSITION_COLOR 半透明；</li>
 *   <li>连线：起点在相机前方 10 格，穿屏补偿（closeToCam）保留。</li>
 * </ul>
 * 调用前矩阵应已 translate(-camX,-camY,-camZ)（坐标传世界坐标）。
 * 1.21.1 的 VertexConsumer 无 next()：每次 vertex() 开新顶点，元素填满即隐式提交。
 */
public enum FrFndrRenderUtils {
    ;

    public static Vec3d getCameraPos() {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera == null)
            return Vec3d.ZERO;
        return camera.getPos();
    }

    public static void applyCameraOffset(MatrixStack matrices) {
        Vec3d cam = getCameraPos();
        matrices.translate(-cam.x, -cam.y, -cam.z);
    }

    /** 连线方向（相机视线）；异常时兜底 (0,1,0) */
    private static Vec3d getTracerLook() {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d look = (camera == null)
            ? Vec3d.ZERO
            : Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        if (!Double.isFinite(look.x) || !Double.isFinite(look.y) || !Double.isFinite(look.z)
            || (look.x == 0 && look.y == 0 && look.z == 0)) {
            look = new Vec3d(0, 1, 0);
        }
        return look;
    }

    // ---------- 批量入口 ----------

    public static void drawTracers(MatrixStack matrices, VertexConsumerProvider provider,
        List<Vec3d> ends, int color, boolean depthTest) {
        VertexConsumer buffer = provider.getBuffer(FrFndrRenderLayers.getLines(depthTest));
        Vec3d start = getTracerLook().multiply(10);
        for (Vec3d end : ends)
            drawLine(matrices, buffer, start, end, color);
    }

    public static void drawTracer(MatrixStack matrices, VertexConsumerProvider provider,
        Vec3d end, int color, boolean depthTest) {
        VertexConsumer buffer = provider.getBuffer(FrFndrRenderLayers.getLines(depthTest));
        drawLine(matrices, buffer, getTracerLook().multiply(10), end, color);
    }

    public static void drawOutlinedBoxes(MatrixStack matrices, VertexConsumerProvider provider,
        List<Box> boxes, int color, boolean depthTest) {
        VertexConsumer buffer = provider.getBuffer(FrFndrRenderLayers.getLines(depthTest));
        for (Box box : boxes)
            drawOutlinedBox(matrices, buffer, box, color);
    }

    public static void drawSolidBoxes(MatrixStack matrices, VertexConsumerProvider provider,
        List<Box> boxes, int color, boolean depthTest) {
        VertexConsumer buffer = provider.getBuffer(FrFndrRenderLayers.getQuads(depthTest));
        for (Box box : boxes)
            drawSolidBox(matrices, buffer, box, color);
    }

    // ---------- 单个入口 ----------

    public static void drawOutlinedBox(MatrixStack matrices, VertexConsumerProvider provider,
        Box box, int color, boolean depthTest) {
        drawOutlinedBox(matrices, provider.getBuffer(FrFndrRenderLayers.getLines(depthTest)), box, color);
    }

    public static void drawSolidBox(MatrixStack matrices, VertexConsumerProvider provider,
        Box box, int color, boolean depthTest) {
        drawSolidBox(matrices, provider.getBuffer(FrFndrRenderLayers.getQuads(depthTest)), box, color);
    }

    // ---------- 顶点发射 ----------

    /** 12 条边的方框描边（LINES 顶点格式：position+color+normal） */
    public static void drawOutlinedBox(MatrixStack matrices, VertexConsumer buffer,
        Box box, int color) {
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // bottom
        line(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        line(matrices, buffer, x1, y1, z1, x1, y1, z2, color);
        line(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        line(matrices, buffer, x1, y1, z2, x2, y1, z2, color);

        // top
        line(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
        line(matrices, buffer, x1, y2, z1, x1, y2, z2, color);
        line(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
        line(matrices, buffer, x1, y2, z2, x2, y2, z2, color);

        // sides
        line(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        line(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
        line(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        line(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
    }

    /** 6 个面的半透明填充（QUADS 顶点序，POSITION_COLOR） */
    public static void drawSolidBox(MatrixStack matrices, VertexConsumer buffer,
        Box box, int color) {
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        vertex(matrices, buffer, x1, y1, z1).color(color);
        vertex(matrices, buffer, x2, y1, z1).color(color);
        vertex(matrices, buffer, x2, y1, z2).color(color);
        vertex(matrices, buffer, x1, y1, z2).color(color);

        vertex(matrices, buffer, x1, y2, z1).color(color);
        vertex(matrices, buffer, x1, y2, z2).color(color);
        vertex(matrices, buffer, x2, y2, z2).color(color);
        vertex(matrices, buffer, x2, y2, z1).color(color);

        vertex(matrices, buffer, x1, y1, z1).color(color);
        vertex(matrices, buffer, x1, y2, z1).color(color);
        vertex(matrices, buffer, x2, y2, z1).color(color);
        vertex(matrices, buffer, x2, y1, z1).color(color);

        vertex(matrices, buffer, x2, y1, z1).color(color);
        vertex(matrices, buffer, x2, y2, z1).color(color);
        vertex(matrices, buffer, x2, y2, z2).color(color);
        vertex(matrices, buffer, x2, y1, z2).color(color);

        vertex(matrices, buffer, x1, y1, z2).color(color);
        vertex(matrices, buffer, x2, y1, z2).color(color);
        vertex(matrices, buffer, x2, y2, z2).color(color);
        vertex(matrices, buffer, x1, y2, z2).color(color);

        vertex(matrices, buffer, x1, y1, z1).color(color);
        vertex(matrices, buffer, x1, y1, z2).color(color);
        vertex(matrices, buffer, x1, y2, z2).color(color);
        vertex(matrices, buffer, x1, y2, z1).color(color);
    }

    /** 连线（相机相对空间），含穿屏补偿：线穿过相机近面时在中点补两个顶点 */
    public static void drawLine(MatrixStack matrices, VertexConsumer buffer,
        Vec3d start, Vec3d end, int color) {
        float x1 = (float) start.x;
        float y1 = (float) start.y;
        float z1 = (float) start.z;
        float x2 = (float) end.x;
        float y2 = (float) end.y;
        float z2 = (float) end.z;
        line(matrices, buffer, x1, y1, z1, x2, y2, z2, color);
    }

    private static void line(MatrixStack matrices, VertexConsumer buffer,
        float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        Vector3f normal = new Vector3f(x2, y2, z2).sub(x1, y1, z1);
        if (normal.lengthSquared() < 1.0e-8f)
            normal.set(0, 1, 0);
        else
            normal.normalize();

        vertex(matrices, buffer, x1, y1, z1).color(color)
            .normal(matrices.peek(), normal.x, normal.y, normal.z);
        vertex(matrices, buffer, x2, y2, z2).color(color)
            .normal(matrices.peek(), normal.x, normal.y, normal.z);
    }

    // ---------- 基础发射器（1.21.1 旧 VertexConsumer API：vertex/color/normal） ----------

    private static VertexConsumer vertex(MatrixStack matrices, VertexConsumer buffer,
        float x, float y, float z) {
        return buffer.vertex(matrices.peek().getPositionMatrix(), x, y, z);
    }
}
