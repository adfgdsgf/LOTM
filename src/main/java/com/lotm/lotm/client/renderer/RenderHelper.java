package com.lotm.lotm.client.renderer;

import com.lotm.lotm.content.skill.AbstractSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 通用渲染工具类
 * General Render Helper
 *
 * 处理图标渲染、进度条绘制、文本缩写生成等。
 */
public class RenderHelper {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Set<ResourceLocation> MISSING_ICONS = new HashSet<>();

    // ==================== 进度条渲染 ====================

    /**
     * 渲染简单的水平进度条
     */
    public static void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
                                         float progress, int bgColor, int fillColor, int borderColor) {
        progress = Mth.clamp(progress, 0, 1);

        // 背景
        graphics.fill(x, y, x + width, y + height, bgColor);

        // 填充
        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, fillColor);
        }

        // 边框
        if (borderColor != 0) {
            graphics.renderOutline(x, y, width, height, borderColor);
        }
    }

    // ==================== 技能图标渲染 ====================

    /**
     * 获取技能图标路径
     * 约定路径：assets/lotmmod/textures/skill/skill_id.png
     */
    @Nullable
    public static ResourceLocation getSkillIconPath(AbstractSkill skill) {
        if (skill == null) return null;
        ResourceLocation id = skill.getId();
        return new ResourceLocation(id.getNamespace(), "textures/skill/" + id.getPath() + ".png");
    }

    /**
     * 检查材质是否存在
     */
    public static boolean textureExists(ResourceLocation location) {
        if (location == null) return false;
        if (MISSING_ICONS.contains(location)) return false;

        try {
            if (MC.getResourceManager().getResource(location).isEmpty()) {
                MISSING_ICONS.add(location);
                return false;
            }
            return true;
        } catch (Exception e) {
            MISSING_ICONS.add(location);
            return false;
        }
    }

    /**
     * 渲染技能图标
     * 如果材质缺失，自动回退到文字图标
     */
    public static void renderSkillIcon(GuiGraphics graphics, @Nullable AbstractSkill skill,
                                       int x, int y, int size) {
        if (skill == null) return;

        ResourceLocation iconPath = getSkillIconPath(skill);

        if (iconPath != null && textureExists(iconPath)) {
            try {
                RenderSystem.enableBlend();
                graphics.blit(iconPath, x, y, 0, 0, size, size, size, size);
                RenderSystem.disableBlend();
                return;
            } catch (Exception e) {
                MISSING_ICONS.add(iconPath);
            }
        }

        // 回退渲染
        renderFallbackIcon(graphics, skill.getDisplayName().getString(), x, y, size);
    }

    /**
     * 渲染备用文字图标（首字母缩写）
     */
    public static void renderFallbackIcon(GuiGraphics graphics, String name, int x, int y, int size) {
        Font font = MC.font;

        // 背景
        graphics.fill(x, y, x + size, y + size, 0xFF444444);
        graphics.renderOutline(x, y, size, size, 0xFF888888);

        if (name == null || name.isEmpty()) return;

        String abbrev = getAbbreviation(name);
        int textWidth = font.width(abbrev);
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size - 8) / 2;

        graphics.drawString(font, abbrev, textX, textY, 0xFFFFFF, false);
    }

    /**
     * 生成缩写 (Fire Ball -> FB)
     */
    public static String getAbbreviation(String name) {
        if (name == null || name.isEmpty()) return "";

        // CJK 处理
        char firstChar = name.charAt(0);
        if (Character.isIdeographic(firstChar)) {
            return name.substring(0, Math.min(name.length(), 1));
        }

        String[] parts = name.split(" ");
        if (parts.length == 1) {
            return name.substring(0, Math.min(name.length(), 2)).toUpperCase();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) sb.append(parts[i].charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    public static void clearIconCache() {
        MISSING_ICONS.clear();
    }
}
