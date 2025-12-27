package com.lotm.lotm.common.capability.skillbar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * 技能栏容器接口 (Interface for Skill Bar Container)
 * <p>
 * 定义了管理玩家快捷技能栏（Skill Bar）的标准操作。
 * 遵循高内聚原则，只负责槽位 ID 的映射管理，不包含技能的具体执行逻辑。
 *
 * @author LotM Dev Team
 */
public interface ISkillBarContainer extends INBTSerializable<CompoundTag> {

    // 默认 9 个技能槽位 (对应按键 1-9 或组合键 Z,X,C,V,B,N,M...)
    int SLOT_COUNT = 9;

    /**
     * 将技能设置到指定槽位
     *
     * @param slotIndex 槽位索引 (0 ~ SLOT_COUNT-1)
     * @param skillId 技能ID (传入 null 表示清除该槽位)
     */
    void setSkillInSlot(int slotIndex, @Nullable ResourceLocation skillId);

    /**
     * 获取指定槽位的技能ID
     *
     * @param slotIndex 槽位索引
     * @return 技能ID，如果槽位为空则返回 null
     */
    @Nullable
    ResourceLocation getSkillInSlot(int slotIndex);

    /**
     * 复制另一个容器的数据
     * <p>
     * 通常用于玩家死亡重生、从末地返回等维度切换场景，
     * 需要将旧玩家实体的数据迁移到新实体。
     *
     * @param other 旧的 Capability 实例
     */
    void copyFrom(ISkillBarContainer other);

    // ==================== 默认方法 (简化调用) ====================

    /**
     * 清除指定槽位
     */
    default void removeSkill(int slotIndex) {
        setSkillInSlot(slotIndex, null);
    }

    /**
     * 检查槽位是否为空
     */
    default boolean isEmpty(int slotIndex) {
        return getSkillInSlot(slotIndex) == null;
    }
}
