package com.lotm.lotm.content.logic.seer.divination;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 占卜搜索工具类
 */
public class DivinationSearcher {

    /**
     * 搜索最近的符合 ID 的方块
     */
    @Nullable
    public static BlockPos findNearestBlock(ServerLevel level, Vec3 center, ResourceLocation blockId, int rangeH, int rangeV) {
        Block targetBlock = ForgeRegistries.BLOCKS.getValue(blockId);
        if (targetBlock == null || targetBlock == net.minecraft.world.level.block.Blocks.AIR) {
            return null;
        }

        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        BlockPos nearestPos = null;
        double minDistanceSqr = Double.MAX_VALUE;

        for (int x = -rangeH; x <= rangeH; x++) {
            for (int y = -rangeV; y <= rangeV; y++) {
                for (int z = -rangeH; z <= rangeH; z++) {
                    mutablePos.setWithOffset(origin, x, y, z);

                    // 优化 1: 距离剪枝
                    if (nearestPos != null && origin.distSqr(mutablePos) >= minDistanceSqr) continue;

                    // ★★★ 优化 2: 防止加载未加载的区块 ★★★
                    // 工业级标准：严禁在主线程逻辑中意外触发区块加载
                    if (!level.hasChunkAt(mutablePos)) continue;

                    BlockState state = level.getBlockState(mutablePos);
                    if (state.is(targetBlock)) {
                        double distSqr = origin.distSqr(mutablePos);
                        if (distSqr < minDistanceSqr) {
                            minDistanceSqr = distSqr;
                            nearestPos = mutablePos.immutable();
                        }
                    }
                }
            }
        }
        return nearestPos;
    }

    /**
     * 搜索最近的符合 ID 的实体
     */
    @Nullable
    public static Entity findNearestEntity(ServerLevel level, Vec3 center, ResourceLocation entityId, int range) {
        EntityType<?> targetType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (targetType == null) {
            return null;
        }

        AABB searchArea = AABB.ofSize(center, range * 2, range * 2, range * 2);

        List<Entity> candidates = level.getEntities((Entity) null, searchArea, e -> {
            return e.getType() == targetType && e.isAlive();
        });

        Optional<Entity> nearest = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(center)));

        return nearest.orElse(null);
    }
}
