package com.lotm.lotm.common.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncAbilityDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncBeyonderDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncSkillBarPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 全量数据同步事件
 * 负责在关键节点（登录、重生、切维度）将服务端数据完整发送给客户端
 */
@Mod.EventBusSubscriber(modid = LotMMod.MODID)
public class CommonCapabilityEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncAllData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncAllData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncAllData(event.getEntity());
    }

    public static void syncAllData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 1. 同步非凡状态
            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                PacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new S2CSyncBeyonderDataPacket(state)
                );
            });

            // 2. 同步技能列表和冷却
            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(container -> {
                PacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new S2CSyncAbilityDataPacket(container)
                );
            });

            // 3. 同步技能栏配置
            player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                PacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new S2CSyncSkillBarPacket(bar)
                );
            });
        }
    }
}
