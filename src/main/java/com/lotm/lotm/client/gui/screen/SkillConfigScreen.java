package com.lotm.lotm.client.gui.screen;

import com.lotm.lotm.client.gui.skillbar.SkillInfoWidget;
import com.lotm.lotm.client.gui.skillbar.SkillListRenderer;
import com.lotm.lotm.client.gui.skillbar.SlotGridRenderer;
import com.lotm.lotm.client.gui.util.DragHandler;
import com.lotm.lotm.client.key.LotMKeyBindings;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.client.util.LotMFuzzySearchHelper;
import com.lotm.lotm.common.capability.AbilityContainer;
import com.lotm.lotm.common.capability.AbilityContainerProvider;
import com.lotm.lotm.common.capability.BeyonderStateProvider;
import com.lotm.lotm.common.capability.skillbar.SkillBarProvider;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.c2s.C2SSwitchPresetPacket;
import com.lotm.lotm.common.network.packet.c2s.C2SToggleSkillPacket;
import com.lotm.lotm.common.network.packet.c2s.C2SUpdateSkillBarPacket;
import com.lotm.lotm.common.registry.LotMPathways;
import com.lotm.lotm.common.registry.LotMSkills;
import com.lotm.lotm.content.pathway.BeyonderPathway;
import com.lotm.lotm.content.skill.AbstractSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 技能配置屏幕 (Skill Configuration Screen)
 * <p>
 * 工业级重构：
 * 1. **零硬编码**：所有坐标偏移、尺寸、间距均提取为 static final 常量。
 * 2. **统一色板**：所有颜色引用均来自 {@link LotMClientColors}。
 * 3. **会话持久化**：使用静态变量缓存分组折叠状态和 Tab 选择状态，优化用户体验。
 * 4. **智能交互**：支持再次按下打开键关闭界面 (Toggle Close)。
 * 5. **动态标题**：分组标题展示 "序列X: 途径名" 而非简单的数字。
 */
public class SkillConfigScreen extends Screen {

    // ==================== 尺寸常量 (Dimensions) ====================
    private static final int MAIN_HEIGHT = 180;
    private static final int LIST_WIDTH = 140;
    private static final int INFO_WIDTH = 100;
    private static final int GRID_AREA_WIDTH = 120;
    private static final int COMPONENT_GAP = 10; // 组件间距

    // ==================== 布局偏移常量 (Layout Offsets) ====================
    // 列表区域
    private static final int LIST_TOP_OFFSET = 40; // 列表顶部留空 (给搜索栏和Tab)
    private static final int SEARCH_BAR_Y_OFFSET = 20;
    private static final int SEARCH_BAR_HEIGHT = 16;
    private static final int TAB_BTN_HEIGHT = 18;

    // 网格区域
    private static final int GRID_Y_OFFSET = -50; // 相对于中心点的 Y 偏移
    private static final int PAGINATION_Y_OFFSET = 60; // 相对于中心点的 Y 偏移
    private static final int PAGE_NUM_Y_OFFSET = 116; // 相对于网格顶部的 Y 偏移
    private static final int PAGINATION_BTN_GAP = 84; // 翻页按钮之间的间距
    private static final int PAGINATION_BTN_SIZE = 20;
    // 网格内容宽度 (3x3 = 32*3 + 4*2 = 104)
    private static final int GRID_CONTENT_WIDTH = 104;

    // 顶部按钮
    private static final int TOP_BTN_WIDTH = 100;
    private static final int TOP_BTN_HEIGHT = 20;
    private static final int TOP_BTN_MARGIN = 10;
    private static final int TOP_BTN_X_OFFSET = 110; // 距离右边界的偏移

    // 标题
    private static final int TITLE_Y = 10;

    // ==================== 持久化状态 (Persistent State) ====================
    // 静态缓存：分组折叠状态
    private static final Map<Integer, Boolean> GROUP_EXPANSION_CACHE = new HashMap<>();
    // 静态缓存：当前选中的 Tab 分类 (默认为 ALL)
    private static Category cachedCategory = Category.ALL;

