package com.local.framefinder.gui;

import com.local.framefinder.FrameFinderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI 搜索界面：输入框（实时匹配预览）+ 当前目标列表 + 命中列表。
 * <p>
 * 实时预览用原版全物品注册表来展示"输入会命中哪些物品"。
 */
public final class FrameFinderScreen extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private TextFieldWidget searchBox;
    private String lastQuery = "";
    private final List<ItemStack> previewMatches = new ArrayList<>();
    private int hitScroll = 0;

    public FrameFinderScreen() {
        super(Text.literal("展示框物品搜索"));
    }

    @Override
    protected void init() {
        super.init();
        int w = 260;
        int x = (this.width - w) / 2;

        searchBox = new TextFieldWidget(this.textRenderer, x, 50, w, 18, Text.literal("物品名"));
        searchBox.setMaxLength(64);
        searchBox.setChangedListener(this::onQueryChanged);
        addDrawableChild(searchBox);
        setInitialFocus(searchBox);
    }

    private void onQueryChanged(String q) {
        if (q.equals(lastQuery)) return;
        lastQuery = q;
        previewMatches.clear();
        String word = q.trim().toLowerCase();
        if (word.length() < 1) return;

        // 从全物品注册表里找匹配
        try {
            for (Item item : Registries.ITEM) {
                if (FrameFinderState.matchesWord(item.getDefaultStack(), word)) {
                    previewMatches.add(item.getDefaultStack());
                    if (previewMatches.size() >= 40) break;
                }
            }
        } catch (Throwable ignored) {
            // 物品表不可用时静默
        }
    }

    @Override
    public void render(DrawContext gfx, int mouseX, int mouseY, float delta) {
        // 背景
        gfx.fill(0, 0, this.width, this.height, 0xB8101418);

        int x = (this.width - 260) / 2;

        gfx.drawText(this.textRenderer, "展示框搜索", x, 30, 0xFFFFD24A, true);

        // 当前目标
        var targets = FrameFinderState.getTargets();
        String tstr = targets.isEmpty() ? "§7（无，回车添加输入框内容）" : String.join("§7, §f", targets);
        gfx.drawText(this.textRenderer, "当前目标: " + tstr, x, 76, 0xFFB8C4D4, true);

        // 实时匹配预览
        if (!previewMatches.isEmpty()) {
            gfx.drawText(this.textRenderer, "匹配的物品 (" + previewMatches.size() + ")，回车设为目标:", x, 96, 0xFF8A94A4, true);
            int row = 0, col = 0;
            for (ItemStack st : previewMatches) {
                int ix = x + col * 26;
                int iy = 108 + row * 26;
                gfx.drawItem(st, ix, iy);
                col++;
                if (col >= 10) { col = 0; row++; }
                if (row >= 3) break;
            }
        }

        // 命中展示框列表（滚轮翻页，可看全部）
        if (FrameFinderState.hasTargets() && !FrameFinderState.hits.isEmpty()) {
            gfx.drawText(this.textRenderer, "命中的展示框 (" + FrameFinderState.hits.size() + ")「滚轮翻页」:", 20, 120, 0xFF8A94A4, true);
            int pageSize = 8;
            hitScroll = Math.max(0, Math.min(hitScroll, FrameFinderState.hits.size() - pageSize));
            for (int shown = 0; shown < pageSize; shown++) {
                int idx = hitScroll + shown;
                if (idx >= FrameFinderState.hits.size()) break;
                FrameFinderState.Hit hit = FrameFinderState.hits.get(idx);
                Entity e = hit.frame;
                gfx.drawText(this.textRenderer, String.format("%3d. %s  ·  %.1f 格  ·  (%d, %d, %d)",
                        idx + 1, hit.itemName, hit.distance,
                        (int) e.getX(), (int) e.getY(), (int) e.getZ()),
                    20, 134 + shown * 12, 0xFFE6EDF5, true);
            }
        }

        gfx.drawText(this.textRenderer, "回车=添加目标  /sou style=连线/方框  K/ESC=关闭", x, this.height - 40, 0xFF8A94A4, true);

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter
            String word = searchBox.getText().trim();
            if (!word.isEmpty()) {
                boolean added = FrameFinderState.toggleTarget(word);
                if (added) {
                    searchBox.setText("");
                    lastQuery = "";
                    previewMatches.clear();
                } else {
                    // 已存在则移除
                    FrameFinderState.toggleTarget(word);
                }
            }
            return true;
        }
        if (keyCode == 256) { // ESC
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        hitScroll += (scrollY > 0 ? -3 : 3);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
