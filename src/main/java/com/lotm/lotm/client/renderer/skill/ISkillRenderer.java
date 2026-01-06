package com.lotm.lotm.client.renderer.skill;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * 技能渲染接口
 * <p>
 * 负责处理技能激活时的客户端视觉效果。
 * 实现类应关注“怎么画”，而不是“能不能画”。
 * <p>
 * 修正：
 * 1. 增加 renderInfo 方法，支持在技能详情页绘制自定义 UI。
 * 2. 使用 default 方法，避免强制实现不需要的方法。
 */
public interface ISkillRenderer {

    /**
     * 渲染世界内的效果 (如：透视框、粒子、连线)
     * 在 RenderLevelStageEvent 中调用
     */
    default void renderLevel(RenderLevelStageEvent event, Player player) {}

    /**
     * 渲染屏幕 UI 效果 (如：全屏滤镜、图标覆盖)
     * 在 RenderGuiOverlayEvent 或类似 HUD 渲染时调用
     */
    default void renderHud(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {}

    /**
     * 在技能配置界面的信息面板中绘制额外信息
     * (例如：熟练度条、特殊的子选项开关等)
     *
     * @param graphics GUI 画笔
     * @param x 起始 X 坐标
     * @param y 起始 Y 坐标
     * @param width 可用宽度
     * @return 绘制内容实际占用的高度 (像素)，用于排版后续内容
     */
    default int renderInfo(GuiGraphics graphics, int x, int y, int width) {
        return 0;
    }
}
