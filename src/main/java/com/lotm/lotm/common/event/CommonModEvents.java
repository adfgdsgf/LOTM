package com.lotm.lotm.common.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.capability.skillbar.ISkillBarContainer;
import com.lotm.lotm.common.registry.LotMAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LotMMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ISkillBarContainer.class);
        // IBeyonderState 和 IAbilityContainer 有 @AutoRegisterCapability
    }

    /**
     * ★★★ 核心：注入自定义属性 ★★★
     * 将 SPIRITUAL_DETECTION 和 SPIRITUAL_CONCEALMENT 注入到所有 LivingEntity。
     * 这确保了无论是原版僵尸、玩家还是其他模组的龙，都有这两个属性用于对抗判定。
     */
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // 遍历所有注册的实体类型
        event.getTypes().forEach(entityType -> {
            // 为所有生物添加新属性
            // 注意：add 方法内部会自动去重，且如果是 LivingEntity 的子类才生效
            if (!event.has(entityType, LotMAttributes.SPIRITUAL_DETECTION.get())) {
                event.add(entityType, LotMAttributes.SPIRITUAL_DETECTION.get());
            }
            if (!event.has(entityType, LotMAttributes.SPIRITUAL_CONCEALMENT.get())) {
                event.add(entityType, LotMAttributes.SPIRITUAL_CONCEALMENT.get());
            }
        });

        // 显式日志，方便调试确认注入成功
        LotMMod.LOGGER.info("Injected LOTM attributes into all registered entity types.");
    }
}
