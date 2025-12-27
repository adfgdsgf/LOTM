package com.lotm.lotm.content.skill.seer;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.CastContext;
import com.lotm.lotm.content.skill.SkillCastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 技能：灵视 (Spirit Vision)
 * <p>
 * 类型：切换型 (Toggle)
 * 效果：开启后持续消耗灵性，允许玩家看到生物的灵体（透视/发光）。
 * 实现：状态存储在 AbilityContainer 中，客户端 Renderer 根据状态渲染。
 * <p>
 * 修正：
 * 1. 移除了重复的 sendSystemMessage 调用，交由基类统一处理。
 * 2. 仅保留音效播放逻辑，职责更单一。
 */
public class SpiritVision extends AbstractSkill {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "spirit_vision");

    public SpiritVision() {
        super(ID, SkillCastType.TOGGLE);
    }

    @Override
    public double getCost(Player player, int sequence) {
        // 启动消耗 (瞬间)
        return 10.0;
    }

    @Override
    public double getUpkeepCost(Player player, int sequence) {
        // 维持消耗 (每 Tick)
        // 序列9消耗 0.5/tick，序列0消耗 0.1/tick
        // 公式：0.5 - (9 - seq) * 0.04 (约数)
        return Math.max(0.1, 0.5 - (9 - sequence) * 0.04);
    }

    @Override
    public int getCooldown(Player player) {
        return 20; // 1秒冷却，防止连点鬼畜
    }

    @Override
    protected void performEffect(ServerPlayer player, Level level, CastContext context) {
        // 开启时的音效 (仅开启瞬间播放一次)
        // 注意：不需要发送消息，基类 AbstractSkill.cast 会自动发送 "已开启: 灵视"
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.5F);
    }

    @Override
    public void onDeactivate(ServerPlayer player) {
        // 关闭时的音效
        // 注意：不需要发送消息，基类 AbstractSkill.cast 会自动发送 "已关闭: 灵视"
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.5F);
    }
}