    // ==================== 组件 ====================
    private SlotGridRenderer slotGrid;
    private SkillListRenderer skillList;
    private SkillInfoWidget infoWidget;
    private final DragHandler dragHandler = new DragHandler();

    private EditBox searchBox;
    private Category currentCategory; // 初始化时从 cachedCategory 读取
    private final List<Button> tabButtons = new ArrayList<>();

    // ==================== 数据缓存 ====================
    private final List<AbstractSkill> allLearnedSkills = new ArrayList<>();
    private List<SkillListRenderer.SkillGroup> skillGroups = new ArrayList<>();

    private int cachedActivePage = 0;
    private AbstractSkill selectedSkill = null;

    private Button prevPageBtn;
    private Button nextPageBtn;

    private enum Category {
        ALL("gui.lotmmod.category.all"),
        ACTIVE("gui.lotmmod.category.active"),
        PASSIVE("gui.lotmmod.category.passive"),
        RECORDED("gui.lotmmod.category.recorded");

        final String translationKey;
        Category(String translationKey) { this.translationKey = translationKey; }
        public Component getDisplayName() { return Component.translatable(translationKey); }
    }

    public SkillConfigScreen() {
        super(Component.translatable("gui.lotmmod.skill_config.title"));
        // 从静态缓存恢复上次的状态
        this.currentCategory = cachedCategory;
    }

