package com.lotm.lotm.client.renderer.skill.impl;

import com.lotm.lotm.client.renderer.skill.ISkillRenderer;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.client.util.VisualEffectHelper;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.content.skill.base.SpiritVision;
import com.lotm.lotm.util.PerceptionEvaluator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Random;

/**
 * 灵视技能渲染器 (Spirit Vision Renderer)
 * <p>
 * 职责：
 * 1. 处理灵视状态下的粒子特效 (Spirit Particles)。
 * 2. 处理灵视状态下的全屏晕影 (HUD Vignette)。
 * <p>
 * 注意：实体的 X-Ray 透视渲染逻辑由 {@link com.lotm.lotm.mixin.client.MixinLivingEntityRenderer} 处理。
 */
public class SpiritVisionRenderer implements ISkillRenderer {

    // 渲染范围 (半径)
    private static final float RENDER_RANGE = 32.0f;
    private static final Random RANDOM = new Random();

    @Override
    public void renderLevel(RenderLevelStageEvent event, Player player) {
        // 性能检查：仅在特定渲染阶段执行
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        // 确保在客户端世界
        if (!(player.level() instanceof ClientLevel level)) return;

        // 检查技能是否激活
        boolean isActive = player.getCapability(AbilityContainerProvider.CAPABILITY)
                .map(cap -> cap.isSkillActive(SpiritVision.ID))
                .orElse(false);

        if (!isActive) return;

        // 遍历渲染距离内的实体
        for (Entity entity : level.entitiesForRendering()) {
            // 跳过自己
            if (entity == player) continue;
            // 仅针对生物
            if (!(entity instanceof LivingEntity livingTarget)) continue;
            // 距离检查 (平方距离更高效)
            if (entity.distanceToSqr(player) > RENDER_RANGE * RENDER_RANGE) continue;

            // 粒子生成逻辑：
            // 如果玩家与目标之间有物理视线 (Line of Sight)，则生成少量粒子辅助判断。
            // 被遮挡的实体由 X-Ray Mixin 处理，此处不生成粒子以免穿模混乱。
            if (player.hasLineOfSight(entity)) {

                // 感知对抗检查 (Detection vs Concealment)
                if (!PerceptionEvaluator.canPerceive(player, livingTarget)) continue;

                // 随机生成粒子 (降低密度，避免遮挡视线)
                if (RANDOM.nextFloat() < 0.2f) {
                    Vector4f color = VisualEffectHelper.getXRayColor(livingTarget);
                    Vector3f particleColor = new Vector3f(color.x, color.y, color.z);
                    spawnSpiritParticles(level, entity, particleColor);
                }
            }
        }
    }

    /**
     * 在实体周围生成灵体粒子
     */
    private void spawnSpiritParticles(ClientLevel level, Entity entity, Vector3f color) {
        double w = entity.getBbWidth();
        double h = entity.getBbHeight();

        // 在实体 Hitbox 范围内随机位置
        double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * w * 1.2;
        double y = entity.getY() + RANDOM.nextDouble() * h;
        double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * w * 1.2;

        // 使用 DustParticle (红石粉类型的粒子，支持自定义颜色)
        level.addParticle(new DustParticleOptions(color, 0.8f), x, y, z, 0, 0, 0);
    }

    @Override
    public void renderHud(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        // 绘制屏幕四周的晕影 (提示玩家灵视已开启)
        // 颜色定义在 LotMClientColors 中，方便统一修改
        int color = LotMClientColors.SPIRIT_VISION_VIGNETTE;

        int borderSize = 10; // 晕影宽度

        // 顶边
        graphics.fill(0, 0, screenWidth, borderSize, color);
        // 底边
        graphics.fill(0, screenHeight - borderSize, screenWidth, screenHeight, color);
        // 左边 (避开顶底，防止重叠颜色加深)
        graphics.fill(0, borderSize, borderSize, screenHeight - borderSize, color);
        // 右边
        graphics.fill(screenWidth - borderSize, borderSize, screenWidth, screenHeight - borderSize, color);
    }
}
