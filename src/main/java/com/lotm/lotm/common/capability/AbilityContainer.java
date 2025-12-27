package com.lotm.lotm.common.capability;

import com.lotm.lotm.api.capability.IAbilityContainer;
import com.lotm.lotm.common.capability.skillbar.ISkillBarContainer;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.SkillCastType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 能力容器实现类
 * <p>
 * 负责管理玩家已习得的技能、冷却时间、持续状态以及预设栏位。
 * 实现了被动技能的缓存与开关逻辑。
 */
public class AbilityContainer implements IAbilityContainer {

    public static final int MAX_PAGES = 5;

    // 已习得的技能列表
    private final List<ResourceLocation> learnedAbilities = new ArrayList<>();

    // 被动技能缓存 (不参与序列化，运行时构建，用于优化 Tick 性能)
    private final List<ResourceLocation> cachedPassiveSkills = new ArrayList<>();

    // 冷却时间映射 (Skill ID -> Ticks)
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();

    // 预设页映射 (Page Index -> (Slot Index -> Skill ID))
    private final Map<Integer, Map<Integer, ResourceLocation>> presets = new HashMap<>();
    private int activePage = 0;

    // 持续/激活状态技能集合 (包含 TOGGLE 开启状态 和 PASSIVE 开启状态)
    private final Set<ResourceLocation> activeSkills = new HashSet<>();

    // 技能激活时长映射 (Skill ID -> Ticks Active)
    private final Map<ResourceLocation, Integer> activeTimeMap = new HashMap<>();

    @Override
    public void learnAbility(ResourceLocation abilityId) {
        if (!learnedAbilities.contains(abilityId)) {
            learnedAbilities.add(abilityId);

            // 检查是否为被动技能并加入缓存
            checkAndCachePassive(abilityId);

            // ★★★ 自动激活逻辑 ★★★
            // 如果是被动技能，习得时默认设为开启状态 (Active)。
            // 这样玩家无需手动操作即可获得被动效果，同时也保留了手动关闭的权利。
            AbstractSkill skill = LotMSkills.getSkill(abilityId);
            if (skill != null && skill.getCastType() == SkillCastType.PASSIVE) {
                activateSkill(abilityId);
            }
        }
    }

    @Override
    public void forgetAbility(ResourceLocation abilityId) {
        learnedAbilities.remove(abilityId);
        cachedPassiveSkills.remove(abilityId); // 移除缓存
        cooldowns.remove(abilityId);
        deactivateSkill(abilityId); // 遗忘时必须关闭状态

        // 从所有预设页中移除该技能
        for (Map<Integer, ResourceLocation> page : presets.values()) {
            page.values().removeIf(id -> id.equals(abilityId));
        }
    }

    @Override
    public boolean hasAbility(ResourceLocation abilityId) {
        return learnedAbilities.contains(abilityId);
    }

    @Override
    public List<ResourceLocation> getLearnedAbilities() {
        return new ArrayList<>(learnedAbilities);
    }

    @Override
    public void setCooldown(ResourceLocation abilityId, int ticks) {
        if (ticks > 0) cooldowns.put(abilityId, ticks);
    }

