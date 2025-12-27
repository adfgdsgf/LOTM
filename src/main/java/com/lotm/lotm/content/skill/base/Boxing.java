package com.lotm.lotm.content.skill.base;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.CastContext;
import com.lotm.lotm.content.skill.SkillCastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Boxing extends AbstractSkill {
    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "boxing");

    public Boxing() {
        super(ID, SkillCastType.INSTANT); // 瞬发类型
    }

    @Override
    public double getCost(Player player, int sequence) {
        return 5.0; // 基础消耗
    }

    @Override
    public int getCooldown(Player player) {
        return 20; // 1秒
    }

    @Override
    protected void performEffect(ServerPlayer player, Level level, CastContext context) {
        // 工业级修正：使用 modifier1 而非 isShiftDown，解耦物理按键
        float damage = context.modifier1() ? 15.0f : 10.0f;

        // 简单的范围伤害逻辑
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(2.0));
        AABB area = AABB.ofSize(center, 3.0, 3.0, 3.0);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            // 击退效果
            target.knockback(1.0, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
    }
}
