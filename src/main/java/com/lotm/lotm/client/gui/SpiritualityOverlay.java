package com.lotm.lotm.client.gui;

import com.lotm.lotm.client.config.LotMClientConfig;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.client.renderer.HudPositionHelper;
import com.lotm.lotm.client.renderer.RenderHelper;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 灵性条 HUD 覆盖层
 * <p>
 * 职责：在屏幕上绘制玩家当前的灵性值进度条。
 * 特性：
 * 1. 支持配置开关。
 * 2. 支持自定义锚点和偏移。
 * 3. 低灵性时变色警示。
 */
public class SpiritualityOverlay implements IGuiOverlay {

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 10;
    private static final int MARGIN = 5;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        // 1. 检查配置是否启用
        if (!LotMClientConfig.CLIENT.enableSpiritualityHud.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || mc.options.hideGui) return;

        mc.player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
            if (!state.isBeyonder()) return;

            double current = state.getCurrentSpirituality();
            double max = state.getMaxSpirituality();
            if (max <= 0) return;

            float progress = (float) (current / max);

            // 2. 从配置读取位置信息
            var anchor = LotMClientConfig.CLIENT.spiritualityAnchor.get();
            int offsetX = LotMClientConfig.CLIENT.spiritualityOffsetX.get();
            int offsetY = LotMClientConfig.CLIENT.spiritualityOffsetY.get();

            // 3. 计算屏幕坐标
            int[] pos = HudPositionHelper.calculatePosition(
                    anchor, offsetX, offsetY,
                    screenWidth, screenHeight,
                    BAR_WIDTH, BAR_HEIGHT, MARGIN
            );
            int x = pos[0];
            int y = pos[1];

            // 4. 确定填充颜色 (低灵性警示)
            int fillColor = progress < 0.2f ? LotMClientColors.SPIRITUALITY_FILL_LOW : LotMClientColors.SPIRITUALITY_FILL;

            // 5. 绘制进度条
            RenderHelper.renderProgressBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT,
                    progress,
                    LotMClientColors.SPIRITUALITY_BAR_BG,
                    fillColor,
                    LotMClientColors.SPIRITUALITY_BORDER
            );

            // 6. 绘制数值文本
            String text = String.format("%.0f/%.0f", current, max);
            int textWidth = mc.font.width(text);
            int textX = x + (BAR_WIDTH - textWidth) / 2;
            int textY = y + (BAR_HEIGHT - 8) / 2 + 1;

            graphics.drawString(mc.font, text, textX, textY, LotMClientColors.TEXT_NORMAL, true);
        });
    }
}
