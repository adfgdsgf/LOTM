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
 * C2S: 切换技能状态请求
 * <p>
 * 职责：响应客户端 UI（如技能列表中的开关按钮）的点击事件。
 * 逻辑：根据技能类型（被动 vs 主动）执行不同的切换逻辑。
 */
public record C2SToggleSkillPacket(ResourceLocation skillId) {

    // 解码构造函数
    public C2SToggleSkillPacket(FriendlyByteBuf buf) {
        this(buf.readResourceLocation());
    }

    // 编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.skillId);
    }

    // 业务处理逻辑
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                AbstractSkill skill = LotMSkills.getSkill(this.skillId);

                // 1. 安全性检查：
                // 确保技能存在，且属于“可切换/维持”的类型 (TOGGLE, SUMMON_MAINTAIN, PASSIVE)
                if (skill == null || !skill.getCastType().isToggleOrMaintain()) return;

                // ★★★ 安全检查：如果试图关闭，必须检查 canBeDeactivated ★★★
                // 如果技能当前是激活状态，且不允许被关闭，则直接忽略请求
                if (abilities.isSkillActive(skill.getId()) && !skill.canBeDeactivated(player)) {
                    return; // 拒绝请求
                }

                // 2. 逻辑分流
                if (skill.getCastType().isPassive()) {
                    // === 分支 A: 被动技能 (PASSIVE) ===
                    // 被动技能不需要经过 cast() 检查（无冷却、无启动消耗），直接切换状态。
                    // 这允许玩家手动“收敛灵性”，关闭直觉以避免被打扰。
                    if (abilities.isSkillActive(skill.getId())) {
                        abilities.deactivateSkill(skill.getId());
                    } else {
                        abilities.activateSkill(skill.getId());
                    }

                    // 仅同步能力数据（被动切换通常不涉及灵性瞬间扣除）
                    PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), player);

                } else {
                    // === 分支 B: 主动持续技能 (TOGGLE / MAINTAIN) ===
                    // 例如“灵视”。需要走完整的 cast() 流程，以处理：
                    // - 灵性是否足够启动
                    // - 是否在冷却中
                    // - 播放开启/关闭音效
                    // - 扣除启动消耗
                    player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                        // AbstractSkill.cast() 内部包含了“若开启则关闭，若关闭则开启”的逻辑
                        if (skill.canCast(player, state, abilities)) {
                            // 传入空上下文，因为 UI 点击通常不涉及 Shift/Ctrl 组合键
                            skill.cast(player, player.serverLevel(), state, abilities, CastContext.EMPTY);

                            // 同步 Ability 数据 (更新 activeSkills 列表)
                            PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), player);
                            // 同步 Beyonder 数据 (更新灵性消耗)
                            PacketHandler.sendToPlayer(new S2CSyncBeyonderDataPacket(state), player);
                        }
                    });
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
