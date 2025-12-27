package com.lotm.lotm.content.skill;

/**
 * 技能释放上下文
 * <p>
 * 封装释放技能时的环境信息，特别是组合键状态。
 * 这是一个 Record 类，保证数据不可变且线程安全。
 * <p>
 * 工业级设计：
 * 使用 modifier1/2/3 而非 isShift/Ctrl/Alt，
 * 实现了逻辑层(Skill)与物理输入层(KeyBinding)的解耦。
 * 具体按键由 {@link com.lotm.lotm.client.key.LotMKeyBindings} 定义。
 *
 * @param modifier1 第一修饰键状态 (默认 Shift)
 * @param modifier2 第二修饰键状态 (默认 Ctrl)
 * @param modifier3 第三修饰键状态 (默认 Alt)
 */
public record CastContext(boolean modifier1, boolean modifier2, boolean modifier3) {

    public static final CastContext EMPTY = new CastContext(false, false, false);

    /**
     * 将组合键状态编码为一个字节，用于网络传输
     * Bit 0: Mod1, Bit 1: Mod2, Bit 2: Mod3
     */
    public byte pack() {
        byte flags = 0;
        if (modifier1) flags |= 1;
        if (modifier2) flags |= 2;
        if (modifier3) flags |= 4;
        return flags;
    }

    /**
     * 从字节解码
     */
    public static CastContext unpack(byte flags) {
        return new CastContext(
                (flags & 1) != 0,
                (flags & 2) != 0,
                (flags & 4) != 0
        );
    }
}
