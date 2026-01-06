package com.lotm.lotm.client.renderer.skill.impl;

import com.lotm.lotm.api.capability.IDivinationContainer;
import com.lotm.lotm.client.renderer.RenderHelper;
import com.lotm.lotm.client.renderer.skill.ISkillRenderer;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.common.capability.DivinationContainerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 占卜技能渲染器
 * <p>
 * 职责：
 * 仅负责在技能信息面板中绘制“占卜熟练度”。
 * 它不需要渲染 HUD 或 World 效果 (那些由 ClientDivinationRenderer 负责)。
 */
public class DivinationSkillRenderer implements ISkillRenderer {

    @Override
    public int renderInfo(GuiGraphics graphics, int x, int y, int width) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;

        // 使用 AtomicInteger 捕获返回值 (高度)
        // 实际上可以直接返回固定高度，因为如果 capability 不存在就不渲染
        int heightUsed = 0;

        var cap = mc.player.getCapability(DivinationContainerProvider.CAPABILITY);
        if (cap.isPresent()) {
            IDivinationContainer div = cap.orElseThrow(IllegalStateException::new);
            int mastery = div.getMastery();
            int max = IDivinationContainer.MAX_MASTERY;
            float progress = (float) mastery / max;

            Font font = mc.font;

            // 1. 绘制标题
            Component title = Component.translatable("gui.lotmmod.divination.mastery");
            graphics.drawString(font, title, x, y, LotMClientColors.TEXT_HIGHLIGHT, false);

            // 2. 绘制进度条
            RenderHelper.renderProgressBar(graphics, x, y + 10, width, 4,
                    progress,
                    0xFF333333, // BG: Dark Gray
                    0xFFFFAA00, // Fill: Gold
                    0           // No Border
            );

            // 3. 绘制数值
            String val = mastery + "/" + max;
            int valW = font.width(val);

            graphics.pose().pushPose();
            float scale = 0.7f;
            graphics.pose().translate(x + width - valW * scale, y + 1, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.drawString(font, val, 0, 0, LotMClientColors.TEXT_DIM, false);
            graphics.pose().popPose();

            heightUsed = 20; // 标题 + 进度条 + 间距
        }

        return heightUsed;
    }
}
