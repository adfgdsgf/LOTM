package com.lotm.lotm.content.skill;

import com.lotm.lotm.api.capability.IAbilityContainer;
import com.lotm.lotm.api.capability.IBeyonderState;
import com.lotm.lotm.util.LotMText;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

/**
 * 技能抽象基类 (升级版)
 * <p>
 * 职责：
 * 1. 定义所有技能的通用行为契约 (消耗、冷却、释放)。
 * 2. 提供 UI 显示所需的元数据接口 (维持消耗、使用条件)。
 * 3. 统一管理切换型技能 (Toggle) 的通用逻辑。
 * <p>
 * 新增特性：
 * - {@link #canBeDeactivated(Player)}: 控制技能是否可以被主动关闭。
 */
public abstract class AbstractSkill {
    private final ResourceLocation id;
    private final SkillCastType castType;

    public AbstractSkill(ResourceLocation id, SkillCastType castType) {
        this.id = id;
        this.castType = castType;
    }

    public ResourceLocation getId() { return id; }
    public ResourceLocation getRegistryName() { return id; }
    public Component getDisplayName() { return LotMText.skillName(id); }
    public SkillCastType getCastType() { return castType; }

    // ==================================================
    //                 核心抽象接口
    // ==================================================

    /**
     * 获取启动消耗 (瞬时扣除)
     * @param player 玩家实体
     * @param sequence 当前序列等级
     * @return 消耗的灵性数值
     */
    public abstract double getCost(Player player, int sequence);

    /**
     * 获取冷却时间 (Ticks)
     * <p>
     * 对于 Toggle 技能，这代表**关闭后的冷却时间**。
     */
    public abstract int getCooldown(Player player);

    /**
     * 执行技能的具体效果
     * @param context 包含组合键信息，用于变体技能判断
     */
    protected abstract void performEffect(ServerPlayer player, Level level, CastContext context);

    // ==================================================
    //                 UI 信息接口 (用于 Tooltip)
    // ==================================================

    /**
     * 获取维持消耗 (每 Tick)
     * <p>
     * 用于 UI 显示 (通常显示为 x/秒) 和服务端扣费逻辑。
     * 默认为 0。
     */
    public double getUpkeepCost(Player player, int sequence) {
        return 0.0;
    }

    /**
     * 获取使用条件描述列表
     * <p>
     * 用于 UI 显示。例如："需要: 灵摆", "需要: 纯露", "环境: 夜晚"。
     * 默认为空。
     */
    public List<Component> getUsageConditions(Player player, int sequence) {
        return Collections.emptyList();
    }

    // ==================================================
    //                 配置钩子 (Hooks)
    // ==================================================

    /**
     * 是否在操作时显示 Action Bar 反馈消息
     * 默认为 true。
     */
    protected boolean shouldShowFeedback() {
        return true;
    }

    /**
     * 检查技能是否允许被主动关闭
     * <p>
     * 用于实现“怪物途径灵感不可控”等机制。
     * 如果返回 false，UI 上的开关将锁定，且 C2S 包会拒绝关闭请求。
     * 默认返回 true。
     */
    public boolean canBeDeactivated(Player player) {
        return true;
    }

    // ==================================================
    //                 持续型技能逻辑
    // ==================================================

    /**
     * 持续激活时的每 Tick 逻辑 (仅服务端调用)
     * 适用于 TOGGLE 和 SUMMON_MAINTAIN 类型。
     *
     * @param ticksActive 已经激活了多少 Tick
     * @return 如果返回 false，则强制关闭该技能 (例如灵性不足)
     */
    public boolean onActiveTick(ServerPlayer player, IBeyonderState state, int ticksActive) {
        // ★★★ 创造模式逻辑：不消耗灵性，永远保持激活 ★★★
        if (player.isCreative()) return true;

        // 默认实现：调用 getUpkeepCost 进行扣费
        double costPerTick = getUpkeepCost(player, state.getSequence());

        if (costPerTick > 0) {
            if (!state.consumeSpirituality(costPerTick)) {
                return false; // 灵性不足，关闭
            }
        }
        return true;
    }

    /**
     * 技能被关闭/取消激活时触发
     * 子类可重写此方法以清理副作用 (如移除药水效果、清除召唤物、播放关闭音效)。
     */
    public void onDeactivate(ServerPlayer player) {
        // 默认空实现
    }

    // ==================================================
    //                 被动技能逻辑
    // ==================================================

