package com.lotm.lotm.client.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * 通用实体关系评估器 (Entity Relation Evaluator)
 * <p>
 * 工业级设计职责：
 * 1. 纯粹逻辑 (Pure Logic)：只负责判断生物与玩家的社会关系。
 * 2. 动态威胁评估 (Dynamic Threat Assessment)：基于属性而非硬编码列表判断生物威胁度。
 */
public class EntityRelationEvaluator {

    /**
     * 实体关系类型枚举
     */
    public enum RelationType {
        HOSTILE,    // 敌对 (Red): 天生的怪物 (Zombie, Skeleton)
        AGGRESSIVE, // 激怒 (Orange): 本该中立但当前正在攻击玩家 (Aggro Wolf, Pigman)
        NEUTRAL,    // 中立 (Yellow): 有反击能力，但当前未攻击玩家 (Polar Bear, Iron Golem, Modded Pig)
        FRIENDLY    // 友善 (Green): 无害生物 (Pig, Cow) 或 盟友 (Tamed Wolf, Villager)
    }

    /**
     * 评估目标与观察者之间的关系
     *
     * @param target 被观察的目标
     * @param observer 观察者 (通常是玩家)
     * @return 关系类型枚举
     */
    public static RelationType evaluate(LivingEntity target, Player observer) {
        // 1. [最高优先级] 动态仇恨检查：它正在打我吗？
        if (isAggressiveTowards(target, observer)) {
            // 如果是怪物正在打我 -> 红色
            if (target instanceof Enemy) {
                return RelationType.HOSTILE;
            }
            // 如果是普通生物(如狼、魔改猪)正在打我 -> 橙色 (提示玩家这是变节行为)
            return RelationType.AGGRESSIVE;
        }

        // 2. 显式盟友检查：它是绝对的自己人吗？
        if (isExplicitlyFriendly(target, observer)) {
            return RelationType.FRIENDLY;
        }

        // 3. 种族检查：它是天生的怪物吗？
        // 即使它没看我，僵尸也是红色的。
        if (target instanceof Enemy) {
            return RelationType.HOSTILE;
        }

        // 4. [核心逻辑变更] 威胁潜力检查
        // 区分 "猪(无害)" 和 "北极熊/魔改猪(有反击能力)"
        if (hasAttackPotential(target)) {
            // 有攻击力，但没打我 -> 黄色 (中立/警戒)
            return RelationType.NEUTRAL;
        }

        // 5. 默认：无害生物 -> 绿色 (友善)
        // 猪、牛、羊、蝙蝠等没有攻击力的生物落入此列
        return RelationType.FRIENDLY;
    }

    /**
     * 判断目标是否正在攻击观察者
     */
    private static boolean isAggressiveTowards(LivingEntity target, Player observer) {
        if (target instanceof Mob mob) {
            LivingEntity currentTarget = mob.getTarget();
            // 如果目标的攻击对象是玩家
            return currentTarget != null && currentTarget.is(observer);
        }
        return false;
    }

    /**
     * 判断目标是否是显式的盟友 (驯服、队伍、村民)
     */
    private static boolean isExplicitlyFriendly(LivingEntity target, Player observer) {
        if (target == observer) return true;

        // 队伍判断
        if (target.getTeam() != null && target.isAlliedTo(observer)) {
            return true;
        }

        // 驯服判断
        if (target instanceof OwnableEntity ownable) {
            return observer.getUUID().equals(ownable.getOwnerUUID());
        }

        // 村民判断 (通常视为绝对友善，除非有模组修改)
        if (target instanceof Villager) {
            return true;
        }

        return false;
    }

    /**
     * 判断目标是否具有攻击潜力 (物理伤害能力)
     * <p>
     * 原理：检查生物是否拥有 ATTACK_DAMAGE 属性且基础值 > 0。
     * 兼容性：完美兼容所有遵循 Minecraft 标准属性系统的模组生物。
     */
    private static boolean hasAttackPotential(LivingEntity target) {
        // 某些生物可能根本没有注册攻击属性 (如原版猪、羊)
        if (target.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            double damage = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
            // 只要攻击力大于 0，就视为有威胁潜力
            return damage > 0;
        }
        return false;
    }
}
