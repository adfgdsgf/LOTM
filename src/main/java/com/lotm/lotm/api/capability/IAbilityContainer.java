package com.lotm.lotm.api.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.List;

/**
 * 能力容器接口
 * 定义了非凡者技能学习、冷却、持续状态管理的核心契约。
 */
@AutoRegisterCapability
public interface IAbilityContainer {
    // --- 技能学习 ---
    void learnAbility(ResourceLocation abilityId);
    void forgetAbility(ResourceLocation abilityId);
    boolean hasAbility(ResourceLocation abilityId);
    List<ResourceLocation> getLearnedAbilities();

    // --- 冷却系统 ---
    void setCooldown(ResourceLocation abilityId, int ticks);
    int getCooldown(ResourceLocation abilityId);
    boolean isOnCooldown(ResourceLocation abilityId);

    // --- 持续状态管理 (Toggle/Maintain) ---
    void activateSkill(ResourceLocation abilityId);
    void deactivateSkill(ResourceLocation abilityId);
    boolean isSkillActive(ResourceLocation abilityId);
    List<ResourceLocation> getActiveSkills();

    /**
     * 获取技能已激活的时间 (Ticks)
     * 用于计算持续消耗
     */
    int getActiveTime(ResourceLocation abilityId);

    // --- 心跳逻辑 ---
    /**
     * 执行每 Tick 逻辑
     * @param player 拥有该能力的玩家实体 (用于被动技能逻辑)
     */
    void tick(Player player);

    // --- 预设栏 ---
    void setPreset(int page, int slot, ResourceLocation abilityId);
    ResourceLocation getAbilityInSlot(int page, int slot);
    int getActivePage();
    void setActivePage(int page);

    // --- 数据同步 ---
    void copyFrom(IAbilityContainer other);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
