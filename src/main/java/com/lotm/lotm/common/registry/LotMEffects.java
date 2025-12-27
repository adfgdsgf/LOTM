package com.lotm.lotm.common.registry;

import com.lotm.lotm.LotMMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 药水效果注册表
 * <p>
 * 使用 Forge 的 DeferredRegister 进行原生注册。
 * 目前为空，保留此类是为了后续添加其他效果时无需修改主类初始化逻辑 (高可拓展性)。
 */
public class LotMEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LotMMod.MODID);

    // 之前的 SpiritVisionEffect 已移除，因为它现在由 Capability 技能系统接管
    // public static final RegistryObject<MobEffect> SPIRIT_VISION = ...;

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
