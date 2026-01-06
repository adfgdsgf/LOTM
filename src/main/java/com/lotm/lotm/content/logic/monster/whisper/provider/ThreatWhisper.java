package com.lotm.lotm.content.logic.monster.whisper.provider;

import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.content.logic.monster.whisper.IWhisperProvider;
import com.lotm.lotm.content.logic.monster.whisper.WhisperManager;
import com.lotm.lotm.util.ThreatEvaluator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;

/**
 * 威胁呓语：感知危险
 * <p>
 * 修正：增加了对“高序列非凡者”的判断逻辑。
 */
public class ThreatWhisper implements IWhisperProvider {
    @Override
    public Component tryGenerateWhisper(ServerPlayer player, int sequence) {
        double range = 20.0 + (9 - sequence) * 5.0;
        AABB box = player.getBoundingBox().inflate(range);

        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        Collections.shuffle(entities);

        for (LivingEntity entity : entities) {
            boolean isThreat = false;
            String key = "message.lotm.whisper.threat.monster"; // 默认为怪物

            // 1. 判断是否为高危非凡者 (玩家)
            if (entity instanceof Player targetPlayer) {
                if (isHighSequenceBeyonder(targetPlayer, sequence)) {
                    isThreat = true;
                    key = "message.lotm.whisper.threat.beyonder"; // "XX方向有令人战栗的非凡波动..."
                }
            }

            // 2. 判断是否为高危怪物 (通过 ThreatEvaluator)
            if (!isThreat) {
                ThreatEvaluator.ThreatLevel threat = ThreatEvaluator.evaluate(player, entity);
                if (threat.value >= ThreatEvaluator.ThreatLevel.HIGH.value) {
                    isThreat = true;
                }
            }

            if (isThreat) {
                Component dir = WhisperManager.getFuzzyDirection(player, entity.position());
                return Component.translatable(key, dir);
            }
        }
        return null;
    }

    /**
     * 判断目标是否为“危险的非凡者”
     * 逻辑：
     * 1. 对方拥有非凡能力。
     * 2. 对方序列比自己高 (数值更小)，或者对方是中高序列 (序列7及以下)。
     */
    private boolean isHighSequenceBeyonder(Player target, int mySequence) {
        return target.getCapability(BeyonderStateProvider.CAPABILITY).map(state -> {
            int targetSeq = state.getSequence();
            // 对方序列比我高 (数值小)，或者对方已经是中序列强者(<=7)
            return targetSeq < mySequence || targetSeq <= 7;
        }).orElse(false);
    }
}
