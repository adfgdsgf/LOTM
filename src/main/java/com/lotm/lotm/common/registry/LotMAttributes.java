package com.lotm.lotm.common.registry;

import com.lotm.lotm.LotMMod;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * LOTM 属性注册表
 * <p>
 * 定义模组专用的实体属性，用于数值对抗系统。
 * 使用 RangedAttribute 确保数值在合理范围内。
 */
public class LotMAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, LotMMod.MODID);

    /**
     * 灵性侦测 (Spiritual Detection)
     * 决定了观察者看破灵性隐蔽的能力。
     * 基础范围: 0.0 - 1024.0
     * 默认值: 0.0 (普通人无灵视)
     */
    public static final RegistryObject<Attribute> SPIRITUAL_DETECTION = ATTRIBUTES.register("spiritual_detection",
            () -> new RangedAttribute("attribute.lotmmod.spiritual_detection", 0.0, 0.0, 1024.0).setSyncable(true));

    /**
     * 灵性收敛 (Spiritual Concealment)
     * 决定了目标隐藏自身灵体气场的能力。
     * 基础范围: 0.0 - 1024.0
     * 默认值: 0.0 (灵性完全外溢/无收敛)
     */
    public static final RegistryObject<Attribute> SPIRITUAL_CONCEALMENT = ATTRIBUTES.register("spiritual_concealment",
            () -> new RangedAttribute("attribute.lotmmod.spiritual_concealment", 0.0, 0.0, 1024.0).setSyncable(true));

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}
