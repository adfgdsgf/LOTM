package com.lotm.lotm.content.pathway;

import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.List;

/**
 * 序列数据定义 (Sequence Definition)
 * <p>
 * 这是一个不可变的数据对象 (Value Object)，用于定义某条途径中某个特定序列的属性。
 * 包含灵性加成、生命加成、攻击加成等基础属性配置。
 */
public record PathwaySequence(
        int level,                      // 序列等级 (9-0)
        double maxSpiritualityBonus,    // 灵性上限加成 (相比前一序列)
        double maxHealthBonus,          // 生命上限加成 (预留，暂未实装逻辑)
        double attackDamageBonus,       // 攻击力加成 (预留，暂未实装逻辑)
        List<ResourceLocation> skills   // 该序列解锁的技能列表
) {
    public PathwaySequence {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("Sequence level must be between 0 and 9");
        }
        if (skills == null) {
            skills = Collections.emptyList();
        }
    }
}
