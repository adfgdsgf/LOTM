package com.lotm.lotm.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 模组自定义 RenderType 集合
 */
public abstract class LotMRenderTypes extends RenderType {

    public LotMRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    // 备用纯白纹理
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("lotmmod", "textures/misc/white.png");

    public static final RenderType SPIRIT_XRAY = create(
            "lotm_spirit_xray",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    // .setCullState(NO_CULL) // 已移除，使用默认的 CULL 以避免视觉混乱
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setDepthTestState(new DepthTestStateShard("always", GL11.GL_ALWAYS))
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    /**
     * 动态获取灵视 X-Ray RenderType (修正版：开启背面剔除)
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

                        // ★★★ 关键修正 ★★★
                        // 移除 setCullState(NO_CULL)。
                        // Minecraft 默认开启背面剔除 (CullFace)。
                        // 这样可以隐藏模型内部的结构（如后脑勺内壁），解决“背后看脸”的穿模问题。

                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setDepthTestState(new DepthTestStateShard("always", GL11.GL_ALWAYS))
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
    }
}
