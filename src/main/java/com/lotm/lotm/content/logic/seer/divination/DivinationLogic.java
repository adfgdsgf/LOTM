package com.lotm.lotm.content.logic.seer.divination;

import com.lotm.lotm.api.capability.IDivinationContainer;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.capability.DivinationContainerProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CDivinationResultPacket;
import com.lotm.lotm.common.network.packet.s2c.S2CSyncDivinationDataPacket;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.util.LotMText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;

public class DivinationLogic {

    public enum Result {
        SUCCESS,
        FAILURE,
        BACKFIRE,
        MISLEADING
    }

    /**
     * 检查玩家是否拥有占卜能力
     */
    public static boolean canDivinate(Player player) {
        return player.getCapability(AbilityContainerProvider.CAPABILITY)
                .map(abilities -> abilities.hasAbility(LotMSkills.DIVINATION.getId()))
                .orElse(false);
    }

    /**
     * 单次占卜入口 (右键使用)
     * 进行随机检定 (bypassRng = false)
     */
    public static void performDivination(ServerPlayer player, ItemStack stack, RandomSource random) {
        executeDivination(player, stack, random, false, false);
    }

    /**
     * 持续占卜入口 (每秒调用)
     * ★★★ 核心修正：bypassRng = true ★★★
     * 跳过随机检定，强制成功。只要目标存在且有蓝，就一直维持，不再“反复施法”导致随机失败。
     *
     * @return true 表示状态正常，继续维持；false 表示丢失目标，应停止。
     */
    public static boolean performContinuousDivination(ServerPlayer player, ItemStack stack, RandomSource random) {
        return executeDivination(player, stack, random, true, true);
    }

