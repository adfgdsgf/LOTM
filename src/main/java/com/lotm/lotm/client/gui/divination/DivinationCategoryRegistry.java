package com.lotm.lotm.client.gui.divination;

import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 占卜分类注册表 (客户端专用)
 * <p>
 * 职责：
 * 管理所有占卜目标分类 (Block, Entity, etc.)。
 * 允许其他模组或插件通过此注册表添加新的分类 Tab。
 */
public class DivinationCategoryRegistry {

    // 使用 LinkedHashMap 保持注册顺序 (Tab 的显示顺序)
    private static final Map<ResourceLocation, IDivinationCategory> CATEGORIES = new LinkedHashMap<>();

    /**
     * 注册一个新的分类
     * @param id 分类 ID (用于唯一标识)
     * @param category 分类实例
     */
    public static void register(ResourceLocation id, IDivinationCategory category) {
        if (CATEGORIES.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate divination category ID: " + id);
        }
        CATEGORIES.put(id, category);
    }

    /**
     * 获取所有已注册的分类
     */
    public static List<IDivinationCategory> getAllCategories() {
        return ImmutableList.copyOf(CATEGORIES.values());
    }

    /**
     * 清空注册表 (仅用于重载资源时，虽然一般 GUI 分类不需要重载)
     */
    public static void clear() {
        CATEGORIES.clear();
    }
}
