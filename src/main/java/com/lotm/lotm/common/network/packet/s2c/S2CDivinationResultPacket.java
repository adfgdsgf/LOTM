package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.client.util.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 占卜结果反馈包
 * <p>
 * 职责：
 * 1. 携带占卜结果文本 (用于 Action Bar)。
 * 2. 携带目标坐标 (用于生成指向性粒子和方块高亮)。
 * 3. 携带目标实体ID (用于实体高亮)。
 * 4. 携带是否成功标志。
 */
public class S2CDivinationResultPacket {

    private final boolean success;
    private final Component message;
    private final double targetX, targetY, targetZ;
    private final int targetEntityId; // -1 表示无实体目标或目标是方块

    public S2CDivinationResultPacket(boolean success, Component message, Vec3 targetPos, int targetEntityId) {
        this.success = success;
        this.message = message;
        if (targetPos != null) {
            this.targetX = targetPos.x;
            this.targetY = targetPos.y;
            this.targetZ = targetPos.z;
        } else {
            this.targetX = 0;
            this.targetY = 0;
            this.targetZ = 0;
        }
        this.targetEntityId = targetEntityId;
    }

    public S2CDivinationResultPacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.message = buf.readComponent();
        this.targetX = buf.readDouble();
        this.targetY = buf.readDouble();
        this.targetZ = buf.readDouble();
        this.targetEntityId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeComponent(message);
        buf.writeDouble(targetX);
        buf.writeDouble(targetY);
        buf.writeDouble(targetZ);
        buf.writeInt(targetEntityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 这里的参数必须与 ClientPacketHandler.handleDivinationResult 的签名严格对应
            // 1. boolean success
            // 2. Component message
            // 3. Vec3 targetPos
            // 4. int entityId
            ClientPacketHandler.handleDivinationResult(
                    success,
                    message,
                    new Vec3(targetX, targetY, targetZ),
                    targetEntityId
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
