package com.lotm.lotm.content.pathway.impl.seer;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import net.minecraft.resources.ResourceLocation;

/**
 * 占卜家途径 (Seer Pathway)
 * <p>
 * 特点：高灵性，低生命/防御。
 */
public class SeerPathway extends BeyonderPathway {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "seer");

    public SeerPathway() {
        super(ID, 0x8A2BE2, new Builder()
                // 序列 9: 占卜家
                // 灵性: +50, 生命: +0, 攻击: +0
                .add(9, 50.0, 0.0, 0.0,
                        LotMSkills.SPIRIT_VISION.getId(),
                        LotMSkills.SPIRITUAL_INTUITION.getId()
                )
                // 序列 8: 小丑 (身体素质开始增强)
                // 灵性: +150, 生命: +2.0 (1心), 攻击: +1.0
                .add(8, 150.0, 2.0, 1.0
                        // LotMSkills.PAPER_SUBSTITUTE.getId()
                )
        );
    }
}
