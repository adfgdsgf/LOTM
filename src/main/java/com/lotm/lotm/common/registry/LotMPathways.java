package com.lotm.lotm.common.registry;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import com.lotm.lotm.content.pathway.impl.seer.SeerPathway;
import com.lotm.lotm.content.pathway.impl.monster.MonsterPathway;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 途径注册中心
 * <p>
 * 管理所有已注册的非凡途径。
 */
public class LotMPathways {

    private static final Map<ResourceLocation, BeyonderPathway> PATHWAYS = new HashMap<>();

    // 预定义途径实例
    public static final SeerPathway SEER = new SeerPathway();
    public static final MonsterPathway MONSTER = new MonsterPathway();

    public static void register() {
        registerPathway(SEER);
        registerPathway(MONSTER);
        // 未来在此处注册更多途径，如 SPECTATOR...
        LotMMod.LOGGER.info("Registered {} beyonder pathways.", PATHWAYS.size());
    }

    private static void registerPathway(BeyonderPathway pathway) {
        if (PATHWAYS.containsKey(pathway.getId())) {
            throw new IllegalStateException("Duplicate pathway ID: " + pathway.getId());
        }
        PATHWAYS.put(pathway.getId(), pathway);
    }

    @Nullable
    public static BeyonderPathway get(ResourceLocation id) {
        return PATHWAYS.get(id);
    }

    /**
     * 获取所有已注册途径的 ID 集合
     * <p>
     * 用于命令自动补全 (Tab Completion)。
     * 返回不可变集合，防止外部修改内部数据，保证安全性。
     */
    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(PATHWAYS.keySet());
    }
}
