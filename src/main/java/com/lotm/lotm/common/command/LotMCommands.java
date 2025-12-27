package com.lotm.lotm.common.command;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.event.CommonCapabilityEvents;
import com.lotm.lotm.common.registry.LotMPathways;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模组指令注册类
 * <p>
 * 提供 /lotm init <pathway> [sequence] 指令。
 * 完全数据驱动，支持动态参数和自动补全。
 */
@Mod.EventBusSubscriber(modid = LotMMod.MODID)
public class LotMCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    /**
     * 自动补全提供者：列出所有已注册的途径 ID
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PATHWAYS = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(LotMPathways.getAllIds(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lotm")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限
                .then(Commands.literal("init")
                        // 参数 1: 途径 ID (支持 Tab 补全)
                        .then(Commands.argument("pathway", ResourceLocationArgument.id())
                                .suggests(SUGGEST_PATHWAYS) // 绑定补全逻辑

                                // 情况 A: 指定序列 /lotm init lotmmod:seer 8
                                .then(Commands.argument("sequence", IntegerArgumentType.integer(0, 9))
                                        .executes(context -> handleInit(
                                                context,
                                                ResourceLocationArgument.getId(context, "pathway"),
                                                IntegerArgumentType.getInteger(context, "sequence")
                                        ))
                                )

                                // 情况 B: 默认序列 /lotm init lotmmod:seer (默认序列 9)
                                .executes(context -> handleInit(
                                        context,
                                        ResourceLocationArgument.getId(context, "pathway"),
                                        9
                                ))
                        )
                )
        );
    }

    /**
     * 核心初始化逻辑
     * <p>
     * 统一处理初始化请求，解耦了参数解析与业务逻辑。
     *
     * @param context 指令上下文
     * @param targetPathwayId 目标途径 ID
     * @param targetSeq 目标序列等级
     */
    private static int handleInit(CommandContext<CommandSourceStack> context, ResourceLocation targetPathwayId, int targetSeq) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // 1. 验证途径是否存在 (拒绝假设，严谨检查)
        BeyonderPathway pathway = LotMPathways.get(targetPathwayId);
        if (pathway == null) {
            // 使用 Component.literal 反馈错误，或者使用翻译键
            context.getSource().sendFailure(Component.literal("§cError: Pathway not found: " + targetPathwayId));
            return 0;
        }

        // 2. 初始化非凡者状态 (BeyonderState)
        player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
            state.setBeyonder(true);
            state.setPathwayId(targetPathwayId);
            state.setSequence(targetSeq);

            // 重新计算并补满灵性 (依赖 BeyonderState 内部的自动计算逻辑)
            double maxSpirit = state.getMaxSpirituality();
            state.setCurrentSpirituality(maxSpirit);
        });

        // 3. 学习对应序列的所有技能 (AbilityContainer)
        player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(abilities -> {
            // 动态获取该序列及以下的所有技能
            List<ResourceLocation> skillsToLearn = pathway.getAvailableSkills(targetSeq);

            // 使用 AtomicInteger 解决 Lambda 变量作用域问题
            AtomicInteger learnCount = new AtomicInteger(0);

            for (ResourceLocation skillId : skillsToLearn) {
                // 防止重复学习
                if (!abilities.hasAbility(skillId)) {
                    abilities.learnAbility(skillId);
                    learnCount.incrementAndGet();
                }
            }

            // 发送详细反馈消息
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§a[LotM] Learned %d skills from %s.", learnCount.get(), targetPathwayId)), true);
        });

        // 4. 立即同步所有数据到客户端 (确保 UI 实时更新)
        CommonCapabilityEvents.syncAllData(player);

        // 发送最终成功消息
        context.getSource().sendSuccess(() -> Component.literal(
                String.format("§a[LotM] Successfully initialized as %s (Seq %d).", targetPathwayId.getPath(), targetSeq)), true);

        return 1;
    }
}
