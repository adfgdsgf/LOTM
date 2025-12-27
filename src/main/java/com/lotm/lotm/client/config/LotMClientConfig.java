package com.lotm.lotm.client.config;

import com.lotm.lotm.client.renderer.HudPositionHelper.AnchorPoint;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 客户端配置 (Client Config)
 * <p>
 * 包含 HUD 位置、渲染效果等仅本地生效的设置。
 * 采用双语注释风格，生成的 toml 文件具备极高的可读性。
 */
public class LotMClientConfig {

    public static final LotMClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<LotMClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LotMClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    // ==================== 灵性 HUD 设置 ====================
    public final ForgeConfigSpec.BooleanValue enableSpiritualityHud;
    public final ForgeConfigSpec.EnumValue<AnchorPoint> spiritualityAnchor;
    public final ForgeConfigSpec.IntValue spiritualityOffsetX;
    public final ForgeConfigSpec.IntValue spiritualityOffsetY;

    // ==================== 技能栏 HUD 设置 ====================
    public final ForgeConfigSpec.BooleanValue enableSkillBarHud;
    public final ForgeConfigSpec.EnumValue<AnchorPoint> skillBarAnchor;
    public final ForgeConfigSpec.IntValue skillBarOffsetX;
    public final ForgeConfigSpec.IntValue skillBarOffsetY;
    public final ForgeConfigSpec.DoubleValue skillBarScale;       // 新增：缩放比例
    public final ForgeConfigSpec.BooleanValue skillBarHorizontal; // 新增：布局方向

    public LotMClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("HUD_Settings");

        // ---------------------------------------------------------
        // 1. 灵性条设置 (Spirituality Bar)
        // ---------------------------------------------------------
        builder.push("Spirituality_Bar");

        enableSpiritualityHud = builder
                .comment(" ",
                        "================================================================",
                        " [Enable Spirituality HUD]",
                        " Whether to display the spirituality bar on screen.",
                        "----------------------------------------------------------------",
                        " [启用灵性HUD]",
                        " 是否在屏幕上显示灵性条。",
                        "================================================================")
                .translation("config.lotmmod.client.hud_enabled")
                .define("EnableSpiritualityHUD", true);

        spiritualityAnchor = builder
                .comment(" [HUD Anchor Point] Reference point.", " [HUD 锚点] 位置参考点。")
                .translation("config.lotmmod.client.hud_anchor")
                .defineEnum("SpiritualityAnchor", AnchorPoint.BOTTOM_RIGHT);

        spiritualityOffsetX = builder
                .comment(" [Offset X] Horizontal offset from anchor.", " [X 偏移] 水平偏移量。")
                .translation("config.lotmmod.client.hud_offset_x")
                .defineInRange("SpiritualityOffsetX", -10, -10000, 10000);

        spiritualityOffsetY = builder
                .comment(" [Offset Y] Vertical offset from anchor.", " [Y 偏移] 垂直偏移量。")
                .translation("config.lotmmod.client.hud_offset_y")
                .defineInRange("SpiritualityOffsetY", -10, -10000, 10000);

        builder.pop();

        // ---------------------------------------------------------
        // 2. 技能栏设置 (Skill Bar)
        // ---------------------------------------------------------
        builder.push("Skill_Bar");

        enableSkillBarHud = builder
                .comment(" ",
                        "================================================================",
                        " [Enable Skill Bar HUD]",
                        " Whether to display the skill bar.",
                        "----------------------------------------------------------------",
                        " [启用技能栏HUD]",
                        " 是否显示技能快捷栏。",
                        "================================================================")
                .translation("config.lotmmod.client.skill_bar_enabled")
                .define("EnableSkillBarHUD", true);

        skillBarAnchor = builder
                .comment(" [Skill Bar Anchor] Reference point.", " [技能栏锚点] 位置参考点。")
                .translation("config.lotmmod.client.skill_bar_anchor")
                .defineEnum("SkillBarAnchor", AnchorPoint.BOTTOM_CENTER);

        skillBarOffsetX = builder
                .comment(" [Offset X] Horizontal offset from anchor.", " [X 偏移] 水平偏移量。")
                .translation("config.lotmmod.client.skill_bar_offset_x")
                .defineInRange("SkillBarOffsetX", 0, -10000, 10000);

        skillBarOffsetY = builder
                .comment(" [Offset Y] Vertical offset from anchor.", " [Y 偏移] 垂直偏移量。")
                .translation("config.lotmmod.client.skill_bar_offset_y")
                .defineInRange("SkillBarOffsetY", -40, -10000, 10000);

        skillBarScale = builder
                .comment(" [Scale] The scale of the skill bar (0.5 to 2.0).", " [缩放] 技能栏的缩放比例 (0.5 到 2.0)。")
                .translation("config.lotmmod.client.skill_bar_scale")
                .defineInRange("SkillBarScale", 1.0, 0.5, 2.0);

        skillBarHorizontal = builder
                .comment(" [Horizontal Layout] True for horizontal, False for vertical.", " [横向布局] True为横向，False为纵向。")
                .translation("config.lotmmod.client.skill_bar_horizontal")
                .define("SkillBarHorizontal", true);

        builder.pop();

        builder.pop(); // End HUD_Settings
    }
}
