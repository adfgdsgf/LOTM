package com.lotm.lotm.content.logic.monster.whisper.provider;

import com.lotm.lotm.content.logic.monster.whisper.IWhisperProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

/**
 * 剧情/风味呓语提供者
 * <p>
 * 修正记录：
 * 1. 移除了内部的概率锁。
 * 2. 现在只要被管理器选中，就必定返回文本，不再出现“判定成功却不说话”的脱裤子放屁行为。
 */
public class LoreWhisper implements IWhisperProvider {
    private static final Random RANDOM = new Random();

    @Override
    public Component tryGenerateWhisper(ServerPlayer player, int sequence) {
        // ★★★ 修正：移除 if (RANDOM.nextFloat() > 0.05f) ★★★
        // 既然外层逻辑已经决定要触发呓语，这里就应该直接生成，不要再进行二次拦截。

        int index = RANDOM.nextInt(5) + 1;
        return Component.translatable("message.lotm.whisper.lore." + index);
    }
}
