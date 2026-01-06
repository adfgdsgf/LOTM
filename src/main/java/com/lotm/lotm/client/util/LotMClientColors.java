package com.lotm.lotm.client.util;

import net.minecraft.util.Mth;
import org.joml.Vector4f;

/**
 * LOTM 客户端统一颜色常量表 (Client Color Palette)
 * <p>
 * 工业级设计职责：
 * 1. 视觉规范中心 (Visual Identity)：统一管理模组内所有的颜色（GUI、HUD、实体渲染、特效）。
 * 2. 语义化命名 (Semantic Naming)：颜色名反映其用途，而非仅仅是颜色本身。
 * 3. 工具集成 (Utility Integration)：提供颜色混合、格式转换等常用方法。
 * <p>
 * 格式：ARGB (0xAARRGGBB) - Alpha, Red, Green, Blue
 */
public final class LotMClientColors {

    private LotMClientColors() {
        // 防止实例化工具类
    }

    // ========================================================================
    // 基础 UI 容器 (Basic UI Containers)
    // ========================================================================
    /** 容器背景色 (半透明黑) */
    public static final int CONTAINER_BG = 0x90000000;
    /** 容器边框色 (深灰) */
    public static final int CONTAINER_BORDER = 0xFF444444;
    /** 分割线颜色 */
    public static final int SEPARATOR_LINE = 0xFF666666;

    // ========================================================================
    // 列表项 (List Items)
    // ========================================================================
    /** 列表项默认背景 */
    public static final int LIST_ITEM_BG_NORMAL = 0xFF222222;
    /** 列表项悬停背景 */
    public static final int LIST_ITEM_BG_HOVER = 0xFF3A3A3A;
    /** 列表项选中背景 (暗金) */
    public static final int LIST_ITEM_BG_SELECTED = 0xFF555522;

    /** 列表项默认边框 */
    public static final int LIST_ITEM_BORDER_NORMAL = 0xFF666666;
    /** 列表项选中边框 (亮金) */
    public static final int LIST_ITEM_BORDER_SELECTED = 0xFFFFD700;

    /** 分组标题背景 (深蓝灰) */
    public static final int GROUP_HEADER_BG = 0xFF2C3E50;
    /** 分组标题悬停背景 */
    public static final int GROUP_HEADER_BG_HOVER = 0xFF34495E;
    /** 折叠箭头颜色 */
    public static final int GROUP_ARROW = 0xFFAAAAAA;

    // ========================================================================
    // 滚动条 (Scroll Bar)
    // ========================================================================
    /** 滚动槽背景 */
    public static final int SCROLL_TRACK = 0x40000000;
    /** 滚动滑块默认颜色 */
    public static final int SCROLL_THUMB_NORMAL = 0xFF666666;
    /** 滚动滑块悬停颜色 */
    public static final int SCROLL_THUMB_HOVER = 0xFFAAAAAA;

    // ========================================================================
    // 文本颜色 (Text Colors)
    // ========================================================================
    /** 标题文本 (淡黄) */
    public static final int TEXT_TITLE = 0xFFFFFFDD;
    /** 普通文本 (白) */
    public static final int TEXT_NORMAL = 0xFFFFFFFF;
    /** 高亮文本 (绿) */
    public static final int TEXT_HIGHLIGHT = 0xFF55FF55;
    /** 错误文本 (红) */
    public static final int TEXT_ERROR = 0xFFFF5555;
    /** 暗淡/提示文本 (灰) */
    public static final int TEXT_DIM = 0xFF888888;

    /** 技能类型文本 (浅绿) */
    public static final int TEXT_TYPE = 0xFF88FF88;
    /** 数值参数文本 (浅蓝) */
    public static final int TEXT_PARAM = 0xFF55FFFF;
    /** 描述文本 (银灰) */
    public static final int TEXT_DESC = 0xFFDDDDDD;

    // ========================================================================
    // 交互组件 (Interactive Widgets)
    // ========================================================================
    /** 开关激活状态 (现代绿) */
    public static final int BTN_TOGGLE_ACTIVE = 0xFF4CAF50;
    /** 开关关闭状态 (深灰) */
    public static final int BTN_TOGGLE_INACTIVE = 0xFF555555;
    /** 开关激活悬停 */
    public static final int BTN_TOGGLE_HOVER_ACTIVE = 0xFF66BB6A;
    /** 开关关闭悬停 */
    public static final int BTN_TOGGLE_HOVER_INACTIVE = 0xFF777777;
    /** 滑块指示器颜色 (白) */
    public static final int BTN_INDICATOR = 0xFFFFFFFF;
    /** 按钮边框颜色 */
    public static final int BTN_BORDER = 0xFF222222;

    // ========================================================================
    // 交互反馈 (Interaction Feedback)
    // ========================================================================
    /** 拖拽操作时的槽位高亮遮罩 (半透明白) */
    public static final int DRAG_HIGHLIGHT_MASK = 0x80FFFFFF;

    // ========================================================================
    // HUD 编辑器 (HUD Editor)
    // ========================================================================
    /** 辅助线颜色 */
    public static final int GUIDE_LINE = 0x40FFFFFF;
    /** 预览框背景 */
    public static final int HUD_PREVIEW_BG = 0x40000000;
    /** 预览框边框 */
    public static final int HUD_PREVIEW_BORDER = 0xFFFFFFFF;
    /** 拖拽中边框 (高亮绿) */
    public static final int HUD_DRAG_BORDER = 0xFF00FF00;
    /** 槽位占位符背景 */
    public static final int HUD_SLOT_PLACEHOLDER = 0x80555555;

