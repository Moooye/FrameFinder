package com.local.framefinder;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.Screen;
import com.local.framefinder.gui.FrameFinderScreen;

/**
 * 快捷键 K（默认）：打开 GUI 搜索界面。
 * 常客逻辑：客户端 tick 里扫描展示框。
 */
final class FrameFinderKeybinds {

    private static KeyMapping openGui;

    private FrameFinderKeybinds() {}

    static void register() {
        openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.framefinder.open",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(FrameFinderClient.MOD_ID, "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FrameFinderState.tick(client);
            while (openGui.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.gui.screen() == null && mc.player != null) {
                    mc.gui.setScreen(new FrameFinderScreen());
                }
            }
        });
    }
}
