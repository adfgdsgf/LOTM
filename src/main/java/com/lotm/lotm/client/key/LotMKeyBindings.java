package com.lotm.lotm.client.key;

import com.lotm.lotm.LotMMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LotMMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LotMKeyBindings {

    public static final String CATEGORY = "key.categories.lotmmod";

    // ===== 技能栏按键 =====
    public static final List<KeyMapping> SKILL_SLOT_KEYS = new ArrayList<>();

    // ===== 技能修饰键 (变体释放) =====
    // 工业级设计：允许玩家改键，而不是硬编码检测 Shift/Ctrl
    public static KeyMapping SKILL_MODIFIER_1; // 对应 CastContext.isShiftDown (默认 Shift)
    public static KeyMapping SKILL_MODIFIER_2; // 对应 CastContext.isCtrlDown (默认 Ctrl)
    public static KeyMapping SKILL_MODIFIER_3; // 对应 CastContext.isAltDown (默认 Alt)

    // ===== 功能按键 =====
    public static KeyMapping OPEN_SKILL_CONFIG; // 打开配置菜单 (K)
    public static KeyMapping PREV_PRESET;       // 上一个预设 ([)
    public static KeyMapping NEXT_PRESET;       // 下一个预设 (])

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        // 1. 初始化技能槽位按键 (Z-N, G-J)
        int[] defaultKeys = {
                GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C,
                GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N,
                GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J
        };

        for (int i = 0; i < defaultKeys.length; i++) {
            KeyMapping key = new KeyMapping(
                    "key.lotmmod.skill_slot_" + (i + 1),
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    defaultKeys[i],
                    CATEGORY
            );
            SKILL_SLOT_KEYS.add(key);
            event.register(key);
        }

        // 2. 初始化技能修饰键
        SKILL_MODIFIER_1 = new KeyMapping(
                "key.lotmmod.skill_modifier_1",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                CATEGORY
        );
        event.register(SKILL_MODIFIER_1);

        SKILL_MODIFIER_2 = new KeyMapping(
                "key.lotmmod.skill_modifier_2",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                CATEGORY
        );
        event.register(SKILL_MODIFIER_2);

        SKILL_MODIFIER_3 = new KeyMapping(
                "key.lotmmod.skill_modifier_3",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                CATEGORY
        );
        event.register(SKILL_MODIFIER_3);

        // 3. 初始化功能按键
        OPEN_SKILL_CONFIG = new KeyMapping(
                "key.lotmmod.open_skill_config",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        );
        event.register(OPEN_SKILL_CONFIG);

        PREV_PRESET = new KeyMapping(
                "key.lotmmod.prev_preset",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET, // 默认 [
                CATEGORY
        );
        event.register(PREV_PRESET);

        NEXT_PRESET = new KeyMapping(
                "key.lotmmod.next_preset",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET, // 默认 ]
                CATEGORY
        );
        event.register(NEXT_PRESET);
    }
}
