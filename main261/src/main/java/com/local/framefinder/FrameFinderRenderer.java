package com.local.framefinder;

import com.local.framefinder.util.FrFndrBufferSource;
import com.local.framefinder.util.FrFndrRenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 世界级渲染（Wurst 架构）：两种高亮样式（/sou style 切换）。
 * <p>
 * 时机 = {@link LevelRenderEvents#END_MAIN}：主 RenderPass 已关闭，
 * 此时才能合法创建我们自己的 RenderPass（避免 "Close the existing render pass" 崩溃）。
 * <p>
 * 渲染走 FrFndrBufferSource（StagedVertexBuffer + 独立 RenderPass），
 * 绘制工具在 FrFndrRenderUtils（移植自 Wurst 7 GPL 源码）。
 */
public final class FrameFinderRenderer {

    private FrameFinderRenderer() {}

    public static void register() {
        LevelRenderEvents.END_MAIN.register(FrameFinderRenderer::render);
    }

    private static void render(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        List<FrameFinderState.Hit> hits = FrameFinderState.hits;
        if (hits.isEmpty()) return;

        boolean boxMode = "BOX".equals(FrFndrConfig.get().style);
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        // 目标：每个命中展示框的中心（世界坐标）
        List<Vec3> ends = new java.util.ArrayList<>();
        for (FrameFinderState.Hit hit : hits)
            ends.add(hit.frame.getBoundingBox().getCenter());

        // 距离渐变色：近绿远橙（ARGB）
        int color = 0xFF40FF40;
        var boxes = hits.stream().map(h -> h.frame.getBoundingBox()).toList();

        if (boxMode) {
            // 方框：用独立矩阵（drawSolidBoxes 内部自带 -cam 补偿）
            PoseStack boxMatrices = new PoseStack();
            FrFndrRenderUtils.drawSolidBoxes(boxMatrices, boxes, 0x50FFFFFF, false);
            FrFndrRenderUtils.drawOutlinedBoxes(boxMatrices, boxes, 0xFFFFFFFF, false);
        } else {
            // 连线：相机相对空间（translate -cam）+ drawTracers 内部 end.add(-cam) 补偿 → 正确
            PoseStack matrices = new PoseStack();
            matrices.translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
            FrFndrRenderUtils.drawTracers(matrices, ends, color, false);
        }
    }
}
