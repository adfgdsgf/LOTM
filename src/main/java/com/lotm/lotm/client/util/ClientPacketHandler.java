package com.lotm.lotm.client.util;

import com.lotm.lotm.client.logic.HallucinationManager;
import com.lotm.lotm.client.renderer.ClientDivinationRenderer;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端包处理器
 */
public class ClientPacketHandler {

    public static void handleSkillBarSync(CompoundTag nbt) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
            bar.deserializeNBT(nbt);
        });
    }

    public static void handleHallucination(double x, double y, double z, int type) {
        Minecraft.getInstance().execute(() -> {
            if (type == 0) {
                HallucinationManager.spawnFakeMonster(x, y, z);
            }
        });
    }

    /**
     * 处理占卜结果反馈
     * <p>
     * 修正：
     * 1. 方法签名增加了 int entityId。
     * 2. 触发 ClientDivinationRenderer 进行视觉高亮。
     */
    public static void handleDivinationResult(boolean success, Component message, Vec3 targetPos, int entityId) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // 1. Action Bar 消息反馈
        player.displayClientMessage(message, true);

        // 2. 视觉反馈 (粒子 + 高亮)
        if (success && targetPos != null) {
            // A. 触发高亮框
            ClientDivinationRenderer.setHighlight(targetPos, entityId);

            // B. 粒子特效
            // 起始点：眼睛位置 + 视线方向 * 1.0 (推远一点，防止在脑袋里生成看不到)
            Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(1.0));

            // 计算指向目标的方向向量
            Vec3 dir = targetPos.subtract(start).normalize();

            // 生成 50 个粒子 (更长、更密)
            for (int i = 0; i < 50; i++) {
                double d = i * 0.15;
                double px = start.x + dir.x * d;
                double py = start.y + dir.y * d;
                double pz = start.z + dir.z * d;

                // 使用 END_ROD (高亮) 和 NAUTILUS (轨迹) 混合
                player.level().addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0, 0);

                // 每隔几个加一点魔法烟雾，增加神秘感
                if (i % 5 == 0) {
                    player.level().addParticle(ParticleTypes.WITCH, px, py, pz, 0, 0, 0);
                }
            }
        } else {
            // 失败特效：在面前冒黑烟
            Vec3 failPos = player.getEyePosition().add(player.getLookAngle().scale(1.5));
            for (int i = 0; i < 15; i++) {
                player.level().addParticle(ParticleTypes.SMOKE,
                        failPos.x + (Math.random() - 0.5) * 0.5,
                        failPos.y + (Math.random() - 0.5) * 0.5,
                        failPos.z + (Math.random() - 0.5) * 0.5,
                        0, 0, 0);
            }
        }
    }
}
