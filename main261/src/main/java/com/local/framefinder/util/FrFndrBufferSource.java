package com.local.framefinder.util;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 26.1 仍有 {@link MultiBufferSource}（StagedVertexBuffer 是 26.2 才引入的）：
 * 直接包装原版的 bufferSource，{@link #uploadAndDraw()} 时对用过的层逐个
 * endBatch（照 Wurst 26.1 的 RenderUtils.getVCP() + endBatch 方式）。
 */
public final class FrFndrBufferSource {
    private final List<RenderType> drawTypes = new ArrayList<>();

    public VertexConsumer getBuffer(RenderType renderType) {
        drawTypes.add(renderType);
        return vanilla().getBuffer(renderType);
    }

    public void uploadAndDraw() {
        try {
            // 连续相同层共用一个批次，只 endBatch 一次
            for (int i = 0; i < drawTypes.size(); i++) {
                RenderType type = drawTypes.get(i);
                if (i + 1 < drawTypes.size() && drawTypes.get(i + 1) == type)
                    continue;
                vanilla().endBatch(type);
            }
        } finally {
            drawTypes.clear();
        }
    }

    private static MultiBufferSource.BufferSource vanilla() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }
}
