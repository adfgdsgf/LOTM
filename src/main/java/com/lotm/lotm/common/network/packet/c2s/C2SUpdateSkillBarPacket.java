package com.lotm.lotm.common.network.packet.c2s;

import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncAbilityDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncSkillBarPacket;
import com.lotm.lotm.common.registry.LotMSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * C2S: 客户端请求更新技能栏槽位 (拖拽放置)
 */
public class C2SUpdateSkillBarPacket {
    private final int slot;
    @Nullable
    private final ResourceLocation skillId;

    public C2SUpdateSkillBarPacket(int slot, @Nullable ResourceLocation skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    public C2SUpdateSkillBarPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        if (buf.readBoolean()) {
            this.skillId = buf.readResourceLocation();
        } else {
            this.skillId = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeBoolean(skillId != null);
        if (skillId != null) {
            buf.writeResourceLocation(skillId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 1. 验证技能是否合法 (是否已学习)
            if (skillId != null) {
                boolean hasLearned = player.getCapability(AbilityContainerProvider.CAPABILITY)
                        .map(c -> c.hasAbility(skillId)).orElse(false);
                if (!hasLearned || LotMSkills.getSkill(skillId) == null) {
                    return; // 拒绝非法请求
                }
            }

            // 2. 更新 AbilityContainer 中的预设数据 (持久化)
            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                int currentPage = abilities.getActivePage();
                abilities.setPreset(currentPage, slot, skillId);

                // 3. 同时更新当前激活的 SkillBarContainer (即时生效)
                player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                    bar.setSkillInSlot(slot, skillId);

                    // 4. 显式同步所有相关数据回客户端 (解耦)
                    PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), player);
                    PacketHandler.sendToPlayer(new S2CSyncSkillBarPacket(bar), player);
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
