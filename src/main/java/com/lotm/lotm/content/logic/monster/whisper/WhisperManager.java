package com.lotm.lotm.content.logic.monster.whisper;

import com.lotm.lotm.content.logic.monster.whisper.provider.EnvironmentWhisper;
import com.lotm.lotm.content.logic.monster.whisper.provider.LoreWhisper;
import com.lotm.lotm.content.logic.monster.whisper.provider.ThreatWhisper;
import com.lotm.lotm.util.LotMText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 呓语管理器
 * <p>
 * 修正记录：
 * 1. 将 Provider 分为“功能性 (Useful)”和“氛围性 (Fluff)”两类。
 * 2. 提供了两个独立的方法分别调用，彻底解决了“高序列屏蔽副作用同时也屏蔽了有用信息”的逻辑漏洞。
 */
public class WhisperManager {

    // 功能性列表：威胁、环境 (这些是能力，必须生效)
    private static final List<IWhisperProvider> USEFUL_PROVIDERS = new ArrayList<>();
    // 氛围性列表：剧情、幻听 (这些是代价，高序列可豁免)
    private static final List<IWhisperProvider> FLUFF_PROVIDERS = new ArrayList<>();

    static {
        // 注册功能性提供者
        USEFUL_PROVIDERS.add(new ThreatWhisper());
        USEFUL_PROVIDERS.add(new EnvironmentWhisper());

        // 注册氛围性提供者
        FLUFF_PROVIDERS.add(new LoreWhisper());
    }

    /**
     * 尝试生成“有用”的信息
     * <p>
     * 逻辑：
     * 1. 优先级：威胁 > 环境。
     * 2. 不受任何概率锁限制，只要扫描到了就必须返回。
     */
    @Nullable
    public static Component generateUseful(ServerPlayer player, int sequence) {
        for (IWhisperProvider provider : USEFUL_PROVIDERS) {
            Component msg = provider.tryGenerateWhisper(player, sequence);
            if (msg != null) return msg;
        }
        return null;
    }

    /**
     * 尝试生成“氛围/废话”信息
     * <p>
     * 逻辑：
     * 1. 通常是随机剧情文本。
     * 2. 仅在没有有用信息且通过了失控概率判定时才调用。
     */
    @Nullable
    public static Component generateFluff(ServerPlayer player, int sequence) {
        for (IWhisperProvider provider : FLUFF_PROVIDERS) {
            Component msg = provider.tryGenerateWhisper(player, sequence);
            if (msg != null) return msg;
        }
        return null;
    }

    // ==================== 工具方法：模糊方向 ====================

    public static Component getFuzzyDirection(ServerPlayer player, Vec3 targetPos) {
        Vec3 playerLook = player.getLookAngle();
        Vec3 toTarget = targetPos.subtract(player.getEyePosition()).normalize();

        double dot = playerLook.x * toTarget.x + playerLook.z * toTarget.z;
        double det = playerLook.x * toTarget.z - playerLook.z * toTarget.x;
        double angle = Math.atan2(det, dot);
        double deg = Math.toDegrees(angle);

        if (deg >= -45 && deg <= 45) return LotMText.DIR_FRONT;
        if (deg > 45 && deg < 135) return LotMText.DIR_LEFT;
        if (deg < -45 && deg > -135) return LotMText.DIR_RIGHT;
        return LotMText.DIR_BACK;
    }
}
