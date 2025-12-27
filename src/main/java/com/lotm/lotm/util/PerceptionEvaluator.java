package com.lotm.lotm.util;

import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.registry.LotMAttributes;
import com.lotm.lotm.content.skill.seer.SpiritVision;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 通用感知评估器 (Perception Evaluator)
 * <p>
 * 职责：
 * 1. 核心博弈逻辑：统一处理“侦测”与“隐蔽”的对抗。
 * 2. 属性驱动：基于 Minecraft Attribute 系统，而非硬编码列表。
 */
public class PerceptionEvaluator {

    /**
     * 判断观察者是否能从**神秘学层面**感知到目标
     * <p>
     * 核心公式：有效侦测值 >= 目标隐蔽值 -> 可感知
     *
     * @param observer 观察者
     * @param target 目标
     * @return true 如果可以感知 (看穿隐蔽)
     */
    public static boolean canPerceive(Player observer, LivingEntity target) {
        if (observer == target) return true;

        // 1. 获取目标的隐蔽值 (Concealment)
        double concealment = getConcealmentValue(target);

        // 如果目标没有隐蔽能力 (0)，则绝对可见 (除非有物理遮挡，那是 RayTrace 的事)
        if (concealment <= 0.0) return true;

        // 2. 获取观察者的侦测值 (Detection)
        double detection = getDetectionValue(observer);

        // 3. 对抗判定
        return detection >= concealment;
    }

    /**
     * 计算目标的当前隐蔽值
     */
    private static double getConcealmentValue(LivingEntity target) {
        // 读取属性 (Attribute)
        // 默认值是 0。如果目标装备了封印物或使用了隐秘技能，此属性会增加。
        double val = target.getAttributeValue(LotMAttributes.SPIRITUAL_CONCEALMENT.get());

        // 兼容性：如果未来有“潜行”增加隐蔽的设定，可以在这里叠加
        // if (target.isCrouching()) val += 5.0;

        return val;
    }

    /**
     * 计算观察者的当前侦测值
     * <p>
     * 算法：基础属性 + 灵视加成(基于序列)
     */
    private static double getDetectionValue(Player player) {
        // 1. 基础属性 (来自装备、魔药被动等)
        double base = player.getAttributeValue(LotMAttributes.SPIRITUAL_DETECTION.get());

        // 2. 检查灵视是否开启
        boolean isSpiritVisionActive = player.getCapability(AbilityContainerProvider.CAPABILITY)
                .map(cap -> cap.isSkillActive(SpiritVision.ID))
                .orElse(false);

        if (isSpiritVisionActive) {
            // 获取序列等级，计算加成
            // 序列 9 -> 加成 10.0
            // 序列 0 -> 加成 100.0
            // 公式：(10 - 序列号) * 10
            int sequence = player.getCapability(BeyonderStateProvider.CAPABILITY)
                    .map(state -> state.getSequence())
                    .orElse(9); // 默认按序列9处理

            double visionBonus = (10 - sequence) * 10.0;
            base += visionBonus;
        }

        return base;
    }
}
