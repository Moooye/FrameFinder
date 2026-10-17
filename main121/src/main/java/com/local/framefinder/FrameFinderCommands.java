package com.local.framefinder;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/**
 * 客户端命令 /sou：
 * <pre>
 *   /sou 钻石        添加/取消目标（重复输入取消）
 *   /sou off         清除全部目标
 *   /sou list        查看当前目标
 *   /sou radius 96   调扫描半径
 *   /sou style       连线/方框切换
 * </pre>
 * 1.21.1 的注册方式：{@code ClientCommandRegistrationCallback.EVENT} +
 * {@code ClientCommandManager.literal/argument}。
 */
final class FrameFinderCommands {

    private FrameFinderCommands() {}

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("sou")
                .then(ClientCommandManager.argument("物品", StringArgumentType.greedyString()).executes(ctx -> {
                    String word = StringArgumentType.getString(ctx, "物品").trim();
                    if (word.equalsIgnoreCase("off")) {
                        FrameFinderState.clearTargets();
                        feedback(ctx.getSource(), "已清除全部搜索目标");
                        return 1;
                    }
                    boolean added = FrameFinderState.toggleTarget(word);
                    if (added) {
                        feedback(ctx.getSource(), "§a开始搜索: §f" + word + " §7(再次输入取消)");
                    } else {
                        feedback(ctx.getSource(), "§e已取消: §f" + word);
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    FrameFinderState.clearTargets();
                    feedback(ctx.getSource(), "已清除全部搜索目标");
                    return 1;
                }))
                .then(ClientCommandManager.literal("list").executes(ctx -> {
                    var list = FrameFinderState.getTargets();
                    if (list.isEmpty()) {
                        feedback(ctx.getSource(), "当前没有搜索目标。用法: /sou <物品名>");
                    } else {
                        feedback(ctx.getSource(), "当前目标(" + list.size() + "): §f" + String.join("§7, §f", list));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("radius")
                    .then(ClientCommandManager.argument("格数", StringArgumentType.word()).executes(ctx -> {
                        try {
                            int r = Integer.parseInt(StringArgumentType.getString(ctx, "格数"));
                            r = Math.max(16, Math.min(256, r));
                            FrameFinderState.setRadius(r);
                            feedback(ctx.getSource(), "扫描半径 = §f" + r + "§7 格");
                        } catch (NumberFormatException e) {
                            feedback(ctx.getSource(), "§c半径需要是数字");
                        }
                        return 1;
                    })))
                .then(ClientCommandManager.literal("style").executes(ctx -> {
                    var cfg = FrFndrConfig.get();
                    cfg.style = "BOX".equals(cfg.style) ? "LINES" : "BOX";
                    FrFndrConfig.save();
                    feedback(ctx.getSource(), "高亮样式 = §f" + ("BOX".equals(cfg.style) ? "方框" : "连线"));
                    return 1;
                }))
            );
        });
    }

    private static void feedback(FabricClientCommandSource src, String msg) {
        src.sendFeedback(Text.literal("" + msg));
    }
}
