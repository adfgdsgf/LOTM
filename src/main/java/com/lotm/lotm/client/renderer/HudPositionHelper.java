package com.lotm.lotm.client.renderer;

/**
 * HUD 定位工具类
 * HUD Position Helper
 *
 * 负责计算 UI 元素在屏幕上的绝对坐标。
 * 支持锚点系统 (Anchor System)。
 */
public class HudPositionHelper {

    public enum AnchorPoint {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    /**
     * 计算 HUD 在屏幕上的位置
     *
     * @param anchor          锚点
     * @param offsetX         像素偏移 X
     * @param offsetY         像素偏移 Y
     * @param screenWidth     屏幕宽度
     * @param screenHeight    屏幕高度
     * @param contentWidth    内容宽度
     * @param contentHeight   内容高度
     * @param margin          边缘边距
     * @return [x, y] 坐标数组
     */
    public static int[] calculatePosition(AnchorPoint anchor,
                                          int offsetX, int offsetY,
                                          int screenWidth, int screenHeight,
                                          int contentWidth, int contentHeight,
                                          int margin) {
        int x = margin;
        int y = margin;

        switch (anchor) {
            case TOP_LEFT -> { /* default */ }
            case TOP_CENTER -> x = (screenWidth - contentWidth) / 2;
            case TOP_RIGHT -> x = screenWidth - contentWidth - margin;
            case CENTER_LEFT -> y = (screenHeight - contentHeight) / 2;
            case CENTER -> {
                x = (screenWidth - contentWidth) / 2;
                y = (screenHeight - contentHeight) / 2;
            }
            case CENTER_RIGHT -> {
                x = screenWidth - contentWidth - margin;
                y = (screenHeight - contentHeight) / 2;
            }
            case BOTTOM_LEFT -> y = screenHeight - contentHeight - margin;
            case BOTTOM_CENTER -> {
                x = (screenWidth - contentWidth) / 2;
                y = screenHeight - contentHeight - margin;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - contentWidth - margin;
                y = screenHeight - contentHeight - margin;
            }
        }

        return new int[]{x + offsetX, y + offsetY};
    }
}
