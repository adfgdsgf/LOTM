package com.lotm.lotm.client.gui.skillbar;

import com.lotm.lotm.client.gui.util.LotMUIHelper;
import com.lotm.lotm.client.gui.util.SkillRenderHelper;
import com.lotm.lotm.client.gui.widget.LotMScrollbar;
import com.lotm.lotm.client.renderer.skill.ISkillRenderer;
import com.lotm.lotm.client.renderer.skill.SkillRendererRegistry;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.util.LotMText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 技能详情展示组件
 * <p>
 * 修正记录：
 * 1. 引入 LotMScrollbar，支持超长描述文本的滚动查看。
 * 2. 采用“即时渲染即时计算”策略，避免布局逻辑重复。
 */
public class SkillInfoWidget {

    private int x, y, width, height;
    private final Font font;
    private AbstractSkill selectedSkill;

    // 滚动条组件
    private final LotMScrollbar scrollbar;
    private static final int SCROLL_BAR_WIDTH = 4;

    // 缓存上一帧计算出的内容总高度
    private int lastContentHeight = 0;

    public SkillInfoWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = Minecraft.getInstance().font;
        // 初始化滚动条
        this.scrollbar = new LotMScrollbar(x + width - SCROLL_BAR_WIDTH - 2, y + 2, SCROLL_BAR_WIDTH, height - 4);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // 更新滚动条位置
        this.scrollbar.setBounds(x + width - SCROLL_BAR_WIDTH - 2, y + 2, SCROLL_BAR_WIDTH, height - 4);
    }

    public void setSelectedSkill(AbstractSkill skill) {
        if (this.selectedSkill != skill) {
            this.selectedSkill = skill;
            this.scrollbar.setScrollOffset(0); // 切换技能时重置滚动
            this.lastContentHeight = 0; // 重置高度缓存
        }
    }

    public void render(GuiGraphics graphics) {
        // 1. 背景
        graphics.fill(x, y, x + width, y + height, LotMClientColors.CONTAINER_BG);
        graphics.renderOutline(x, y, width, height, LotMClientColors.CONTAINER_BORDER);

        if (selectedSkill == null) {
            drawCenteredString(graphics, Component.translatable("gui.lotmmod.skill_info.select_hint"), y + height / 2 - 5, LotMClientColors.TEXT_DIM);
            return;
        }

        // 2. 设置滚动条内容高度 (使用上一帧计算的值)
        this.scrollbar.setContentHeight(lastContentHeight);

        // 3. 开启裁剪 (Scissor)
        // 留出边框各 1 像素
        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);

        // 4. 实际渲染内容
        graphics.pose().pushPose();
        // 应用滚动偏移
        graphics.pose().translate(0, -scrollbar.getScrollOffset(), 0);

        // ★★★ 核心调用：渲染并计算高度 ★★★
        renderContent(graphics);

        graphics.pose().popPose();
        graphics.disableScissor();

        // 5. 渲染滚动条
        this.scrollbar.render(graphics, 0, 0); // 鼠标位置由外部事件处理，这里仅渲染视觉
    }

    /**
     * 渲染具体内容，并更新 lastContentHeight
     */
    private void renderContent(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        int padding = 6;
        int startY = y + padding;
        int currentY = startY;
        int contentWidth = width - padding * 2 - SCROLL_BAR_WIDTH; // 减去滚动条宽度

        // --- 图标 + 名称 ---
        int iconSize = 32;
        SkillRenderHelper.renderSkillIcon(graphics, selectedSkill, x + padding, currentY, iconSize);

        int textX = x + padding + iconSize + 8;
        // 标题支持滚动文本 (防止超长名称)
        LotMUIHelper.renderScrollingString(graphics, font, selectedSkill.getDisplayName(), textX, currentY + 2, contentWidth - iconSize - 8, LotMClientColors.TEXT_TITLE);

        Component typeText = selectedSkill.getCastType().getDisplayName();
        graphics.drawString(font, typeText, textX, currentY + 14, LotMClientColors.TEXT_TYPE, false);

        currentY += iconSize + 8;

        int sequence = 9;
        if (mc.player != null) {
            var cap = mc.player.getCapability(BeyonderStateProvider.CAPABILITY);
            if (cap.isPresent()) sequence = cap.orElse(null).getSequence();
        }

        // --- 消耗显示 ---
        double instantCost = selectedSkill.getCost(mc.player, sequence);
        double upkeepCost = selectedSkill.getUpkeepCost(mc.player, sequence);

        if (instantCost > 0) {
            Component costText = Component.translatable("gui.lotmmod.skill_info.cost.instant", (int)instantCost);
            graphics.drawString(font, costText, x + padding, currentY, LotMClientColors.TEXT_PARAM, false);
            currentY += 10;
        }

        if (upkeepCost > 0) {
            String valStr = String.format("%.1f", upkeepCost * 20);
            Component upkeepText = Component.translatable("gui.lotmmod.skill_info.cost.upkeep", valStr);
            graphics.drawString(font, upkeepText, x + padding, currentY, LotMClientColors.TEXT_PARAM, false);
            currentY += 10;
        }

        // --- 使用条件 ---
        List<Component> conditions = selectedSkill.getUsageConditions(mc.player, sequence);
        if (!conditions.isEmpty()) {
            currentY += 4;
            graphics.drawString(font, Component.translatable("gui.lotmmod.skill_info.conditions"), x + padding, currentY, LotMClientColors.TEXT_ERROR, false);
            currentY += 10;

            for (Component cond : conditions) {
                graphics.drawString(font, Component.literal("• ").append(cond), x + padding + 4, currentY, LotMClientColors.TEXT_DIM, false);
                currentY += 10;
            }
        }

        // --- 自定义渲染器 (如熟练度) ---
        ISkillRenderer renderer = SkillRendererRegistry.getRenderer(selectedSkill.getId());
        if (renderer != null) {
            currentY += 5;
            // 调用渲染器，并获取它占用的高度
            int renderedHeight = renderer.renderInfo(graphics, x + padding, currentY, contentWidth);
            currentY += renderedHeight;
            if (renderedHeight > 0) currentY += 5; // 额外间距
        }

        // --- 分割线 ---
        currentY += 4;
        graphics.fill(x + padding, currentY, x + width - padding, currentY + 1, LotMClientColors.SEPARATOR_LINE);
        currentY += 6;

        // --- 描述文本 ---
        Component desc = LotMText.skillDesc(selectedSkill.getId());
        List<FormattedCharSequence> lines = font.split(desc, contentWidth);

        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x + padding, currentY, LotMClientColors.TEXT_DESC, false);
            currentY += 10;
        }

        // ★★★ 更新高度缓存 ★★★
        // 总高度 = 当前Y - 起始Y + 底部Padding
        this.lastContentHeight = (currentY - y) + padding;
    }

    // ==================== 交互事件转发 ====================

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            return this.scrollbar.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.scrollbar.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.scrollbar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            return this.scrollbar.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }

    private void drawCenteredString(GuiGraphics graphics, Component text, int y, int color) {
        int w = font.width(text);
        graphics.drawString(font, text, x + (width - w) / 2, y, color, false);
    }
}
