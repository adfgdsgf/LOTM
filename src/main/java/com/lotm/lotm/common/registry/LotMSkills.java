package com.lotm.lotm.common.registry;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.base.Boxing;
import com.lotm.lotm.content.skill.base.SpiritualIntuition;
import com.lotm.lotm.content.skill.seer.Divination;
import com.lotm.lotm.content.skill.base.SpiritVision;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能注册表 (Skill Registry)
 * <p>
 * 负责管理模组中所有的自定义技能实例。
 * 这是一个自定义的注册表，不依赖于 Forge 的 Registry 系统，
 * 因为技能逻辑主要在数据层处理，且需要更灵活的查找方式。
 */
public class LotMSkills {

    /**
     * 技能存储映射
     * Key: 技能的 ResourceLocation (如 lotmmod:boxing)
     * Value: 技能的具体实例
     */
    private static final Map<ResourceLocation, AbstractSkill> SKILLS = new HashMap<>();

    // ==================================================
    //                 基础技能 (Base)
    // ==================================================

    /**
     * 拳击 (Boxing)
     * 基础格斗技能，所有途径通用或作为默认技能
     */
    public static final Boxing BOXING = new Boxing();
    // 新增：灵性直觉
    public static final SpiritualIntuition SPIRITUAL_INTUITION = new SpiritualIntuition();
    public static final Divination DIVINATION = new Divination();

    // ==================================================
    //               占卜家途径 (Seer)
    // ==================================================

    /**
     * 灵视 (Spirit Vision)
     * 序列 9 占卜家能力
     */
    public static final SpiritVision SPIRIT_VISION = new SpiritVision();

    // 未来可以在这里添加更多，例如：
    // public static final Divination DIVINATION = new Divination();
    // public static final PaperSubstitute PAPER_SUBSTITUTE = new PaperSubstitute();

    /**
     * 执行技能注册逻辑
     * 在 Mod 初始化阶段调用 (LotMRegistries.register 中调用)
     */
    public static void register() {
        // 1. 注册基础技能
        registerSkill(BOXING);

        // 2. 注册占卜家技能
        registerSkill(SPIRIT_VISION);
        registerSkill(SPIRITUAL_INTUITION);
        registerSkill(DIVINATION);

        // 注册更多...

        LotMMod.LOGGER.info("Registered {} skills.", SKILLS.size());
    }

    /**
     * 注册单个技能
     *
     * @param skill 要注册的技能实例
     * @throws IllegalStateException 如果 ID 重复
     */
    private static void registerSkill(AbstractSkill skill) {
        if (SKILLS.containsKey(skill.getId())) {
            throw new IllegalStateException("Duplicate skill ID: " + skill.getId());
        }
        SKILLS.put(skill.getId(), skill);
    }

    /**
     * 根据 ID 获取技能实例
     *
     * @param id 技能的 ResourceLocation
     * @return 技能实例，如果不存在则返回 null
     */
    public static AbstractSkill getSkill(ResourceLocation id) {
        return SKILLS.get(id);
    }
}
