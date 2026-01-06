package com.lotm.lotm.common.network.packet.c2s;

import com.lotm.lotm.common.registry.LotMRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SRequestDivinationPacket {

    private final InteractionHand hand;
    private final String targetType; // 改为 String 以支持扩展
    private final ResourceLocation targetId;

    public C2SRequestDivinationPacket(InteractionHand hand, String targetType, ResourceLocation targetId) {
        this.hand = hand;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public C2SRequestDivinationPacket(FriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.targetType = buf.readUtf();
        this.targetId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeUtf(targetType);
        buf.writeResourceLocation(targetId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(LotMRegistries.SPIRIT_PENDULUM.get())) {
                return;
            }

            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("DivinationType", targetType);
            tag.putString("DivinationTarget", targetId.toString());

            player.displayClientMessage(Component.translatable("message.lotm.divination.target_set"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
