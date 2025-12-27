package com.lotm.lotm.common.capability;

import com.lotm.lotm.api.capability.IBeyonderState;
import com.lotm.lotm.common.config.LotMCommonConfig;
import com.lotm.lotm.common.registry.LotMPathways;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class BeyonderState implements IBeyonderState {
    private boolean isBeyonder = false;
    private ResourceLocation pathwayId = new ResourceLocation("minecraft", "empty");
    private int sequence = 9;

    private double currentSpirituality = 0;
    private double cachedMaxSpirituality = 100;
    private boolean dirty = true;

    // 计时器
    private int regenTimer = 0;
    private int combatTimer = 0; // 战斗状态剩余 ticks (0 表示脱战)

    @Override
    public boolean isBeyonder() { return isBeyonder; }

    @Override
    public void setBeyonder(boolean isBeyonder) {
        this.isBeyonder = isBeyonder;
        markDirty();
    }

    @Override
    public ResourceLocation getPathwayId() { return pathwayId; }

    @Override
    public void setPathwayId(ResourceLocation id) {
        this.pathwayId = id == null ? new ResourceLocation("minecraft", "empty") : id;
        markDirty();
    }

    @Override
    public int getSequence() { return sequence; }

    @Override
    public void setSequence(int sequence) {
        this.sequence = Math.max(0, Math.min(9, sequence));
        markDirty();
    }

    @Override
    public double getCurrentSpirituality() { return currentSpirituality; }

    @Override
    public void setCurrentSpirituality(double value) {
        recalculateStatsIfNeeded();
        this.currentSpirituality = Math.max(0, Math.min(value, cachedMaxSpirituality));
    }

    @Override
    public double getMaxSpirituality() {
        recalculateStatsIfNeeded();
        return cachedMaxSpirituality;
    }

    @Override
    public boolean consumeSpirituality(double amount) {
        if (this.currentSpirituality >= amount) {
            this.currentSpirituality -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void recoverSpirituality(double amount) {
        recalculateStatsIfNeeded();
        setCurrentSpirituality(this.currentSpirituality + amount);
    }

    // --- 战斗状态 ---

    @Override
    public void enterCombat() {
        this.combatTimer = 200; // 10秒 * 20 tick/秒
    }

    @Override
    public boolean isInCombat() {
        return this.combatTimer > 0;
    }

    // --- 心跳逻辑 ---

    @Override
    public void tick() {
        if (!isBeyonder) return;

        recalculateStatsIfNeeded();

        // 1. 战斗状态计时
        if (combatTimer > 0) {
            combatTimer--;
        }

        // 2. 灵性自然恢复 (每秒执行一次)
        if (++regenTimer >= 20) {
            regenTimer = 0;
            if (currentSpirituality < cachedMaxSpirituality) {
                performRegeneration();
            }
        }
    }

    /**
     * 执行灵性恢复计算
     * 公式: (基础恢复 + 0.1%上限) * 战斗修正
     */
    private void performRegeneration() {
        // 获取配置的基础恢复 (默认1.0)
        double baseRegen = LotMCommonConfig.COMMON.baseSpiritualityRegen.get();

        // 计算百分比加成 (0.1% Max)
        double scalingRegen = cachedMaxSpirituality * 0.001;

        double totalRegen = baseRegen + scalingRegen;

        // 战斗状态修正 (10%)
        if (isInCombat()) {
            totalRegen *= 0.1;
        }

        recoverSpirituality(totalRegen);
    }

    private void markDirty() {
        this.dirty = true;
    }

    private void recalculateStatsIfNeeded() {
        if (dirty) {
            BeyonderPathway pathway = LotMPathways.get(pathwayId);
            if (pathway != null) {
                this.cachedMaxSpirituality = pathway.getTotalMaxSpirituality(sequence);
            } else {
                this.cachedMaxSpirituality = 100;
            }
            if (this.currentSpirituality > this.cachedMaxSpirituality) {
                this.currentSpirituality = this.cachedMaxSpirituality;
            }
            dirty = false;
        }
    }

    @Override
    public void copyFrom(IBeyonderState other) {
        this.isBeyonder = other.isBeyonder();
        this.pathwayId = other.getPathwayId();
        this.sequence = other.getSequence();
        this.currentSpirituality = other.getCurrentSpirituality();
        // 战斗状态通常不跨维度继承，或者可以选择继承，这里暂重置
        this.combatTimer = 0;
        markDirty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isBeyonder", isBeyonder);
        tag.putString("pathway", pathwayId.toString());
        tag.putInt("sequence", sequence);
        tag.putDouble("currentSpirituality", currentSpirituality);
        tag.putInt("combatTimer", combatTimer); // 保存战斗状态，防止重登脱战
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.isBeyonder = nbt.getBoolean("isBeyonder");
        this.pathwayId = new ResourceLocation(nbt.getString("pathway"));
        this.sequence = nbt.getInt("sequence");
        this.currentSpirituality = nbt.getDouble("currentSpirituality");
        this.combatTimer = nbt.getInt("combatTimer");
        markDirty();
    }
}
