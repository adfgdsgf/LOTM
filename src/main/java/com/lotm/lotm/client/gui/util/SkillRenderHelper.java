package com.lotm.lotm.client.gui.util;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * 技能渲染工具类
 * <p>
 * 职责：
 * 1. 渲染技能图标 (支持材质缺失自动回退)。
 * 2. 渲染冷却遮罩。
 * 3. 管理材质有效性缓存。
 */
public class SkillRenderHelper {

    private static final ResourceLocation UNKNOWN_ICON = new ResourceLocation(LotMMod.MODID, "textures/gui/skills/unknown.png");
    // 缓存缺失的材质，避免每帧重复检测造成 IO 卡顿
    private static final Set<ResourceLocation> MISSING_TEXTURES = new HashSet<>();

    /**
     * 智能渲染技能图标
     * 如果材质缺失，自动回退到文字缩写模式
     */
    public static void renderSkillIcon(GuiGraphics graphics, AbstractSkill skill, int x, int y, int size) {
        if (skill == null) return;

        ResourceLocation texture = getSkillTexture(skill);

        // 检查材质是否有效
        if (isTextureValid(texture)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, texture);
            graphics.blit(texture, x, y, size, size, 0, 0, 256, 256, 256, 256);
            RenderSystem.disableBlend();
        } else {
            // 材质缺失，渲染回退样式
            renderFallbackIcon(graphics, skill, x, y, size);
        }
    }

    /**
     * 渲染回退图标 (透明背景 + 文字缩写)
     */
    private static void renderFallbackIcon(GuiGraphics graphics, AbstractSkill skill, int x, int y, int size) {
        // 绘制背景 (透明)
        graphics.fill(x, y, x + size, y + size, LotMClientColors.ICON_FALLBACK_BG);
        // 绘制边框
        graphics.renderOutline(x, y, size, size, LotMClientColors.ICON_FALLBACK_BORDER);

        // 绘制缩写
        String abbrev = LotMUIHelper.getAbbreviation(skill.getDisplayName().getString());
        Font font = Minecraft.getInstance().font;

        // 缩放文字以适应图标大小
        float scale = size / 24.0f; // 假设标准大小是 24
        if (abbrev.length() > 1) scale *= 0.8f;

        graphics.pose().pushPose();
        // 居中计算
        float textWidth = font.width(abbrev) * scale;
        float textHeight = 8 * scale;
        float tx = x + (size - textWidth) / 2;
        float ty = y + (size - textHeight) / 2 + 1;

        graphics.pose().translate(tx, ty, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, abbrev, 0, 0, LotMClientColors.TEXT_NORMAL, true); // 带阴影
        graphics.pose().popPose();
    }

    /**
     * 检查材质是否有效 (带缓存)
     */
    private static boolean isTextureValid(ResourceLocation location) {
        if (MISSING_TEXTURES.contains(location)) return false;

        // 尝试获取资源管理器中的资源
        var manager = Minecraft.getInstance().getResourceManager();
        if (manager.getResource(location).isEmpty()) {
            MISSING_TEXTURES.add(location); // 标记为缺失
            return false;
        }
        return true;
    }

    /**
     * 渲染冷却遮罩层
     */
    public static void renderCooldownOverlay(GuiGraphics graphics, int x, int y, int size, float cooldownFactor) {
        if (cooldownFactor > 0) {
            int maskHeight = (int) (size * cooldownFactor);
            graphics.fill(x, y + size - maskHeight, x + size, y + size, LotMClientColors.SKILL_COOLDOWN_OVERLAY);
        }
    }

    private static ResourceLocation getSkillTexture(AbstractSkill skill) {
        ResourceLocation id = skill.getRegistryName();
        if (id == null) return UNKNOWN_ICON;
        return new ResourceLocation(id.getNamespace(), "textures/skill/" + id.getPath() + ".png");
    }

    public static void clearCache() {
        MISSING_TEXTURES.clear();
    }
}
