package com.lotm.lotm.client.gui.util;

import com.lotm.lotm.content.skill.AbstractSkill;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nullable;

/**
 * 拖拽处理器 (Drag Handler)
 * <p>
 * 负责管理 GUI 中的拖拽状态。
 * 采用中介者模式，解耦 Screen 的交互逻辑与渲染逻辑。
 */
public class DragHandler {

    @Nullable
    private AbstractSkill draggingSkill = null;

    // 拖拽起始坐标 (用于判断是否开始拖拽)
    private int startX, startY;

    // 当前拖拽坐标 (用于渲染跟随图标)
    private int currentX, currentY;

    // 来源槽位 (-1 表示来自列表，>=0 表示来自技能栏槽位)
    private int sourceSlot = -1;

    // 拖拽阈值 (防止点击时的微小抖动被误判为拖拽)
    private static final int DRAG_THRESHOLD = 5;

    /**
     * 开始拖拽操作
     *
     * @param skill      被拖拽的技能对象
     * @param x          鼠标起始 X 坐标
     * @param y          鼠标起始 Y 坐标
     * @param sourceSlot 来源槽位索引 (-1 代表从列表拖拽)
     */
    public void startDrag(AbstractSkill skill, int x, int y, int sourceSlot) {
        this.draggingSkill = skill;
        this.startX = x;
        this.startY = y;
        this.currentX = x;
        this.currentY = y;
        this.sourceSlot = sourceSlot;
    }

    /**
     * 更新拖拽位置 (在 mouseDragged 中调用)
     */
    public void updatePosition(int x, int y) {
        this.currentX = x;
        this.currentY = y;
    }

    /**
     * 停止拖拽并重置所有状态
     */
    public void stopDrag() {
        this.draggingSkill = null;
        this.sourceSlot = -1;
    }

    /**
     * 检查当前是否处于拖拽状态
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
     * 检查是否移动了足够距离以触发拖拽逻辑
     * (通常用于区分点击和拖拽)
     */
    public boolean hasMovedEnough() {
        return Math.abs(currentX - startX) > DRAG_THRESHOLD ||
                Math.abs(currentY - startY) > DRAG_THRESHOLD;
    }

    /**
     * 渲染拖拽中的图标 (跟随鼠标)
     */
    public void render(GuiGraphics graphics) {
        if (draggingSkill != null) {
            int iconSize = 24;
            // 居中渲染图标，使鼠标位于图标中心
            SkillRenderHelper.renderSkillIcon(graphics, draggingSkill,
                    currentX - iconSize / 2, currentY - iconSize / 2, iconSize);
        }
    }
}
