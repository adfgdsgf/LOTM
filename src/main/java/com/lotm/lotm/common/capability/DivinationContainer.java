package com.lotm.lotm.common.capability;

import com.lotm.lotm.api.capability.IDivinationContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class DivinationContainer implements IDivinationContainer {

    private int mastery = 0;

    @Override
    public int getMastery() {
        return mastery;
    }

    @Override
    public void setMastery(int value) {
        this.mastery = Mth.clamp(value, 0, MAX_MASTERY);
    }

    @Override
    public void addMastery(int amount) {
        setMastery(this.mastery + amount);
    }

    @Override
    public void copyFrom(IDivinationContainer other) {
        this.mastery = other.getMastery();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mastery", mastery);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.mastery = nbt.getInt("Mastery");
    }
}
