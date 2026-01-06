package com.lotm.lotm.content.skill.base;

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

public class SpiritVision extends AbstractSkill {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "spirit_vision");

    public SpiritVision() {
        super(ID, SkillCastType.TOGGLE);
    }

    @Override
    public double getCost(Player player, int sequence) {
        return 3.0; // 启动消耗
    }

    @Override
    public double getUpkeepCost(Player player, int sequence) {
        return Math.max(0.1, 0.5 - (9 - sequence) * 0.04);
    }

    @Override
    public int getCooldown(Player player) {
        // ★★★ 这里的冷却现在是指“关闭后多久能再次开启” ★★★
        return 60; // 3秒冷却
    }

    @Override
    protected void performEffect(ServerPlayer player, Level level, CastContext context) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.5F);
    }

    @Override
    public void onDeactivate(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.5F);
    }
}
