package com.lotm.lotm.client.renderer.skill;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * 技能渲染接口
 * <p>
 * 负责处理技能激活时的客户端视觉效果。
 * 实现类应关注“怎么画”，而不是“能不能画”（状态判断由调用方处理）。
 */
public interface ISkillRenderer {

    /**
     * 渲染世界内的效果 (如：透视框、粒子、连线)
     * 在 RenderLevelStageEvent 中调用
     *
     * @param event 渲染事件
     * @param player 释放技能的玩家 (通常是客户端玩家)
     */
    void renderLevel(RenderLevelStageEvent event, Player player);

    /**
     * 渲染屏幕 UI 效果 (如：全屏滤镜、图标覆盖)
     * 在 RenderGuiOverlayEvent 或类似 HUD 渲染时调用
     *
     * @param graphics GUI 画笔
     * @param partialTick 插值 tick
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     */
    void renderHud(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight);
}
