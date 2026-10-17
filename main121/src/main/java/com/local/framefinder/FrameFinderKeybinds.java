package com.local.framefinder;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import com.local.framefinder.gui.FrameFinderScreen;

/**
 * 快捷键 K（默认）：打开 GUI 搜索界面。
 * 常客逻辑：客户端 tick 里扫描展示框。
 */
final class FrameFinderKeybinds {

    private static KeyBinding openGui;

    private FrameFinderKeybinds() {}

    static void register() {
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.frfndr.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.frfndr"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FrameFinderState.tick(client);
            while (openGui.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen == null && mc.player != null) {
                    mc.setScreen(new FrameFinderScreen());
                }
            }
        });
    }
}
