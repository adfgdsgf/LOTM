package com.lotm.lotm.common.network.packet.c2s;

import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncAbilityDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncBeyonderDataPacket;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.CastContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: 释放技能请求 (升级版)
 * 携带组合键信息
 */
public record C2SCastSkillPacket(ResourceLocation skillId, byte modifiers) {

    public C2SCastSkillPacket(ResourceLocation skillId, CastContext context) {
        this(skillId, context.pack());
    }

    public C2SCastSkillPacket(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.skillId);
        buf.writeByte(this.modifiers);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                    AbstractSkill skill = LotMSkills.getSkill(this.skillId);

                    // 解码上下文
                    CastContext context = CastContext.unpack(this.modifiers);

                    if (skill != null && skill.canCast(player, state, abilities)) {
                        // 传入 context 执行技能
                        skill.cast(player, player.serverLevel(), state, abilities, context);

                        // 同步数据
                        PacketHandler.sendToPlayer(new S2CSyncBeyonderDataPacket(state), player);
                        PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), player);
                    }
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
