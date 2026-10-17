package com.local.framefinder;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FrameFinder —— 高亮物品展示框。
 * <p>
 * 输入物品名（英文 id 或中文名），自动找出附近所有装着该物品的展示框，
 * 从玩家位置画一条连线指向它（并标距离）。
 * <ul>
 *   <li>{@code /sou <物品>} —— 添加搜索目标（再次输入取消）；{@code /sou off} 全清</li>
 *   <li>{@code K} 键（默认）打开 GUI 搜索界面，可看匹配列表</li>
 * </ul>
 * 纯客户端，服务端无需安装。
 */
public final class FrameFinderClient implements ClientModInitializer {

    public static final String MOD_ID = "frfndr";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOG.info("[FrFndr] 初始化 (1.21.1)");
        FrFndrConfig.load();
        FrameFinderState.restoreTargets();
        FrameFinderCommands.register();
        FrameFinderKeybinds.register();
        FrameFinderRenderer.register();
    }
}
