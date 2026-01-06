package com.lotm.lotm.client.gui.skillbar;

import com.lotm.lotm.client.gui.util.SkillRenderHelper;
import com.lotm.lotm.client.key.LotMKeyBindings;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.skillbar.ISkillBarContainer;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.skill.AbstractSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 技能槽位网格渲染器
 */
public class SlotGridRenderer {

    private int x, y;
    private int slotSize = 32;
    private int slotPadding = 4;
    private final Font font;

    public SlotGridRenderer() {
        this.font = Minecraft.getInstance().font;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSlotSize(int size) {
        this.slotSize = size;
        this.slotPadding = Math.max(2, size / 8);
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.lotmmod.skill_bar.slots"), x, y - 15, LotMClientColors.TEXT_TITLE, true);

        for (int i = 0; i < ISkillBarContainer.SLOT_COUNT; i++) {
            renderSlot(graphics, i, mouseX, mouseY);
        }

        renderHints(graphics);
    }

    private void renderHints(GuiGraphics graphics) {
        int gridHeight = (ISkillBarContainer.SLOT_COUNT / 3) * (slotSize + slotPadding);
        int hintY = y + gridHeight + 40;

        graphics.pose().pushPose();
        graphics.pose().scale(0.8f, 0.8f, 1.0f);
        int scaledX = (int)(x / 0.8f);
        int scaledY = (int)(hintY / 0.8f);

        graphics.drawString(font, Component.translatable("gui.lotmmod.skill_bar.hint1"), scaledX, scaledY, LotMClientColors.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.lotmmod.skill_bar.hint2"), scaledX, scaledY + 10, LotMClientColors.TEXT_DIM, false);
        graphics.pose().popPose();
    }

    private void renderSlot(GuiGraphics graphics, int slot, int mouseX, int mouseY) {
        int[] pos = getSlotPosition(slot);
        int slotX = pos[0];
        int slotY = pos[1];
        boolean isHovered = isInSlot(mouseX, mouseY, slot);

        int bgColor = isHovered ? LotMClientColors.SKILL_SLOT_BG_HOVER : LotMClientColors.SKILL_SLOT_BG_NORMAL;
        int borderColor = isHovered ? LotMClientColors.SKILL_BORDER_ACTIVE : LotMClientColors.SKILL_BORDER_NORMAL;

        graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, bgColor);
        graphics.renderOutline(slotX, slotY, slotSize, slotSize, borderColor);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(container -> {
                ResourceLocation skillId = container.getSkillInSlot(slot);
                if (skillId != null) {
                    AbstractSkill skill = LotMSkills.getSkill(skillId);
                    if (skill != null) {
                        int padding = 2;

                        // ★★★ 核心修改：检查冷却状态并变暗 ★★★
                        boolean isCooldown = false;
                        var cap = player.getCapability(AbilityContainerProvider.CAPABILITY);
                        if (cap.isPresent()) {
                            isCooldown = cap.orElse(null).isOnCooldown(skillId);
                        }

                        SkillRenderHelper.renderSkillIcon(graphics, skill, slotX + padding, slotY + padding, slotSize - padding * 2, isCooldown);

                        if (cap.isPresent() && cap.orElse(null).isSkillActive(skillId)) {
                            SkillRenderHelper.renderActiveState(graphics, slotX, slotY, slotSize, slotSize);
                        }
                    }
                }
            });
        }

        if (slot < LotMKeyBindings.SKILL_SLOT_KEYS.size()) {
            Component keyName = LotMKeyBindings.SKILL_SLOT_KEYS.get(slot).getTranslatedKeyMessage();
            float scale = 0.7f;
            graphics.pose().pushPose();
            graphics.pose().translate(slotX + 2, slotY + slotSize - 7, 100);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.drawString(font, keyName, 0, 0, LotMClientColors.SKILL_KEYBIND_TEXT, true);
            graphics.pose().popPose();
        }

        String slotNum = String.valueOf(slot + 1);
        int numW = font.width(slotNum);
        graphics.drawString(font, slotNum, slotX + slotSize - numW - 2, slotY + 2, LotMClientColors.TEXT_DIM, false);
    }

    public int[] getSlotPosition(int slot) {
        int col = slot % 3;
        int row = slot / 3;
        return new int[]{
                x + col * (slotSize + slotPadding),
                y + row * (slotSize + slotPadding)
        };
    }

    public int getSlotAt(int mouseX, int mouseY) {
        for (int i = 0; i < ISkillBarContainer.SLOT_COUNT; i++) {
            if (isInSlot(mouseX, mouseY, i)) return i;
        }
        return -1;
    }

    public boolean isInSlot(int mouseX, int mouseY, int slot) {
        int[] pos = getSlotPosition(slot);
        return mouseX >= pos[0] && mouseX < pos[0] + slotSize &&
                mouseY >= pos[1] && mouseY < pos[1] + slotSize;
    }

    public void renderSlotHighlight(GuiGraphics graphics, int slot, int color) {
        if (slot < 0 || slot >= ISkillBarContainer.SLOT_COUNT) return;
        int[] pos = getSlotPosition(slot);
        graphics.fill(pos[0], pos[1], pos[0] + slotSize, pos[1] + slotSize, color);
    }

    public boolean isInGridArea(int mouseX, int mouseY) {
        int gridWidth = 3 * (slotSize + slotPadding);
        int gridHeight = 3 * (slotSize + slotPadding);
        return mouseX >= x && mouseX < x + gridWidth &&
                mouseY >= y && mouseY < y + gridHeight;
    }
}
