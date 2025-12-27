package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.common.capability.skillbar.ISkillBarContainer;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 同步技能栏数据
 * 统一标准：使用构造函数解码，实例方法编码。
 */
public class S2CSyncSkillBarPacket {

    private final CompoundTag nbtData;

    // 业务构造函数
    public S2CSyncSkillBarPacket(ISkillBarContainer container) {
        this.nbtData = container.serializeNBT();
    }

    // 基础构造函数
    public S2CSyncSkillBarPacket(CompoundTag nbtData) {
        this.nbtData = nbtData;
    }

    // 解码构造函数 (适配 PacketHandler::new)
    public S2CSyncSkillBarPacket(FriendlyByteBuf buf) {
        this.nbtData = buf.readNbt();
    }

    // 实例编码方法 (适配 PacketHandler::encode)
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(nbtData);
    }

    // 处理逻辑
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handlePacket(nbtData));
        });
        context.get().setPacketHandled(true);
    }

    // 物理端隔离
    private static class ClientHandler {
        public static void handlePacket(CompoundTag nbt) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(skillBar -> {
                    skillBar.deserializeNBT(nbt);
                });
            }
        }
    }
}
