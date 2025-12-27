package com.lotm.lotm;

import com.lotm.lotm.client.config.LotMClientConfig;
import com.lotm.lotm.common.config.LotMCommonConfig;
import com.lotm.lotm.common.network.PacketHandler;
// import com.lotm.lotm.common.registry.LotMEffects; // 移除导入
import com.lotm.lotm.common.registry.LotMRegistries;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LotMMod.MODID)
public class LotMMod {
    public static final String MODID = "lotmmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LotMMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ==========================================
        // 1. 注册配置文件
        // ==========================================
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LotMCommonConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LotMClientConfig.CLIENT_SPEC);

        // ==========================================
        // 2. 注册注册表 (Registries)
        // ==========================================

        // 统一通过 LotMRegistries 管理所有注册表
        LotMRegistries.register(modEventBus);

        // 已移除：LotMEffects.register(modEventBus);
        // 理由：已移动至 LotMRegistries 以保持高内聚。

        // ==========================================
        // 3. 生命周期
        // ==========================================
        modEventBus.addListener(this::commonSetup);

        // ==========================================
        // 4. 注册 Forge 事件
        // ==========================================
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }
}
