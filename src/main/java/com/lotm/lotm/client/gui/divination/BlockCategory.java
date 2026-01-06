package com.lotm.lotm.client.gui.divination;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 方块分类
 * <p>
 * 修正记录：
 * 1. 使用 stack.getHoverName() 替代 item.getDescription()。
 *    getHoverName() 返回的是经过完整本地化处理的名称 (如 "橡木原木")，
 *    而 getDescription() 有时仅返回 Translation Key (如 "block.minecraft.oak_log")，
 *    这导致模糊搜索无法匹配中文字符。
 */
public class BlockCategory implements IDivinationCategory {
    private final List<TargetEntry> entries = new ArrayList<>();

    public BlockCategory() {
        for (Item item : ForgeRegistries.ITEMS) {
            // 仅添加方块物品
            if (item instanceof BlockItem blockItem) {
                // 排除空气
                if (blockItem.getBlock() == Blocks.AIR) continue;

                ItemStack stack = new ItemStack(item);

                // ★★★ 核心修正：使用 getHoverName() 获取准确的本地化名称 ★★★
                Component name = stack.getHoverName();

                entries.add(new TargetEntry("BLOCK", ForgeRegistries.ITEMS.getKey(item), stack, name));
            }
        }
        // 按名称排序，方便查找
        entries.sort(Comparator.comparing(e -> e.name().getString()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.lotmmod.divination.tab.blocks");
    }

    @Override
    public List<TargetEntry> getEntries() {
        return entries;
    }
}
