package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.api.capability.IDivinationContainer;
import com.lotm.lotm.common.capability.DivinationContainerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSyncDivinationDataPacket(CompoundTag data) {

    public S2CSyncDivinationDataPacket(IDivinationContainer container) {
        this(container.serializeNBT());
    }

    public S2CSyncDivinationDataPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }

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
        public static void handle(S2CSyncDivinationDataPacket msg) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(DivinationContainerProvider.CAPABILITY).ifPresent(cap -> {
                    cap.deserializeNBT(msg.data);
                });
            }
        }
    }
}