    // ========================================================================
    // 技能栏 HUD (Skill Bar HUD)
    // ========================================================================
    /** 技能栏背景 */
    public static final int SKILL_BAR_BG = 0x80000000;
    /** 技能栏边框 */
    public static final int SKILL_BAR_BORDER = 0xFF444444;
    /** 冷却时间文本 */
    public static final int SKILL_COOLDOWN_TEXT = 0xFFFF5555;
    /** 按键绑定提示文本 */
    public static final int SKILL_KEYBIND_TEXT = 0xFFFFFFAA;
    /** 冷却遮罩层 (半透明黑) */
    public static final int SKILL_COOLDOWN_OVERLAY = 0x80000000;

    /** 技能槽边框 (默认) */
    public static final int SKILL_BORDER_NORMAL = 0xFF888888;
    /** 技能槽边框 (激活/悬停) */
    public static final int SKILL_BORDER_ACTIVE = 0xFF00FF00;
    /** 技能槽背景 (默认) */
    public static final int SKILL_SLOT_BG_NORMAL = 0xFF333333;
    /** 技能槽背景 (悬停) */
    public static final int SKILL_SLOT_BG_HOVER = 0xFF555555;

    /** 技能激活状态遮罩 (半透明绿) - 用于 Toggle 技能开启时的持续高亮 */
    public static final int SKILL_ACTIVE_OVERLAY = 0x4055FF55;
    /** 技能激活状态边框 (亮绿) - 用于 Toggle 技能开启时的持续高亮 */
    public static final int SKILL_ACTIVE_BORDER = 0xFF55FF55;

    /** 复制/记录槽位背景 (神秘紫) */
    public static final int COPY_SLOT_BG = 0xFF332244;
    /** 复制/记录槽位边框 (亮紫) */
    public static final int COPY_SLOT_BORDER = 0xFFAA55FF;

    // ========================================================================
    // 灵性条 (Spirituality HUD)
    // ========================================================================
    public static final int SPIRITUALITY_BAR_BG = 0xFF222222;
    public static final int SPIRITUALITY_FILL = 0xFF00AADD;
    public static final int SPIRITUALITY_FILL_LOW = 0xFFAA3333;
    public static final int SPIRITUALITY_BORDER = 0xFF444444;

    // ========================================================================
    // 灵视特效 (Spirit Vision)
    // ========================================================================
    /** 敌对 (红) */
    public static final int SPIRIT_VISION_HOSTILE = 0xFFFF5555;
    /** 激怒/警戒 (橙) */
    public static final int SPIRIT_VISION_AGGRO = 0xFFFFAA00;
    /** 中立 (黄) */
    public static final int SPIRIT_VISION_NEUTRAL = 0xFFFFFF55;
    /** 友善 (绿) */
    public static final int SPIRIT_VISION_FRIENDLY = 0xFF55FF55;
    /** 盟友 (青) */
    public static final int SPIRIT_VISION_ALLY = 0xFF55FFFF;

    /** 灵视 HUD 晕影 (ARGB: Alpha=30, R=0, G=255, B=200) */
    public static final int SPIRIT_VISION_VIGNETTE = 0x1E00FFC8;

    // ========================================================================
    // 占卜特效 (Divination)
    // ========================================================================
    /** 占卜目标高亮 (金色) */
    public static final int DIVINATION_HIGHLIGHT = 0xFFFFD700;

    // ========================================================================
    // 图标回退 (Icon Fallback)
    // ========================================================================
    /** 缺失图标背景 (透明) */
    public static final int ICON_FALLBACK_BG = 0x00000000;
    /** 缺失图标边框 (灰) */
    public static final int ICON_FALLBACK_BORDER = 0xFF888888;

    // ========================================================================
    // 工具方法 (Utilities)
    // ========================================================================

    /**
     * 工具方法：修改 ARGB 颜色的透明度 (Alpha)
     *
     * @param color 原始 ARGB 颜色
     * @param alpha 新的 Alpha 值 (0-255)
     * @return 修改后的 ARGB 颜色
     */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * 工具方法：颜色混合 (Linear Interpolation)
     * 用于实现悬停高亮、渐变等效果
     *
     * @param c1 颜色1 (ARGB)
     * @param c2 颜色2 (ARGB)
     * @param ratio 混合比例 (0.0 = c1, 1.0 = c2)
     * @return 混合后的颜色 (ARGB)
     */
    public static int blend(int c1, int c2, float ratio) {
        ratio = Mth.clamp(ratio, 0, 1);
        float iRatio = 1.0f - ratio;

        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 * iRatio + a2 * ratio);
        int r = (int) (r1 * iRatio + r2 * ratio);
        int g = (int) (g1 * iRatio + g2 * ratio);
        int b = (int) (b1 * iRatio + b2 * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 工具方法：将 ARGB int 转换为 JOML Vector4f (用于 Shader 渲染)
     * <p>
     * 3D 渲染系统通常需要 0.0f - 1.0f 范围的 RGBA 向量。
     *
     * @param color ARGB 整数颜色
     * @param alphaOverride 覆盖 Alpha 值 (0.0 - 1.0)。如果为 null，则使用颜色本身的 Alpha。
     * @return Vector4f (x=R, y=G, z=B, w=A) 范围 0.0-1.0
     */
    public static Vector4f toVector4f(int color, Float alphaOverride) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        if (alphaOverride != null) {
            a = alphaOverride;
        }

        return new Vector4f(r, g, b, a);
    }
}
