package com.lotm.lotm.client.gui.widget;

import com.lotm.lotm.client.util.LotMClientColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class LotMScrollbar {

    private int x, y, width, height;
    private int contentHeight = 0;
    private double scrollOffset = 0;
    private boolean isScrolling = false;

    public LotMScrollbar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight); // 防止负数
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, getMaxScroll());
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(double offset) {
        this.scrollOffset = Mth.clamp(offset, 0, getMaxScroll());
    }

    private int getMaxScroll() {
        return Math.max(0, contentHeight - height);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (contentHeight <= height || contentHeight <= 0) return; // ★★★ 防御性检查：防止除以零

        int maxScroll = getMaxScroll();
        // 计算滑块高度
        int barHeight = (int) ((float) height / contentHeight * height);
        barHeight = Math.max(20, barHeight);

        int trackHeight = height;
        int movableSpace = trackHeight - barHeight;

        // 防止 maxScroll 为 0 导致的除以零 (虽然上面已经检查了 contentHeight <= height)
        int barY = y;
        if (maxScroll > 0) {
            barY += (int) ((scrollOffset / maxScroll) * movableSpace);
        }

        graphics.fill(x, y, x + width, y + height, LotMClientColors.SCROLL_TRACK);

        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= barY && mouseY <= barY + barHeight;
        int thumbColor = (isScrolling || isHovered) ? LotMClientColors.SCROLL_THUMB_HOVER : LotMClientColors.SCROLL_THUMB_NORMAL;

        graphics.fill(x, barY, x + width, barY + barHeight, thumbColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && contentHeight > height) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                this.isScrolling = true;
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasScrolling = this.isScrolling;
            this.isScrolling = false;
            return wasScrolling;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling && contentHeight > height) {
            int maxScroll = getMaxScroll();
            int barHeight = (int) ((float) height / contentHeight * height);
            barHeight = Math.max(20, barHeight);

            int movableSpace = height - barHeight;

            if (movableSpace > 0) {
                double scale = (double) maxScroll / movableSpace;
                this.scrollOffset += dragY * scale;
                this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll);
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentHeight > height) {
            this.scrollOffset -= delta * 20;
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0, getMaxScroll());
            return true;
        }
        return false;
    }
}
