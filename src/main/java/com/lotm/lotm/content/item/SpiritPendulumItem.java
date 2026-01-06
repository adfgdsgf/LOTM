package com.lotm.lotm.content.item;

import com.lotm.lotm.client.gui.screen.DivinationSelectionScreen;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.content.logic.seer.divination.DivinationLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class SpiritPendulumItem extends Item {

    private static final int REQUIRED_DURATION = 40;

    public SpiritPendulumItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();

        // 1. 切换关闭
        if (tag.getBoolean("DivinationActive")) {
            if (!level.isClientSide) {
                tag.putBoolean("DivinationActive", false);
                player.sendSystemMessage(Component.translatable("message.lotm.divination.stop").withStyle(ChatFormatting.YELLOW));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 0.5f);
            }
            return InteractionResultHolder.success(stack);
        }

        // 2. 检查技能
        if (!DivinationLogic.canDivinate(player) && !player.isCreative()) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.lotm.divination.fail.no_skill").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResultHolder.fail(stack);
        }

        // 3. 检查是否有目标
        if (!tag.contains("DivinationTarget")) {
            if (level.isClientSide && !player.isCrouching()) {
                Minecraft.getInstance().setScreen(new DivinationSelectionScreen(hand));
            }
            return InteractionResultHolder.success(stack);
        }

        // 4. 蹲下时也打开 GUI
        if (player.isCrouching()) {
            if (level.isClientSide) {
                Minecraft.getInstance().setScreen(new DivinationSelectionScreen(hand));
            }
            return InteractionResultHolder.success(stack);
        }

        // 5. 开始蓄力
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (!(livingEntity instanceof Player player)) return;

        int duration = this.getUseDuration(stack);
        int ticksUsed = duration - count;

        // 蓄力完成
        if (ticksUsed >= REQUIRED_DURATION) {
            if (!level.isClientSide) {
                // 服务端：执行激活逻辑
                activateDivination(player, stack, level);
                // 停止使用
                player.stopUsingItem();
            } else {
                // 客户端：强制中断输入
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft.getInstance().options.keyUse.setDown(false);
                });
            }
            return;
        }

        // 蓄力音效
        if (ticksUsed % 10 == 0 && !level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5f, 0.5f + (ticksUsed / 40.0f));
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) return;

        // 1. 如果已经激活了，直接返回
        if (stack.hasTag() && stack.getTag().getBoolean("DivinationActive")) {
            return;
        }

        int duration = this.getUseDuration(stack) - timeCharged;

        // 2. 兜底激活
        if (duration >= REQUIRED_DURATION) {
            activateDivination(player, stack, level);
        }
        // 3. 时间不足，提示取消
        else if (!player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.lotm.divination.cancel"), true);
        }
    }

    private void activateDivination(Player player, ItemStack stack, Level level) {
        if (stack.getOrCreateTag().getBoolean("DivinationActive")) return;

        stack.getOrCreateTag().putBoolean("DivinationActive", true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        player.sendSystemMessage(Component.translatable("message.lotm.divination.start_continuous").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean("DivinationActive")) return;

        boolean isHolding = (player.getMainHandItem() == stack) || (player.getOffhandItem() == stack);
        if (!isHolding) {
            tag.putBoolean("DivinationActive", false);
            player.sendSystemMessage(Component.translatable("message.lotm.divination.stop_unequipped").withStyle(ChatFormatting.RED));
            return;
        }

        if (level.getGameTime() % 20 == 0) {
            boolean hasSpirit;
            if (player.isCreative()) {
                hasSpirit = true;
            } else {
                hasSpirit = player.getCapability(BeyonderStateProvider.CAPABILITY)
                        .map(state -> state.consumeSpirituality(1.0))
                        .orElse(false);
            }

            if (hasSpirit) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.2f, 2.0f);

                // ★★★ 核心逻辑：调用持续占卜，并根据返回值决定是否继续 ★★★
                // performContinuousDivination 内部已强制 bypassRng=true，不会因为随机数失败
                boolean keepRunning = DivinationLogic.performContinuousDivination(player, stack, level.random);

                if (!keepRunning) {
                    // 如果返回 false (说明找不到目标了)，则自动关闭
                    tag.putBoolean("DivinationActive", false);
                }
            } else {
                tag.putBoolean("DivinationActive", false);
                player.sendSystemMessage(Component.translatable("message.lotm.divination.fail.no_spirit_continuous").withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("DivinationActive");
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }
}
