package com.lotm.lotm.client.renderer.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能渲染器注册表 (客户端单例)
 */
public class SkillRendererRegistry {
    private static final Map<ResourceLocation, ISkillRenderer> RENDERERS = new HashMap<>();

    public static void register(ResourceLocation skillId, ISkillRenderer renderer) {
        RENDERERS.put(skillId, renderer);
    }

    public static ISkillRenderer getRenderer(ResourceLocation skillId) {
        return RENDERERS.get(skillId);
    }
}
