package com.lotm.lotm.content.skill.base;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.api.capability.IBeyonderState;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CIntuitionAlertPacket;
import com.lotm.lotm.content.logic.base.intuition.IntuitionLogic;
import com.lotm.lotm.content.logic.base.intuition.MonsterIntuitionLogic;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.CastContext;
import com.lotm.lotm.content.skill.SkillCastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 技能：灵性直觉 (Spiritual Intuition)
 * <p>
 * 修正记录：
 * 1. 将通用检测频率从 20 ticks (1秒) 调整为 200 ticks (10秒)，解决其他途径（如占卜家）的刷屏问题。
 * 2. 保持怪物途径的分流逻辑，防止双重触发。
 */
public class SpiritualIntuition extends AbstractSkill {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "spiritual_intuition");
    private static final ResourceLocation PATHWAY_MONSTER = new ResourceLocation(LotMMod.MODID, "monster");

    public SpiritualIntuition() {
        super(ID, SkillCastType.PASSIVE);
    }

    @Override
    public double getCost(Player player, int sequence) { return 0; }

    @Override
    public int getCooldown(Player player) { return 0; }

    @Override
    protected void performEffect(ServerPlayer player, Level level, CastContext context) {}

    @Override
    public boolean canBeDeactivated(Player player) {
        return !isMonsterPathway(player);
    }

    @Override
    public void onPassiveTick(Player player, IBeyonderState state) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;

        boolean isMonster = isMonsterPathway(player);

        // 1. 基础危险感知 (通用逻辑 - 适用于占卜家等其他途径)
        // ★★★ 核心修复：频率改为 200 ticks (10秒)，与怪物途径保持一致，防止刷屏 ★★★
        if (!isMonster && player.tickCount % 200 == 0) {
            int dangerLevel = IntuitionLogic.scanForDanger(serverPlayer, state.getSequence(), false);
            if (dangerLevel > 0) {
                PacketHandler.sendToPlayer(new S2CIntuitionAlertPacket(dangerLevel), serverPlayer);
            }
        }

        // 2. 怪物途径特有逻辑 (专属逻辑)
        // MonsterPathwayLogic 内部已控制频率为 200 ticks (10秒)
        // 这里分流是为了避免怪物途径玩家同时触发上面的通用逻辑和下面的专属逻辑，导致双重提示
        if (isMonster) {
            MonsterIntuitionLogic.tick(serverPlayer, state);
        }
    }

    private boolean isMonsterPathway(Player player) {
        var cap = player.getCapability(com.lotm.lotm.common.capability.BeyonderStateProvider.CAPABILITY);
        return cap.map(state -> PATHWAY_MONSTER.equals(state.getPathwayId())).orElse(false);
    }
}
