package com.lotm.lotm.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 玩家视线检测工具类 (增强版)
 * <p>
 * 职责：
 * 1. 几何视锥检测 (Broad Phase): 判断目标是否在视野范围内。
 * 2. 物理射线检测 (Narrow Phase): 判断准星是否精确指着目标的碰撞箱。
 * 3. 时间状态追踪 (State Tracker): 辅助判断“持续注视”逻辑。
 * 4. ★★★ 遮挡判断 (Occlusion): 提供比原版更智能的多点视线检查。
 */
public class PlayerGazeHelper {

    // ==================================================
    // 1. 精准射线检测 (Ray Tracing) - 推荐用于交互判定
    // ==================================================

    /**
     * 判断观察者的准星是否精确落在目标实体的碰撞箱上
     */
    public static boolean isAimingAt(LivingEntity observer, Entity target, double maxDistance, double buffer) {
        if (observer == null || target == null || observer == target) return false;

        Vec3 eyePos = observer.getEyePosition();
        Vec3 lookVec = observer.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

        // 获取目标的包围盒并根据 buffer 进行膨胀
        AABB targetBox = target.getBoundingBox().inflate(buffer);

        // 检测射线是否穿过该包围盒
        Optional<Vec3> hit = targetBox.clip(eyePos, endPos);

        if (hit.isPresent()) {
            // ★★★ 核心优化：此处不再调用原版 hasLineOfSight ★★★
            // 因为 hasLineOfSight 太严格。
            // 既然已经发生了碰撞，具体的遮挡逻辑应交由 clip 自身的物理检测或外部的前置检查处理。
            // 在 DangerSenseHandler 中，我们会先调用 canSeeAnyPart 确保没有完全遮挡。

            // 进一步的物理防穿墙检测：
            // 检查眼睛到碰撞点之间是否有方块阻挡
            Vec3 hitPos = hit.get();
            double distToHit = eyePos.distanceTo(hitPos);

            // 发射一条物理射线检测方块
            HitResult blockHit = observer.level().clip(new ClipContext(
                    eyePos, hitPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, observer));

            // 如果中间没有方块阻挡 (MISS) 或者 阻挡点比实体碰撞点更远，则视为击中
            if (blockHit.getType() == HitResult.Type.MISS) {
                return true;
            }

            double distToBlock = eyePos.distanceTo(blockHit.getLocation());
            return distToBlock >= distToHit;
        }
        return false;
    }

    // ==================================================
    // 2. 几何视锥检测 (View Cone) - 推荐用于范围触发
    // ==================================================

    public static boolean isInViewCone(LivingEntity observer, Entity target, double maxDistance, double fovDegrees) {
        if (observer == null || target == null) return false;
        double distSqr = observer.distanceToSqr(target);
        if (distSqr > maxDistance * maxDistance) return false;

        // 这里不进行遮挡检查，遮挡检查应独立调用

        Vec3 lookVec = observer.getViewVector(1.0F).normalize();
        Vec3 toTargetVec = target.getBoundingBox().getCenter().subtract(observer.getEyePosition()).normalize();

        double dot = lookVec.dot(toTargetVec);
        double threshold = Math.cos(Math.toRadians(fovDegrees));

        return dot > threshold;
    }

    // ==================================================
    // 3. 智能遮挡检测 (Smart Occlusion) - 新增
    // ==================================================

    /**
     * 判断观察者是否能看到目标的**任意**部位
     * <p>
     * 解决了原版 `hasLineOfSight` 只检测眼睛导致“露脚不报警”的问题。
     * 检测点包括：眼睛、中心、脚底、头顶。
     * 只要有一条线通畅，即视为可见。
     */
    public static boolean canSeeAnyPart(LivingEntity observer, Entity target) {
        if (observer.hasLineOfSight(target)) return true; // 原版检测通过直接返回

        Vec3 eyePos = observer.getEyePosition();
        AABB box = target.getBoundingBox();

        // 选取关键点进行检测
        Vec3[] pointsToCheck = new Vec3[]{
                box.getCenter(), // 中心
                new Vec3(box.minX + (box.maxX - box.minX) * 0.5, box.minY + 0.1, box.minZ + (box.maxZ - box.minZ) * 0.5), // 脚底 (抬高一点防地面干扰)
                new Vec3(box.minX + (box.maxX - box.minX) * 0.5, box.maxY - 0.1, box.minZ + (box.maxZ - box.minZ) * 0.5)  // 头顶 (降低一点防天花板干扰)
        };

        for (Vec3 point : pointsToCheck) {
            HitResult result = observer.level().clip(new ClipContext(
                    eyePos, point,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, observer));

            // 如果射线没有碰到方块 (MISS)，说明该点可见
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }

    // ==================================================
    // 4. 时间状态追踪器 (Time Tracker)
    // ==================================================

    public static class GazeTracker {
        private int gazeTicks = 0;
        private boolean isGazing = false;

        public void update(boolean currentlyGazing) {
            this.isGazing = currentlyGazing;
            if (currentlyGazing) {
                this.gazeTicks++;
            } else {
                this.gazeTicks = 0;
            }
        }

        public boolean isTriggered(int requiredTicks) {
            return isGazing && gazeTicks >= requiredTicks;
        }

        public float getProgress(int maxTicks) {
            return Math.min(1.0f, (float) gazeTicks / maxTicks);
        }

        public int getTicks() { return gazeTicks; }

        public void reset() {
            this.gazeTicks = 0;
            this.isGazing = false;
        }
    }
}