    @Override
    public int getCooldown(ResourceLocation abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    @Override
    public boolean isOnCooldown(ResourceLocation abilityId) {
        return getCooldown(abilityId) > 0;
    }

    // --- 持续/激活状态管理 ---

    @Override
    public void activateSkill(ResourceLocation abilityId) {
        if (hasAbility(abilityId)) {
            activeSkills.add(abilityId);
            activeTimeMap.put(abilityId, 0);
        }
    }

    @Override
    public void deactivateSkill(ResourceLocation abilityId) {
        activeSkills.remove(abilityId);
        activeTimeMap.remove(abilityId);
    }

    @Override
    public boolean isSkillActive(ResourceLocation abilityId) {
        return activeSkills.contains(abilityId);
    }

    @Override
    public List<ResourceLocation> getActiveSkills() {
        return new ArrayList<>(activeSkills);
    }

    @Override
    public int getActiveTime(ResourceLocation abilityId) {
        return activeTimeMap.getOrDefault(abilityId, 0);
    }

    // --- 心跳逻辑 ---

    @Override
    public void tick(Player player) {
        // 1. 冷却逻辑
        if (!cooldowns.isEmpty()) {
            cooldowns.entrySet().removeIf(entry -> {
                int newVal = entry.getValue() - 1;
                if (newVal <= 0) return true;
                entry.setValue(newVal);
                return false;
            });
        }

        // 2. 持续技能计时增加
        for (ResourceLocation id : activeSkills) {
            activeTimeMap.merge(id, 1, Integer::sum);
        }

        // 3. 被动技能逻辑 (仅服务端执行，或者双端执行视需求而定)
        // 工业级优化：只遍历缓存列表，避免遍历所有技能
        if (!cachedPassiveSkills.isEmpty()) {
            // 获取非凡状态用于判断强度
            player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                for (ResourceLocation passiveId : cachedPassiveSkills) {
                    // ★★★ 开关检查 ★★★
                    // 只有当被动技能处于 Active 状态时才执行逻辑。
                    // 这允许玩家通过 UI 关闭被动技能（收敛灵性）。
                    if (isSkillActive(passiveId)) {
                        AbstractSkill skill = LotMSkills.getSkill(passiveId);
                        if (skill != null) {
                            skill.onPassiveTick(player, state);
                        }
                    }
                }
            });
        }
    }

    // --- 预设逻辑 ---

    @Override
    public void setPreset(int page, int slot, ResourceLocation abilityId) {
        if (page < 0 || page >= MAX_PAGES) return;
        if (slot < 0 || slot >= ISkillBarContainer.SLOT_COUNT) return;
        presets.computeIfAbsent(page, k -> new HashMap<>());
        if (abilityId == null) presets.get(page).remove(slot);
        else presets.get(page).put(slot, abilityId);
    }

    @Override
    public ResourceLocation getAbilityInSlot(int page, int slot) {
        return presets.getOrDefault(page, new HashMap<>()).get(slot);
    }

    @Override
    public int getActivePage() { return activePage; }

    @Override
    public void setActivePage(int page) {
        this.activePage = Math.max(0, Math.min(page, MAX_PAGES - 1));
    }

    // --- 同步与序列化 ---

    @Override
    public void copyFrom(IAbilityContainer other) {
        // 1. 复制已学技能
        this.learnedAbilities.clear();
        this.learnedAbilities.addAll(other.getLearnedAbilities());

        // 2. 重建被动缓存
        this.cachedPassiveSkills.clear();
        for (ResourceLocation id : this.learnedAbilities) {
            checkAndCachePassive(id);
        }

        // 3. 清除冷却 (重生/跨维度通常重置冷却，或者视设计而定，这里选择重置)
        this.cooldowns.clear();

        // 4. 处理激活状态的继承
        this.activeSkills.clear();
        this.activeTimeMap.clear();

        for (ResourceLocation id : other.getActiveSkills()) {
            AbstractSkill skill = LotMSkills.getSkill(id);
            if (skill != null) {
                // ★★★ 状态继承逻辑 ★★★
                // PASSIVE (被动): 继承状态。如果玩家之前关了灵感，重生后应该还是关着的。
                if (skill.getCastType() == SkillCastType.PASSIVE) {
                    this.activeSkills.add(id);
                    this.activeTimeMap.put(id, 0);
                }
                // TOGGLE (主动持续): 不继承。例如灵视，重生后应自动关闭，防止不知情下扣光灵性。
            }
        }

        // 5. 复制预设
        this.presets.clear();
        for (int i = 0; i < MAX_PAGES; i++) {
            for (int j = 0; j < ISkillBarContainer.SLOT_COUNT; j++) {
                ResourceLocation skill = other.getAbilityInSlot(i, j);
                if (skill != null) this.setPreset(i, j, skill);
            }
        }
        this.activePage = other.getActivePage();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Learned
        ListTag learnedList = new ListTag();
        learnedAbilities.forEach(id -> learnedList.add(StringTag.valueOf(id.toString())));
        tag.put("learned", learnedList);

        // Cooldowns
        CompoundTag cdTag = new CompoundTag();
        cooldowns.forEach((k, v) -> cdTag.putInt(k.toString(), v));
        tag.put("cooldowns", cdTag);

        // Active Skills (持久化激活状态)
        ListTag activeList = new ListTag();
        activeSkills.forEach(id -> activeList.add(StringTag.valueOf(id.toString())));
        tag.put("activeSkills", activeList);

        // Presets
        CompoundTag presetsTag = new CompoundTag();
        presets.forEach((page, slots) -> {
            if (!slots.isEmpty()) {
                CompoundTag pageTag = new CompoundTag();
                slots.forEach((slot, skillId) -> pageTag.putString(String.valueOf(slot), skillId.toString()));
                presetsTag.put(String.valueOf(page), pageTag);
            }
        });
        tag.put("presets", presetsTag);

        tag.putInt("activePage", activePage);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        learnedAbilities.clear();
        cachedPassiveSkills.clear(); // 清空缓存

        if (nbt.contains("learned")) {
            ListTag list = nbt.getList("learned", Tag.TAG_STRING);
            list.forEach(t -> {
                try {
                    ResourceLocation id = new ResourceLocation(t.getAsString());
                    learnedAbilities.add(id);
                    checkAndCachePassive(id); // 重建缓存
                } catch (Exception ignored) {}
            });
        }

        cooldowns.clear();
        if (nbt.contains("cooldowns")) {
            CompoundTag cdTag = nbt.getCompound("cooldowns");
            cdTag.getAllKeys().forEach(k -> cooldowns.put(new ResourceLocation(k), cdTag.getInt(k)));
        }

        activeSkills.clear();
        activeTimeMap.clear();
        if (nbt.contains("activeSkills")) {
            ListTag list = nbt.getList("activeSkills", Tag.TAG_STRING);
            list.forEach(t -> {
                try {
                    ResourceLocation id = new ResourceLocation(t.getAsString());
                    activeSkills.add(id);
                    activeTimeMap.put(id, 0);
                } catch (Exception ignored) {}
            });
        }

        presets.clear();
        if (nbt.contains("presets")) {
            CompoundTag presetsTag = nbt.getCompound("presets");
            presetsTag.getAllKeys().forEach(pageKey -> {
                try {
                    int page = Integer.parseInt(pageKey);
                    CompoundTag pageTag = presetsTag.getCompound(pageKey);
                    pageTag.getAllKeys().forEach(slotKey -> {
                        int slot = Integer.parseInt(slotKey);
                        setPreset(page, slot, new ResourceLocation(pageTag.getString(slotKey)));
                    });
                } catch (Exception ignored) {}
            });
        }

        this.activePage = nbt.getInt("activePage");
    }

    /**
     * 辅助方法：检查技能是否为被动，如果是则加入缓存
     */
    private void checkAndCachePassive(ResourceLocation id) {
        AbstractSkill skill = LotMSkills.getSkill(id);
        if (skill != null && skill.getCastType() == SkillCastType.PASSIVE) {
            cachedPassiveSkills.add(id);
        }
    }
}
