package com.lotm.lotm.client.util;

import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端包处理器
 * 专门处理 S2C 数据包的逻辑，防止服务端加载客户端类导致崩溃。
 */
public class ClientPacketHandler {

    public static void handleSkillBarSync(CompoundTag nbt) {
        // 获取客户端玩家
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // 更新客户端 Capability 副本
        player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
            bar.deserializeNBT(nbt);
        });
    }
}
