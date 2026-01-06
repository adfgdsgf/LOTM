package com.lotm.lotm.client.renderer;

import com.lotm.lotm.client.util.VisualEffectHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端占卜高亮渲染器
 * <p>
 * 职责：
 * 1. 接收来自服务端的占卜结果。
 * 2. 渲染方块高亮框。
 * 3. 维护实体高亮状态，供 VisualEffectHelper 复用灵视 X-Ray 效果。
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientDivinationRenderer {

    private static BlockPos targetBlockPos = null;
    private static int targetEntityId = -1;
    private static int highlightTicks = 0;
    private static final int DEFAULT_HIGHLIGHT_TICKS = 60; // 默认 3 秒

    /**
     * 设置高亮目标 (默认时长)
     */
    public static void setHighlight(Vec3 pos, int entityId) {
        setHighlight(pos, entityId, DEFAULT_HIGHLIGHT_TICKS);
    }

    /**
     * 设置高亮目标 (自定义时长)
     * <p>
     * 修正：修复了 pos 为 null 时无法设置实体高亮的逻辑错误。
     */
    public static void setHighlight(Vec3 pos, int entityId, int duration) {
        // 只要 pos 有效 或者 entityId 有效，就执行设置
        if (pos != null || entityId != -1) {
            targetBlockPos = (pos != null) ? BlockPos.containing(pos) : null;
            targetEntityId = entityId;
            highlightTicks = duration;
        }
    }

    /**
     * 检查指定实体是否为当前的占卜目标
     * <p>
     * 供 VisualEffectHelper 调用，以复用灵视的 X-Ray 渲染逻辑。
     */
    public static boolean isDivinationTarget(int entityId) {
        return highlightTicks > 0 && targetEntityId != -1 && targetEntityId == entityId;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && highlightTicks > 0) {
            highlightTicks--;
            if (highlightTicks <= 0) {
                targetBlockPos = null;
                targetEntityId = -1;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 仅在半透明物体后渲染，确保透过水/玻璃可见
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (highlightTicks <= 0) return;

        // ★★★ 注意：实体高亮逻辑已移交 VisualEffectHelper (复用灵视 X-Ray) ★★★
        // 此处仅处理方块高亮。如果 targetEntityId 不为 -1，说明目标是实体，
        // 此时不应该渲染方块框，而是由 MixinLivingEntityRenderer -> VisualEffectHelper 渲染实体发光。
        if (targetBlockPos == null || targetEntityId != -1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        // 计算淡出效果 alpha
        // 逻辑：最后 20 ticks (1秒) 开始淡出。如果总时长很长，前面都保持 1.0。
        float alpha = (float) highlightTicks / 20.0f;
        if (alpha > 1.0f) alpha = 1.0f;

        // ★★★ 核心修改：使用自定义的透视线框 RenderType ★★★
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(LotMRenderTypes.DIVINATION_LINES);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // 渲染方块框 (调用 VisualEffectHelper 中的封装方法)
        VisualEffectHelper.renderDivinationBlockOutline(poseStack, consumer, targetBlockPos, alpha);

        poseStack.popPose();
    }
}
