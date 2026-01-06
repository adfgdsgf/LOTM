package com.lotm.lotm.client.gui.skillbar;

import com.lotm.lotm.client.gui.util.LotMUIHelper;
import com.lotm.lotm.client.gui.util.SkillRenderHelper;
import com.lotm.lotm.client.gui.widget.LotMScrollbar;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.util.LotMText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能列表渲染器 (层级折叠版)
 * <p>
 * 重构记录：
 * 1. 引入 LotMScrollbar 托管滚动逻辑。
 */
public class SkillListRenderer {

    private static final int HEADER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 26;
    private static final int SCROLL_BAR_WIDTH = 4;
    private static final int SWITCH_WIDTH = 24;
    private static final int SWITCH_HEIGHT = 12;

    public static class SkillGroup {
        public final int sequence;
        public final Component displayName;
        public final List<AbstractSkill> skills = new ArrayList<>();
        public boolean expanded = true;

        public SkillGroup(int sequence, Component displayName) {
            this.sequence = sequence;
            this.displayName = displayName;
        }

        public Component getDisplayName() { return displayName; }
    }

    private final Font font;
    private int x, y, width, height;
    private AbstractSkill selectedSkill = null;

    // ★★★ 使用通用滚动条组件 ★★★
    private final LotMScrollbar scrollbar;

    public SkillListRenderer(int x, int y, int width, int height) {
        this.font = Minecraft.getInstance().font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // 初始化滚动条
        this.scrollbar = new LotMScrollbar(x + width - SCROLL_BAR_WIDTH - 2, y + 2, SCROLL_BAR_WIDTH, height - 4);
    }

