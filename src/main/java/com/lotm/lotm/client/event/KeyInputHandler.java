package com.lotm.lotm.client.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.client.gui.screen.SkillConfigScreen;
import com.lotm.lotm.client.key.LotMKeyBindings;
import com.lotm.lotm.common.capability.AbilityContainer;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.c2s.C2SCastSkillPacket;
import com.lotm.lotm.common.network.packet.c2s.C2SSwitchPresetPacket;
import com.lotm.lotm.content.skill.CastContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LotMMod.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.screen != null) return;

        // 构建当前的组合键上下文
        // 工业级修正：使用注册的 KeyMapping 状态，而非硬编码的 Screen.hasShiftDown()
        CastContext context = new CastContext(
                LotMKeyBindings.SKILL_MODIFIER_1.isDown(),
                LotMKeyBindings.SKILL_MODIFIER_2.isDown(),
                LotMKeyBindings.SKILL_MODIFIER_3.isDown()
        );

        // 处理技能释放
        for (int i = 0; i < LotMKeyBindings.SKILL_SLOT_KEYS.size(); i++) {
            while (LotMKeyBindings.SKILL_SLOT_KEYS.get(i).consumeClick()) {
                handleSkillCast(player, i, context);
            }
        }

        // 处理功能按键
        while (LotMKeyBindings.OPEN_SKILL_CONFIG.consumeClick()) {
            mc.setScreen(new SkillConfigScreen());
        }
        while (LotMKeyBindings.PREV_PRESET.consumeClick()) {
            handlePresetSwitch(player, -1);
        }
        while (LotMKeyBindings.NEXT_PRESET.consumeClick()) {
            handlePresetSwitch(player, 1);
        }
    }

    private static void handleSkillCast(LocalPlayer player, int slotIndex, CastContext context) {
        player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(container -> {
            ResourceLocation skillId = container.getSkillInSlot(slotIndex);
            if (skillId != null) {
                // 发送携带 Context 的包
                PacketHandler.CHANNEL.sendToServer(new C2SCastSkillPacket(skillId, context));
            }
        });
    }

    private static void handlePresetSwitch(LocalPlayer player, int delta) {
        player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(container -> {
            int currentPage = container.getActivePage();
            int maxPages = AbilityContainer.MAX_PAGES;
            // 轮循切换逻辑
            int newPage = (currentPage + delta + maxPages) % maxPages;
            if (newPage != currentPage) {
                PacketHandler.CHANNEL.sendToServer(new C2SSwitchPresetPacket(newPage));
                player.displayClientMessage(Component.translatable("msg.lotmmod.preset_switched", newPage + 1), true);
            }
        });
    }
}
