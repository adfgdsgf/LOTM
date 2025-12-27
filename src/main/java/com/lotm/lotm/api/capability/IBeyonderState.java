package com.lotm.lotm.api.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
    void tick();

    // --- 数据同步 ---
    void copyFrom(IBeyonderState other);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
