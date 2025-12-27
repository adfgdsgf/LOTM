package com.lotm.lotm.client.gui.skillbar;

import com.lotm.lotm.client.gui.util.SkillRenderHelper;
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
 * 职责：在界面右侧（或指定位置）显示选中技能的详细信息。
 * 包括：大图标、名称、类型、消耗(启动/维持)、使用条件、以及自动换行的长文本描述。
 */
public class SkillInfoWidget {

    private int x, y, width, height;
    private final Font font;
    private AbstractSkill selectedSkill;

    public SkillInfoWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = Minecraft.getInstance().font;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setSelectedSkill(AbstractSkill skill) {
        this.selectedSkill = skill;
    }

    /**
     * 渲染详情面板
     */
    public void render(GuiGraphics graphics) {
        // 1. 绘制背景面板
        graphics.fill(x, y, x + width, y + height, LotMClientColors.CONTAINER_BG);
        graphics.renderOutline(x, y, width, height, LotMClientColors.CONTAINER_BORDER);

        if (selectedSkill == null) {
            // 提示选择技能
            drawCenteredString(graphics, Component.translatable("gui.lotmmod.skill_info.select_hint"), y + height / 2 - 5, LotMClientColors.TEXT_DIM);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int padding = 6;
        int currentY = y + padding;

        // 2. 绘制头部 (图标 + 名称)
        int iconSize = 32;
        SkillRenderHelper.renderSkillIcon(graphics, selectedSkill, x + padding, currentY, iconSize);

        int textX = x + padding + iconSize + 8;
        graphics.drawString(font, selectedSkill.getDisplayName(), textX, currentY + 2, LotMClientColors.TEXT_TITLE, true);

        // 绘制类型标签
        Component typeText = selectedSkill.getCastType().getDisplayName();
        graphics.drawString(font, typeText, textX, currentY + 14, LotMClientColors.TEXT_TYPE, false);

        currentY += iconSize + 8;

        // 获取当前序列 (用于计算消耗)
        int sequence = 9;
        if (mc.player != null) {
            var cap = mc.player.getCapability(BeyonderStateProvider.CAPABILITY);
            if (cap.isPresent()) sequence = cap.orElse(null).getSequence();
        }

        // 3. 绘制消耗 (启动 & 维持)
        double instantCost = selectedSkill.getCost(mc.player, sequence);
        double upkeepCost = selectedSkill.getUpkeepCost(mc.player, sequence); // per tick

        // 3.1 启动消耗
        if (instantCost > 0) {
            Component costText = Component.translatable("gui.lotmmod.skill_info.cost.instant", (int)instantCost);
            graphics.drawString(font, costText, x + padding, currentY, LotMClientColors.TEXT_PARAM, false);
            currentY += 10;
        }

        // 3.2 维持消耗 (显示为 x/s)
        if (upkeepCost > 0) {
            String valStr = String.format("%.1f", upkeepCost * 20); // 转换为每秒
            Component upkeepText = Component.translatable("gui.lotmmod.skill_info.cost.upkeep", valStr);
            graphics.drawString(font, upkeepText, x + padding, currentY, LotMClientColors.TEXT_PARAM, false);
            currentY += 10;
        }

        // 4. 绘制使用条件 (如果有)
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

        // 5. 绘制分割线
        currentY += 4;
        graphics.fill(x + padding, currentY, x + width - padding, currentY + 1, LotMClientColors.SEPARATOR_LINE);
        currentY += 6;

        // 6. 绘制描述 (自动换行)
        // 读取 key: skill.lotmmod.<name>.desc
        Component desc = LotMText.skillDesc(selectedSkill.getId());
        List<FormattedCharSequence> lines = font.split(desc, width - padding * 2);

        for (FormattedCharSequence line : lines) {
            // 防止超出面板底部
            if (currentY + 9 > y + height - padding) break;
            graphics.drawString(font, line, x + padding, currentY, LotMClientColors.TEXT_DESC, false);
            currentY += 10;
        }
    }

    private void drawCenteredString(GuiGraphics graphics, Component text, int y, int color) {
        int w = font.width(text);
        graphics.drawString(font, text, x + (width - w) / 2, y, color, false);
    }
}
