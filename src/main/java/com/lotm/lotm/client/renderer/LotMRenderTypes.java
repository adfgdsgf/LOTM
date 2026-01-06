package com.lotm.lotm.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

/**
 * 模组自定义 RenderType 集合
 * <p>
 * 包含：
 * 1. 实体透视 (SPIRIT_XRAY) - 用于 MixinLivingEntityRenderer
 * 2. 方块透视线框 (DIVINATION_LINES) - 用于 ClientDivinationRenderer
 */
public abstract class LotMRenderTypes extends RenderType {

    public LotMRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    // 备用纯白纹理
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("lotmmod", "textures/misc/white.png");

    // =================================================================
    //  1. 实体 X-Ray 渲染类型 (保留原有逻辑)
    // =================================================================

    /**
     * 动态获取灵视 X-Ray RenderType
     * 关键参数：
     * 1. RENDERTYPE_EYES_SHADER: 支持发光和透明度
     * 2. TRANSLUCENT_TRANSPARENCY: 开启半透明混合
     * 3. NO_LIGHTMAP: 忽略环境光 (自带发光)
     * 4. GL_ALWAYS: ★★★ 核心 ★★★ 深度测试永远通过，实现透视效果
     */
    public static RenderType getSpiritXRay(ResourceLocation texture) {
        return create(
                "lotm_spirit_xray_textured",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_EYES_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        // 保持默认剔除 (CullFace)，防止看到模型内部
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setDepthTestState(new DepthTestStateShard("always", GL11.GL_ALWAYS)) // 透视的关键
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }

    // =================================================================
    //  2. 方块透视线框渲染类型 (新增)
    // =================================================================

    /**
     * 占卜方块高亮线框
     * <p>
     * 特性：
     * 1. NO_DEPTH_TEST: 关闭深度测试，实现穿墙透视。
     * 2. LINES: 线框模式。
     * 3. LineWidth 2.5: 加粗线条，使其更明显。
     * 4. VIEW_OFFSET_Z_LAYERING: 防止与方块表面重叠时的 Z-fighting 闪烁。
     */
    public static final RenderType DIVINATION_LINES = create(
            "lotm_divination_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.5D))) // 线宽 2.5
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING) // 防止 Z-fighting
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST) // ★★★ 核心：关闭深度测试，实现穿墙透视 ★★★
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );
}
