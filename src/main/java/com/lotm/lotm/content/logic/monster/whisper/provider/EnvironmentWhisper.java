package com.lotm.lotm.content.logic.monster.whisper.provider;

import com.lotm.lotm.content.logic.monster.whisper.IWhisperProvider;
import com.lotm.lotm.content.logic.monster.whisper.WhisperManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * 环境呓语：高价值物品探测
 * <p>
 * 修正记录：
 * 1. 废弃了“单点随机抽样”的愚蠢逻辑。
 * 2. 改为“范围遍历扫描”，确保只要范围内有东西就一定能扫到。
 * 3. 增加了距离判断，优先报告最近的目标。
 */
public class EnvironmentWhisper implements IWhisperProvider {

    // 扫描半径 (水平 8 格，垂直 4 格)
    // 扫描体积约 17x9x17 = 2601 个方块，对于 5 秒一次的频率，性能消耗可忽略不计。
    private static final int H_RANGE = 8;
    private static final int V_RANGE = 8;

    @Override
    public Component tryGenerateWhisper(ServerPlayer player, int sequence) {
        Level level = player.level();
        BlockPos center = player.blockPosition();

        BlockPos foundPos = null;
        double minDistSq = Double.MAX_VALUE;
        boolean isContainer = false;

        // 遍历扫描
        for (int x = -H_RANGE; x <= H_RANGE; x++) {
            for (int y = -V_RANGE; y <= V_RANGE; y++) {
                for (int z = -H_RANGE; z <= H_RANGE; z++) {
                    // 忽略玩家脚下极近距离的方块 (避免一直报脚下的矿)
                    if (Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1) continue;

                    BlockPos pos = center.offset(x, y, z);
                    double distSq = center.distSqr(pos);

                    // 优化：如果已经找到了一个目标，且当前方块比已找到的目标更远，则直接跳过详细检查
                    // 我们只关心最近的那个
                    if (distSq >= minDistSq) continue;

                    BlockState state = level.getBlockState(pos);

                    // 1. 检查容器 (优先级略高，或者视为同等)
                    if (state.hasBlockEntity()) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                            minDistSq = distSq;
                            foundPos = pos;
                            isContainer = true;
                            continue; // 找到后继续循环，看有没有更近的
                        }
                    }

                    // 2. 检查矿物
                    if (state.is(Tags.Blocks.ORES)) {
                        minDistSq = distSq;
                        foundPos = pos;
                        isContainer = false;
                    }
                }
            }
        }

        // 如果扫描到了目标，生成提示文本
        if (foundPos != null) {
            Component dir = WhisperManager.getFuzzyDirection(player, foundPos.getCenter());
            if (isContainer) {
                return Component.translatable("message.lotm.whisper.treasure.chest", dir);
            } else {
                return Component.translatable("message.lotm.whisper.treasure.ore", dir);
            }
        }

        return null;
    }
}
