package com.lotm.lotm.client.util;

import com.lotm.lotm.client.renderer.ClientDivinationRenderer;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.content.skill.base.SpiritVision;
import com.lotm.lotm.util.PerceptionEvaluator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用视觉特效辅助类 (Visual Effect Helper)
 * <p>
 * 职责：
 * 1. 实体 X-Ray 逻辑 (判断与颜色)。
 * 2. 方块高亮渲染逻辑。
 * 3. 颜色空间转换工具。
 * 4. 描边颜色计算。
 */
public class VisualEffectHelper {

    private static final float VISION_RANGE_SQR = 32.0f * 32.0f;
    private static final float XRAY_ALPHA = 0.4f;

    // =================================================================
    //  实体 X-Ray 逻辑
    // =================================================================

    public static boolean shouldApplyXRay(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || target == null || target == player) return false;

        // 1. 占卜高亮检查 (最高优先级)
        // 如果该实体是当前的占卜目标，无视距离、无视灵视开关、无视遮挡，强制高亮
        if (ClientDivinationRenderer.isDivinationTarget(target.getId())) {
            return true;
        }

        // 2. 灵视常规检查
        if (target.distanceToSqr(player) > VISION_RANGE_SQR) return false;

        boolean isSpiritVisionActive = player.getCapability(AbilityContainerProvider.CAPABILITY)
                .map(cap -> cap.isSkillActive(SpiritVision.ID))
                .orElse(false);

        if (!isSpiritVisionActive) return false;

        if (player.hasLineOfSight(target)) return false;
        if (!PerceptionEvaluator.canPerceive(player, target)) return false;

        return true;
    }

    /**
     * 获取实体的动态颜色 (ARGB int 格式)
     * <p>
     * 专门供 MixinEntityClient 使用，用于确定描边的颜色。
     */
    public static int getEntityColorInt(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return LotMClientColors.SPIRIT_VISION_NEUTRAL;

        // 1. 占卜高亮颜色 (金色)
        if (ClientDivinationRenderer.isDivinationTarget(target.getId())) {
            return LotMClientColors.DIVINATION_HIGHLIGHT;
        }

        // 2. 灵视颜色逻辑 (根据关系判断)
        EntityRelationEvaluator.RelationType relation = EntityRelationEvaluator.evaluate(target, player);
        switch (relation) {
            case HOSTILE:
                return LotMClientColors.SPIRIT_VISION_HOSTILE;
            case AGGRESSIVE:
                return LotMClientColors.SPIRIT_VISION_AGGRO;
            case FRIENDLY:
                return LotMClientColors.SPIRIT_VISION_FRIENDLY;
            case NEUTRAL:
            default:
                return LotMClientColors.SPIRIT_VISION_NEUTRAL;
        }
    }

    public static Vector4f getDynamicXRayColor(LivingEntity target) {
        // 复用 getEntityColorInt 获取基础颜色，保证描边和本体颜色一致
        int baseColorInt = getEntityColorInt(target);

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return LotMClientColors.toVector4f(baseColorInt, XRAY_ALPHA);

        float a = ((baseColorInt >> 24) & 0xFF) / 255.0f;
        float r = ((baseColorInt >> 16) & 0xFF) / 255.0f;
        float g = ((baseColorInt >> 8) & 0xFF) / 255.0f;
        float b = (baseColorInt & 0xFF) / 255.0f;

        // 距离增强逻辑 (离得越近越亮)
        double distSqr = target.distanceToSqr(player);
        double closeRangeSqr = 8.0 * 8.0;

        if (distSqr < closeRangeSqr) {
            float boostFactor = 1.0f - (float)(distSqr / closeRangeSqr);
            boostFactor = Mth.clamp(boostFactor, 0.0f, 1.0f);

            float[] hsb = rgbToHsb(r, g, b);
            float newSat = Mth.clamp(hsb[1] + 0.3f * boostFactor, 0f, 1f);
            float newBri = Mth.clamp(hsb[2] + 0.5f * boostFactor, 0f, 1f);
            float[] rgb = hsbToRgb(hsb[0], newSat, newBri);

            r = rgb[0];
            g = rgb[1];
            b = rgb[2];
            a = Mth.clamp(XRAY_ALPHA + 0.2f * boostFactor, 0f, 0.8f);
        } else {
            a = XRAY_ALPHA;
        }

        // 生命值状态调整 (血量越低越暗)
        float healthRatio = target.getHealth() / target.getMaxHealth();
        healthRatio = Mth.clamp(healthRatio, 0.0f, 1.0f);

        if (healthRatio < 1.0f) {
            float healthFactor = 0.6f + 0.4f * healthRatio;
            r *= healthFactor;
            g *= healthFactor;
            b *= healthFactor;
        }

        return new Vector4f(r, g, b, a);
    }

    public static Vector4f getXRayColor(LivingEntity target) {
        return getDynamicXRayColor(target);
    }

    // =================================================================
    //  方块高亮逻辑 (Block Highlighting)
    // =================================================================

    public static Vector4f getDivinationColor(float alpha) {
        return LotMClientColors.toVector4f(LotMClientColors.DIVINATION_HIGHLIGHT, alpha);
    }

    public static void renderDivinationBlockOutline(PoseStack poseStack, VertexConsumer consumer, BlockPos pos, float alpha) {
        Vector4f color = getDivinationColor(alpha);
        AABB aabb = new AABB(pos).inflate(0.01);
        LevelRenderer.renderLineBox(poseStack, consumer, aabb, color.x, color.y, color.z, color.w);
    }

    // =================================================================
    //  工具方法
    // =================================================================

    public static <T extends LivingEntity, M extends EntityModel<T>> List<RenderLayer<T, M>> filterLayers(List<RenderLayer<T, M>> originalLayers) {
        List<RenderLayer<T, M>> filtered = new ArrayList<>();
        for (RenderLayer<T, M> layer : originalLayers) {
            if (layer instanceof ItemInHandLayer) {
                filtered.add(layer);
            }
        }
        return filtered;
    }

    private static float[] rgbToHsb(float r, float g, float b) {
        float hue, saturation, brightness;
        float cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        float cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = cmax;
        if (cmax != 0) saturation = (cmax - cmin) / cmax;
        else saturation = 0;

        if (saturation == 0) hue = 0;
        else {
            float redc = (cmax - r) / (cmax - cmin);
            float greenc = (cmax - g) / (cmax - cmin);
            float bluec = (cmax - b) / (cmax - cmin);
            if (r == cmax) hue = bluec - greenc;
            else if (g == cmax) hue = 2.0f + redc - bluec;
            else hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0) hue = hue + 1.0f;
        }
        return new float[]{hue, saturation, brightness};
    }

    private static float[] hsbToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (t * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 1 -> { r = (int) (q * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 2 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (t * 255.0f + 0.5f); }
                case 3 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (q * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 4 -> { r = (int) (t * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 5 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (q * 255.0f + 0.5f); }
            }
        }
        return new float[]{r / 255.0f, g / 255.0f, b / 255.0f};
    }
}
