package com.lotm.lotm.content.pathway;

import com.lotm.lotm.LotMMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 非凡途径抽象基类 (Abstract Beyonder Pathway)
 * <p>
 * 定义了一条完整的途径（从序列9到序列0）。
 * 使用 Builder 模式进行构建，确保数据的完整性和不可变性。
 */
public abstract class BeyonderPathway {

    private final ResourceLocation id;
    private final int color; // 途径代表色 (用于 UI)

    // 存储每个序列的数据 (Key: 0-9)
    private final Map<Integer, PathwaySequence> sequences = new HashMap<>();

    protected BeyonderPathway(ResourceLocation id, int color, Builder builder) {
        this.id = id;
        this.color = color;
        this.sequences.putAll(builder.sequences);
    }

    public ResourceLocation getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    /**
     * 获取途径的本地化名称
     * key: pathway.lotmmod.seer
     */
    public MutableComponent getDisplayName() {
        return Component.translatable("pathway." + id.getNamespace() + "." + id.getPath());
    }

    /**
     * 获取特定序列的本地化名称
     * key: pathway.lotmmod.seer.seq9
     */
    public MutableComponent getSequenceName(int seq) {
        return Component.translatable("pathway." + id.getNamespace() + "." + id.getPath() + ".seq" + seq);
    }

    /**
     * 获取特定序列的数据
     */
    @Nullable
    public PathwaySequence getSequenceData(int seq) {
        return sequences.get(seq);
    }

    /**
     * 计算某序列的总灵性上限
     * <p>
     * 算法：基础值 + 序列9到当前序列的所有加成之和
     */
    public double getTotalMaxSpirituality(int currentSeq) {
        double total = 100.0; // 基础灵性
        for (int i = 9; i >= currentSeq; i--) {
            PathwaySequence data = sequences.get(i);
            if (data != null) {
                total += data.maxSpiritualityBonus();
            }
        }
        return total;
    }

    /**
     * ★★★ 新增：计算某序列的总侦测能力 ★★★
     * 算法：基础值(0) + 序列9到当前序列的所有加成之和
     */
    public double getTotalDetection(int currentSeq) {
        double total = 0.0;
        for (int i = 9; i >= currentSeq; i--) {
            PathwaySequence data = sequences.get(i);
            if (data != null) {
                total += data.detectionBonus();
            }
        }
        return total;
    }

    /**
     * ★★★ 新增：计算某序列的总隐蔽能力 ★★★
     * 算法：基础值(0) + 序列9到当前序列的所有加成之和
     */
    public double getTotalConcealment(int currentSeq) {
        double total = 0.0;
        for (int i = 9; i >= currentSeq; i--) {
            PathwaySequence data = sequences.get(i);
            if (data != null) {
                total += data.concealmentBonus();
            }
        }
        return total;
    }

    /**
     * 获取某序列所有可用的技能 (包括低序列的)
     */
    public List<ResourceLocation> getAvailableSkills(int currentSeq) {
        List<ResourceLocation> list = new ArrayList<>();
        for (int i = 9; i >= currentSeq; i--) {
            PathwaySequence data = sequences.get(i);
            if (data != null) {
                list.addAll(data.skills());
            }
        }
        return list;
    }

    // ==================== Builder ====================

    public static class Builder {
        private final Map<Integer, PathwaySequence> sequences = new HashMap<>();

        /**
         * 添加序列配置 (基础版，无额外属性)
         */
        public Builder add(int level, double spiritBonus, ResourceLocation... skills) {
            return add(level, spiritBonus, 0.0, 0.0, 0.0, 0.0, skills);
        }

        /**
         * 添加序列配置 (旧版全参数，默认无侦测/隐蔽)
         */
        public Builder add(int level, double spiritBonus, double healthBonus, double damageBonus, ResourceLocation... skills) {
            return add(level, spiritBonus, healthBonus, damageBonus, 0.0, 0.0, skills);
        }

        /**
         * ★★★ 添加序列配置 (完整版，包含感知属性) ★★★
         * @param level 序列等级
         * @param spiritBonus 灵性上限加成
         * @param healthBonus 生命上限加成
         * @param damageBonus 攻击力加成
         * @param detectionBonus 灵性侦测加成 (新增)
         * @param concealmentBonus 灵性隐蔽加成 (新增)
         * @param skills 解锁技能
         */
        public Builder add(int level, double spiritBonus, double healthBonus, double damageBonus,
                           double detectionBonus, double concealmentBonus,
                           ResourceLocation... skills) {
            sequences.put(level, new PathwaySequence(
                    level,
                    spiritBonus,
                    healthBonus,
                    damageBonus,
                    detectionBonus,
                    concealmentBonus,
                    List.of(skills)
            ));
            return this;
        }

        public Map<Integer, PathwaySequence> build() {
            return sequences;
        }
    }
}
