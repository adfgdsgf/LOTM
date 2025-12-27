package com.lotm.lotm.client.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.client.renderer.skill.ISkillRenderer;
import com.lotm.lotm.client.renderer.skill.SkillRendererRegistry;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 客户端 Forge 事件总线监听器
 * 处理：渲染循环挂钩 (Render Level / Render GUI)
 */
@Mod.EventBusSubscriber(modid = LotMMod.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {

    /**
     * 1. 挂钩世界渲染 (Render Level)
     * 用于绘制实体透视框、粒子连线等 3D 效果。
     * 必须使用 AFTER_TRANSLUCENT_BLOCKS 以确保透视效果正确叠加。
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 性能检查：仅在特定阶段执行，避免每帧执行多次
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // 获取玩家能力容器
        player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
            List<ResourceLocation> activeSkills = abilities.getActiveSkills();
            if (activeSkills.isEmpty()) return;

            // 遍历所有激活的技能
            for (ResourceLocation skillId : activeSkills) {
                // 从注册表中查找对应的渲染器
                ISkillRenderer renderer = SkillRendererRegistry.getRenderer(skillId);
                if (renderer != null) {
                    // 执行世界渲染
                    renderer.renderLevel(event, player);
                }
            }
        });
    }

    /**
     * 2. 挂钩 HUD 渲染 (Render GUI)
     * 用于绘制全屏滤镜、额外 UI 元素。
     * 仅在 HOTBAR 渲染后执行，确保位于底层 UI 之上。
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // 确保只在主 HUD (Hotbar) 渲染后绘制，避免覆盖调试信息或聊天框
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
            List<ResourceLocation> activeSkills = abilities.getActiveSkills();
            if (activeSkills.isEmpty()) return;

            for (ResourceLocation skillId : activeSkills) {
                ISkillRenderer renderer = SkillRendererRegistry.getRenderer(skillId);
                if (renderer != null) {
                    // 执行 HUD 渲染
                    renderer.renderHud(event.getGuiGraphics(), event.getPartialTick(),
                            event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
                }
            }
        });
    }
}
