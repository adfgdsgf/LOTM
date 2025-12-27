package com.lotm.lotm.common.event;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncAbilityDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncBeyonderDataPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncSkillBarPacket;
import com.lotm.lotm.common.registry.LotMPathways;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import com.lotm.lotm.content.skill.AbstractSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Forge 事件监听器
 * 处理：Capability 附加、玩家克隆、Tick 心跳、战斗检测、数据同步与自动修复
 */
@Mod.EventBusSubscriber(modid = LotMMod.MODID)
public class CommonForgeEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(BeyonderStateProvider.CAPABILITY).isPresent()) {
                event.addCapability(new ResourceLocation(LotMMod.MODID, "beyonder_state"), new BeyonderStateProvider());
            }
            if (!event.getObject().getCapability(AbilityContainerProvider.CAPABILITY).isPresent()) {
                event.addCapability(new ResourceLocation(LotMMod.MODID, "abilities"), new AbilityContainerProvider());
            }
            if (!event.getObject().getCapability(SkillBarProvider.CAPABILITY).isPresent()) {
                event.addCapability(new ResourceLocation(LotMMod.MODID, "skill_bar"), new SkillBarProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(oldStore -> {
            event.getEntity().getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(newStore -> newStore.copyFrom(oldStore));
        });
        event.getOriginal().getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(oldStore -> {
            event.getEntity().getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(newStore -> newStore.copyFrom(oldStore));
        });
        event.getOriginal().getCapability(SkillBarProvider.CAPABILITY).ifPresent(oldStore -> {
            event.getEntity().getCapability(SkillBarProvider.CAPABILITY).ifPresent(newStore -> newStore.copyFrom(oldStore));
        });
    }

    // ==================== 战斗状态检测 ====================

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                if (state.isBeyonder()) state.enterCombat();
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide) {
            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                if (state.isBeyonder()) state.enterCombat();
            });
        }
    }

    // ==================== 数据同步与自动修复 ====================

    public static void syncAllData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new S2CSyncBeyonderDataPacket(state));
            });
            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(container -> {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new S2CSyncAbilityDataPacket(container));
            });
            player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new S2CSyncSkillBarPacket(bar));
            });
        }
    }

    /**
     * ★★★ 核心逻辑：校验并补发缺失技能 ★★★
     * 解决模组更新后，老存档缺少新技能的问题。
     */
    private static void validateAndRefreshSkills(ServerPlayer player) {
        player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
            if (!state.isBeyonder()) return;

            ResourceLocation pathwayId = state.getPathwayId();
            int sequence = state.getSequence();
            BeyonderPathway pathway = LotMPathways.get(pathwayId);

            if (pathway != null) {
                player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                    // 获取当前等级应该拥有的所有技能
                    List<ResourceLocation> expectedSkills = pathway.getAvailableSkills(sequence);
                    AtomicInteger addedCount = new AtomicInteger(0);

                    for (ResourceLocation skillId : expectedSkills) {
                        // 如果玩家没有这个技能，则补发
                        if (!abilities.hasAbility(skillId)) {
                            abilities.learnAbility(skillId);
                            addedCount.incrementAndGet();
                            LotMMod.LOGGER.info("Auto-learned missing skill {} for player {}", skillId, player.getName().getString());
                        }
                    }

                    // 如果有新技能补发，通知玩家（可选）
                    if (addedCount.get() > 0) {
                        player.sendSystemMessage(Component.translatable("message.lotm.skills_updated", addedCount.get()));
                    }
                });
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 1. 先执行自动修复逻辑
            validateAndRefreshSkills(serverPlayer);
            // 2. 再同步数据到客户端
            syncAllData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 重生时也检查一次，防止某些极端情况数据丢失
            validateAndRefreshSkills(serverPlayer);
            syncAllData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { syncAllData(event.getEntity()); }

    /**
     * 核心心跳逻辑
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;

        // 客户端：仅驱动冷却 (以及视觉上的战斗计时，暂不处理)
        if (player.level().isClientSide) {
            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(cap -> cap.tick(player));
            return;
        }

        // 服务端逻辑
        if (player instanceof ServerPlayer serverPlayer) {

            // 1. 能力系统心跳 (冷却、持续消耗、被动技能)
            player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
                abilities.tick(player);

                player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                    List<ResourceLocation> activeSkills = abilities.getActiveSkills();

                    if (!activeSkills.isEmpty()) {
                        boolean stateChanged = false;
                        List<ResourceLocation> toDeactivate = new ArrayList<>();

                        for (ResourceLocation id : activeSkills) {
                            AbstractSkill skill = LotMSkills.getSkill(id);
                            if (skill != null) {
                                int activeTime = abilities.getActiveTime(id);
                                if (!skill.onActiveTick(serverPlayer, state, activeTime)) {
                                    toDeactivate.add(id);
                                }
                            }
                        }

                        for (ResourceLocation id : toDeactivate) {
                            abilities.deactivateSkill(id);
                            AbstractSkill skill = LotMSkills.getSkill(id);
                            if (skill != null) {
                                skill.onDeactivate(serverPlayer);
                                serverPlayer.sendSystemMessage(Component.translatable("message.lotm.skill.auto_deactivated", skill.getDisplayName()));
                            }
                            stateChanged = true;
                        }

                        if (stateChanged) {
                            PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), serverPlayer);
                        }
                    }
                });

                if (player.tickCount % 20 == 0) {
                    PacketHandler.sendToPlayer(new S2CSyncAbilityDataPacket(abilities), serverPlayer);
                }
            });

            // 2. 非凡状态心跳 (灵性恢复、战斗计时)
            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                double oldVal = state.getCurrentSpirituality();
                state.tick(); // 执行自然恢复逻辑 (即使在创造模式也执行，方便测试恢复速度)

                if (Math.abs(state.getCurrentSpirituality() - oldVal) > 0.01) {
                    PacketHandler.sendToPlayer(new S2CSyncBeyonderDataPacket(state), serverPlayer);
                }
            });
        }
    }
}
