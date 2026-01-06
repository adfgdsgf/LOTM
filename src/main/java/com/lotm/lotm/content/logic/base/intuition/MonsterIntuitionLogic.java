package com.lotm.lotm.content.logic.base.intuition;

import com.lotm.lotm.api.capability.IBeyonderState;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CHallucinationPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CIntuitionAlertPacket;
import com.lotm.lotm.content.logic.monster.whisper.WhisperManager;
import com.lotm.lotm.util.ThreatEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 怪物途径专属逻辑
 * <p>
 * 修正记录：
 * 1. 完善了幻觉生成坐标算法：增加射线检测(防卡墙)和贴地检测(防浮空)。
 */
public class MonsterIntuitionLogic {

    private static final Random RANDOM = new Random();

    // 记录玩家上次收到“有用信息”的时间戳
    private static final Map<UUID, Long> USEFUL_WHISPER_COOLDOWN = new HashMap<>();
    private static final long USEFUL_COOLDOWN_TICKS = 400; // 20秒

    public static void tick(ServerPlayer player, IBeyonderState state) {
        int seq = state.getSequence();
        int ticks = player.tickCount;

        if (ticks % 100 == 0) handleWhispers(player, seq);
        if (ticks % 200 == 50) handleVisualHallucinations(player, seq);
        if (ticks % 200 == 100) handleSpiritualIntuition(player, seq);
    }

    private static float getInstability(int seq) {
        if (seq <= 4) return 0.0f;
        else if (seq <= 7) return 0.1f;
        else return 0.05f;
    }

    private static void handleWhispers(ServerPlayer player, int seq) {
        long gameTime = player.level().getGameTime();

        // 1. 有用信息
        Component usefulMsg = WhisperManager.generateUseful(player, seq);
        if (usefulMsg != null) {
            long lastTime = USEFUL_WHISPER_COOLDOWN.getOrDefault(player.getUUID(), 0L);
            if (gameTime - lastTime >= USEFUL_COOLDOWN_TICKS) {
                player.displayClientMessage(usefulMsg, true);
                player.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.3f, 0.1f);
                USEFUL_WHISPER_COOLDOWN.put(player.getUUID(), gameTime);
            }
            return;
        }

        // 2. 废话
        float instability = getInstability(seq);
        if (instability <= 0) return;

        if (RANDOM.nextFloat() < instability) {
            Component fluffMsg = WhisperManager.generateFluff(player, seq);
            if (fluffMsg != null) {
                player.displayClientMessage(fluffMsg, true);
                player.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.3f, 0.1f);
            }
        }
    }

    private static void handleVisualHallucinations(ServerPlayer player, int seq) {
        float chance = getInstability(seq);
        if (chance <= 0) return;
        if (RANDOM.nextFloat() < chance) triggerVisualHallucination(player);
    }

    private static void handleSpiritualIntuition(ServerPlayer player, int seq) {
        double range = 16.0 + (9 - seq) * 4.0;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive());
        int maxThreatValue = 0;
        for (LivingEntity target : entities) {
            ThreatEvaluator.ThreatLevel level = ThreatEvaluator.evaluate(player, target);
            if (level.value > maxThreatValue) maxThreatValue = level.value;
            if (maxThreatValue >= ThreatEvaluator.ThreatLevel.FATAL.value) break;
        }
        if (maxThreatValue > 0) PacketHandler.sendToPlayer(new S2CIntuitionAlertPacket(maxThreatValue), player);
    }

    /**
     * 触发视觉幻觉 - 智能坐标计算
     */
    private static void triggerVisualHallucination(ServerPlayer player) {
        Level level = player.level();

        // 1. 获取基础视线方向 (忽略Y轴，保证只在水平面上找)
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0, look.z).normalize();

        // 2. 随机偏移角度 (左右 30 度之内)，让幻觉不总是正正地出现在准星中间
        double angleOffset = (RANDOM.nextDouble() - 0.5) * Math.toRadians(60); // +/- 30度
        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);
        double newX = horizontalLook.x * cos - horizontalLook.z * sin;
        double newZ = horizontalLook.x * sin + horizontalLook.z * cos;
        Vec3 targetDir = new Vec3(newX, 0, newZ).normalize();

        // 3. 射线检测：防止生成在墙里
        // 尝试生成在 3 到 7 格之间
        double distance = 3.0 + RANDOM.nextDouble() * 4.0;
        Vec3 startPos = player.getEyePosition();
        Vec3 endPos = startPos.add(targetDir.scale(distance));

        // 发射射线，检测方块碰撞
        BlockHitResult hitResult = level.clip(new ClipContext(
                startPos,
                endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 spawnPos;
        if (hitResult.getType() != HitResult.Type.MISS) {
            // 如果撞墙了，生成在墙壁前面一点点 (退回 0.5 格)，防止嵌在墙里
            spawnPos = hitResult.getLocation().subtract(targetDir.scale(0.5));
        } else {
            // 没撞墙，就用最大距离
            spawnPos = endPos;
        }

        // 4. 贴地检测：防止生成在半空中
        // 从目标点向下寻找地面
        BlockPos currentBlock = BlockPos.containing(spawnPos);
        BlockPos groundPos = null;

        // 向下搜寻 5 格
        for (int i = 0; i < 5; i++) {
            BlockPos check = currentBlock.below(i);
            if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) {
                // 找到了固体方块，生成在它的上面
                groundPos = check.above();
                break;
            }
        }

        // 如果找不到地面（比如在悬崖边），或者地面太远，就不生成了，避免鬼畜
        if (groundPos == null) return;

        // 发送包到客户端生成
        PacketHandler.sendToPlayer(new S2CHallucinationPacket(
                spawnPos.x,
                groundPos.getY(), // 使用贴地后的 Y 轴
                spawnPos.z,
                0
        ), player);
    }
}
