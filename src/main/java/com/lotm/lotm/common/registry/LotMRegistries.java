package com.lotm.lotm.common.registry;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.content.item.SpiritPendulumItem;
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
 * <p>
 * 修正记录：
 * 1. 注册了 spirit_pendulum 物品。
 */
public class LotMRegistries {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LotMMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LotMMod.MODID);

    // =========================================
    //               物品定义
    // =========================================

    public static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test_item",
            () -> new Item(new Item.Properties()));

    // 新增：灵摆
    public static final RegistryObject<Item> SPIRIT_PENDULUM = ITEMS.register("spirit_pendulum",
            () -> new SpiritPendulumItem(new Item.Properties().stacksTo(1)));

    // =========================================
    //             物品栏定义
    // =========================================

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + LotMMod.MODID + ".main"))
            .icon(() -> new ItemStack(SPIRIT_PENDULUM.get())) // 使用灵摆作为图标
            .displayItems((params, output) -> {
                ITEMS.getEntries().forEach(reg -> output.accept(reg.get()));
            })
            .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
        LotMAttributes.register(bus);
        LotMEffects.register(bus);

        LotMSkills.register();
        LotMPathways.register();
    }
}
