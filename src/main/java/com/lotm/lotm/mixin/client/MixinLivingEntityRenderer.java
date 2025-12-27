package com.lotm.lotm.mixin.client;

import com.lotm.lotm.client.renderer.LotMRenderTypes;
import com.lotm.lotm.client.util.VisualEffectHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 实体渲染 Mixin (轻量化版)
 * <p>
 * 工业级规范：
 * Mixin 仅作为钩子 (Hook)，负责拦截和转发。
 * 具体的数学计算、逻辑判断全部委托给 {@link VisualEffectHelper}。
 * <p>
 * 职责：
 * 1. 拦截 getRenderType -> 切换 X-Ray 模式。
 * 2. 重定向 renderToBuffer -> 应用动态颜色。
 * 3. 重定向 layers 访问 -> 过滤杂物层。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {

    // Shadow 父类中的 protected 字段，以便直接访问
    @Shadow protected List<RenderLayer<T, M>> layers;

    protected MixinLivingEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    // 标记当前帧是否处于 X-Ray 渲染模式
    @Unique
    private boolean lotm$isXRayActive = false;

    // 缓存过滤后的渲染层列表 (避免每帧分配内存)
    @Unique
    private List<RenderLayer<T, M>> lotm$filteredLayers = null;

    /**
     * 1. 拦截 getRenderType
     * <p>
     * 目的：判断是否开启 X-Ray，如果开启则替换为透视 RenderType。
     */
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void injectGetRenderType(T entity, boolean pBodyVisible, boolean pTranslucent, boolean pGlowing, CallbackInfoReturnable<RenderType> cir) {
        // 逻辑委托：判断是否应用 X-Ray
        if (VisualEffectHelper.shouldApplyXRay(entity)) {
            ResourceLocation texture = this.getTextureLocation(entity);
            // 使用自定义的 X-Ray RenderType
            cir.setReturnValue(LotMRenderTypes.getSpiritXRay(texture));
            lotm$isXRayActive = true;
        } else {
            lotm$isXRayActive = false;
        }
    }

    /**
     * 2. 重定向 renderToBuffer (主体染色逻辑)
     * <p>
     * 目的：在 X-Ray 模式下，覆盖原始的纹理颜色，使用灵体颜色。
     */
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V")
    )
    private void redirectRenderToBuffer(
            M model,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red, float green, float blue, float alpha,
            T entity
    ) {
        if (lotm$isXRayActive) {
            // 逻辑委托：获取计算好的动态颜色 (含距离增强)
            Vector4f color = VisualEffectHelper.getDynamicXRayColor(entity);
            model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color.x, color.y, color.z, color.w);
        } else {
            // 正常渲染
            model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    /**
     * 3. 重定向 layers 访问 (层过滤逻辑)
     * <p>
     * 目的：在 X-Ray 模式下，隐藏盔甲、披风等杂物，只保留手持物品。
     */
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;layers:Ljava/util/List;")
    )
    private List<RenderLayer<T, M>> redirectLayersAccess(LivingEntityRenderer<T, M> instance) {
        // 如果不是 X-Ray 模式，直接返回原始列表
        if (!this.lotm$isXRayActive) {
            return this.layers;
        }

        // 简单的缓存机制保留在 Mixin 实例中 (因为缓存是实例相关的)
        if (lotm$filteredLayers == null) {
            // 逻辑委托：过滤层列表
            lotm$filteredLayers = VisualEffectHelper.filterLayers(this.layers);
        }

        return lotm$filteredLayers;
    }
}
