package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.client.util.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 幻觉数据包 (S2C)
 * <p>
 * 职责：服务端通知客户端生成视觉幻觉。
 * 包含：坐标、幻觉类型。
 * <p>
 * 修正记录：
 * 1. 方法名统一为 encode 以匹配 PacketHandler。
 */
public class S2CHallucinationPacket {

    private final double x, y, z;
    private final int type; // 0 = 假怪物, 1 = 假宝物(预留)

    public S2CHallucinationPacket(double x, double y, double z, int type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public S2CHallucinationPacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.type = buf.readInt();
    }

    // 修正：方法名从 toBytes 改为 encode
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(type);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // 逻辑转发给客户端处理器
            ClientPacketHandler.handleHallucination(x, y, z, type);
        });
        context.setPacketHandled(true);
    }
}
