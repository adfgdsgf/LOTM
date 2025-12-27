package com.lotm.lotm.client.gui.screen;

import com.lotm.lotm.client.config.LotMClientConfig;
import com.lotm.lotm.client.renderer.HudPositionHelper;
import com.lotm.lotm.client.renderer.HudPositionHelper.AnchorPoint;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.common.capability.skillbar.ISkillBarContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 技能栏 HUD 编辑界面 (HUD Editor)
 * <p>
 * 功能：
 * 1. 可视化拖拽调整 HUD 位置。
 * 2. 切换布局方向 (横向/纵向)。
 * 3. 调整缩放比例。
 * 4. 实时预览并保存配置。
 */
public class SkillBarEditScreen extends Screen {

    private final Screen parentScreen;
    private AnchorPoint anchor;
    private int offsetX;
    private int offsetY;
    private float scale;
    private boolean horizontal;

    private boolean isDragging = false;
    private int dragStartX, dragStartY;
    private int initialOffsetX, initialOffsetY;

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_SPACING = 2;
    private static final int MARGIN = 5;

    public SkillBarEditScreen(@Nullable Screen parentScreen) {
        super(Component.translatable("gui.lotmmod.skill_bar_edit.title"));
        this.parentScreen = parentScreen;
        this.anchor = LotMClientConfig.CLIENT.skillBarAnchor.get();
        this.offsetX = LotMClientConfig.CLIENT.skillBarOffsetX.get();
        this.offsetY = LotMClientConfig.CLIENT.skillBarOffsetY.get();
        this.scale = LotMClientConfig.CLIENT.skillBarScale.get().floatValue();
        this.horizontal = LotMClientConfig.CLIENT.skillBarHorizontal.get();
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 100;
        int btnHeight = 20;
        int padding = 10;
        int startY = 40;

        // 布局切换
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("gui.lotmmod.layout.horizontal"),
                        Component.translatable("gui.lotmmod.layout.vertical"))
                .withInitialValue(this.horizontal)
                .create(padding, startY, 150, btnHeight,
                        Component.translatable("gui.lotmmod.layout_label"),
                        (btn, val) -> this.horizontal = val));

        // 缩放控制
        int scaleY = startY + btnHeight + 5;
        this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> this.scale = Math.max(0.5f, this.scale - 0.1f))
                .bounds(padding, scaleY, 20, btnHeight).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> this.scale = Math.min(2.0f, this.scale + 0.1f))
                .bounds(padding + 130, scaleY, 20, btnHeight).build());

        // 保存
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> {
            saveConfig();
            this.minecraft.setScreen(parentScreen);
        }).bounds(this.width - btnWidth - padding, this.height - btnHeight - padding, btnWidth, btnHeight).build());

        // 取消
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> this.minecraft.setScreen(parentScreen))
                .bounds(this.width - btnWidth * 2 - padding * 2, this.height - btnHeight - padding, btnWidth, btnHeight).build());

        // 重置
        this.addRenderableWidget(Button.builder(Component.translatable("gui.lotmmod.reset"), btn -> {
            this.anchor = AnchorPoint.BOTTOM_CENTER;
            this.offsetX = 0;
            this.offsetY = -40;
            this.scale = 1.0f;
            this.horizontal = true;
            this.rebuildWidgets();
        }).bounds(padding, this.height - btnHeight - padding, 60, btnHeight).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 辅助线
        graphics.fill(this.width / 2, 0, this.width / 2 + 1, this.height, LotMClientColors.GUIDE_LINE);
        graphics.fill(0, this.height / 2, this.width, this.height / 2 + 1, LotMClientColors.GUIDE_LINE);

        graphics.drawCenteredString(this.font, Component.translatable("gui.lotmmod.skill_bar_edit.drag_hint"), this.width / 2, 10, LotMClientColors.TEXT_NORMAL);

        Component coordsText = Component.translatable("gui.lotmmod.skill_bar_edit.coords", offsetX, offsetY, anchor.name());
        graphics.drawString(this.font, coordsText, 10, 20, LotMClientColors.TEXT_DIM, false);

        Component scaleText = Component.translatable("gui.lotmmod.skill_bar_edit.scale_value", String.format("%.1f", scale));
        graphics.drawString(this.font, scaleText, 40, 70, LotMClientColors.TEXT_NORMAL, false);

        renderPreview(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics) {
        int slotCount = ISkillBarContainer.SLOT_COUNT;
        int baseWidth = horizontal ? slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING : SLOT_SIZE;
        int baseHeight = horizontal ? SLOT_SIZE : slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING;
        int scaledWidth = (int) (baseWidth * scale);
        int scaledHeight = (int) (baseHeight * scale);

        int[] pos = HudPositionHelper.calculatePosition(anchor, offsetX, offsetY, this.width, this.height, scaledWidth, scaledHeight, MARGIN);
        int startX = pos[0];
        int startY = pos[1];

        int padding = 2;
        int borderColor = isDragging ? LotMClientColors.HUD_DRAG_BORDER : LotMClientColors.HUD_PREVIEW_BORDER;
        graphics.renderOutline(startX - padding, startY - padding, scaledWidth + padding * 2, scaledHeight + padding * 2, borderColor);
        graphics.fill(startX - padding, startY - padding, startX + scaledWidth + padding, startY + scaledHeight + padding, LotMClientColors.HUD_PREVIEW_BG);

        graphics.pose().pushPose();
        graphics.pose().translate(startX, startY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < slotCount; i++) {
            int x = horizontal ? i * (SLOT_SIZE + SLOT_SPACING) : 0;
            int y = horizontal ? 0 : i * (SLOT_SIZE + SLOT_SPACING);

            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LotMClientColors.HUD_SLOT_PLACEHOLDER);
            graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, LotMClientColors.SKILL_BORDER_NORMAL);

            String num = String.valueOf(i + 1);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 2, y + 2, 0);
            graphics.pose().scale(0.8f, 0.8f, 1f);
            graphics.drawString(this.font, num, 0, 0, LotMClientColors.TEXT_DIM, false);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int slotCount = ISkillBarContainer.SLOT_COUNT;
            int baseWidth = horizontal ? slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING : SLOT_SIZE;
            int baseHeight = horizontal ? SLOT_SIZE : slotCount * SLOT_SIZE + (slotCount - 1) * SLOT_SPACING;
            int scaledWidth = (int) (baseWidth * scale);
            int scaledHeight = (int) (baseHeight * scale);
            int[] pos = HudPositionHelper.calculatePosition(anchor, offsetX, offsetY, this.width, this.height, scaledWidth, scaledHeight, MARGIN);
            int x = pos[0];
            int y = pos[1];
            int padding = 2;

            if (mouseX >= x - padding && mouseX <= x + scaledWidth + padding && mouseY >= y - padding && mouseY <= y + scaledHeight + padding) {
                isDragging = true;
                dragStartX = (int) mouseX;
                dragStartY = (int) mouseY;
                initialOffsetX = offsetX;
                initialOffsetY = offsetY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            int dx = (int) mouseX - dragStartX;
            int dy = (int) mouseY - dragStartY;
            this.offsetX = initialOffsetX + dx;
            this.offsetY = initialOffsetY + dy;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void saveConfig() {
        LotMClientConfig.CLIENT.skillBarAnchor.set(anchor);
        LotMClientConfig.CLIENT.skillBarOffsetX.set(offsetX);
        LotMClientConfig.CLIENT.skillBarOffsetY.set(offsetY);
        LotMClientConfig.CLIENT.skillBarScale.set((double) scale);
        LotMClientConfig.CLIENT.skillBarHorizontal.set(horizontal);
        LotMClientConfig.CLIENT_SPEC.save();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
