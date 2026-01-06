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

    // ★★★ 新增：GUI 文本 ★★★
    public static final MutableComponent GUI_LOCKED = translatable(PREFIX_GUI + "locked");

    // ★★★ 新增：方向与呓语 ★★★
    public static final MutableComponent DIR_FRONT = translatable(PREFIX_MSG + "dir.front");
    public static final MutableComponent DIR_BACK = translatable(PREFIX_MSG + "dir.back");
    public static final MutableComponent DIR_LEFT = translatable(PREFIX_MSG + "dir.left");
    public static final MutableComponent DIR_RIGHT = translatable(PREFIX_MSG + "dir.right");
    public static final MutableComponent DIR_ABOVE = translatable(PREFIX_MSG + "dir.above");
    public static final MutableComponent DIR_BELOW = translatable(PREFIX_MSG + "dir.below");
}
