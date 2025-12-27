package com.lotm.lotm.client.util;

import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.content.skill.seer.SpiritVision;
import com.lotm.lotm.util.PerceptionEvaluator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用视觉特效辅助类 (Visual Effect Helper)
 * <p>
 * 职责：
 * 1. 封装复杂的视觉判断逻辑 (如 X-Ray 透视条件)。
 * 2. 计算动态颜色 (基于实体关系、生命值、距离增强)。
 * 3. 封装渲染层过滤逻辑 (用于 Mixin 调用)。
 * <p>
 * 工业级重构：
 * 将原 Mixin 中的具体业务逻辑剥离至此，确保 Mixin 仅作为胶水代码，
 * 提高代码的可测试性和可维护性。
 */
public class VisualEffectHelper {

    private static final float VISION_RANGE_SQR = 32.0f * 32.0f;
    private static final float XRAY_ALPHA = 0.4f;

    /**
     * 判断目标是否应该应用 X-Ray 透视渲染
     *
     * @param target 目标实体
     * @return 是否应用透视
     */
    public static boolean shouldApplyXRay(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || target == null || target == player) return false;
        if (target.distanceToSqr(player) > VISION_RANGE_SQR) return false;

        // 1. 检查技能是否开启 (前提条件)
        boolean isSpiritVisionActive = player.getCapability(AbilityContainerProvider.CAPABILITY)
                .map(cap -> cap.isSkillActive(SpiritVision.ID))
                .orElse(false);

        if (!isSpiritVisionActive) return false;

        // 2. 物理视线检查
        // 如果能直接看见实体，则不需要 X-Ray (交给粒子或普通渲染)
        // 只有被遮挡时才需要 X-Ray
        if (player.hasLineOfSight(target)) return false;

        // 3. ★★★ 核心对抗接入 ★★★
        // 判断侦测等级是否足以看穿目标的隐蔽
        if (!PerceptionEvaluator.canPerceive(player, target)) return false;

        return true;
    }

    /**
     * 获取 X-Ray 渲染颜色 (包含距离增强逻辑)
     * <p>
     * 整合了：
     * 1. 基础关系颜色判断。
     * 2. 距离衰减反转 (近距离增强饱和度/亮度)。
     * 3. 生命值状态调整。
     *
     * @param target 目标实体
     * @return 计算后的 RGBA 向量 (用于 Shader)
     */
    public static Vector4f getDynamicXRayColor(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 默认回退颜色
        if (player == null) return LotMClientColors.toVector4f(LotMClientColors.SPIRIT_VISION_NEUTRAL, XRAY_ALPHA);

        // 1. 获取基础颜色 (从常量表获取)
        EntityRelationEvaluator.RelationType relation = EntityRelationEvaluator.evaluate(target, player);
        int baseColorInt;
        switch (relation) {
            case HOSTILE:
                baseColorInt = LotMClientColors.SPIRIT_VISION_HOSTILE;
                break;
            case AGGRESSIVE:
                baseColorInt = LotMClientColors.SPIRIT_VISION_AGGRO;
                break;
            case FRIENDLY:
                baseColorInt = LotMClientColors.SPIRIT_VISION_FRIENDLY;
                break;
            case NEUTRAL:
            default:
                baseColorInt = LotMClientColors.SPIRIT_VISION_NEUTRAL;
                break;
        }

        // 2. 解析 ARGB 分量
        float a = ((baseColorInt >> 24) & 0xFF) / 255.0f;
        float r = ((baseColorInt >> 16) & 0xFF) / 255.0f;
        float g = ((baseColorInt >> 8) & 0xFF) / 255.0f;
        float b = (baseColorInt & 0xFF) / 255.0f;

        // 3. ★★★ 距离增强逻辑 ★★★
        double distSqr = target.distanceToSqr(player);
        double closeRangeSqr = 8.0 * 8.0; // 8格内增强

        if (distSqr < closeRangeSqr) {
            // 计算增强系数 (距离越近，系数越大，最大 1.0)
            float boostFactor = 1.0f - (float)(distSqr / closeRangeSqr);
            boostFactor = Mth.clamp(boostFactor, 0.0f, 1.0f);

            // 转换到 HSB 空间进行调整
            float[] hsb = Color.RGBtoHSB((int)(r * 255), (int)(g * 255), (int)(b * 255), null);

            // 增加饱和度 (Saturation) + 30% * factor
            float newSat = Mth.clamp(hsb[1] + 0.3f * boostFactor, 0f, 1f);
            // 增加亮度 (Brightness) + 50% * factor
            float newBri = Mth.clamp(hsb[2] + 0.5f * boostFactor, 0f, 1f);

            // 转回 RGB
            int rgb = Color.HSBtoRGB(hsb[0], newSat, newBri);
            r = ((rgb >> 16) & 0xFF) / 255.0f;
            g = ((rgb >> 8) & 0xFF) / 255.0f;
            b = (rgb & 0xFF) / 255.0f;

            // 近距离稍微增加不透明度，使其更显眼
            a = Mth.clamp(XRAY_ALPHA + 0.2f * boostFactor, 0f, 0.8f);
        } else {
            // 远距离保持默认 Alpha
            a = XRAY_ALPHA;
        }

        // 4. 基于生命值的动态调整 (叠加逻辑)
        float healthRatio = target.getHealth() / target.getMaxHealth();
        healthRatio = Mth.clamp(healthRatio, 0.0f, 1.0f);

        // 这里简单降低一点亮度来表示虚弱，避免过度复杂的 HSB 转换叠加
        if (healthRatio < 1.0f) {
            float healthFactor = 0.6f + 0.4f * healthRatio; // 最低 60% 亮度
            r *= healthFactor;
            g *= healthFactor;
            b *= healthFactor;
        }

        return new Vector4f(r, g, b, a);
    }

    /**
     * 过滤渲染层 (Layer Filtering)
     * <p>
     * 目的：在灵视状态下，只保留手持物品层，隐藏盔甲、披风等杂物。
     *
     * @param originalLayers 原始渲染层列表
     * @param <T> 实体类型
     * @param <M> 模型类型
     * @return 过滤后的新列表
     */
    public static <T extends LivingEntity, M extends EntityModel<T>> List<RenderLayer<T, M>> filterLayers(List<RenderLayer<T, M>> originalLayers) {
        List<RenderLayer<T, M>> filtered = new ArrayList<>();
        for (RenderLayer<T, M> layer : originalLayers) {
            // 仅保留手持物品层 (ItemInHandLayer)
            if (layer instanceof ItemInHandLayer) {
                filtered.add(layer);
            }
        }
        return filtered;
    }

    /**
     * 兼容旧方法调用 (重定向到新逻辑)
     */
    public static Vector4f getXRayColor(LivingEntity target) {
        return getDynamicXRayColor(target);
    }
}
