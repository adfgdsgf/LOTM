package com.lotm.lotm.client.gui.skillbar;

import com.lotm.lotm.client.gui.util.LotMUIHelper;
import com.lotm.lotm.client.gui.util.SkillRenderHelper;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.content.skill.AbstractSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能列表渲染器 (层级折叠版)
 * <p>
 * 职责：渲染左侧的可滚动技能列表，支持按序列分组折叠。
 * 特性：
 * 1. 递归层级结构 (Sequence -> Skills)。
 * 2. 折叠/展开动画逻辑。
 * 3. 保持原有的拖拽、开关、高亮功能。
 */
public class SkillListRenderer {

    // ==================== 布局常量 ====================
    private static final int HEADER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 26;
    private static final int SCROLL_BAR_WIDTH = 4;
    private static final int SWITCH_WIDTH = 24;
    private static final int SWITCH_HEIGHT = 12;

    // ==================== 内部数据结构 ====================
    /**
     * 技能分组类
     * <p>
     * 工业级改造：
     * displayName 不再硬编码，而是由外部传入。
     * 这样可以支持 "序列9: 占卜家" 这种富文本标题。
     */
    public static class SkillGroup {
        public final int sequence; // 序列号 (9-0)
        public final Component displayName; // 分组显示名称
        public final List<AbstractSkill> skills = new ArrayList<>();
        public boolean expanded = true; // 默认展开

        public SkillGroup(int sequence, Component displayName) {
            this.sequence = sequence;
            this.displayName = displayName;
        }

        public Component getDisplayName() {
            return displayName;
        }
    }

    // ==================== 状态变量 ====================
    private final Font font;
    private int x, y, width, height;
    private double scrollOffset = 0;
    private AbstractSkill selectedSkill = null;

