package com.lotm.lotm.content.pathway.impl.seer;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import net.minecraft.resources.ResourceLocation;

/**
 * 占卜家途径 (Seer Pathway)
 * <p>
 * 特点：高灵性，低生命/防御。
 * <p>
 * 数值设定：
 * 侦测：较高 (每级 +10)
 * 隐蔽：较高 (每级 +10) -> 无面人阶段会大幅提升
 */
public class SeerPathway extends BeyonderPathway {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "seer");

    public SeerPathway() {
        super(ID, 0x8A2BE2, new Builder()
                // 序列 9: 占卜家
                // 灵性: +50
                // 侦测: +10.0 (标准非凡者水平)
                // 隐蔽: +10.0 (标准非凡者水平)
                .add(9, 50.0, 0.0, 0.0, 10.0, 10.0,
                        LotMSkills.SPIRIT_VISION.getId(),
                        LotMSkills.SPIRITUAL_INTUITION.getId(),
                        LotMSkills.DIVINATION.getId()
                )
                // 序列 8: 小丑 (Clown)
                // 灵性: +150, 生命: +2.0, 攻击: +1.0
                // 侦测: +10.0
                // 隐蔽: +10.0
                .add(8, 150.0, 2.0, 1.0, 10.0, 10.0
                        // LotMSkills.PAPER_SUBSTITUTE.getId()
                )
        );
    }
}
