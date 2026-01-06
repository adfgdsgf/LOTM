package com.lotm.lotm.util;

import com.lotm.lotm.client.util.EntityRelationEvaluator;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 威胁评估器 (Threat Evaluator)
 * <p>
 * 职责：
 * 1. 综合判断目标对观察者的威胁程度。
 * 2. 考虑因素：序列压制、敌对关系、生物属性。
 */
public class ThreatEvaluator {

    public enum ThreatLevel {
        NONE(0),
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        FATAL(4);

        public final int value;
        ThreatLevel(int value) { this.value = value; }
    }

    /**
     * 评估目标对观察者的威胁等级
     */
    public static ThreatLevel evaluate(Player observer, LivingEntity target) {
        if (observer == target) return ThreatLevel.NONE;

        // 1. 基础敌对关系检查
        // 如果是绝对盟友（如驯服的狼、同队玩家），则视为无威胁
        EntityRelationEvaluator.RelationType relation = EntityRelationEvaluator.evaluate(target, observer);
        if (relation == EntityRelationEvaluator.RelationType.FRIENDLY) {
            return ThreatLevel.NONE;
        }

        // 2. 非凡者对抗逻辑 (PVP)
        if (target instanceof Player targetPlayer) {
            return evaluateBeyonderThreat(observer, targetPlayer);
        }

        // 3. 怪物对抗逻辑 (PVE)
        return evaluateMobThreat(observer, target);
    }

    private static ThreatLevel evaluateBeyonderThreat(Player observer, Player target) {
        // 获取双方序列 (普通人默认为序列 10，方便计算)
        int obsSeq = getSequence(observer);
        int tgtSeq = getSequence(target);

        // 序列压制判断 (数值越小越强)
        // diff > 0 表示观察者序列更低（弱），目标序列更高（强）
        // 例如：观察者 Seq9(9)，目标 Seq7(7) -> diff = 2
        int diff = obsSeq - tgtSeq;

        if (diff >= 2) return ThreatLevel.FATAL;   // 差2个序列以上，极度危险
        if (diff >= 0) return ThreatLevel.HIGH;    // 同级或略高，高威胁
        if (diff == -1) return ThreatLevel.MEDIUM; // 目标低1级，中等威胁 (仍有反杀可能)

        return ThreatLevel.LOW; // 目标低2级以上，低威胁
    }

    private static ThreatLevel evaluateMobThreat(Player observer, LivingEntity target) {
        // 简单基于属性判断
        // 未来可以接入 EntityType Tags 来标记 Boss 或强力生物
        boolean isEnemy = target instanceof Enemy;

        // 如果不是天生敌人且没打我，威胁较低 (中立生物)
        if (!isEnemy && target.getLastHurtByMob() != observer) {
            return ThreatLevel.LOW;
        }

        double health = target.getMaxHealth();
        // 获取攻击力属性，如果没有则默认为 0
        double damage = 0;
        if (target.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
            damage = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        }

        // 简单的评分算法：生命值 + 攻击力 * 5
        double score = health + damage * 5;

        // 阈值调整
        if (score > 100) return ThreatLevel.FATAL; // 类似 Warden/Wither/Elder Guardian
        if (score > 50) return ThreatLevel.HIGH;   // 类似 Iron Golem/Enderman/Ravager
        if (score > 20) return ThreatLevel.MEDIUM; // 类似 Zombie/Skeleton/Creeper

        return ThreatLevel.LOW; // 类似 Silverfish/Small Slime
    }

    private static int getSequence(Player player) {
        return player.getCapability(BeyonderStateProvider.CAPABILITY)
                .map(state -> state.isBeyonder() ? state.getSequence() : 10)
                .orElse(10);
    }
}
