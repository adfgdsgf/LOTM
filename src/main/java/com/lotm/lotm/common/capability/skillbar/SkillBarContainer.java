package com.lotm.lotm.common.capability.skillbar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * 技能栏容器实现类 (Implementation)
 * <p>
 * 严格遵循 ISkillBarContainer 接口定义。
 * 负责存储玩家快捷栏的技能 ID 数据，并处理 NBT 持久化。
 */
public class SkillBarContainer implements ISkillBarContainer {

    // 存储数组，大小由接口定义
    private final ResourceLocation[] slots = new ResourceLocation[SLOT_COUNT];

    @Override
    public void setSkillInSlot(int slotIndex, @Nullable ResourceLocation skillId) {
        if (isValidSlot(slotIndex)) {
            this.slots[slotIndex] = skillId;
        }
    }

    @Nullable
    @Override
    public ResourceLocation getSkillInSlot(int slotIndex) {
        if (isValidSlot(slotIndex)) {
            return this.slots[slotIndex];
        }
        return null;
    }

    /**
     * 从另一个容器复制数据
     * <p>
     * 修正：不再依赖已移除的 getAllSlots() 方法，而是通过标准接口遍历复制。
     * 这样即使 "other" 是不同的实现类也能正常工作。
     */
    @Override
    public void copyFrom(ISkillBarContainer other) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.slots[i] = other.getSkillInSlot(i);
        }
    }

    /**
     * 边界检查辅助方法
     */
    private boolean isValidSlot(int index) {
        return index >= 0 && index < SLOT_COUNT;
    }

    // ==================== NBT 序列化逻辑 ====================

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        for (int i = 0; i < SLOT_COUNT; i++) {
            ResourceLocation id = slots[i];
            // 如果为空，存储空字符串，避免存储 null 导致错误
            String idStr = (id == null) ? "" : id.toString();
            list.add(StringTag.valueOf(idStr));
        }

        tag.put("Slots", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Slots", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("Slots", Tag.TAG_STRING);

            // 确保读取长度不超过当前数组大小，防止越界
            int loopLength = Math.min(list.size(), SLOT_COUNT);

            for (int i = 0; i < loopLength; i++) {
                String idStr = list.getString(i);
                if (idStr.isEmpty()) {
                    slots[i] = null;
                } else {
                    // 增加异常捕获，防止因为脏数据（错误的ResourceLocation格式）导致崩服
                    try {
                        slots[i] = new ResourceLocation(idStr);
                    } catch (Exception e) {
                        slots[i] = null; // 格式错误则视为无技能
                        // 在开发环境可以打印日志，生产环境保持静默或从简
                    }
                }
            }

            // 如果 NBT 列表比数组短（例如扩容了槽位），剩余部分保持 null
            for (int i = loopLength; i < SLOT_COUNT; i++) {
                slots[i] = null;
            }
        }
    }
}
