package com.lotm.lotm.client.gui.util;

import com.lotm.lotm.content.skill.AbstractSkill;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nullable;

/**
 * 拖拽处理器 (Drag Handler)
 * <p>
 * 职责：管理 GUI 中的拖拽状态机。
 * <p>
 * 工业级优化：
 * 引入 **距离阈值 (Hysteresis)** 机制。
 * 用户按下鼠标时，不会立即触发拖拽（显示图标），而是进入 "Pending" 状态。
 * 只有当鼠标移动距离超过 {@link #DRAG_THRESHOLD_SQR} 像素时，才正式确认为拖拽行为。
 * 这完美解决了“点击选择时闪烁拖拽图标”的糟糕体验。
 */
public class DragHandler {

    // 拖拽阈值 (像素平方)：5px * 5px = 25
    // 使用平方距离避免开方运算，提升性能
    private static final double DRAG_THRESHOLD_SQR = 25.0;

    @Nullable
    private AbstractSkill pendingSkill = null; // 预备拖拽的技能
    @Nullable
    private AbstractSkill draggingSkill = null; // 正在拖拽的技能 (已确认)

    // 拖拽起始坐标
    private double startX, startY;
    // 当前鼠标坐标
    private double currentX, currentY;

    // 来源槽位 (-1 表示来自列表，>=0 表示来自技能栏槽位)
    private int sourceSlot = -1;

    /**
     * 鼠标按下 (Press)
     * 此时仅记录意图，不开始拖拽。
     */
    public void startPress(AbstractSkill skill, double x, double y, int sourceSlot) {
        this.pendingSkill = skill;
        this.draggingSkill = null; // 尚未确认
        this.startX = x;
        this.startY = y;
        this.currentX = x;
        this.currentY = y;
        this.sourceSlot = sourceSlot;
    }

    /**
     * 鼠标移动 (Drag)
     * 在 Screen 的 mouseDragged 中调用。
     * 负责检测是否突破阈值。
     */
    public void onMouseDragged(double x, double y) {
        this.currentX = x;
        this.currentY = y;

        // 如果处于预备状态，检查距离
        if (pendingSkill != null && draggingSkill == null) {
            double dx = x - startX;
            double dy = y - startY;
            // 距离判定
            if (dx * dx + dy * dy > DRAG_THRESHOLD_SQR) {
                // 突破阈值，正式开始拖拽
                this.draggingSkill = pendingSkill;
            }
        }
    }

    /**
     * 鼠标释放 (Release)
     * @return 如果刚才是一个有效的拖拽操作，返回 true；如果是点击操作，返回 false。
     */
    public boolean stopDrag() {
        boolean wasDragging = (draggingSkill != null);

        // 重置所有状态
        this.pendingSkill = null;
        this.draggingSkill = null;
        this.sourceSlot = -1;

        return wasDragging;
    }

    /**
     * 检查当前是否处于“视觉上的”拖拽状态
     * 用于决定是否渲染跟随图标
     */
    public boolean isDragging() {
        return draggingSkill != null;
    }

    /**
     * 获取当前正在拖拽的技能
     */
    @Nullable
    public AbstractSkill getDraggingSkill() {
        return draggingSkill;
    }

    /**
     * 获取拖拽的来源槽位
     */
    public int getSourceSlot() {
        return sourceSlot;
    }

    /**
     * 渲染拖拽中的图标 (跟随鼠标)
     */
    public void render(GuiGraphics graphics) {
        // 只有正式确认为拖拽后，才渲染图标
        if (draggingSkill != null) {
            int iconSize = 24;
            // 居中渲染图标，使鼠标位于图标中心
            // 强转 int 可能会有微小抖动，但在 GUI 渲染中通常可接受
            SkillRenderHelper.renderSkillIcon(graphics, draggingSkill,
                    (int)currentX - iconSize / 2, (int)currentY - iconSize / 2, iconSize);
        }
    }
}
