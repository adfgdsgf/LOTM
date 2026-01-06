package com.lotm.lotm.content.skill.seer;

import com.lotm.lotm.LotMMod;
import com.lotm.lotm.content.skill.AbstractSkill;
import com.lotm.lotm.content.skill.CastContext;
import com.lotm.lotm.content.skill.SkillCastType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class Divination extends AbstractSkill {

    public static final ResourceLocation ID = new ResourceLocation(LotMMod.MODID, "divination");

    public Divination() {
        super(ID, SkillCastType.PASSIVE);
    }

    @Override
    public double getCost(Player player, int sequence) { return 0; }

    @Override
    public int getCooldown(Player player) { return 0; }

    @Override
    protected void performEffect(ServerPlayer player, Level level, CastContext context) {
        // 被动技能
    }

    // ★★★ 新增：在这里添加额外的说明文本 ★★★
    @Override
    public List<Component> getUsageConditions(Player player, int sequence) {
        return List.of(
                // 提示 1：需要媒介
                Component.translatable("gui.lotmmod.skill.condition.medium")
                        .withStyle(ChatFormatting.YELLOW),
                // 提示 2：使用时消耗灵性
                Component.translatable("gui.lotmmod.skill.condition.consume_on_use")
                        .withStyle(ChatFormatting.GRAY)
        );
    }
}
