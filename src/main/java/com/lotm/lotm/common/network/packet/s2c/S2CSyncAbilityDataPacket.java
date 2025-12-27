package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.api.capability.IAbilityContainer;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSyncAbilityDataPacket(CompoundTag data) {

    // 业务构造函数
    public S2CSyncAbilityDataPacket(IAbilityContainer container) {
        this(container.serializeNBT());
    }

    // 解码构造函数 (适配 PacketHandler::new)
    public S2CSyncAbilityDataPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }

    // 实例编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(this.data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(S2CSyncAbilityDataPacket msg) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(container -> {
                    container.deserializeNBT(msg.data);
                });
            }
        }
    }
}
