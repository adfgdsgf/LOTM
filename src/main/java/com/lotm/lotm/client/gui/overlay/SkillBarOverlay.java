package com.lotm.lotm.client.gui.overlay;

import com.lotm.lotm.api.capability.IAbilityContainer;
import com.lotm.lotm.client.config.LotMClientConfig;
import com.lotm.lotm.client.gui.util.SkillRenderHelper;
import com.lotm.lotm.client.key.LotMKeyBindings;
import com.lotm.lotm.client.renderer.HudPositionHelper;
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
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 技能栏 HUD 渲染层
 */
public class SkillBarOverlay implements IGuiOverlay {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LotMClientConfig.CLIENT.enableSkillBarHud.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.options.hideGui) return;

        player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                renderSkillBar(graphics, player, bar, abilities, screenWidth, screenHeight);
            });
        });
    }

    private void renderSkillBar(GuiGraphics graphics, Player player, ISkillBarContainer bar,
                                IAbilityContainer abilities, int screenWidth, int screenHeight) {
        boolean horizontal = LotMClientConfig.CLIENT.skillBarHorizontal.get();
        float scale = LotMClientConfig.CLIENT.skillBarScale.get().floatValue();
        var anchor = LotMClientConfig.CLIENT.skillBarAnchor.get();
        int offsetX = LotMClientConfig.CLIENT.skillBarOffsetX.get();
        int offsetY = LotMClientConfig.CLIENT.skillBarOffsetY.get();

        int slotCount = ISkillBarContainer.SLOT_COUNT;
        int baseWidth = horizontal ?
                slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING :
                SLOT_SIZE;
        int baseHeight = horizontal ?
                SLOT_SIZE :
                slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING;

        int scaledWidth = (int) (baseWidth * scale);
        int scaledHeight = (int) (baseHeight * scale);

        int[] pos = HudPositionHelper.calculatePosition(
                anchor, offsetX, offsetY,
                screenWidth, screenHeight,
                scaledWidth, scaledHeight, 5
        );
        int startX = pos[0];
        int startY = pos[1];

        graphics.pose().pushPose();
        graphics.pose().translate(startX, startY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < slotCount; i++) {
            int x = horizontal ? i * (SLOT_SIZE + SLOT_SPACING) : 0;
            int y = horizontal ? 0 : i * (SLOT_SIZE + SLOT_SPACING);
            renderSingleSlot(graphics, player, bar, abilities, i, x, y);
        }

        graphics.pose().popPose();
    }

    private void renderSingleSlot(GuiGraphics graphics, Player player, ISkillBarContainer bar,
                                  IAbilityContainer abilities, int slotIndex, int x, int y) {
        // A. 背景框
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LotMClientColors.SKILL_BAR_BG);
        graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, LotMClientColors.SKILL_BAR_BORDER);

        // B. 技能内容
        ResourceLocation skillId = bar.getSkillInSlot(slotIndex);
        if (skillId != null) {
            AbstractSkill skill = LotMSkills.getSkill(skillId);
            if (skill != null) {
                // 获取冷却状态
                boolean onCooldown = abilities.isOnCooldown(skillId);

                // ★★★ 核心修改：传递 isCooldown 参数，实现变暗效果 ★★★
                SkillRenderHelper.renderSkillIcon(graphics, skill, x + 1, y + 1, SLOT_SIZE - 2, onCooldown);

                // 激活状态显示
                if (abilities.isSkillActive(skillId)) {
                    SkillRenderHelper.renderActiveState(graphics, x, y, SLOT_SIZE, SLOT_SIZE);
                }

                // 冷却处理
                if (onCooldown) {
                    int currentCd = abilities.getCooldown(skillId);
                    int maxCd = skill.getCooldown(player);

                    if (maxCd > 0) {
                        float factor = (float) currentCd / maxCd;
                        // 渲染扇形时钟遮罩
                        SkillRenderHelper.renderCooldownOverlay(graphics, x + 1, y + 1, SLOT_SIZE - 2, factor);
                        // 渲染倒计时文字
                        renderCooldownText(graphics, currentCd, x, y);
                    }
                }
            }
        }

        // C. 按键提示
        renderKeyBindHint(graphics, slotIndex, x, y);
    }

    private void renderCooldownText(GuiGraphics graphics, int ticks, int x, int y) {
        Component text;
        Font font = Minecraft.getInstance().font;

        if (ticks >= 1200) {
            text = Component.translatable("gui.lotmmod.cooldown.minutes", ticks / 1200);
        } else {
            text = Component.literal(String.valueOf(ticks / 20 + 1));
        }

        float scale = 0.75f;
        int textWidth = font.width(text);

        graphics.pose().pushPose();
        graphics.pose().translate(x + (SLOT_SIZE - textWidth * scale) / 2, y + (SLOT_SIZE - 8 * scale) / 2, 200);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, LotMClientColors.SKILL_COOLDOWN_TEXT, true);
        graphics.pose().popPose();
    }

    private void renderKeyBindHint(GuiGraphics graphics, int slotIndex, int x, int y) {
        if (slotIndex >= LotMKeyBindings.SKILL_SLOT_KEYS.size()) return;

        Component keyName = LotMKeyBindings.SKILL_SLOT_KEYS.get(slotIndex).getTranslatedKeyMessage();
        float scale = 0.6f;

        graphics.pose().pushPose();
        graphics.pose().translate(x + 1, y + 1, 300);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(Minecraft.getInstance().font, keyName, 0, 0, LotMClientColors.SKILL_KEYBIND_TEXT, false);
        graphics.pose().popPose();
    }
}
