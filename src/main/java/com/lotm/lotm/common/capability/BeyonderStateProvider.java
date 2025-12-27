package com.lotm.lotm.common.capability;

import com.lotm.lotm.api.capability.IBeyonderState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BeyonderStateProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IBeyonderState> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final IBeyonderState backend = new BeyonderState();
    private final LazyOptional<IBeyonderState> optional = LazyOptional.of(() -> backend);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return backend.serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { backend.deserializeNBT(nbt); }

    public void invalidate() { optional.invalidate(); }
}
