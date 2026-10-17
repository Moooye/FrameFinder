package com.local.framefinder.util;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 */
public enum FrFndrRenderUtils {
    ;

    public static Vec3 getCameraPos() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null)
            return Vec3.ZERO;
        return camera.position();
    }

    public static void applyCameraOffset(PoseStack matrices) {
        Vec3 cam = getCameraPos();
        matrices.translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
    }

    public static Vec3 getTracerOrigin() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 look = camera == null ? Vec3.ZERO
            : Vec3.directionFromRotation(camera.xRot(), camera.yRot());
        return getCameraPos().add(look.scale(10));
    }

    public static void drawTracers(PoseStack matrices, List<Vec3> ends,
        int color, boolean depthTest) {
        FrFndrBufferSource bs = new FrFndrBufferSource();
        var layer = com.local.framefinder.FrFndrRenderLayers.getLines(depthTest);
        VertexConsumer buffer = bs.getBuffer(layer);

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 look = (camera == null)
            ? Vec3.ZERO
            : Vec3.directionFromRotation(camera.xRot(), camera.yRot());
        if (!Double.isFinite(look.x) || !Double.isFinite(look.y) || !Double.isFinite(look.z)
            || (look.x == 0 && look.y == 0 && look.z == 0)) {
            look = new Vec3(0, 1, 0);
        }
        Vec3 start = getCameraPos().add(look.scale(10));
        com.local.framefinder.FrameFinderClient.LOG.info("[FrFndr] tracer start={} look={} ends={}",
            start, look, ends.size());
        for (Vec3 end : ends)
            drawLine(matrices, buffer, start, end, color);

        bs.uploadAndDraw();
    }

    public static void drawOutlinedBoxes(PoseStack matrices, List<AABB> boxes,
        int color, boolean depthTest) {
        FrFndrBufferSource bs = new FrFndrBufferSource();
        var layer = com.local.framefinder.FrFndrRenderLayers.getLines(depthTest);
        VertexConsumer buffer = bs.getBuffer(layer);

        Vec3 camOffset = getCameraPos().scale(-1);
        for (AABB box : boxes)
            drawOutlinedBox(matrices, buffer, box.move(camOffset), color);

        bs.uploadAndDraw();
    }

    public static void drawSolidBoxes(PoseStack matrices, List<AABB> boxes,
        int color, boolean depthTest) {
        FrFndrBufferSource bs = new FrFndrBufferSource();
        var layer = com.local.framefinder.FrFndrRenderLayers.getQuads(depthTest);
        VertexConsumer buffer = bs.getBuffer(layer);

        Vec3 camOffset = getCameraPos().scale(-1);
        for (AABB box : boxes)
            drawSolidBox(matrices, buffer, box.move(camOffset), color);

        bs.uploadAndDraw();
    }

    public static void drawSolidBox(PoseStack matrices, VertexConsumer buffer,
        AABB box, int color) {
        PoseStack.Pose entry = matrices.last();
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        buffer.addVertex(entry, x1, y1, z1).setColor(color);
        buffer.addVertex(entry, x2, y1, z1).setColor(color);
        buffer.addVertex(entry, x2, y1, z2).setColor(color);
        buffer.addVertex(entry, x1, y1, z2).setColor(color);

        buffer.addVertex(entry, x1, y2, z1).setColor(color);
        buffer.addVertex(entry, x1, y2, z2).setColor(color);
        buffer.addVertex(entry, x2, y2, z2).setColor(color);
        buffer.addVertex(entry, x2, y2, z1).setColor(color);

        buffer.addVertex(entry, x1, y1, z1).setColor(color);
        buffer.addVertex(entry, x1, y2, z1).setColor(color);
        buffer.addVertex(entry, x2, y2, z1).setColor(color);
        buffer.addVertex(entry, x2, y1, z1).setColor(color);

        buffer.addVertex(entry, x2, y1, z1).setColor(color);
        buffer.addVertex(entry, x2, y2, z1).setColor(color);
        buffer.addVertex(entry, x2, y2, z2).setColor(color);
        buffer.addVertex(entry, x2, y1, z2).setColor(color);

        buffer.addVertex(entry, x1, y1, z2).setColor(color);
        buffer.addVertex(entry, x2, y1, z2).setColor(color);
        buffer.addVertex(entry, x2, y2, z2).setColor(color);
        buffer.addVertex(entry, x1, y2, z2).setColor(color);

        buffer.addVertex(entry, x1, y1, z1).setColor(color);
        buffer.addVertex(entry, x1, y1, z2).setColor(color);
        buffer.addVertex(entry, x1, y2, z2).setColor(color);
        buffer.addVertex(entry, x1, y2, z1).setColor(color);
    }

    public static void drawOutlinedBox(PoseStack matrices,
        VertexConsumer buffer, AABB box, int color) {
        PoseStack.Pose entry = matrices.last();
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // bottom
        buffer.addVertex(entry, x1, y1, z1).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y1, z1).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);
        buffer.addVertex(entry, x1, y1, z1).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x1, y1, z2).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x2, y1, z1).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x2, y1, z2).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x1, y1, z2).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y1, z2).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);

        // top
        buffer.addVertex(entry, x1, y2, z1).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y2, z1).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);
        buffer.addVertex(entry, x1, y2, z1).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x1, y2, z2).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x2, y2, z1).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x2, y2, z2).setColor(color)
            .setNormal(entry, 0, 0, 1).setLineWidth(2);
        buffer.addVertex(entry, x1, y2, z2).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y2, z2).setColor(color)
            .setNormal(entry, 1, 0, 0).setLineWidth(2);

        // sides
        buffer.addVertex(entry, x1, y1, z1).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x1, y2, z1).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y1, z1).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y2, z1).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x1, y1, z2).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x1, y2, z2).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y1, z2).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
        buffer.addVertex(entry, x2, y2, z2).setColor(color)
            .setNormal(entry, 0, 1, 0).setLineWidth(2);
    }

    public static void drawLine(PoseStack matrices, VertexConsumer buffer,
        Vec3 start, Vec3 end, int color) {
        PoseStack.Pose entry = matrices.last();
        float x1 = (float) start.x;
        float y1 = (float) start.y;
        float z1 = (float) start.z;
        float x2 = (float) end.x;
        float y2 = (float) end.y;
        float z2 = (float) end.z;
        drawLine(entry, buffer, x1, y1, z1, x2, y2, z2, color);
    }

    public static void drawLine(PoseStack.Pose entry, VertexConsumer buffer,
        float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        org.joml.Vector3f normal = new org.joml.Vector3f(x2, y2, z2)
            .sub(x1, y1, z1).normalize();
        buffer.addVertex(entry, x1, y1, z1).setColor(color)
            .setNormal(entry, normal).setLineWidth(2);

        // If the line goes through the screen, add another vertex there.
        float t = new org.joml.Vector3f(x1, y1, z1).negate().dot(normal);
        float length = new org.joml.Vector3f(x2, y2, z2).sub(x1, y1, z1).length();
        if (t > 0 && t < length) {
            org.joml.Vector3f closeToCam = new org.joml.Vector3f(normal).mul(t)
                .add(x1, y1, z1);
            buffer.addVertex(entry, closeToCam).setColor(color)
                .setNormal(entry, normal).setLineWidth(2);
            buffer.addVertex(entry, closeToCam).setColor(color)
                .setNormal(entry, normal).setLineWidth(2);
        }

        buffer.addVertex(entry, x2, y2, z2).setColor(color)
            .setNormal(entry, normal).setLineWidth(2);
    }

    public static void drawLineRaw(VertexConsumer buffer, PoseStack.Pose pose,
        float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        org.joml.Vector3f normal = new org.joml.Vector3f(x2, y2, z2)
            .sub(x1, y1, z1).normalize();
        buffer.addVertex(pose, x1, y1, z1).setColor(argb)
            .setNormal(pose, normal).setLineWidth(2);
        float t = new org.joml.Vector3f(x1, y1, z1).negate().dot(normal);
        float length = new org.joml.Vector3f(x2, y2, z2).sub(x1, y1, z1).length();
        if (t > 0 && t < length) {
            org.joml.Vector3f closeToCam = new org.joml.Vector3f(normal).mul(t).add(x1, y1, z1);
            buffer.addVertex(pose, closeToCam).setColor(argb)
                .setNormal(pose, normal).setLineWidth(2);
            buffer.addVertex(pose, closeToCam).setColor(argb)
                .setNormal(pose, normal).setLineWidth(2);
        }
        buffer.addVertex(pose, x2, y2, z2).setColor(argb)
            .setNormal(pose, normal).setLineWidth(2);
    }
}
