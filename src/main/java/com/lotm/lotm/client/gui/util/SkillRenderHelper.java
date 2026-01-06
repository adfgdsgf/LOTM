package com.lotm.lotm.client.gui.util;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

public class SkillRenderHelper {

    private static final ResourceLocation UNKNOWN_ICON = new ResourceLocation(LotMMod.MODID, "textures/gui/skills/unknown.png");
    private static final Set<ResourceLocation> MISSING_TEXTURES = new HashSet<>();

    public static void renderSkillIcon(GuiGraphics graphics, AbstractSkill skill, int x, int y, int size, boolean isCooldown) {
        if (skill == null) return;

        ResourceLocation texture = getSkillTexture(skill);

        if (isTextureValid(texture)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, texture);

            // 冷却时图标变暗 (0.4)，正常时原色 (1.0)
            if (isCooldown) {
                RenderSystem.setShaderColor(0.4f, 0.4f, 0.4f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            graphics.blit(texture, x, y, size, size, 0, 0, 256, 256, 256, 256);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } else {
            renderFallbackIcon(graphics, skill, x, y, size);
        }
    }

    public static void renderSkillIcon(GuiGraphics graphics, AbstractSkill skill, int x, int y, int size) {
        renderSkillIcon(graphics, skill, x, y, size, false);
    }

    /**
     * 渲染时钟遮罩
     */
    public static void renderCooldownOverlay(GuiGraphics graphics, int x, int y, int size, float cooldownFactor) {
        if (cooldownFactor <= 0 || cooldownFactor >= 1.0f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        // ★★★ 关键修复：抬高 Z 轴，防止被图标遮挡 ★★★
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
        Matrix4f matrix = graphics.pose().last().pose();

        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        // 半径稍微大一点，覆盖圆角
        float radius = size * 0.8f;

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // 遮罩颜色：半透明黑 (Alpha 160)
        int r = 0, g = 0, b = 0, a = 160;

        // 圆心
        buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a).endVertex();

        // 12点钟方向是 -90度 (在数学坐标系中)
        double startAngle = -Math.PI / 2.0;
        // 顺时针扫过的角度
        double totalAngle = Math.PI * 2.0 * cooldownFactor;

        int segments = 30;

        // 启用裁剪，限制在图标方框内
        graphics.enableScissor(x, y, x + size, y + size);

        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (totalAngle * i / segments);
            float px = cx + (float) Math.cos(angle) * radius;
            float py = cy + (float) Math.sin(angle) * radius;
            buffer.vertex(matrix, px, py, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();

        graphics.disableScissor();
        graphics.pose().popPose(); // 恢复 Pose
        RenderSystem.disableBlend();
    }

    public static void renderActiveState(GuiGraphics graphics, int x, int y, int width, int height) {
        // 激活状态也需要抬高一点点，防止被背景遮挡
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 5);
        graphics.fill(x, y, x + width, y + height, LotMClientColors.SKILL_ACTIVE_OVERLAY);
        graphics.renderOutline(x, y, width, height, LotMClientColors.SKILL_ACTIVE_BORDER);
        graphics.pose().popPose();
    }

    private static void renderFallbackIcon(GuiGraphics graphics, AbstractSkill skill, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, LotMClientColors.ICON_FALLBACK_BG);
        graphics.renderOutline(x, y, size, size, LotMClientColors.ICON_FALLBACK_BORDER);

        String abbrev = LotMUIHelper.getAbbreviation(skill.getDisplayName().getString());
        Font font = Minecraft.getInstance().font;
        float scale = size / 24.0f;
        if (abbrev.length() > 1) scale *= 0.8f;

        graphics.pose().pushPose();
        float textWidth = font.width(abbrev) * scale;
        float textHeight = 8 * scale;
        float tx = x + (size - textWidth) / 2;
        float ty = y + (size - textHeight) / 2 + 1;

        graphics.pose().translate(tx, ty, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, abbrev, 0, 0, LotMClientColors.TEXT_NORMAL, true);
        graphics.pose().popPose();
    }

    private static boolean isTextureValid(ResourceLocation location) {
        if (MISSING_TEXTURES.contains(location)) return false;
        var manager = Minecraft.getInstance().getResourceManager();
        if (manager.getResource(location).isEmpty()) {
            MISSING_TEXTURES.add(location);
            return false;
        }
        return true;
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
