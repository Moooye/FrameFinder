package com.local.framefinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 搜索目标与展示框扫描。
 * <p>
 * 匹配规则（三层，任一命中即算）：
 * <ol>
 *   <li>注册名包含：输入 {@code diamond} 命中 {@code minecraft:diamond}</li>
 *   <li>中文翻译名包含：输入 {@code 钻石} 命中 {@code 钻石}</li>
 *   <li>英文翻译名包含：输入 {@code sword} 命中 {@code Diamond Sword}</li>
 * </ol>
 * 扫描范围：玩家周围已加载区块内的全部展示框（含发光展示框），默认 64 格。
 */
public final class FrameFinderState {

    /** 当前激活的搜索词（null = 未激活） */
    private static final Set<String> targets = new LinkedHashSet<>();

    /** 扫描半径（格）——持久化到 FrFndrConfig */
    public static int getRadius() { return FrFndrConfig.get().radius; }
    public static void setRadius(int r) { FrFndrConfig.get().radius = r; FrFndrConfig.save(); }

    /** 扫描结果缓存：命中的展示框 */
    public static final List<Hit> hits = new ArrayList<>();
    private static long lastScanAt = 0;
    private static final long SCAN_INTERVAL_MS = 250;
    public static volatile int lastScanFramesTotal = 0;

    /** 一个命中的展示框 */
    public static final class Hit {
        public final ItemFrameEntity frame;
        public final ItemStack stack;
        public final double distance;
        public final String itemName;

        Hit(ItemFrameEntity frame, ItemStack stack, double distance, String itemName) {
            this.frame = frame;
            this.stack = stack;
            this.distance = distance;
            this.itemName = itemName;
        }
    }

    private FrameFinderState() {}

    // ---------- 目标管理 ----------

    /** 添加目标；返回 false 表示已存在（即取消该目标） */
    public static boolean toggleTarget(String word) {
        String key = normalize(word);
        if (key.isEmpty()) return true;
        if (targets.contains(key)) {
            targets.remove(key);
            saveTargets();
            return false;
        }
        targets.add(key);
        saveTargets();
        return true;
    }

    public static void clearTargets() {
        targets.clear();
        hits.clear();
        saveTargets();
    }

    public static boolean hasTargets() {
        return !targets.isEmpty();
    }

    public static List<String> getTargets() {
        return new ArrayList<>(targets);
    }

    /** 启动时从配置恢复目标 */
    public static void restoreTargets() {
        targets.clear();
        targets.addAll(FrFndrConfig.get().targets);
    }

    private static void saveTargets() {
        FrFndrConfig.get().targets = new ArrayList<>(targets);
        FrFndrConfig.save();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    // ---------- 扫描（主线程定时调用） ----------

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        if (!hasTargets()) {
            if (!hits.isEmpty()) hits.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScanAt < SCAN_INTERVAL_MS) return;
        lastScanAt = now;

        scan(mc);
    }

    private static void scan(MinecraftClient mc) {
        ClientWorld level = mc.world;
        hits.clear();

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        double r2 = (double) getRadius() * getRadius();

        // 遍历附近实体（客户端已知的全部实体，含展示框/发光展示框）
        for (Entity e : level.getEntities()) {
            if (!(e instanceof ItemFrameEntity frame)) continue;

            double dx = e.getX() - px;
            double dy = e.getY() - py;
            double dz = e.getZ() - pz;
            double dist2 = dx * dx + dy * dy + dz * dz;
            if (dist2 > r2) continue;

            ItemStack stack;
            try {
                stack = frame.getHeldItemStack();
            } catch (Throwable t) {
                continue;
            }
            if (stack == null || stack.isEmpty()) continue;

            if (matches(stack, targets)) {
                String name = stack.getName().getString();
                hits.add(new Hit(frame, stack, Math.sqrt(dist2), name));
            }
        }

        // 近的排前面
        hits.sort((a, b) -> Double.compare(a.distance, b.distance));
        lastScanFramesTotal = hits.size();
    }

    /** 匹配：注册名 / 显示名 / 附魔书内容（附魔描述名） */
    static boolean matches(ItemStack stack, Set<String> targets) {
        // 1) 注册名
        String idPath;
        try {
            idPath = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            idPath = "";
        }
        // 2) 中文名 / 3) 英文名
        String display = stack.getName().getString().toLowerCase(Locale.ROOT);

        for (String t : targets) {
            if (idPath.contains(t)) return true;
            if (display.contains(t)) return true;
            // 4) 附魔书：把书里存的附魔描述名也算进来
            if (isEnchantedBook(stack) && enchantmentText(stack, t)) return true;
        }
        return false;
    }

    /** 是否为含存储附魔的书 */
    private static boolean isEnchantedBook(ItemStack stack) {
        try {
            return stack.isOf(Items.ENCHANTED_BOOK);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 附魔书里的附魔描述名是否包含目标词 */
    private static boolean enchantmentText(ItemStack stack, String target) {
        try {
            ItemEnchantmentsComponent stored =
                stack.getComponents().get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (stored == null || stored.isEmpty()) return false;
            for (RegistryEntry<Enchantment> holder : stored.getEnchantments()) {
                Enchantment ench = holder.value();
                String desc = ench.description().getString().toLowerCase(Locale.ROOT);
                if (desc.contains(target)) return true;
                // 附魔注册名（soul_speed 等）也支持
                String enchId = holder.getKey()
                        .map(k -> k.getValue().getPath()).orElse("");
                if (enchId.contains(target)) return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /** GUI 搜索用：按输入词试匹配一个 stack */
    public static boolean matchesWord(ItemStack stack, String word) {
        Set<String> one = Set.of(normalize(word));
        return matches(stack, one);
    }
}
