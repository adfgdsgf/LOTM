package com.lotm.lotm.client.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.client.gui.SpiritualityOverlay;
import com.lotm.lotm.client.gui.divination.BlockCategory;
import com.lotm.lotm.client.gui.divination.DivinationCategoryRegistry;
import com.lotm.lotm.client.gui.divination.EntityCategory;
import com.lotm.lotm.client.gui.overlay.SkillBarOverlay;
import com.lotm.lotm.client.renderer.skill.SkillRendererRegistry;
import com.lotm.lotm.client.renderer.skill.impl.DivinationSkillRenderer;
import com.lotm.lotm.client.renderer.skill.impl.SpiritVisionRenderer;
import com.lotm.lotm.common.registry.LotMSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LotMMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("spirituality_overlay", new SpiritualityOverlay());
        event.registerAboveAll("skill_bar_overlay", new SkillBarOverlay());
    }


    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 1. 注册技能渲染器
            // 灵视：负责 World 和 HUD 渲染
            SkillRendererRegistry.register(LotMSkills.SPIRIT_VISION.getId(), new SpiritVisionRenderer());

            // ★★★ 新增：占卜技能渲染器 (负责 Info 面板) ★★★
            SkillRendererRegistry.register(LotMSkills.DIVINATION.getId(), new DivinationSkillRenderer());

            LotMMod.LOGGER.info("Registered client skill renderers.");

            // 2. 注册占卜分类 (GUI Tabs)
            DivinationCategoryRegistry.register(new ResourceLocation(LotMMod.MODID, "blocks"), new BlockCategory());
            DivinationCategoryRegistry.register(new ResourceLocation(LotMMod.MODID, "entities"), new EntityCategory());

            LotMMod.LOGGER.info("Registered divination categories.");
        });
    }
}
