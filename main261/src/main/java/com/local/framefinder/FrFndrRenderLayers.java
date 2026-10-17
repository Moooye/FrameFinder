package com.local.framefinder;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * 自定义渲染层（照 Wurst 26.3 的 WurstRenderLayers 方式）：
 * <ul>
 *   <li>{@link #LINES} —— 有深度测试的线（被墙挡住）；</li>
 *   <li>{@link #ESP_LINES} —— 无深度测试的线（穿墙可见）；</li>
 *   <li>{@link #QUADS} / {@link #ESP_QUADS} —— 半透明填充面。</li>
 * </ul>
 * 管线基于官方 LINES_SNIPPET / DEBUG_FILLED_SNIPPET（POSITION_COLOR 格式，
 * 无雾），带 VIEW_OFFSET_Z_LAYERING 层级变换。
 */
public enum FrFndrRenderLayers {
    ;

    private static RenderPipeline.Builder linesSnippet() {
        return RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(
                DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
                VertexFormat.Mode.LINES);
    }

    /** 有深度测试的线 */
    public static final RenderType LINES = RenderType.create("frfndr:lines",
        RenderSetup.builder(RenderPipelines.register(linesSnippet()
            .withLocation(Identifier.parse("frfndr:pipeline/depth_test_lines"))
            .withDepthStencilState(DepthStencilState.DEFAULT).build()))
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup());

    /** 无深度测试的线（穿墙） */
    public static final RenderType ESP_LINES = RenderType.create("frfndr:esp_lines",
        RenderSetup.builder(RenderPipelines.register(linesSnippet()
            .withLocation(Identifier.parse("frfndr:pipeline/esp_lines"))
            .withDepthStencilState(java.util.Optional.empty()).build()))
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup());

    private static RenderPipeline.Builder quadsSnippet() {
        return RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withDepthStencilState(DepthStencilState.DEFAULT).withCull(true);
    }

    /** 半透明填充面（有深度测试） */
    public static final RenderType QUADS = RenderType.create("frfndr:quads",
        RenderSetup.builder(RenderPipelines.register(quadsSnippet()
            .withLocation(Identifier.parse("frfndr:pipeline/quads")).build()))
            .sortOnUpload().createRenderSetup());

    /** 半透明填充面（穿墙） */
    public static final RenderType ESP_QUADS = RenderType.create("frfndr:esp_quads",
        RenderSetup.builder(RenderPipelines.register(RenderPipeline
                .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.parse("frfndr:pipeline/esp_quads"))
                .withDepthStencilState(java.util.Optional.empty()).withCull(true).build()))
            .sortOnUpload().createRenderSetup());

    /** 按深度测试开关返回填充层 */
    public static RenderType getQuads(boolean depthTest) {
        return depthTest ? QUADS : ESP_QUADS;
    }

    /** 按深度测试开关返回线层 */
    public static RenderType getLines(boolean depthTest) {
        return depthTest ? LINES : ESP_LINES;
    }
}
