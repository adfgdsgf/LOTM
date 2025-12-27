package com.lotm.lotm.common.network.packet.s2c;

import com.lotm.lotm.LotMMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 灵性直觉预警包
 */
public record S2CIntuitionAlertPacket(int dangerLevel) {

    public S2CIntuitionAlertPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dangerLevel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(this));
        });
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        public static void handle(S2CIntuitionAlertPacket msg) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // 1. 听觉反馈：心跳声
            // 使用音符盒的底鼓声 (Base Drum) 模拟心跳 "咚...咚..."
            // 音调随危险等级升高 (0.6 ~ 1.0)，模拟心跳加速
            float pitch = 0.6f + (msg.dangerLevel * 0.05f);
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASEDRUM.get(), // 修复点：使用更稳定的音效常量
                    SoundSource.PLAYERS,
                    1.0f, pitch);

            // 2. 视觉反馈：Action Bar 提示
            player.displayClientMessage(
                    Component.translatable("message.lotmmod.intuition_alert", msg.dangerLevel),
                    true
            );
        }
    }
}
