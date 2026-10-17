package com.local.framefinder;

import com.local.framefinder.gui.FrameFinderScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

/**
 * ModMenu 集成：mod 列表里的「设置」按钮打开展示框搜索界面。
 */
@Environment(EnvType.CLIENT)
public class FrFndrModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {

    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new FrameFinderScreen();
    }
}