package com.lotm.lotm.api.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IBeyonderState {
    // --- 基础属性 ---
    boolean isBeyonder();
    void setBeyonder(boolean isBeyonder);

    ResourceLocation getPathwayId();
    void setPathwayId(ResourceLocation id);

    int getSequence();
    void setSequence(int sequence); // 9 to 0

    // --- 灵性管理 ---
    double getCurrentSpirituality();
    void setCurrentSpirituality(double value);

    double getMaxSpirituality();
    // setMaxSpirituality 已废弃，由途径数据决定

    boolean consumeSpirituality(double amount);
    void recoverSpirituality(double amount);

    // --- 战斗状态管理 (新增) ---
    /**
     * 进入或刷新战斗状态
     * 将战斗计时器重置为指定秒数 (默认10秒)
     */
    void enterCombat();

    /**
     * 检查是否处于战斗状态
     */
    boolean isInCombat();

    // --- 心跳逻辑 ---
    /**
     * 执行每 Tick 逻辑
     * <p>
     * 修改说明：
     * 增加 Player 参数，以便在心跳逻辑中访问玩家的 Attribute 系统，
     * 从而动态更新灵性侦测和隐蔽属性。
     *
     * @param player 宿主玩家实体
     */
    void tick(Player player);

    // --- 数据同步 ---
    void copyFrom(IBeyonderState other);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
