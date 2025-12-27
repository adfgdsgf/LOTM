package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.api.capability.IBeyonderState;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSyncBeyonderDataPacket(CompoundTag data) {

    // 业务构造函数
    public S2CSyncBeyonderDataPacket(IBeyonderState state) {
        this(state.serializeNBT());
    }

    // 解码构造函数 (适配 PacketHandler::new)
    public S2CSyncBeyonderDataPacket(FriendlyByteBuf buf) {
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
        public static void handle(S2CSyncBeyonderDataPacket msg) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                    state.deserializeNBT(msg.data);
                });
            }
        }
    }
}