    /**
     * 被动技能心跳逻辑
     * 仅当 castType 为 PASSIVE 且玩家已学习该技能时，每 Tick 调用。
     */
    public void onPassiveTick(Player player, IBeyonderState state) {
        // 默认空实现
    }

    // ==================================================
    //                 核心释放流程
    // ==================================================

    /**
     * 检查技能是否可以释放
     */
    public boolean canCast(Player player, IBeyonderState state, IAbilityContainer abilities) {
        // 被动技能不可主动释放
        if (castType == SkillCastType.PASSIVE) return false;

        // ★★★ 创造模式逻辑：无视所有限制 (冷却、灵性、非凡者状态) ★★★
        if (player.isCreative()) return true;

        if (!state.isBeyonder()) return false;

        if (abilities.isOnCooldown(id)) {
            if (!player.level().isClientSide && shouldShowFeedback()) {
                // ★★★ 优化：冷却提示发送到 Action Bar ★★★
                player.displayClientMessage(LotMText.MSG_COOLDOWN, true);
            }
            return false;
        }

        // 检查是否正在尝试关闭技能
        boolean isDeactivating = castType.isToggleOrMaintain() && abilities.isSkillActive(id);

        // ★★★ 核心检查：如果试图关闭一个不可关闭的技能 ★★★
        if (isDeactivating && !canBeDeactivated(player)) {
            return false;
        }

        // 如果不是关闭操作（即开启操作），则检查灵性是否足够
        if (!isDeactivating && state.getCurrentSpirituality() < getCost(player, state.getSequence())) {
            if (!player.level().isClientSide && shouldShowFeedback()) {
                // ★★★ 优化：灵性不足提示发送到 Action Bar ★★★
                player.displayClientMessage(LotMText.MSG_SPIRITUALITY_LOW, true);
            }
            return false;
        }

        return true;
    }

    /**
     * 释放入口 (Template Method 模式)
     */
    public void cast(ServerPlayer player, Level level, IBeyonderState state, IAbilityContainer abilities, CastContext context) {
        // 被动技能无法通过 cast 调用
        if (castType == SkillCastType.PASSIVE) return;

        // ============================================================
        // 分支 A: 切换型技能 (Toggle / Maintain)
        // ============================================================
        if (castType.isToggleOrMaintain()) {
            if (abilities.isSkillActive(id)) {
                // ----------------------------------------------------
                // 动作：关闭技能 (Deactivate)
                // ----------------------------------------------------

                // 双重检查，防止绕过 canCast 直接调用
                if (!player.isCreative() && !canBeDeactivated(player)) {
                    return;
                }

                abilities.deactivateSkill(id);
                this.onDeactivate(player); // 调用子类清理逻辑 (仅音效/特效)

                // ★★★ 核心逻辑：关闭时才应用完整的冷却时间 ★★★
                if (!player.isCreative()) {
                    abilities.setCooldown(id, getCooldown(player));
                }

                // 提示反馈
                if (shouldShowFeedback()) {
                    player.displayClientMessage(Component.translatable("message.lotm.skill.deactivated", getDisplayName()), true);
                }
                return; // 结束，不执行 performEffect
            } else {
                // ----------------------------------------------------
                // 动作：开启技能 (Activate)
                // ----------------------------------------------------
                abilities.activateSkill(id);

                // ★★★ 核心逻辑：开启时只给一个极短的冷却 (0.5秒)，防止手滑连点 ★★★
                if (!player.isCreative()) {
                    abilities.setCooldown(id, 10); // 10 ticks = 0.5s
                    // 扣除启动消耗
                    state.consumeSpirituality(getCost(player, state.getSequence()));
                }

                // 提示反馈
                if (shouldShowFeedback()) {
                    player.displayClientMessage(Component.translatable("message.lotm.skill.activated", getDisplayName()), true);
                }

                // 继续向下执行 performEffect (通常用于播放开启音效)
            }
        }

        // ============================================================
        // 通用逻辑：执行具体效果
        // ============================================================
        performEffect(player, level, context);

        // ============================================================
        // 分支 B: 瞬发型技能 (Instant / Charging / etc)
        // ============================================================
        // 只有非 Toggle 类才在这里扣费和计算冷却
        if (!castType.isToggleOrMaintain() && !player.isCreative()) {
            state.consumeSpirituality(getCost(player, state.getSequence()));
            abilities.setCooldown(id, getCooldown(player));
        }
    }
}
