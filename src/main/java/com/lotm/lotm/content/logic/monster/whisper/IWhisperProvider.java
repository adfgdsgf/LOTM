package com.lotm.lotm.content.logic.monster.whisper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * 呓语提供者接口
 * <p>
 * 策略模式：定义不同类型的呓语生成逻辑。
 * 实现类可以扫描周围环境、实体或根据剧情生成文本。
 */
public interface IWhisperProvider {
    /**
     * 尝试生成一条呓语
     * @param player 玩家
     * @param sequence 序列等级
     * @return 呓语文本，如果没有合适的内容则返回 null
     */
    @Nullable
    Component tryGenerateWhisper(ServerPlayer player, int sequence);
}
