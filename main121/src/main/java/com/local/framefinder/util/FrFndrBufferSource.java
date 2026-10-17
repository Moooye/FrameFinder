package com.local.framefinder.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

/**
 * 1.21.1 旧管线没有 26.x 的 StagedVertexBuffer / 自建 RenderPass 问题，
 * 直接复用世界渲染共享的 {@link VertexConsumerProvider.Immediate}。
 * 此类保留与 26.x 版本相同的封装形态：getBuffer / draw 两步。
 */
public final class FrFndrBufferSource {

    private FrFndrBufferSource() {}

    /** 从共享 immediate 拿顶点缓冲 */
    public static VertexConsumer getBuffer(VertexConsumerProvider provider, RenderLayer layer) {
        return provider.getBuffer(layer);
    }

    /** 立即提交当前批次（保证本帧可见） */
    public static void draw(VertexConsumerProvider provider) {
        if (provider instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.drawCurrentLayer();
        }
    }
}
