package com.lotm.lotm.content.logic.base.intuition;

import com.lotm.lotm.util.ThreatEvaluator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 直觉系统通用逻辑
 * <p>
 * 职责：封装危险扫描算法，供技能类调用。
 * 将计算逻辑从 Skill 类中剥离，实现解耦。
 */
public class IntuitionLogic {

    /**
     * 扫描周围的危险等级
     *
     * @param player 玩家
     * @param sequence 序列等级
     * @param isMonsterPathway 是否为怪物途径 (影响感知范围和判定逻辑)
     * @return 最高威胁等级数值 (0-4)
     */
    public static int scanForDanger(ServerPlayer player, int sequence, boolean isMonsterPathway) {
        // 1. 计算感知范围
        double range = 10.0 + (9 - sequence) * 4.0;
        if (isMonsterPathway) range *= 1.5; // 怪物途径范围更大

        AABB searchBox = player.getBoundingBox().inflate(range);

        // 2. 获取范围内实体
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive());

        int maxThreat = 0;

        for (LivingEntity entity : entities) {
            // 怪物途径能感知隐形单位
            if (!isMonsterPathway && entity.isInvisible()) continue;

            // 3. 评估威胁
            ThreatEvaluator.ThreatLevel threat = ThreatEvaluator.evaluate(player, entity);

            // 4. 过滤逻辑
            // 普通途径只感知针对自己的威胁 (Targeting Me) 或极近距离的敌人
            if (!isMonsterPathway) {
                boolean isTargetingMe = (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player);
                // 如果没有针对我，且距离较远，忽略
                if (!isTargetingMe && entity.distanceToSqr(player) > 25.0) continue;
            }
            // 怪物途径感知所有环境威胁 (Map Hack 模式)

            if (threat.value > maxThreat) {
                maxThreat = threat.value;
            }
        }

        return maxThreat;
    }
}