    public SkillListRenderer(int x, int y, int width, int height) {
        this.font = Minecraft.getInstance().font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setSelectedSkill(AbstractSkill skill) {
        this.selectedSkill = skill;
    }

    /**
     * 核心渲染方法
     * @param groups 已分组的技能数据
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, List<SkillGroup> groups) {
        // 1. 背景
        graphics.fill(x, y, x + width, y + height, LotMClientColors.CONTAINER_BG);
        graphics.renderOutline(x, y, width, height, LotMClientColors.CONTAINER_BORDER);

        // 2. 计算总高度
        int totalHeight = 0;
        for (SkillGroup group : groups) {
            totalHeight += HEADER_HEIGHT + 1;
            if (group.expanded) {
                totalHeight += group.skills.size() * (ITEM_HEIGHT + 1);
            }
        }
        totalHeight += 2; // padding

        // 3. 滚动计算
        int maxScroll = Math.max(0, totalHeight - height);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);

        // 4. 开启裁剪
        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);

        int currentY = (int) (y + 2 - scrollOffset);

        // 5. 遍历渲染
        for (SkillGroup group : groups) {
            // 渲染分组标题
            if (currentY + HEADER_HEIGHT > y && currentY < y + height) {
                renderGroupHeader(graphics, group, x + 2, currentY, width - 4 - SCROLL_BAR_WIDTH, mouseX, mouseY);
            }
            currentY += HEADER_HEIGHT + 1;

            // 如果展开，渲染子项
            if (group.expanded) {
                for (AbstractSkill skill : group.skills) {
                    if (currentY + ITEM_HEIGHT > y && currentY < y + height) {
                        renderSkillEntry(graphics, skill, x + 8, currentY, width - 12 - SCROLL_BAR_WIDTH, mouseX, mouseY);
                    }
                    currentY += ITEM_HEIGHT + 1;
                }
            }
        }

        graphics.disableScissor();

        // 6. 滚动条
        if (maxScroll > 0) {
            renderScrollBar(graphics, mouseX, mouseY, totalHeight, maxScroll);
        }
    }

    private void renderGroupHeader(GuiGraphics graphics, SkillGroup group, int hX, int hY, int hW, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= hX && mouseX < hX + hW && mouseY >= hY && mouseY < hY + HEADER_HEIGHT;
        int bgColor = isHovered ? LotMClientColors.GROUP_HEADER_BG_HOVER : LotMClientColors.GROUP_HEADER_BG;

        graphics.fill(hX, hY, hX + hW, hY + HEADER_HEIGHT, bgColor);

        // 箭头
        String arrow = group.expanded ? "▼" : "▶";
        graphics.drawString(font, arrow, hX + 4, hY + 6, LotMClientColors.GROUP_ARROW, false);

        // 标题
        graphics.drawString(font, group.getDisplayName(), hX + 16, hY + 6, LotMClientColors.TEXT_TITLE, true);
    }

    private void renderSkillEntry(GuiGraphics graphics, AbstractSkill skill, int entryX, int entryY, int entryWidth, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= entryX && mouseX < entryX + entryWidth &&
                mouseY >= entryY && mouseY < entryY + ITEM_HEIGHT;
        boolean isSelected = (skill == this.selectedSkill);

        // 背景
        int bgColor;
        if (isSelected) bgColor = LotMClientColors.LIST_ITEM_BG_SELECTED;
        else if (isHovered) bgColor = LotMClientColors.LIST_ITEM_BG_HOVER;
        else bgColor = LotMClientColors.LIST_ITEM_BG_NORMAL;

        graphics.fill(entryX, entryY, entryX + entryWidth, entryY + ITEM_HEIGHT, bgColor);

        // 边框
        if (isSelected || isHovered) {
            int borderColor = isSelected ? LotMClientColors.LIST_ITEM_BORDER_SELECTED : LotMClientColors.LIST_ITEM_BORDER_NORMAL;
            graphics.renderOutline(entryX, entryY, entryWidth, ITEM_HEIGHT, borderColor);
        }

        // 图标
        int iconSize = ITEM_HEIGHT - 4;
        SkillRenderHelper.renderSkillIcon(graphics, skill, entryX + 2, entryY + 2, iconSize);

        // 文本
        int textX = entryX + iconSize + 6;
        int textMaxWidth = entryWidth - (iconSize + 6) - 4;

        // 开关
        boolean hasToggle = skill.getCastType().isToggleOrMaintain();
        if (hasToggle) {
            textMaxWidth -= (SWITCH_WIDTH + 6);
            renderToggleButton(graphics, skill, entryX, entryY, entryWidth, mouseX, mouseY);
        }

        // 名称
        Component name = skill.getDisplayName();
        int textY = entryY + (ITEM_HEIGHT - 8) / 2;
        int textColor = isSelected ? LotMClientColors.TEXT_HIGHLIGHT : LotMClientColors.TEXT_NORMAL;
        LotMUIHelper.renderScrollingString(graphics, font, name, textX, textY, textMaxWidth, textColor);
    }

    private void renderToggleButton(GuiGraphics graphics, AbstractSkill skill, int entryX, int entryY, int entryWidth, int mouseX, int mouseY) {
        int btnX = entryX + entryWidth - SWITCH_WIDTH - 4;
        int btnY = entryY + (ITEM_HEIGHT - SWITCH_HEIGHT) / 2;

        boolean isActive = false;
        var cap = Minecraft.getInstance().player.getCapability(AbilityContainerProvider.CAPABILITY);
        if (cap.isPresent()) {
            isActive = cap.orElse(null).isSkillActive(skill.getId());
        }

        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + SWITCH_WIDTH &&
                mouseY >= btnY && mouseY < btnY + SWITCH_HEIGHT;

        LotMUIHelper.renderToggleSwitch(graphics, btnX, btnY, SWITCH_WIDTH, SWITCH_HEIGHT, isActive, isBtnHovered);
    }

    private void renderScrollBar(GuiGraphics graphics, int mouseX, int mouseY, int contentHeight, int maxScroll) {
        int barHeight = Math.max(20, (int) ((float) height / contentHeight * height));
        int barY = y + (int) ((float) scrollOffset / maxScroll * (height - barHeight));
        int barX = x + width - SCROLL_BAR_WIDTH - 2;

        graphics.fill(barX, y + 2, barX + SCROLL_BAR_WIDTH, y + height - 2, LotMClientColors.SCROLL_TRACK);
        boolean isHovered = mouseX >= barX && mouseX <= barX + SCROLL_BAR_WIDTH;
        int thumbColor = isHovered ? LotMClientColors.SCROLL_THUMB_HOVER : LotMClientColors.SCROLL_THUMB_NORMAL;
        graphics.fill(barX, barY, barX + SCROLL_BAR_WIDTH, barY + barHeight, thumbColor);
    }

    // ==================== 交互逻辑 ====================

    /**
     * 处理点击事件
     * @return true 如果事件被处理
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, List<SkillGroup> groups) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;

        double relativeY = mouseY - y - 2 + scrollOffset;
        int currentY = 0;

        for (SkillGroup group : groups) {
            // 检查标题点击 (折叠/展开)
            if (relativeY >= currentY && relativeY < currentY + HEADER_HEIGHT) {
                group.expanded = !group.expanded;
                return true;
            }
            currentY += HEADER_HEIGHT + 1;

            if (group.expanded) {
                for (AbstractSkill skill : group.skills) {
                    // 检查条目点击
                    if (relativeY >= currentY && relativeY < currentY + ITEM_HEIGHT) {
                        return false; // 交给 Screen 处理具体逻辑 (选中/拖拽/开关)
                    }
                    currentY += ITEM_HEIGHT + 1;
                }
            }
        }
        return false;
    }

    /**
     * 获取鼠标位置下的技能 (用于 Screen 的选中/拖拽逻辑)
     */
    public AbstractSkill getSkillAt(double mouseX, double mouseY, List<SkillGroup> groups) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return null;

        double relativeY = mouseY - y - 2 + scrollOffset;
        int currentY = 0;

        for (SkillGroup group : groups) {
            currentY += HEADER_HEIGHT + 1;
            if (group.expanded) {
                for (AbstractSkill skill : group.skills) {
                    if (relativeY >= currentY && relativeY < currentY + ITEM_HEIGHT) {
                        return skill;
                    }
                    currentY += ITEM_HEIGHT + 1;
                }
            }
        }
        return null;
    }

    /**
     * 获取开关按钮区域检测
     */
    public boolean isOverToggleButton(double mouseX, double mouseY, AbstractSkill skill, List<SkillGroup> groups) {
        if (!skill.getCastType().isToggleOrMaintain()) return false;

        // 重新计算位置 (性能略低但逻辑安全)
        int currentY = (int) (y + 2 - scrollOffset);
        for (SkillGroup group : groups) {
            currentY += HEADER_HEIGHT + 1;
            if (group.expanded) {
                for (AbstractSkill s : group.skills) {
                    if (s == skill) {
                        // 找到了目标技能的 Y 坐标
                        int entryWidth = width - 12 - SCROLL_BAR_WIDTH;
                        int entryX = x + 8;
                        int btnX = entryX + entryWidth - SWITCH_WIDTH - 4;
                        int btnY = currentY + (ITEM_HEIGHT - SWITCH_HEIGHT) / 2;
                        return mouseX >= btnX && mouseX < btnX + SWITCH_WIDTH &&
                                mouseY >= btnY && mouseY < btnY + SWITCH_HEIGHT;
                    }
                    currentY += ITEM_HEIGHT + 1;
                }
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            this.scrollOffset -= delta * 20;
            return true;
        }
        return false;
    }
}
