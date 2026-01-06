package com.lotm.lotm.client.gui.divination;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 占卜分类接口
 * <p>
 * 实现此接口以添加新的占卜目标类型 (如: 生物群系, 结构, 玩家)。
 */
public interface IDivinationCategory {
    /**
     * 分类名称 (用于 Tab 按钮显示)
     */
    Component getDisplayName();

    /**
     * 获取该分类下的所有候选项
     * @return 候选项列表
     */
    List<TargetEntry> getEntries();

    /**
     * 目标条目数据类
     */
    record TargetEntry(String typeId, ResourceLocation id, ItemStack icon, Component name) {}
}
