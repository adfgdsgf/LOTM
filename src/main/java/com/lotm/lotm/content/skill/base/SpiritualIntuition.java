package com.lotm.lotm.content.skill.base;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.api.capability.IBeyonderState;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CIntuitionAlertPacket;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.CastContext;
import com.lotm.lotm.content.skill.SkillCastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 技能：灵性直觉 (Spiritual Intuition)
 */
public class SpiritualIntuition extends AbstractSkill {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "spiritual_intuition");

    public SpiritualIntuition() {
        super(ID, SkillCastType.PASSIVE);
    }

    @Override
    public double getCost(Player player, int sequence) {
        return 0;
    }

    @Override
    public int getCooldown(Player player) {
        return 0;
    }

    @Override
    protected void performEffect(ServerPlayer player, Level level, CastContext context) {
    }

    @Override
    public void onPassiveTick(Player player, IBeyonderState state) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;

        if (player.tickCount % 20 != 0) return;

        int dangerLevel = scanForDanger(serverPlayer, state.getSequence());

        if (dangerLevel > 0) {
            PacketHandler.sendToPlayer(new S2CIntuitionAlertPacket(dangerLevel), serverPlayer);
        }
    }

    private int scanForDanger(ServerPlayer player, int sequence) {
        double range = getDetectionRange(sequence);
        AABB searchBox = player.getBoundingBox().inflate(range, range / 2, range);

        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, searchBox, mob -> {
            // 修复点：移除了 mob == player 的检查
            // 因为 Mob 类和 Player 类是互斥的，编译器知道 mob 永远不可能是 player
            if (!mob.isAlive()) return false;

            boolean isTargetingMe = mob.getTarget() == player;

            boolean isEnemy = mob instanceof Enemy;
            boolean isVeryClose = mob.distanceToSqr(player) < 25.0;

            return isTargetingMe || (isEnemy && isVeryClose);
        });

        return mobs.size();
    }

    private double getDetectionRange(int sequence) {
        return 10.0 + (9 - sequence) * 4.0;
    }
}
