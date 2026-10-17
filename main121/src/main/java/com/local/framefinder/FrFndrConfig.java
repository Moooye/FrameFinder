package com.local.framefinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置持久化（.minecraft/config/frfndr.json）：
 * 保存扫描半径与上次搜索目标，重启游戏不丢。
 */
public final class FrFndrConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
        FabricLoader.getInstance().getConfigDir().resolve("frfndr.json");

    /** 扫描半径（格） */
    public int radius = 64;
    /** 上次搜索目标 */
    public List<String> targets = new ArrayList<>();
    /** 高亮样式: LINES = 连线, BOX = 方框 */
    public String style = "LINES";

    private static FrFndrConfig INSTANCE = new FrFndrConfig();

    public static FrFndrConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                String raw = Files.readString(FILE, StandardCharsets.UTF_8);
                raw = raw.replace("\uFEFF", "");
                FrFndrConfig loaded = GSON.fromJson(raw, FrFndrConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    INSTANCE.radius = Math.max(16, Math.min(256, INSTANCE.radius));
                }
            }
        } catch (Throwable t) {
            FrameFinderClient.LOG.warn("[FrFndr] 配置读取失败：{}", t.toString());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            FrameFinderClient.LOG.warn("[FrFndr] 配置保存失败：{}", t.toString());
        }
    }

    /** 便捷：targets 序列化辅助 */
    public static final Type TARGET_LIST = new TypeToken<List<String>>() {}.getType();
}
