package com.lotm.lotm.client.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.client.gui.SpiritualityOverlay;
import com.lotm.lotm.client.gui.overlay.SkillBarOverlay;
import com.lotm.lotm.client.renderer.skill.SkillRendererRegistry;
import com.lotm.lotm.client.renderer.skill.impl.SpiritVisionRenderer;
import com.lotm.lotm.common.registry.LotMSkills;
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
        // 注册技能渲染器
        // 这里的 ID 必须与 SpiritVision.ID 一致
        event.enqueueWork(() -> {
            SkillRendererRegistry.register(LotMSkills.SPIRIT_VISION.getId(), new SpiritVisionRenderer());

            // Log 确认
            LotMMod.LOGGER.info("Registered client skill renderers.");
        });
    }
}
