package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.client.renderer.ClientDivinationRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 设置客户端高亮包
 * <p>
 * 用于危险感知或占卜成功后，通知客户端高亮显示某个实体。
 */
public class S2CSetDivinationHighlightPacket {

    private final int targetEntityId;
    private final int duration;

    public S2CSetDivinationHighlightPacket(int targetEntityId, int duration) {
        this.targetEntityId = targetEntityId;
        this.duration = duration;
    }

    public S2CSetDivinationHighlightPacket(FriendlyByteBuf buf) {
        this.targetEntityId = buf.readInt();
        this.duration = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(targetEntityId);
        buf.writeInt(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(S2CSetDivinationHighlightPacket msg) {
            // 调用客户端渲染器设置高亮
            // pos 传 null，表示仅锁定实体 ID
            ClientDivinationRenderer.setHighlight(null, msg.targetEntityId, msg.duration);
        }
    }
}
