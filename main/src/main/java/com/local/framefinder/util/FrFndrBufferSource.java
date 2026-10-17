package com.local.framefinder.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Simple wrapper around {@link StagedVertexBuffer} to replace Minecraft's
 * MultiBufferSource which was removed in 26.2-snapshot-5.
 * （移植自 Wurst 7 GPL 源码 WurstBufferSource）
 */
public final class FrFndrBufferSource {
    private final StagedVertexBuffer stagedBuffer = new StagedVertexBuffer(
        () -> "FrFndrBufferSource", RenderType.BIG_BUFFER_SIZE);
    private final List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
    private final List<RenderType> drawTypes = new ArrayList<>();

    public VertexConsumer getBuffer(RenderType renderType) {
        if (!drawTypes.isEmpty() && drawTypes.getLast() == renderType
            && renderType.canConsolidateConsecutiveGeometry())
            return stagedBuffer.getVertexBuilder(draws.getLast());

        StagedVertexBuffer.Draw draw =
            stagedBuffer.appendDraw(renderType.format(),
                renderType.primitiveTopology(), renderType.sortOnUpload()
                    ? RenderSystem.getProjectionType().vertexSorting() : null);

        draws.add(draw);
        drawTypes.add(renderType);
        return stagedBuffer.getVertexBuilder(draw);
    }

    public void uploadAndDraw() {
        try {
            if (draws.isEmpty())
                return;

            stagedBuffer.upload();
            RenderTarget renderTarget =
                Minecraft.getInstance().gameRenderer.mainRenderTarget();

            try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "FrFndrBufferSource",
                    renderTarget.getColorTextureView(), Optional.empty(),
                    renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(renderPass);

                for (int i = 0; i < draws.size(); i++)
                    draw(drawTypes.get(i), draws.get(i), renderPass);
            }

            stagedBuffer.endDraw();

        } finally {
            draws.clear();
            drawTypes.clear();
            stagedBuffer.close();
        }
    }

    private void draw(RenderType type, StagedVertexBuffer.Draw draw,
        RenderPass renderPass) {
        StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(draw);

        if (info != null)
            type.prepare().drawFromBuffer(info, renderPass);
    }
}