    public void updateBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollbar.setBounds(x + width - SCROLL_BAR_WIDTH - 2, y + 2, SCROLL_BAR_WIDTH, height - 4);
    }

    public void setSelectedSkill(AbstractSkill skill) {
        this.selectedSkill = skill;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, List<SkillGroup> groups) {
        graphics.fill(x, y, x + width, y + height, LotMClientColors.CONTAINER_BG);
        graphics.renderOutline(x, y, width, height, LotMClientColors.CONTAINER_BORDER);

        // 计算总高度
        int totalHeight = 0;
        for (SkillGroup group : groups) {
            totalHeight += HEADER_HEIGHT + 1;
            if (group.expanded) {
                totalHeight += group.skills.size() * (ITEM_HEIGHT + 1);
            }
        }
        totalHeight += 2;

        // 更新滚动条内容
        this.scrollbar.setContentHeight(totalHeight);

        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);

        int currentY = (int) (y + 2 - scrollbar.getScrollOffset());

        for (SkillGroup group : groups) {
            if (currentY + HEADER_HEIGHT > y && currentY < y + height) {
                renderGroupHeader(graphics, group, x + 2, currentY, width - 4 - SCROLL_BAR_WIDTH, mouseX, mouseY);
            }
            currentY += HEADER_HEIGHT + 1;

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

        // 渲染滚动条
        this.scrollbar.render(graphics, mouseX, mouseY);
    }

    private void renderGroupHeader(GuiGraphics graphics, SkillGroup group, int hX, int hY, int hW, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= hX && mouseX < hX + hW && mouseY >= hY && mouseY < hY + HEADER_HEIGHT;
        int bgColor = isHovered ? LotMClientColors.GROUP_HEADER_BG_HOVER : LotMClientColors.GROUP_HEADER_BG;

        graphics.fill(hX, hY, hX + hW, hY + HEADER_HEIGHT, bgColor);
        String arrow = group.expanded ? "▼" : "▶";
        graphics.drawString(font, arrow, hX + 4, hY + 6, LotMClientColors.GROUP_ARROW, false);
        graphics.drawString(font, group.getDisplayName(), hX + 16, hY + 6, LotMClientColors.TEXT_TITLE, true);
    }

    private void renderSkillEntry(GuiGraphics graphics, AbstractSkill skill, int entryX, int entryY, int entryWidth, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= entryX && mouseX < entryX + entryWidth &&
                mouseY >= entryY && mouseY < entryY + ITEM_HEIGHT;
        boolean isSelected = (skill == this.selectedSkill);

        int bgColor;
        if (isSelected) bgColor = LotMClientColors.LIST_ITEM_BG_SELECTED;
        else if (isHovered) bgColor = LotMClientColors.LIST_ITEM_BG_HOVER;
        else bgColor = LotMClientColors.LIST_ITEM_BG_NORMAL;

        graphics.fill(entryX, entryY, entryX + entryWidth, entryY + ITEM_HEIGHT, bgColor);

        if (isSelected || isHovered) {
            int borderColor = isSelected ? LotMClientColors.LIST_ITEM_BORDER_SELECTED : LotMClientColors.LIST_ITEM_BORDER_NORMAL;
            graphics.renderOutline(entryX, entryY, entryWidth, ITEM_HEIGHT, borderColor);
        }

        int iconSize = ITEM_HEIGHT - 4;
        SkillRenderHelper.renderSkillIcon(graphics, skill, entryX + 2, entryY + 2, iconSize);

        int textX = entryX + iconSize + 6;
        int textMaxWidth = entryWidth - (iconSize + 6) - 4;

        boolean hasToggle = skill.getCastType().isToggleOrMaintain();
        if (hasToggle) {
            textMaxWidth -= (SWITCH_WIDTH + 6);
            renderToggleButton(graphics, skill, entryX, entryY, entryWidth, mouseX, mouseY);
        }

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

        Player player = Minecraft.getInstance().player;
        boolean canToggle = player.isCreative() || skill.canBeDeactivated(player);

        if (!canToggle && isActive) {
            graphics.fill(btnX, btnY, btnX + SWITCH_WIDTH, btnY + SWITCH_HEIGHT, 0xFF555555);
            graphics.renderOutline(btnX, btnY, SWITCH_WIDTH, SWITCH_HEIGHT, 0xFF333333);

            Component lockText = LotMText.GUI_LOCKED;
            float scale = 0.5f;
            int textWidth = font.width(lockText);
            float textX = btnX + (SWITCH_WIDTH - textWidth * scale) / 2.0f;
            float textY = btnY + (SWITCH_HEIGHT - 8 * scale) / 2.0f + 1.0f;

            graphics.pose().pushPose();
            graphics.pose().translate(textX, textY, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.drawString(font, lockText, 0, 0, 0xFFAAAAAA, false);
            graphics.pose().popPose();
        } else {
            boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + SWITCH_WIDTH &&
                    mouseY >= btnY && mouseY < btnY + SWITCH_HEIGHT;
            LotMUIHelper.renderToggleSwitch(graphics, btnX, btnY, SWITCH_WIDTH, SWITCH_HEIGHT, isActive, isBtnHovered);
        }
    }

    // ==================== 交互逻辑 ====================

    public boolean mouseClicked(double mouseX, double mouseY, int button, List<SkillGroup> groups) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;

        // ★★★ 优先处理滚动条 ★★★
        if (this.scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        double relativeY = mouseY - y - 2 + scrollbar.getScrollOffset();
        int currentY = 0;

        for (SkillGroup group : groups) {
            if (relativeY >= currentY && relativeY < currentY + HEADER_HEIGHT) {
                group.expanded = !group.expanded;
                return true;
            }
            currentY += HEADER_HEIGHT + 1;

            if (group.expanded) {
                for (AbstractSkill skill : group.skills) {
                    if (relativeY >= currentY && relativeY < currentY + ITEM_HEIGHT) {
                        return false;
                    }
                    currentY += ITEM_HEIGHT + 1;
                }
            }
        }
        return false;
    }

    // ★★★ 转发释放事件 ★★★
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.scrollbar.mouseReleased(mouseX, mouseY, button);
    }

    // ★★★ 转发拖拽事件 ★★★
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.scrollbar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public AbstractSkill getSkillAt(double mouseX, double mouseY, List<SkillGroup> groups) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return null;

        double relativeY = mouseY - y - 2 + scrollbar.getScrollOffset();
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

    public boolean isOverToggleButton(double mouseX, double mouseY, AbstractSkill skill, List<SkillGroup> groups) {
        if (!skill.getCastType().isToggleOrMaintain()) return false;

        Player player = Minecraft.getInstance().player;
        boolean isActive = false;
        var cap = player.getCapability(AbilityContainerProvider.CAPABILITY);
        if (cap.isPresent()) isActive = cap.orElse(null).isSkillActive(skill.getId());

        if (isActive && !player.isCreative() && !skill.canBeDeactivated(player)) {
            return false;
        }

        int currentY = (int) (y + 2 - scrollbar.getScrollOffset());
        for (SkillGroup group : groups) {
            currentY += HEADER_HEIGHT + 1;
            if (group.expanded) {
                for (AbstractSkill s : group.skills) {
                    if (s == skill) {
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
            return this.scrollbar.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
}
