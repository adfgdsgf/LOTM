package com.lotm.lotm.client.gui.util;

import com.lotm.lotm.client.util.LotMClientColors;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * LOTM UI 通用渲染助手
 * 集成了动态文本、控件渲染等高级功能
 */
public class LotMUIHelper {

    /**
     * 渲染滑动开关 (Toggle Switch)
     *
     * @param x         左上角 X
     * @param y         左上角 Y
     * @param width     宽度 (建议 24-32)
     * @param height    高度 (建议 12-16)
     * @param active    是否激活
     * @param isHovered 鼠标是否悬停
     */
    public static void renderToggleSwitch(GuiGraphics graphics, int x, int y, int width, int height, boolean active, boolean isHovered) {
        // 1. 绘制轨道背景
        int trackColor = active ? LotMClientColors.BTN_TOGGLE_ACTIVE : LotMClientColors.BTN_TOGGLE_INACTIVE;
        if (isHovered) {
            // 悬停时与白色混合，使其变亮 20%
            trackColor = LotMClientColors.blend(trackColor, 0xFFFFFFFF, 0.2f);
        }

        // 绘制背景 (带边框)
        graphics.fill(x, y, x + width, y + height, trackColor);
        graphics.renderOutline(x, y, width, height, LotMClientColors.BTN_BORDER);

        // 2. 绘制滑块 (Knob)
        int knobSize = height - 2;
        // 激活时靠右，未激活时靠左
        int knobX = active ? (x + width - knobSize - 1) : (x + 1);
        int knobY = y + 1;

        // 滑块颜色 (激活时纯白，未激活时灰白)
        int knobColor = active ? LotMClientColors.BTN_INDICATOR : 0xFFDDDDDD;

        graphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knobColor);

        // 3. 绘制状态文字 (仅当宽度足够时显示)
        if (width >= 30) {
            Font font = Minecraft.getInstance().font;
            // 使用本地化键
            Component text = active ?
                    Component.translatable("gui.lotmmod.toggle.on") :
                    Component.translatable("gui.lotmmod.toggle.off");

            float scale = 0.5f; // 缩小字体
            int textWidth = font.width(text);

            graphics.pose().pushPose();
            // 文字放在滑块的另一侧
            float textX = active ? (x + 5) : (x + width - 5 - textWidth * scale);
            float textY = y + (height - 8 * scale) / 2 + 1;

            graphics.pose().translate(textX, textY, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.drawString(font, text, 0, 0, LotMClientColors.TEXT_NORMAL, false);
            graphics.pose().popPose();
        }
    }

    /**
     * 渲染滚动文本 (Marquee Effect)
     * 当文本宽度超过限制时，自动来回滚动
     */
    public static void renderScrollingString(GuiGraphics graphics, Font font, Component text, int x, int y, int maxWidth, int color) {
        String textStr = text.getString();
        int textWidth = font.width(textStr);

        if (textWidth <= maxWidth) {
            // 文本够短，直接渲染
            graphics.drawString(font, textStr, x, y, color, false);
        } else {
            // 文本过长，计算滚动
            long time = Util.getMillis();
            int scrollRange = textWidth - maxWidth;
            double speed = 0.05; // 滚动速度

            // 算法：停留 1s -> 滚动 -> 停留 1s -> 滚动回
            double progress = (time * speed) % (scrollRange * 2 + 200);
            double offset = 0;

            if (progress < scrollRange) {
                offset = progress; // 向左滚
            } else if (progress < scrollRange + 100) {
                offset = scrollRange; // 停留
            } else if (progress < scrollRange * 2 + 100) {
                offset = scrollRange - (progress - (scrollRange + 100)); // 向右滚回
            } else {
                offset = 0; // 停留
            }

            offset = Mth.clamp(offset, 0, scrollRange);

            // 开启裁剪 (Scissor Test)，防止文字溢出到其他区域
            graphics.enableScissor(x, y, x + maxWidth, y + 10);
            graphics.drawString(font, textStr, (int) (x - offset), y, color, false);
            graphics.disableScissor();
        }
    }

    /**
     * 生成名称缩写 (用于图标缺失时的回退显示)
     * 例如: "Fire Ball" -> "FB", "Spirit Body" -> "SB"
     */
    public static String getAbbreviation(String name) {
        if (name == null || name.isEmpty()) return "?";

        // 中文/日文等 CJK 字符，取第一个字
        if (Character.isIdeographic(name.codePointAt(0))) {
            return name.substring(0, 1);
        }

        String[] parts = name.split(" ");
        if (parts.length == 1) {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i].charAt(0));
            }
        }
        return sb.toString().toUpperCase();
    }
}
