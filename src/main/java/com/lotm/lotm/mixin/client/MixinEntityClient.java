package com.lotm.lotm.mixin.client;

import com.lotm.lotm.client.util.VisualEffectHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实体客户端逻辑 Mixin
 * 职责：欺骗 Minecraft 渲染引擎，让符合条件的实体“看起来”是发光的，并且指定发光颜色。
 */
@Mixin(Entity.class)
public abstract class MixinEntityClient {

    /**
     * 1. 强制开启发光状态
     * <p>
     * 必须拦截这个方法！
     * 如果返回 false，Minecraft 压根就不会启动描边渲染管线 (Glowing Shader)。
     */
    @Inject(method = "hasGlowingTag", at = @At("HEAD"), cancellable = true)
    private void injectHasGlowingTag(CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        // 只有符合灵视条件的生物，才强制视为发光
        if (self instanceof LivingEntity entity && VisualEffectHelper.shouldApplyXRay(entity)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 2. 强制指定描边颜色
     * <p>
     * 当上面那个方法触发了描边渲染后，Minecraft 会调用这个方法来决定描边是什么颜色。
     */
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void injectGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (self instanceof LivingEntity entity && VisualEffectHelper.shouldApplyXRay(entity)) {
            // 返回我们计算好的颜色 (红/黄/绿)
            cir.setReturnValue(VisualEffectHelper.getEntityColorInt(entity));
        }
    }
}