    /**
     * 统一执行逻辑
     * @param bypassRng 是否跳过随机检定 (持续施法时为 true)
     */
    private static boolean executeDivination(ServerPlayer player, ItemStack stack, RandomSource random, boolean silent, boolean bypassRng) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("DivinationTarget")) {
            if (!silent) {
                player.displayClientMessage(Component.translatable("message.lotm.divination.fail.no_target")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return false;
        }

        String targetIdStr = tag.getString("DivinationTarget");
        String typeStr = tag.getString("DivinationType");
        ResourceLocation targetId = new ResourceLocation(targetIdStr);

        // 使用 AtomicBoolean 从 lambda 内部传出结果
        AtomicBoolean shouldContinue = new AtomicBoolean(false);

        player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
            player.getCapability(DivinationContainerProvider.CAPABILITY).ifPresent(divination -> {

                Result result;
                if (bypassRng) {
                    // ★★★ 持续施法模式：直接成功，不再掷骰子 ★★★
                    result = Result.SUCCESS;
                } else {
                    // 单次模式：进行计算
                    result = calculateResult(state.getSequence(), divination.getMastery(), 30, random);
                }

                boolean outcome = handleResult(player, result, divination, targetId, typeStr, state.getSequence(), random, silent);
                shouldContinue.set(outcome);
            });
        });

        return shouldContinue.get();
    }

    private static Result calculateResult(int sequence, int mastery, int difficulty, RandomSource random) {
        float chance = 0.6f;
        chance += (9 - sequence) * 0.05f;
        chance += (mastery / 1000.0f) * 0.2f;
        chance -= (difficulty / 100.0f);

        float roll = random.nextFloat();

        if (roll < chance) {
            return Result.SUCCESS;
        } else if (roll < chance + 0.15f) {
            return Result.MISLEADING;
        } else if (roll < chance + 0.25f) {
            return Result.BACKFIRE;
        } else {
            return Result.FAILURE;
        }
    }

    /**
     * 处理结果
     * @return true=继续; false=停止
     */
    private static boolean handleResult(ServerPlayer player, Result result, IDivinationContainer divination,
                                        ResourceLocation targetId, String typeStr, int sequence, RandomSource random, boolean silent) {

        int baseRange = 32 + (9 - sequence) * 20;
        int rangeH = baseRange;
        int rangeV = Math.max(16, baseRange / 2);

        switch (result) {
            case SUCCESS -> {
                Vec3 targetPos = null;
                int targetEntityId = -1;
                ServerLevel level = player.serverLevel();

                if ("BLOCK".equals(typeStr)) {
                    BlockPos pos = DivinationSearcher.findNearestBlock(level, player.position(), targetId, rangeH, rangeV);
                    if (pos != null) targetPos = Vec3.atCenterOf(pos);
                } else if ("ENTITY".equals(typeStr)) {
                    Entity entity = DivinationSearcher.findNearestEntity(level, player.position(), targetId, rangeH * 2);
                    if (entity != null) {
                        targetPos = entity.position();
                        targetEntityId = entity.getId();
                    }
                }

                if (targetPos != null) {
                    // 成功且找到目标 -> 继续运行 (true)
                    Component msg = silent ? Component.empty() : getDirectionFeedback(player, targetPos);
                    PacketHandler.sendToPlayer(new S2CDivinationResultPacket(true, msg, targetPos, targetEntityId), player);

                    if (!silent || random.nextFloat() < 0.1f) {
                        grantXp(divination, player.getRandom());
                        PacketHandler.sendToPlayer(new S2CSyncDivinationDataPacket(divination), player);
                    }
                    return true;
                } else {
                    // ★★★ 占卜成功，但范围内没找到 -> 停止 (false) ★★★
                    Component msg = Component.translatable("message.lotm.divination.not_found").withStyle(ChatFormatting.YELLOW);
                    PacketHandler.sendToPlayer(new S2CDivinationResultPacket(false, msg, null, -1), player);
                    return false;
                }
            }
            case MISLEADING -> {
                double offsetX = (random.nextDouble() * 2 - 1) * rangeH;
                double offsetY = (random.nextDouble() * 2 - 1) * (rangeV / 2.0);
                double offsetZ = (random.nextDouble() * 2 - 1) * rangeH;
                Vec3 fakePos = player.position().add(offsetX, offsetY, offsetZ);

                Component msg = silent ? Component.empty() : getDirectionFeedback(player, fakePos);
                PacketHandler.sendToPlayer(new S2CDivinationResultPacket(true, msg, fakePos, -1), player);

                if (random.nextBoolean()) grantXp(divination, random);
                PacketHandler.sendToPlayer(new S2CSyncDivinationDataPacket(divination), player);
                return true;
            }
            case FAILURE -> {
                if (!silent) {
                    Component msg = Component.translatable("message.lotm.divination.fail.generic").withStyle(ChatFormatting.RED);
                    PacketHandler.sendToPlayer(new S2CDivinationResultPacket(false, msg, null, -1), player);
                }
                return false; // 失败停止
            }
            case BACKFIRE -> {
                if (!silent) {
                    Component msg = Component.translatable("message.lotm.divination.backfire").withStyle(ChatFormatting.DARK_PURPLE);
                    PacketHandler.sendToPlayer(new S2CDivinationResultPacket(false, msg, null, -1), player);
                }
                return false; // 反噬停止
            }
        }
        return false;
    }

    private static Component getDirectionFeedback(ServerPlayer player, Vec3 targetPos) {
        Vec3 playerPos = player.position();
        Vec3 dir = targetPos.subtract(playerPos).normalize();
        float playerYawRad = player.getYRot() * Mth.DEG_TO_RAD;

        double dx = dir.x;
        double dz = dir.z;
        double localX = dx * Math.cos(-playerYawRad) - dz * Math.sin(-playerYawRad);
        double localZ = dx * Math.sin(-playerYawRad) + dz * Math.cos(-playerYawRad);

        MutableComponent hText = Component.empty();
        boolean hasH = false;

        if (localZ > 0.3) { hText.append(LotMText.DIR_FRONT); hasH = true; }
        else if (localZ < -0.3) { hText.append(LotMText.DIR_BACK); hasH = true; }

        if (localX > 0.3) { if (hasH) hText.append(" "); hText.append(LotMText.DIR_LEFT); }
        else if (localX < -0.3) { if (hasH) hText.append(" "); hText.append(LotMText.DIR_RIGHT); }

        MutableComponent vText = Component.empty();
        if (dir.y > 0.5) vText.append(LotMText.DIR_ABOVE);
        else if (dir.y < -0.5) vText.append(LotMText.DIR_BELOW);

        MutableComponent msg = Component.translatable("message.lotm.divination.result_prefix").withStyle(ChatFormatting.AQUA);
        msg.append(" ").append(hText.withStyle(ChatFormatting.GOLD));

        if (!vText.getSiblings().isEmpty() || !vText.getString().isEmpty()) {
            msg.append(" ").append(Component.literal("&").withStyle(ChatFormatting.GRAY)).append(" ");
            msg.append(vText.withStyle(ChatFormatting.GOLD));
        }
        return msg;
    }

    public static void grantXp(IDivinationContainer container, RandomSource random) {
        container.addMastery(1 + random.nextInt(3));
    }
}
