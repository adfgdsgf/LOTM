package com.lotm.lotm.client.gui.divination;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EntityCategory implements IDivinationCategory {
    private final List<TargetEntry> entries = new ArrayList<>();

    public EntityCategory() {
        // 工业级修复：不再假设刷怪蛋的 RegistryName 格式。
        // 而是遍历所有物品，检查其是否为 SpawnEggItem 的实例。
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof SpawnEggItem eggItem) {
                EntityType<?> type = eggItem.getType(null);

                // 过滤掉非生物或杂项实体 (如画、矿车)
                if (type.getCategory() == MobCategory.MISC) continue;

                ItemStack icon = new ItemStack(item);

                // 使用 EntityType 的描述作为显示名称
                Component name = type.getDescription();

                entries.add(new TargetEntry("ENTITY", ForgeRegistries.ENTITY_TYPES.getKey(type), icon, name));
            }
        }

        // 补充：某些实体可能没有刷怪蛋 (如 Boss)，如果需要支持，需额外手动添加逻辑
        // 但对于通用占卜，基于刷怪蛋的列表通常足够且安全。

        entries.sort(Comparator.comparing(e -> e.name().getString()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.lotmmod.divination.tab.entities");
    }

    @Override
    public List<TargetEntry> getEntries() {
        return entries;
    }
}
