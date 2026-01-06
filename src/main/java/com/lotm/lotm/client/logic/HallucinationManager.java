package com.lotm.lotm.client.logic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 幻觉管理器 (客户端独有)
 * <p>
 * 修正记录：
 * 1. 增加了 lookAtPlayer 方法，强制幻觉实体朝向玩家。
 * 2. 优化了生成逻辑，确保实体一出生就盯着玩家看。
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class HallucinationManager {

    private static final Random RANDOM = new Random();
    private static final List<HallucinationEntry> activeHallucinations = new ArrayList<>();
    private static List<EntityType<?>> cachedMonsterTypes = null;

    private static void initMonsterCache() {
        cachedMonsterTypes = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            if (type.getCategory() == MobCategory.MONSTER && type.canSummon()) {
                cachedMonsterTypes.add(type);
            }
        }
        if (cachedMonsterTypes.isEmpty()) {
            cachedMonsterTypes.add(EntityType.ZOMBIE);
        }
    }

    public static void spawnFakeMonster(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;

        if (level == null || player == null) return;

        if (cachedMonsterTypes == null) initMonsterCache();

        EntityType<?> randomType = cachedMonsterTypes.get(RANDOM.nextInt(cachedMonsterTypes.size()));
        Entity fakeEntity = randomType.create(level);
        if (fakeEntity == null) return;

        // 1. 设置位置
        fakeEntity.setPos(x, y, z);

        // 2. ★★★ 关键修正：让幻觉强制盯着玩家 ★★★
        // 计算朝向角度
        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        // atan2 返回的是弧度，需要转角度。Minecraft 的 0 度是正南，需要转换坐标系
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);

        fakeEntity.setYRot(yaw);
        fakeEntity.setYHeadRot(yaw); // 头部也转过去
        fakeEntity.setXRot(0); // 平视

        // 如果是 Mob，还需要设置 body 旋转，防止只有头转了身体没转
        if (fakeEntity instanceof Mob mob) {
            mob.setYBodyRot(yaw);
            mob.setNoAi(false); // 开启 AI 以播放 idle 动画 (呼吸等)
            // 禁用移动和攻击，只保留外观
            mob.setSpeed(0);
        }

        // 3. 属性设置
        fakeEntity.setSilent(true);
        int fakeId = -(10000 + RANDOM.nextInt(1000000));
        fakeEntity.setId(fakeId);

        level.addFreshEntity(fakeEntity);
        activeHallucinations.add(new HallucinationEntry(fakeEntity, 40 + RANDOM.nextInt(40)));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            activeHallucinations.clear();
            return;
        }

        if (!activeHallucinations.isEmpty()) {
            Iterator<HallucinationEntry> iterator = activeHallucinations.iterator();
            while (iterator.hasNext()) {
                HallucinationEntry entry = iterator.next();
                entry.ticksLeft--;

                // 持续更新朝向：让幻觉一直盯着玩家 (即便玩家移动了)
                // 这会让效果更惊悚，仿佛它在注视你
                Entity e = entry.entity;
                if (e.isAlive()) {
                    double dx = mc.player.getX() - e.getX();
                    double dz = mc.player.getZ() - e.getZ();
                    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
                    e.setYRot(yaw);
                    e.setYHeadRot(yaw);
                }

                if (!entry.entity.isAlive() || entry.ticksLeft <= 0) {
                    entry.entity.remove(Entity.RemovalReason.DISCARDED);
                    iterator.remove();
                }
            }
        }
    }

    private static class HallucinationEntry {
        Entity entity;
        int ticksLeft;

        public HallucinationEntry(Entity entity, int ticksLeft) {
            this.entity = entity;
            this.ticksLeft = ticksLeft;
        }
    }
}
