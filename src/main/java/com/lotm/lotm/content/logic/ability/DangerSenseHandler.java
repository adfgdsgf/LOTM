package com.lotm.lotm.content.logic.ability;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.s2c.S2CSetDivinationHighlightPacket;
import com.lotm.lotm.common.registry.LotMAttributes;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.util.PlayerGazeHelper;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 危险感知逻辑处理器 (Danger Sense Handler)
 * <p>
 * 核心职责：
 * 1. **视线感知 (Gaze Sensing)**:
 *    检测其他玩家是否正在注视拥有“危险感知”能力的非凡者。
 *    结合了物理视线检测、视锥角度判断、距离判定以及属性对抗。
 *
 * 2. **名字感知 (Name Sensing)**:
 *    检测高序列非凡者（半神及以上）的名字是否被提及。
 *    覆盖了公屏聊天 (Chat) 和私聊指令 (Command)。
 * <p>
 * 核心特性：
 * - **属性对抗 (Attribute Contest)**: 基于 {@link LotMAttributes} 的侦测与隐蔽属性动态计算发现延迟。
 * - **智能遮挡 (Smart Occlusion)**: 使用多点射线检测，防止“只露脚不报警”的情况。
 * - **防刷屏 (Anti-Spam)**: 引入 20 秒的 Action Bar 消息冷却机制。
 * - **沉浸式反馈 (Immersive Feedback)**: 警报信息发送至 Action Bar，避免污染聊天栏。
 *
 * @author LotM Dev Team
 */
@Mod.EventBusSubscriber(modid = LotMMod.MODID)
public class DangerSenseHandler {

    // 怪物途径 ID，用于特殊判定 (怪物途径拥有极高的灵性直觉)
    private static final ResourceLocation PATHWAY_MONSTER = new ResourceLocation(LotMMod.MODID, "monster");

    // 绝对隐蔽阈值 (Ticks)
    // 如果计算出的发现时间超过此值 (3.5秒)，则判定为无法发现，直接短路逻辑。
    private static final int ABSOLUTE_CONCEALMENT_THRESHOLD = 70;

    // 文本提示冷却时间 (Ticks)
    // 20秒内不再重复提示同一个人的注视/提及，防止 Action Bar 闪烁过快。
    private static final long ALERT_COOLDOWN_TICKS = 400L;

    // 怪物途径的感知倍率修正
    // 距离更远 (x2.0)，判定更宽 (x2.0)，反应更快 (x0.5)
    private static final double MONSTER_DISTANCE_MULTIPLIER = 1.2;
    private static final double MONSTER_BUFFER_MULTIPLIER = 1.2;
    private static final double MONSTER_DELAY_MULTIPLIER = 0.85;

    // 用于存储每个玩家被注视的状态追踪器
    // Key: "TargetUUID_ObserverUUID"
    private static final Map<String, PlayerGazeHelper.GazeTracker> TRACKERS = new HashMap<>();

    // 用于存储上次发送提示的时间戳
    // Key: "TargetUUID_ObserverUUID"
    private static final Map<String, Long> LAST_ALERT_TIMES = new HashMap<>();

    // 需要监听的通讯类指令集合 (支持原版常用私聊指令)
    // 这些指令通常包含目标玩家名，触发名字感知逻辑
    private static final Set<String> CHAT_COMMANDS = new HashSet<>(Arrays.asList(
            "msg", "tell", "w", "whisper", // 私聊
            "say", "me",                   // 公开动作
            "teammsg", "tm"                // 队伍聊天
    ));

    /**
     * 感知配置参数类 (Configuration Record)
     */
    public record SenseConfig(
            double hitBuffer,     // 碰撞箱膨胀范围 (米)。0.0=精准; 5.0=鼠标在周围5格内都算
            int baseTriggerTicks, // 基础触发所需 tick 数 (无属性修正时的延迟)
            double maxDistance    // 最大感知距离 (米)
    ) {}

    // ==================================================
    //                 核心逻辑：视线感知
    // ==================================================

