package com.lotm.lotm.common.registry;

import com.lotm.lotm.LotMMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组核心注册中心
 * LotM Core Registries
 *
 * 负责统一管理模组中所有的 DeferredRegister 实例。
 * 包含物品、方块、属性、药水效果、创造模式物品栏等基础游戏元素的注册入口。
 */
public class LotMRegistries {

    /**
     * 物品注册器
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LotMMod.MODID);

    /**
     * 创造模式物品栏注册器
     */
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LotMMod.MODID);

    // =========================================
    //               物品定义
    // =========================================

    public static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test_item",
            () -> new Item(new Item.Properties()));

    // =========================================
    //             物品栏定义
    // =========================================

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + LotMMod.MODID + ".main"))
            .icon(() -> new ItemStack(TEST_ITEM.get()))
            .displayItems((params, output) -> {
                ITEMS.getEntries().forEach(reg -> output.accept(reg.get()));
            })
            .build());

    /**
     * 注册所有内容到事件总线
     *
     * @param bus Mod主事件总线
     */
    public static void register(IEventBus bus) {
        // 1. 注册 Forge 原生注册表 (DeferredRegister)
        ITEMS.register(bus);
        TABS.register(bus);

        // ★★★ 统一注册入口 ★★★
        LotMAttributes.register(bus); // 属性
        LotMEffects.register(bus);    // 药水效果

        // 2. 初始化并注册自定义数据驱动系统
        // 注意：这些系统不使用 DeferredRegister，而是静态初始化
        LotMSkills.register();
        LotMPathways.register();
    }
}
