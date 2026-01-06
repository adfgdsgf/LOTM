package com.lotm.lotm.api.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

/**
 * 占卜能力容器接口
 * <p>
 * 职责：
 * 1. 存储玩家在神秘学占卜领域的熟练度 (Mastery)。
 * 2. 熟练度越高，占卜成功率越高，反噬概率越低。
 */
@AutoRegisterCapability
public interface IDivinationContainer {

    // 最大熟练度
    int MAX_MASTERY = 1000;

    /**
     * 获取当前熟练度 (0 - 1000)
     */
    int getMastery();

    /**
     * 设置熟练度
     */
    void setMastery(int value);

    /**
     * 增加熟练度
     * @param amount 增加量
     */
    void addMastery(int amount);

    // --- 数据同步 ---
    void copyFrom(IDivinationContainer other);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
