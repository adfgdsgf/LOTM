package com.lotm.lotm.common.network;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.network.packet.c2s.*;
import com.lotm.lotm.common.network.packet.s2c.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络包处理器 (Packet Handler)
 * <p>
 * 负责注册和分发所有的网络消息。
 * 遵循协议版本控制，确保客户端和服务端版本一致。
 */
public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LotMMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        // ==================== 客户端 -> 服务端 (C2S) ====================

        // 1. 释放技能请求
        CHANNEL.messageBuilder(C2SCastSkillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SCastSkillPacket::new)
                .encoder(C2SCastSkillPacket::encode)
                .consumerMainThread(C2SCastSkillPacket::handle)
                .add();

        // 2. 更新技能栏配置 (拖拽技能)
        CHANNEL.messageBuilder(C2SUpdateSkillBarPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SUpdateSkillBarPacket::new)
                .encoder(C2SUpdateSkillBarPacket::encode)
                .consumerMainThread(C2SUpdateSkillBarPacket::handle)
                .add();

        // 3. 切换预设页请求
        CHANNEL.messageBuilder(C2SSwitchPresetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SSwitchPresetPacket::new)
                .encoder(C2SSwitchPresetPacket::encode)
                .consumerMainThread(C2SSwitchPresetPacket::handle)
                .add();

        // 4. 切换技能状态 (UI开关用)
        CHANNEL.messageBuilder(C2SToggleSkillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SToggleSkillPacket::new)
                .encoder(C2SToggleSkillPacket::encode)
                .consumerMainThread(C2SToggleSkillPacket::handle)
                .add();

        // 5. 占卜请求包
        CHANNEL.messageBuilder(C2SRequestDivinationPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SRequestDivinationPacket::new)
                .encoder(C2SRequestDivinationPacket::encode)
                .consumerMainThread(C2SRequestDivinationPacket::handle)
                .add();

        // ==================== 服务端 -> 客户端 (S2C) ====================

        // 5. 同步非凡者属性 (灵性, 序列等)
        CHANNEL.messageBuilder(S2CSyncBeyonderDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CSyncBeyonderDataPacket::new)
                .encoder(S2CSyncBeyonderDataPacket::encode)
                .consumerMainThread(S2CSyncBeyonderDataPacket::handle)
                .add();

        // 6. 同步技能栏数据 (HUD渲染用)
        CHANNEL.messageBuilder(S2CSyncSkillBarPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CSyncSkillBarPacket::new)
                .encoder(S2CSyncSkillBarPacket::encode)
                .consumerMainThread(S2CSyncSkillBarPacket::handle)
                .add();

        // 7. 同步已习得能力列表 (GUI显示用)
        CHANNEL.messageBuilder(S2CSyncAbilityDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CSyncAbilityDataPacket::new)
                .encoder(S2CSyncAbilityDataPacket::encode)
                .consumerMainThread(S2CSyncAbilityDataPacket::handle)
                .add();

        // 8. 灵性直觉预警
        CHANNEL.messageBuilder(S2CIntuitionAlertPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CIntuitionAlertPacket::new)
                .encoder(S2CIntuitionAlertPacket::encode)
                .consumerMainThread(S2CIntuitionAlertPacket::handle)
                .add();

        // 9. 幻觉数据包
        CHANNEL.messageBuilder(S2CHallucinationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CHallucinationPacket::new)
                .encoder(S2CHallucinationPacket::encode)
                .consumerMainThread(S2CHallucinationPacket::handle)
                .add();

        // 10. 占卜数据同步
        CHANNEL.messageBuilder(S2CSyncDivinationDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CSyncDivinationDataPacket::new)
                .encoder(S2CSyncDivinationDataPacket::encode)
                .consumerMainThread(S2CSyncDivinationDataPacket::handle)
                .add();

        // 11. 占卜结果包
        CHANNEL.messageBuilder(S2CDivinationResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CDivinationResultPacket::new)
                .encoder(S2CDivinationResultPacket::encode)
                .consumerMainThread(S2CDivinationResultPacket::handle)
                .add();

        // 12. 高亮设置包
        CHANNEL.messageBuilder(S2CSetDivinationHighlightPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CSetDivinationHighlightPacket::new)
                .encoder(S2CSetDivinationHighlightPacket::encode)
                .consumerMainThread(S2CSetDivinationHighlightPacket::handle)
                .add();
    }

    /**
     * 发送数据包给指定玩家 (服务端 -> 客户端)
     * <p>
     * 用于状态同步、更新通知等。
     * @param msg 数据包实例
     * @param player 目标玩家
     */
    public static void sendToPlayer(Object msg, ServerPlayer player) {
        if (player != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
        }
    }

    /**
     * 发送数据包给所有玩家 (广播)
     */
    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