    /**
     * 检查视线危险 (Check Gaze Danger)
     * <p>
     * 建议在 ServerTick 或 PlayerTick 中调用。
     *
     * @param observer 观察者 (看的人)
     * @param target   目标 (拥有危险感知的非凡者)
     */
    public static void checkGazeDanger(Player observer, ServerPlayer target) {
        if (observer == target) return;

        // 1. 智能遮挡检查 (Smart Occlusion Check)
        // 使用多点检测替代原版 hasLineOfSight。
        // 只要能看到目标的头、脚、中心任意一点，就算“可见”。
        if (!PlayerGazeHelper.canSeeAnyPart(observer, target)) return;

        // 2. 视锥检查 (View Cone Check)
        // 确保目标在观察者的屏幕范围内 (FOV 90度，距离 128米内粗筛)。
        if (!PlayerGazeHelper.isInViewCone(observer, target, 128.0, 90.0)) return;

        // 3. 检查目标是否拥有“灵性直觉”技能
        // 只有开启了该被动技能的玩家才能感知视线。
        boolean hasIntuition = target.getCapability(AbilityContainerProvider.CAPABILITY)
                .map(abilities -> abilities.hasAbility(LotMSkills.SPIRITUAL_INTUITION.getId()))
                .orElse(false);

        if (!hasIntuition) return;

        // 4. 获取目标的非凡能力状态
        target.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
            int sequence = state.getSequence();
            ResourceLocation pathwayId = state.getPathwayId();

            // 5. 获取基础配置 (基于途径和序列)
            SenseConfig config = getSensitivity(sequence, pathwayId);

            // 6. 计算动态延迟 (属性对抗)
            // 观察者隐蔽越高，发现越慢；目标侦测越高，发现越快。
            int finalTriggerTicks = calculateDynamicTriggerTicks(observer, target, config.baseTriggerTicks);

            // 绝对隐蔽判定：如果延迟太高，说明对方隐蔽极好，无法感知。
            if (finalTriggerTicks > ABSOLUTE_CONCEALMENT_THRESHOLD) {
                return;
            }

            // 7. 执行精准视线检测 (Ray Trace)
            // 判断准星是否精确瞄准了目标的碰撞箱 (考虑膨胀范围)。
            // 此时已经通过了遮挡预检，isAimingAt 内部会再次进行物理射线检测以确保准确性。
            boolean isLooking = PlayerGazeHelper.isAimingAt(observer, target, config.maxDistance, config.hitBuffer);

            // 8. 更新时间追踪器
            String key = target.getStringUUID() + "_" + observer.getStringUUID();
            PlayerGazeHelper.GazeTracker tracker = TRACKERS.computeIfAbsent(key, k -> new PlayerGazeHelper.GazeTracker());

            tracker.update(isLooking);

            // 9. 判定触发
            if (tracker.isTriggered(finalTriggerTicks)) {
                long gameTime = target.level().getGameTime();
                long lastAlert = LAST_ALERT_TIMES.getOrDefault(key, 0L);

                // A. 视觉高亮 (Visual Highlight) - 始终触发
                // 高序列 (Seq 4+) 才能看到高亮轮廓。
                if (sequence <= 4) {
                    int durationSeconds = switch (sequence) {
                        case 4 -> 20;
                        case 3 -> 40;
                        case 2 -> 60;
                        case 1 -> 80;
                        case 0 -> 120;
                        default -> 0;
                    };

                    if (durationSeconds > 0) {
                        PacketHandler.sendToPlayer(
                                new S2CSetDivinationHighlightPacket(observer.getId(), durationSeconds * 20),
                                target
                        );
                    }
                }

                // B. 文本提示 (Text Alert) - 带冷却
                // 发送到 Action Bar，防止刷屏。
                if (gameTime - lastAlert > ALERT_COOLDOWN_TICKS) {
                    triggerDangerAlert(target, observer, "gaze");
                    LAST_ALERT_TIMES.put(key, gameTime);
                }

                // 重置追踪器，准备下一次判定
                tracker.reset();
            }
        });
    }

    /**
     * 计算动态触发延迟
     * <p>
     * 公式：最终延迟 = 基础延迟 + (观察者隐蔽 - 目标侦测) * 系数
     *
     * @param observer 观察者
     * @param target 目标
     * @param baseTicks 基础延迟
     * @return 修正后的延迟 (最小为 0)
     */
    private static int calculateDynamicTriggerTicks(Player observer, Player target, int baseTicks) {
        // 获取属性值 (默认为 0.0)
        double obsConcealment = observer.getAttributeValue(LotMAttributes.SPIRITUAL_CONCEALMENT.get());
        double tgtDetection = target.getAttributeValue(LotMAttributes.SPIRITUAL_DETECTION.get());

        // 计算差值：正值表示观察者隐蔽高，负值表示目标侦测高
        double diff = obsConcealment - tgtDetection;

        // 系数：每 1 点差值影响 2 ticks (0.1秒)
        // 例如：观察者隐蔽比目标侦测高 10 点 -> 延迟增加 20 ticks (1秒)
        int modifier = (int) (diff * 2.0);

        int finalTicks = baseTicks + modifier;

        // 限制下限为 0 (瞬间感知)
        return Math.max(0, finalTicks);
    }

    /**
     * 根据序列等级和途径类型返回感知灵敏度配置
     * <p>
     * 优化：以普通途径为基准，怪物途径在此基础上应用倍率修正。
     */
    private static SenseConfig getSensitivity(int sequence, ResourceLocation pathwayId) {
        // 1. 获取基准配置 (普通途径标准)
        SenseConfig baseConfig = switch (sequence) {
            case 9 -> new SenseConfig(0.1, 40, 32.0);  // 占卜家：基础需注视2秒
            case 8 -> new SenseConfig(0.2, 35, 40.0);
            case 7 -> new SenseConfig(0.3, 30, 48.0);
            case 6 -> new SenseConfig(0.5, 25, 64.0);
            case 5 -> new SenseConfig(0.7, 20, 80.0);
            case 4 -> new SenseConfig(1.0, 15, 128.0);
            case 3 -> new SenseConfig(1.3, 10, 160.0);
            case 2 -> new SenseConfig(1.6, 5, 256.0);
            case 1 -> new SenseConfig(1.9, 2, 300.0);
            case 0 -> new SenseConfig(2.2, 0, 384.0);
            default -> new SenseConfig(0.0, 60, 16.0); // 兜底
        };

        // 2. 如果是怪物途径，应用倍率修正
        if (PATHWAY_MONSTER.equals(pathwayId)) {
            return new SenseConfig(
                    baseConfig.hitBuffer * MONSTER_BUFFER_MULTIPLIER,
                    (int) (baseConfig.baseTriggerTicks * MONSTER_DELAY_MULTIPLIER),
                    baseConfig.maxDistance * MONSTER_DISTANCE_MULTIPLIER
            );
        }

        return baseConfig;
    }

    /**
     * 触发警报
     * 发送至 Action Bar (经验条上方)，避免聊天栏刷屏。
     *
     * @param target 接收警报的玩家
     * @param source 来源玩家
     * @param type 警报类型 ("gaze" 或 "chat")
     */
    private static void triggerDangerAlert(ServerPlayer target, Player source, String type) {
        // 使用本地化 Key: message.lotmmod.danger_sense.gaze / chat
        target.displayClientMessage(
                Component.translatable(
                        "message.lotmmod.danger_sense." + type,
                        source.getDisplayName()
                ).withStyle(ChatFormatting.RED),
                true // true = Action Bar
        );
    }

    // ==================================================
    //                 核心逻辑：名字感知
    // ==================================================

    /**
     * 监听公屏聊天事件
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        ServerPlayer sender = event.getPlayer();
        checkNameMention(sender, message);
    }

    /**
     * 监听指令事件 (捕获私聊)
     */
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        // 1. 只有玩家发出的指令才触发感知 (命令方块/控制台不触发)
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer sender)) {
            return;
        }

        // 2. 解析指令结构
        ParseResults<CommandSourceStack> parse = event.getParseResults();
        List<ParsedCommandNode<CommandSourceStack>> nodes = parse.getContext().getNodes();

        if (nodes.isEmpty()) return;

        // 3. 获取指令名称 (例如 "msg")
        String commandName = nodes.get(0).getNode().getName();

        // 4. 检查是否为通讯类指令
        if (CHAT_COMMANDS.contains(commandName)) {
            // 5. 获取完整的指令输入字符串 (例如 "/msg Alice Let's kill Bob")
            // 直接检查整个字符串包含的名字，简单且覆盖面广
            String fullInput = parse.getReader().getString();
            checkNameMention(sender, fullInput);
        }
    }

    /**
     * 统一的名字提及检查逻辑
     *
     * @param sender 发言者
     * @param content 内容 (聊天内容或指令全名)
     */
    private static void checkNameMention(ServerPlayer sender, String content) {
        // 遍历所有在线玩家
        for (ServerPlayer target : sender.serverLevel().players()) {
            // 排除自己提到自己
            if (target == sender) continue;

            // 检查内容是否包含目标名字
            if (content.contains(target.getName().getString())) {
                target.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                    // 统一阈值：半神 (Seq 4) 以上才能感知名字被提及
                    if (state.getSequence() <= 4) {
                        // 同样应用冷却机制，防止连续刷屏
                        String key = target.getStringUUID() + "_" + sender.getStringUUID();
                        long gameTime = target.level().getGameTime();
                        long lastAlert = LAST_ALERT_TIMES.getOrDefault(key, 0L);

                        if (gameTime - lastAlert > ALERT_COOLDOWN_TICKS) {
                            triggerDangerAlert(target, sender, "chat");
                            LAST_ALERT_TIMES.put(key, gameTime);
                        }
                    }
                });
            }
        }
    }

    // ==================================================
    //                 清理逻辑
    // ==================================================

    /**
     * 玩家登出时清理缓存
     * 防止内存泄漏和无效的追踪数据
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerUuid = event.getEntity().getStringUUID();

        // 清理该玩家相关的追踪器数据
        Iterator<Map.Entry<String, PlayerGazeHelper.GazeTracker>> it = TRACKERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PlayerGazeHelper.GazeTracker> entry = it.next();
            if (entry.getKey().contains(playerUuid)) {
                it.remove();
            }
        }

        // 清理该玩家相关的警报冷却数据
        Iterator<Map.Entry<String, Long>> itAlert = LAST_ALERT_TIMES.entrySet().iterator();
        while (itAlert.hasNext()) {
            Map.Entry<String, Long> entry = itAlert.next();
            if (entry.getKey().contains(playerUuid)) {
                itAlert.remove();
            }
        }
    }
}