    @Override
    protected void init() {
        super.init();
        loadData();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 计算整体布局起始点 (左中右三栏)
        int totalWidth = LIST_WIDTH + GRID_AREA_WIDTH + INFO_WIDTH + COMPONENT_GAP * 2;
        int startX = centerX - (totalWidth / 2);
        int startY = centerY - (MAIN_HEIGHT / 2);

        // ---------------------------------------------------------
        // 1. 左侧：列表区域
        // ---------------------------------------------------------
        int listX = startX;
        int listY = startY + LIST_TOP_OFFSET;
        int listH = MAIN_HEIGHT - LIST_TOP_OFFSET;

        if (this.skillList == null) {
            this.skillList = new SkillListRenderer(listX, listY, LIST_WIDTH, listH);
        } else {
            this.skillList.updateBounds(listX, listY, LIST_WIDTH, listH);
        }

        // 1.1 搜索栏
        this.searchBox = new EditBox(this.font, listX, startY + SEARCH_BAR_Y_OFFSET, LIST_WIDTH, SEARCH_BAR_HEIGHT, Component.translatable("gui.lotmmod.search_hint"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.addRenderableWidget(this.searchBox);

        // 1.2 Tab 按钮 (4个按钮平分宽度)
        initCategoryTabs(listX, startY);

        // ---------------------------------------------------------
        // 2. 中间：网格区域
        // ---------------------------------------------------------
        int gridX = listX + LIST_WIDTH + COMPONENT_GAP;

        if (this.slotGrid == null) this.slotGrid = new SlotGridRenderer();
        this.slotGrid.setSlotSize(32);

        // 计算网格居中位置
        int gridRenderX = gridX + (GRID_AREA_WIDTH - GRID_CONTENT_WIDTH) / 2;
        int gridRenderY = centerY + GRID_Y_OFFSET;

        this.slotGrid.setPosition(gridRenderX, gridRenderY);

        // 2.1 翻页按钮
        // 按钮X轴基准点也基于 gridRenderX
        int pageBtnY = centerY + PAGINATION_Y_OFFSET;

        // 左箭头
        this.prevPageBtn = Button.builder(Component.literal("<"), btn -> changePage(-1))
                .bounds(gridRenderX, pageBtnY, PAGINATION_BTN_SIZE, PAGINATION_BTN_SIZE).build();

        // 右箭头 (gridRenderX + 84)
        this.nextPageBtn = Button.builder(Component.literal(">"), btn -> changePage(1))
                .bounds(gridRenderX + PAGINATION_BTN_GAP, pageBtnY, PAGINATION_BTN_SIZE, PAGINATION_BTN_SIZE).build();

        this.addRenderableWidget(prevPageBtn);
        this.addRenderableWidget(nextPageBtn);

        // ---------------------------------------------------------
        // 3. 右侧：详情面板
        // ---------------------------------------------------------
        int infoX = gridX + GRID_AREA_WIDTH + COMPONENT_GAP;
        if (this.infoWidget == null) {
            this.infoWidget = new SkillInfoWidget(infoX, startY, INFO_WIDTH, MAIN_HEIGHT);
        } else {
            this.infoWidget.setBounds(infoX, startY, INFO_WIDTH, MAIN_HEIGHT);
        }

        this.infoWidget.setSelectedSkill(selectedSkill);
        if (this.skillList != null) this.skillList.setSelectedSkill(selectedSkill);

        // ---------------------------------------------------------
        // 4. 顶部功能按钮
        // ---------------------------------------------------------
        updateButtonStates();
        initTopButtons();
        updateFilteredList();
    }

    private void initCategoryTabs(int startX, int startY) {
        this.tabButtons.clear();
        int btnWidth = LIST_WIDTH / 4;

        Category[] cats = Category.values();

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            // 最后一个按钮占据剩余像素，防止除法误差导致缺口
            int w = (i == cats.length - 1) ? (LIST_WIDTH - i * btnWidth) : btnWidth;

            Button btn = Button.builder(cat.getDisplayName(), b -> {
                this.currentCategory = cat;
                // ★★★ 更新静态缓存 ★★★
                cachedCategory = cat;

                updateFilteredList();
                updateButtonStates();
            }).bounds(startX + i * btnWidth, startY, w, TAB_BTN_HEIGHT).build();

            this.addRenderableWidget(btn);
            this.tabButtons.add(btn);
        }
    }

    private void initTopButtons() {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.lotmmod.skill_config.edit_hud"),
                        btn -> Minecraft.getInstance().setScreen(new SkillBarEditScreen(this)))
                .bounds(this.width - TOP_BTN_X_OFFSET, TOP_BTN_MARGIN, TOP_BTN_WIDTH, TOP_BTN_HEIGHT).build());
    }

    private void updateFilteredList() {
        String query = this.searchBox.getValue();

        // 1. 获取玩家当前途径 (用于解析序列名称)
        AtomicReference<BeyonderPathway> currentPathway = new AtomicReference<>();
        if (this.minecraft.player != null) {
            this.minecraft.player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                currentPathway.set(LotMPathways.get(state.getPathwayId()));
            });
        }
        BeyonderPathway pathway = currentPathway.get();

        // 2. 预构建 技能ID -> 序列 的映射表 (优化查询性能)
        Map<ResourceLocation, Integer> skillToSeqMap = new HashMap<>();
        if (pathway != null) {
            for (int i = 0; i <= 9; i++) {
                var seqData = pathway.getSequenceData(i);
                if (seqData != null) {
                    for (ResourceLocation skillId : seqData.skills()) {
                        skillToSeqMap.put(skillId, i);
                    }
                }
            }
        }

        // 3. 筛选列表
        List<AbstractSkill> filtered = this.allLearnedSkills.stream()
                .filter(skill -> LotMFuzzySearchHelper.matches(skill.getDisplayName().getString(), query))
                .filter(skill -> {
                    switch (currentCategory) {
                        case ACTIVE: return !skill.getCastType().isPassive();
                        case PASSIVE: return skill.getCastType().isPassive();
                        case RECORDED: return false; // TODO: 实现记录技能筛选
                        default: return true;
                    }
                }).collect(Collectors.toList());

        // 4. 分组逻辑
        Map<Integer, List<AbstractSkill>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(skill -> {
                    // 优先使用途径中的映射关系
                    if (skillToSeqMap.containsKey(skill.getId())) {
                        return skillToSeqMap.get(skill.getId());
                    }
                    // 如果技能不在途径定义中（如通用格斗技能），默认归为序列 9
                    // 这样就会显示为 "序列9: 占卜家" (假设当前是占卜家途径)
                    return 9;
                }));

        this.skillGroups.clear();
        grouped.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<AbstractSkill>>comparingByKey().reversed())
                .forEach(entry -> {
                    int sequence = entry.getKey();

                    // ★★★ 动态生成分组标题 ★★★
                    Component groupName;
                    if (pathway != null && sequence >= 0 && sequence <= 9) {
                        // 使用途径定义的本地化名称 (e.g., "序列9: 占卜家")
                        groupName = pathway.getSequenceName(sequence);
                    } else {
                        // 回退到默认格式
                        groupName = Component.translatable("gui.lotmmod.skill_list.sequence_group", sequence);
                    }

                    SkillListRenderer.SkillGroup group = new SkillListRenderer.SkillGroup(sequence, groupName);
                    group.skills.addAll(entry.getValue());
                    group.skills.sort(Comparator.comparing(s -> s.getDisplayName().getString()));

                    // ★★★ 从静态缓存加载折叠状态 ★★★
                    group.expanded = GROUP_EXPANSION_CACHE.getOrDefault(sequence, true);

                    this.skillGroups.add(group);
                });
    }

    private void loadData() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        allLearnedSkills.clear();
        player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(container -> {
            for (ResourceLocation id : container.getLearnedAbilities()) {
                AbstractSkill skill = LotMSkills.getSkill(id);
                if (skill != null) allLearnedSkills.add(skill);
            }
            this.cachedActivePage = container.getActivePage();
        });
    }

    private void changePage(int delta) {
        int newPage = cachedActivePage + delta;
        if (newPage >= 0 && newPage < AbilityContainer.MAX_PAGES) {
            this.cachedActivePage = newPage;
            PacketHandler.CHANNEL.sendToServer(new C2SSwitchPresetPacket(newPage));
            playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);
            updateButtonStates();
        }
    }

    private void updateButtonStates() {
        if (prevPageBtn != null) prevPageBtn.active = cachedActivePage > 0;
        if (nextPageBtn != null) nextPageBtn.active = cachedActivePage < AbilityContainer.MAX_PAGES - 1;
        for (int i = 0; i < tabButtons.size(); i++) {
            Category[] cats = Category.values();
            if (i < cats.length) {
                tabButtons.get(i).active = (cats[i] != currentCategory);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, TITLE_Y, LotMClientColors.TEXT_TITLE);

        if (slotGrid != null) {
            String pageStr = String.format("%d/%d", cachedActivePage + 1, AbilityContainer.MAX_PAGES);
            int w = font.width(pageStr);
            graphics.drawString(font, pageStr, slotGrid.getX() + (GRID_CONTENT_WIDTH - w)/2, slotGrid.getY() + PAGE_NUM_Y_OFFSET, LotMClientColors.TEXT_DIM, false);
        }

        if (this.slotGrid != null) this.slotGrid.render(graphics, mouseX, mouseY);
        if (this.skillList != null) this.skillList.render(graphics, mouseX, mouseY, skillGroups);
        if (this.infoWidget != null) this.infoWidget.render(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (dragHandler.isDragging()) {
            dragHandler.render(graphics);
            if (slotGrid != null) {
                int hoverSlot = slotGrid.getSlotAt(mouseX, mouseY);
                if (hoverSlot != -1) slotGrid.renderSlotHighlight(graphics, hoverSlot, LotMClientColors.DRAG_HIGHLIGHT_MASK);
            }
        } else {
            if (slotGrid != null) {
                int hoveredSlot = slotGrid.getSlotAt(mouseX, mouseY);
                if (hoveredSlot != -1) {
                    Minecraft.getInstance().player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                        ResourceLocation skillId = bar.getSkillInSlot(hoveredSlot);
                        if (skillId != null) {
                            AbstractSkill skill = LotMSkills.getSkill(skillId);
                            if (skill != null) graphics.renderTooltip(this.font, skill.getDisplayName(), mouseX, mouseY);
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 1. 优先处理搜索框输入
        if (this.searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            boolean res = this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            if (res) updateFilteredList();
            return res;
        }

        // 2. ★★★ 再次按下打开键时关闭界面 (Toggle Close) ★★★
        // 只有当搜索框没有焦点时才生效，避免打字时误触关闭
        if (LotMKeyBindings.OPEN_SKILL_CONFIG.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused()) {
            boolean res = this.searchBox.charTyped(codePoint, modifiers);
            if (res) updateFilteredList();
            return res;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (button == 0) {
            // A. 列表交互
            if (skillList != null) {
                if (skillList.mouseClicked(mx, my, button, skillGroups)) {
                    playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);

                    // ★★★ 同步折叠状态到静态缓存 ★★★
                    for (SkillListRenderer.SkillGroup group : skillGroups) {
                        GROUP_EXPANSION_CACHE.put(group.sequence, group.expanded);
                    }
                    return true;
                }

                AbstractSkill skill = skillList.getSkillAt(mx, my, skillGroups);
                if (skill != null) {
                    this.selectedSkill = skill;
                    this.infoWidget.setSelectedSkill(skill);
                    this.skillList.setSelectedSkill(skill);
                    playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);

                    if (skillList.isOverToggleButton(mx, my, skill, skillGroups)) {
                        PacketHandler.CHANNEL.sendToServer(new C2SToggleSkillPacket(skill.getId()));
                        Minecraft.getInstance().player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(cap -> {
                            if (cap.isSkillActive(skill.getId())) cap.deactivateSkill(skill.getId());
                            else cap.activateSkill(skill.getId());
                        });
                        return true;
                    }
                    dragHandler.startDrag(skill, mx, my, -1);
                    return true;
                }
            }

            // B. 网格交互
            if (slotGrid != null) {
                int slot = slotGrid.getSlotAt(mx, my);
                if (slot != -1) {
                    Minecraft.getInstance().player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                        ResourceLocation skillId = bar.getSkillInSlot(slot);
                        if (skillId != null) {
                            AbstractSkill skill = LotMSkills.getSkill(skillId);
                            if (skill != null) {
                                this.selectedSkill = skill;
                                this.infoWidget.setSelectedSkill(skill);
                                this.skillList.setSelectedSkill(skill);
                                dragHandler.startDrag(skill, mx, my, slot);
                                playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                            }
                        }
                    });
                    return true;
                }
            }
        }

        if (button == 1 && slotGrid != null) {
            int slot = slotGrid.getSlotAt(mx, my);
            if (slot != -1) {
                PacketHandler.CHANNEL.sendToServer(new C2SUpdateSkillBarPacket(slot, null));
                playUiSound(SoundEvents.UI_BUTTON_CLICK, 0.9f);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragHandler.isDragging()) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (slotGrid != null) {
                int targetSlot = slotGrid.getSlotAt(mx, my);
                int sourceSlot = dragHandler.getSourceSlot();
                AbstractSkill skill = dragHandler.getDraggingSkill();

                if (targetSlot != -1 && skill != null) {
                    PacketHandler.CHANNEL.sendToServer(new C2SUpdateSkillBarPacket(targetSlot, skill.getId()));
                    playUiSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f);
                } else if (sourceSlot != -1 && !slotGrid.isInGridArea(mx, my)) {
                    PacketHandler.CHANNEL.sendToServer(new C2SUpdateSkillBarPacket(sourceSlot, null));
                    playUiSound(SoundEvents.UI_BUTTON_CLICK, 0.9f);
                }
            }
            dragHandler.stopDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragHandler.isDragging()) {
            dragHandler.updatePosition((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (skillList != null) return skillList.mouseScrolled(mouseX, mouseY, delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void playUiSound(Holder<SoundEvent> soundHolder, float pitch) {
        playUiSound(soundHolder.value(), pitch);
    }
    private void playUiSound(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }
}
