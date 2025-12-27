package com.lotm.lotm.common.network.packet.c2s;

import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.skillbar.ISkillBarContainer;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncAbilityDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncSkillBarPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: 客户端请求切换预设页
 */
public record C2SSwitchPresetPacket(int pageIndex) {

    public C2SSwitchPresetPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(pageIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                // 1. 更新激活页码
                abilities.setActivePage(pageIndex);
                int validPage = abilities.getActivePage();

                // 2. 将新页面的数据加载到 SkillBarContainer (覆盖当前激活栏)
                player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                    for (int i = 0; i < ISkillBarContainer.SLOT_COUNT; i++) {
                        ResourceLocation skill = abilities.getAbilityInSlot(validPage, i);
                        bar.setSkillInSlot(i, skill);
                    }

                    // 3. 显式同步回客户端 (解耦)
                    PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), player);
                    PacketHandler.sendToPlayer(new S2CSyncSkillBarPacket(bar), player);
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
