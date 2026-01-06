package com.lotm.lotm.common.network.packet.s2c;

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

            // 1. 听觉反馈：危险等级决定音调 (0.6 ~ 1.2)
            // 越危险，心跳越急促/尖锐
            float pitch = 0.6f + (msg.dangerLevel * 0.15f);

            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASEDRUM.get(),
                    SoundSource.PLAYERS,
                    1.0f, pitch);

            // 2. 视觉反馈：Action Bar 提示
            // ★★★ 修正：不再格式化数字，只显示纯文本 ★★★
            // 文本会自动在约 3 秒后淡出，配合服务端 10 秒的 CD，会有 7 秒的清净时间。
            player.displayClientMessage(
                    Component.translatable("message.lotmmod.intuition_alert")
                            .withStyle(style -> style.withColor(0xFF5555)), // 保持红色警示
                    true
            );
        }
    }
}
