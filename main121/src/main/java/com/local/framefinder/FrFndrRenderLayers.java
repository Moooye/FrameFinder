package com.local.framefinder;

import net.minecraft.client.render.RenderLayer;

/**
 * 渲染层选择（1.21.1 旧管线）：
 * <ul>
 *   <li>描边 / 连线 → 原版 {@link RenderLayer#getLines()}（LINES 顶点格式，normal=边方向）；</li>
 *   <li>半透明填充 → 原版 debug 系 {@link RenderLayer#getDebugFilledBox()}（POSITION_COLOR）。</li>
 * </ul>
 * 说明：1.21.1 中 {@code RenderLayer.of(...)} 为包私有，无法在模组侧自建
 * no-depth RenderLayer；因此 depthTest=false 分支同样回退到原版层
 * （26.x 的 main 模块有 ESP_LINES / ESP_QUADS 穿墙实现，1.21.1 暂不支持穿墙）。
 */
public enum FrFndrRenderLayers {
    ;

    /** 按深度测试开关返回线层 */
    public static RenderLayer getLines(boolean depthTest) {
        return RenderLayer.getLines();
    }

    /** 按深度测试开关返回填充层（debug 系） */
    public static RenderLayer getQuads(boolean depthTest) {
        return RenderLayer.getDebugFilledBox();
    }
}
