package com.lotm.lotm.util;

import com.lotm.lotm.LotMMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class LotMText {
    // 前缀常量
    private static final String PREFIX_PATHWAY = "pathway." + LotMMod.MODID + ".";
    private static final String PREFIX_SKILL = "skill." + LotMMod.MODID + ".";
    private static final String PREFIX_MSG = "message." + LotMMod.MODID + ".";
    private static final String PREFIX_GUI = "gui." + LotMMod.MODID + ".";

    // --- 通用获取方法 ---
    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    // --- 具体业务的 Key 生成器 ---

    // 获取途径名称: pathway.lotmmod.seer
    public static MutableComponent pathwayName(ResourceLocation pathwayId) {
        return translatable(PREFIX_PATHWAY + pathwayId.getPath());
    }

    // 获取序列名称: pathway.lotmmod.seer.seq9
    public static MutableComponent sequenceName(ResourceLocation pathwayId, int sequence) {
        return translatable(PREFIX_PATHWAY + pathwayId.getPath() + ".seq" + sequence);
    }

    // 获取技能名称: skill.lotmmod.boxing
    public static MutableComponent skillName(ResourceLocation skillId) {
        return translatable(PREFIX_SKILL + skillId.getPath());
    }

    // 获取技能描述: skill.lotmmod.boxing.desc
    public static MutableComponent skillDesc(ResourceLocation skillId) {
        return translatable(PREFIX_SKILL + skillId.getPath() + ".desc");
    }

    // --- 常用消息 ---
    public static final MutableComponent MSG_NOT_BEYONDER = translatable(PREFIX_MSG + "not_beyonder");
    public static final MutableComponent MSG_SPIRITUALITY_LOW = translatable(PREFIX_MSG + "spirituality_low");
    public static final MutableComponent MSG_COOLDOWN = translatable(PREFIX_MSG + "cooldown");
}
