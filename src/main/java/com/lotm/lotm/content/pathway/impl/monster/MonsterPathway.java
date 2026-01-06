package com.lotm.lotm.content.pathway.impl.monster;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import net.minecraft.resources.ResourceLocation;

/**
 * 怪物途径 (Monster Pathway) - 命运之轮途径
 * <p>
 * 特点：极高的灵性直觉，被动能力为主。
 * 颜色：银白色 (0xC0C0C0)
 * <p>
 * 数值设定：
 * 侦测：极高 (每级 +15 ~ +20)
 * 隐蔽：中等 (每级 +5 ~ +10)
 */
public class MonsterPathway extends BeyonderPathway {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "monster");

    public MonsterPathway() {
        super(ID, 0xC0C0C0, new Builder()
                        // 序列 9: 怪物 (Monster)
                        // 灵性: +100
                        // 侦测: +20.0 (极高)
                        // 隐蔽: +5.0  (一般)
                        .add(9, 100.0, 0.0, 0.0, 20.0, 10.0,
                                LotMSkills.SPIRIT_VISION.getId(),
                                LotMSkills.SPIRITUAL_INTUITION.getId()
                        )
                // 序列 8: 机器 (Machine) - 预留
                // 假设数值：灵性+150, 侦测+15, 隐蔽+5
                // .add(8, 150.0, 1.0, 0.0, 15.0, 5.0, ...)
        );
    }
}
