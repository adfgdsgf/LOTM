package com.lotm.lotm.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 通用配置 (Common Config)
 * 包含游戏逻辑、数值平衡等需要服务端同步的设置。
 */
public class LotMCommonConfig {

    public static final LotMCommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<LotMCommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LotMCommonConfig::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    // ==================== 数值平衡 ====================
    public final ForgeConfigSpec.DoubleValue spiritualityCostMultiplier;
    public final ForgeConfigSpec.IntValue baseSpiritualityRegen;

    public LotMCommonConfig(ForgeConfigSpec.Builder builder) {
        builder.push("General_Settings");

        spiritualityCostMultiplier = builder
                .comment(" ",
                        "================================================================",
                        " [Spirituality Cost Multiplier]",
                        " Global multiplier for ability spirituality consumption.",
                        "----------------------------------------------------------------",
                        " [灵性消耗倍率]",
                        " 所有技能灵性消耗的全局倍率。",
                        "================================================================")
                .translation("config.lotmmod.common.spirituality_cost")
                .defineInRange("SpiritualityCostMultiplier", 1.0, 0.0, 100.0);

        baseSpiritualityRegen = builder
                .comment(" ",
                        "================================================================",
                        " [Base Spirituality Regen]",
                        " Amount of spirituality recovered per second (base).",
                        "----------------------------------------------------------------",
                        " [基础灵性恢复]",
                        " 每秒恢复的基础灵性数值。",
                        "================================================================")
                .translation("config.lotmmod.common.base_regen")
                .defineInRange("BaseSpiritualityRegen", 1, 0, 1000);

        builder.pop();
    }
}
