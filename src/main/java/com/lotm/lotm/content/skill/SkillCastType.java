package com.lotm.lotm.content.skill;

import com.lotm.lotm.LotMMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 技能释放类型枚举
 * <p>
 * 定义技能的释放方式和行为模式。
 * 用于 UI 显示标签，以及后端逻辑判断（如是否需要持续扣费）。
 */
public enum SkillCastType {
    /**
     * 瞬时释放型 (Instant)
     * 按下即触发，一次性消耗，无持续状态。
     * 例：空气弹、火焰跳跃。
     */
    INSTANT("instant"),

    /**
     * 切换型 (Toggle)
     * 开启后持续生效，再次释放关闭。通常伴随持续灵性消耗或灵性上限占用。
     * 例：灵视、冥想。
     */
    TOGGLE("toggle"),

    /**
     * 持续召唤型 (Summon - Maintain)
     * 召唤物存在期间持续消耗灵性。可以在 UI 中手动关闭。
     * 例：召唤历史投影（维持）。
     */
    SUMMON_MAINTAIN("summon_maintain"),

    /**
     * 一次性召唤型 (Summon - One Shot)
     * 支付一次性代价召唤，之后不再消耗。
     * 例：召唤灵界生物（契约）。
     */
    SUMMON_INSTANT("summon_instant"),

    /**
     * 蓄力型 (Charging)
     * 按住按键进行蓄力，松开时释放。蓄力时间越长效果越强。
     * 例：阳炎（蓄力版）。
     */
    CHARGING("charging"),

    /**
     * 长按引导型 (Channeling)
     * 必须按住按键才能维持效果，松开即停止。每 Tick 消耗灵性。
     * 例：飓风（持续控制）。
     */
    CHANNELING("channeling"),

    /**
     * 吟唱型 (Chanting)
     * 按下后进入吟唱状态，不可移动或受限，吟唱结束后释放。
     * 例：高序列仪式魔法。
     */
    CHANTING("chanting"),

    /**
     * 被动型 (Passive)
     * 习得即生效，无需放入快捷栏，无需主动释放。
     * 通常在服务端/客户端的 Tick 事件中自动触发。
     */
    PASSIVE("passive");

    private final String key;

    SkillCastType(String key) {
        this.key = key;
    }

    /**
     * 获取本地化名称
     * key: skill_type.lotmmod.instant
     */
    public MutableComponent getDisplayName() {
        return Component.translatable("skill_type." + LotMMod.MODID + "." + key);
    }

    /**
     * 是否为持续激活类型（需要在 AbilityContainer 中存储状态）
     */
    public boolean isToggleOrMaintain() {
        return this == TOGGLE || this == SUMMON_MAINTAIN || this == PASSIVE;
    }

    /**
     * 是否为被动技能
     */
    public boolean isPassive() {
        return this == PASSIVE;
    }
}
