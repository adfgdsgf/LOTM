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
 * 该界面允许玩家管理已学习的非凡能力。主要功能包括：
 * 1. 查看已学习的所有技能列表（支持搜索、分类、按序列分组）。
 * 2. 查看技能详细信息（消耗、条件、描述）。
 * 3. 配置技能快捷栏（支持拖拽操作）。
 * 4. 切换技能预设页。
 * 5. 快速开启/关闭持续性技能。
 */
public class SkillConfigScreen extends Screen {

    // --- 布局常量 ---
    private static final int MAIN_HEIGHT = 180;
    private static final int LIST_WIDTH = 140;
    private static final int INFO_WIDTH = 100;
    private static final int GRID_AREA_WIDTH = 120;
    private static final int COMPONENT_GAP = 10;

    private static final int LIST_TOP_OFFSET = 40;
    private static final int SEARCH_BAR_Y_OFFSET = 20;
    private static final int SEARCH_BAR_HEIGHT = 16;
    private static final int TAB_BTN_HEIGHT = 18;

    private static final int GRID_Y_OFFSET = -50;
    private static final int PAGINATION_Y_OFFSET = 60;
    private static final int PAGE_NUM_Y_OFFSET = 116;
    private static final int PAGINATION_BTN_GAP = 84;
    private static final int PAGINATION_BTN_SIZE = 20;
    private static final int GRID_CONTENT_WIDTH = 104;

    private static final int TOP_BTN_WIDTH = 100;
    private static final int TOP_BTN_HEIGHT = 20;
    private static final int TOP_BTN_MARGIN = 10;
    private static final int TOP_BTN_X_OFFSET = 110;

    private static final int TITLE_Y = 10;

    // --- 状态缓存 ---
    // 记录技能分组的折叠状态，防止重绘时重置
    private static final Map<Integer, Boolean> GROUP_EXPANSION_CACHE = new HashMap<>();
    // 记录上次选择的分类
    private static Category cachedCategory = Category.ALL;
    // 记录上次选中的技能 ID
    private static ResourceLocation lastSelectedSkillId = null;

    // --- 子组件渲染器 ---
    private SlotGridRenderer slotGrid;      // 中间：技能槽位网格
    private SkillListRenderer skillList;    // 左侧：技能列表
    private SkillInfoWidget infoWidget;     // 右侧：技能详情
    private final DragHandler dragHandler = new DragHandler(); // 拖拽逻辑处理器

    // --- 控件 ---
    private EditBox searchBox;
    private Category currentCategory;
    private final List<Button> tabButtons = new ArrayList<>();
    private Button prevPageBtn;
    private Button nextPageBtn;

    // --- 数据 ---
    private final List<AbstractSkill> allLearnedSkills = new ArrayList<>();
    private List<SkillListRenderer.SkillGroup> skillGroups = new ArrayList<>();
    private int cachedActivePage = 0;
    private AbstractSkill selectedSkill = null;

    /**
     * 技能分类枚举
     */
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
        this.currentCategory = cachedCategory;
    }

    /**
     * 初始化屏幕组件
     * <p>
     * 在屏幕打开或窗口大小改变时调用。
     * 负责加载数据、计算布局坐标、初始化按钮和子组件。
     */
    @Override
    protected void init() {
        super.init();
        loadData();

        // 恢复上次选中的技能
        if (lastSelectedSkillId != null) {
            for (AbstractSkill skill : allLearnedSkills) {
                if (skill.getId().equals(lastSelectedSkillId)) {
                    this.selectedSkill = skill;
                    break;
                }
            }
        }

        // 计算居中布局坐标
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int totalWidth = LIST_WIDTH + GRID_AREA_WIDTH + INFO_WIDTH + COMPONENT_GAP * 2;
        int startX = centerX - (totalWidth / 2);
        int startY = centerY - (MAIN_HEIGHT / 2);

        // 1. 初始化左侧技能列表区域
        int listX = startX;
        int listY = startY + LIST_TOP_OFFSET;
        int listH = MAIN_HEIGHT - LIST_TOP_OFFSET;

        if (this.skillList == null) {
            this.skillList = new SkillListRenderer(listX, listY, LIST_WIDTH, listH);
        } else {
            this.skillList.updateBounds(listX, listY, LIST_WIDTH, listH);
        }

        // 搜索框
        this.searchBox = new EditBox(this.font, listX, startY + SEARCH_BAR_Y_OFFSET, LIST_WIDTH, SEARCH_BAR_HEIGHT, Component.translatable("gui.lotmmod.search_hint"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.addRenderableWidget(this.searchBox);

        initCategoryTabs(listX, startY);

        // 2. 初始化中间技能槽位区域
        int gridX = listX + LIST_WIDTH + COMPONENT_GAP;

        if (this.slotGrid == null) this.slotGrid = new SlotGridRenderer();
        this.slotGrid.setSlotSize(32);

        int gridRenderX = gridX + (GRID_AREA_WIDTH - GRID_CONTENT_WIDTH) / 2;
        int gridRenderY = centerY + GRID_Y_OFFSET;

        this.slotGrid.setPosition(gridRenderX, gridRenderY);

        // 翻页按钮
        int pageBtnY = centerY + PAGINATION_Y_OFFSET;
        this.prevPageBtn = Button.builder(Component.literal("<"), btn -> changePage(-1))
                .bounds(gridRenderX, pageBtnY, PAGINATION_BTN_SIZE, PAGINATION_BTN_SIZE).build();
        this.nextPageBtn = Button.builder(Component.literal(">"), btn -> changePage(1))
                .bounds(gridRenderX + PAGINATION_BTN_GAP, pageBtnY, PAGINATION_BTN_SIZE, PAGINATION_BTN_SIZE).build();

        this.addRenderableWidget(prevPageBtn);
        this.addRenderableWidget(nextPageBtn);

        // 3. 初始化右侧详情区域
        int infoX = gridX + GRID_AREA_WIDTH + COMPONENT_GAP;
        if (this.infoWidget == null) {
            this.infoWidget = new SkillInfoWidget(infoX, startY, INFO_WIDTH, MAIN_HEIGHT);
        } else {
            this.infoWidget.setBounds(infoX, startY, INFO_WIDTH, MAIN_HEIGHT);
        }

        // 同步选中状态
        this.infoWidget.setSelectedSkill(selectedSkill);
        if (this.skillList != null) this.skillList.setSelectedSkill(selectedSkill);

        updateButtonStates();
        initTopButtons();
        updateFilteredList();
    }

    /**
     * 初始化分类标签页按钮
     */
    private void initCategoryTabs(int startX, int startY) {
        this.tabButtons.clear();
        int btnWidth = LIST_WIDTH / 4;

        Category[] cats = Category.values();

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            // 最后一个按钮补齐宽度，防止像素误差
            int w = (i == cats.length - 1) ? (LIST_WIDTH - i * btnWidth) : btnWidth;

            Button btn = Button.builder(cat.getDisplayName(), b -> {
                this.currentCategory = cat;
                cachedCategory = cat;
                updateFilteredList();
                updateButtonStates();
            }).bounds(startX + i * btnWidth, startY, w, TAB_BTN_HEIGHT).build();

            this.addRenderableWidget(btn);
            this.tabButtons.add(btn);
        }
    }

    /**
     * 初始化顶部功能按钮 (如 HUD 编辑)
     */
    private void initTopButtons() {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.lotmmod.skill_config.edit_hud"),
                        btn -> Minecraft.getInstance().setScreen(new SkillBarEditScreen(this)))
                .bounds(this.width - TOP_BTN_X_OFFSET, TOP_BTN_MARGIN, TOP_BTN_WIDTH, TOP_BTN_HEIGHT).build());
    }

    /**
     * 更新过滤后的技能列表
     * <p>
     * 根据搜索关键词、当前分类、以及玩家的途径序列，
     * 筛选出符合条件的技能，并按序列等级进行分组。
     */
    private void updateFilteredList() {
        String query = this.searchBox.getValue();

        // 获取玩家当前途径数据，用于显示序列名称
        AtomicReference<BeyonderPathway> currentPathway = new AtomicReference<>();
        if (this.minecraft.player != null) {
            this.minecraft.player.getCapability(BeyonderStateProvider.CAPABILITY).ifPresent(state -> {
                currentPathway.set(LotMPathways.get(state.getPathwayId()));
            });
        }
        BeyonderPathway pathway = currentPathway.get();

        // 建立技能ID到序列等级的映射
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

        // 执行过滤
        List<AbstractSkill> filtered = this.allLearnedSkills.stream()
                .filter(skill -> LotMFuzzySearchHelper.matches(skill.getDisplayName().getString(), query))
                .filter(skill -> {
                    switch (currentCategory) {
                        case ACTIVE: return !skill.getCastType().isPassive();
                        case PASSIVE: return skill.getCastType().isPassive();
                        case RECORDED: return false;
                        default: return true;
                    }
                }).collect(Collectors.toList());

        // 按序列分组
        Map<Integer, List<AbstractSkill>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(skill -> {
                    if (skillToSeqMap.containsKey(skill.getId())) {
                        return skillToSeqMap.get(skill.getId());
                    }
                    return 9; // 默认归为序列9或未知
                }));

        // 构建显示用的 Group 对象
        this.skillGroups.clear();
        grouped.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<AbstractSkill>>comparingByKey().reversed()) // 高序列在前
                .forEach(entry -> {
                    int sequence = entry.getKey();
                    Component groupName;
                    if (pathway != null && sequence >= 0 && sequence <= 9) {
                        groupName = pathway.getSequenceName(sequence);
                    } else {
                        groupName = Component.translatable("gui.lotmmod.skill_list.sequence_group", sequence);
                    }

                    SkillListRenderer.SkillGroup group = new SkillListRenderer.SkillGroup(sequence, groupName);
                    group.skills.addAll(entry.getValue());
                    group.skills.sort(Comparator.comparing(s -> s.getDisplayName().getString()));
                    // 恢复折叠状态
                    group.expanded = GROUP_EXPANSION_CACHE.getOrDefault(sequence, true);

                    this.skillGroups.add(group);
                });
    }

    /**
     * 从 Capability 加载玩家数据
     */
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

    /**
     * 切换技能预设页
     * @param delta 页码变化量 (+1 或 -1)
     */
    private void changePage(int delta) {
        int newPage = cachedActivePage + delta;
        if (newPage >= 0 && newPage < AbilityContainer.MAX_PAGES) {
            this.cachedActivePage = newPage;
            PacketHandler.CHANNEL.sendToServer(new C2SSwitchPresetPacket(newPage));
            playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);
            updateButtonStates();
        }
    }

    /**
     * 更新按钮的激活/禁用状态
     */
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

    /**
     * 渲染屏幕内容
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, TITLE_Y, LotMClientColors.TEXT_TITLE);

        // 渲染页码
        if (slotGrid != null) {
            String pageStr = String.format("%d/%d", cachedActivePage + 1, AbilityContainer.MAX_PAGES);
            int w = font.width(pageStr);
            graphics.drawString(font, pageStr, slotGrid.getX() + (GRID_CONTENT_WIDTH - w)/2, slotGrid.getY() + PAGE_NUM_Y_OFFSET, LotMClientColors.TEXT_DIM, false);
        }

        // 渲染子组件
        if (this.slotGrid != null) this.slotGrid.render(graphics, mouseX, mouseY);
        if (this.skillList != null) this.skillList.render(graphics, mouseX, mouseY, skillGroups);
        if (this.infoWidget != null) this.infoWidget.render(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        // 渲染拖拽中的图标
        if (dragHandler.isDragging()) {
            dragHandler.render(graphics);
            if (slotGrid != null) {
                int hoverSlot = slotGrid.getSlotAt(mouseX, mouseY);
                if (hoverSlot != -1) slotGrid.renderSlotHighlight(graphics, hoverSlot, LotMClientColors.DRAG_HIGHLIGHT_MASK);
            }
        } else {
            // 渲染槽位悬停提示
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

    /**
     * 处理键盘按下事件
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 搜索框焦点处理
        if (this.searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            boolean res = this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            if (res) updateFilteredList();
            return res;
        }

        // 快捷键关闭界面
        if (LotMKeyBindings.OPEN_SKILL_CONFIG.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 处理字符输入事件 (用于搜索框)
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused()) {
            boolean res = this.searchBox.charTyped(codePoint, modifiers);
            if (res) updateFilteredList();
            return res;
        }
        return super.charTyped(codePoint, modifiers);
    }

    /**
     * 处理鼠标点击事件
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // 1. 优先处理详情页的滚动条交互
        if (this.infoWidget != null && this.infoWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) { // 左键点击
            // 2. 处理技能列表点击
            if (skillList != null) {
                // 处理分组折叠
                if (skillList.mouseClicked(mx, my, button, skillGroups)) {
                    playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                    for (SkillListRenderer.SkillGroup group : skillGroups) {
                        GROUP_EXPANSION_CACHE.put(group.sequence, group.expanded);
                    }
                    return true;
                }

                // 处理技能项点击
                AbstractSkill skill = skillList.getSkillAt(mx, my, skillGroups);
                if (skill != null) {
                    // 处理开关按钮
                    if (skillList.isOverToggleButton(mx, my, skill, skillGroups)) {
                        PacketHandler.CHANNEL.sendToServer(new C2SToggleSkillPacket(skill.getId()));
                        Minecraft.getInstance().player.getCapability(AbilityContainerProvider.CAPABILITY).ifPresent(cap -> {
                            if (cap.isSkillActive(skill.getId())) cap.deactivateSkill(skill.getId());
                            else cap.activateSkill(skill.getId());
                        });
                        playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                        return true;
                    }

                    // 选中技能并开始拖拽准备
                    this.selectedSkill = skill;
                    lastSelectedSkillId = skill.getId();

                    this.infoWidget.setSelectedSkill(skill);
                    this.skillList.setSelectedSkill(skill);
                    playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);

                    dragHandler.startPress(skill, mx, my, -1);
                    return true;
                }
            }

            // 3. 处理技能槽位点击
            if (slotGrid != null) {
                int slot = slotGrid.getSlotAt(mx, my);
                if (slot != -1) {
                    Minecraft.getInstance().player.getCapability(SkillBarProvider.CAPABILITY).ifPresent(bar -> {
                        ResourceLocation skillId = bar.getSkillInSlot(slot);
                        if (skillId != null) {
                            AbstractSkill skill = LotMSkills.getSkill(skillId);
                            if (skill != null) {
                                this.selectedSkill = skill;
                                lastSelectedSkillId = skill.getId();

                                this.infoWidget.setSelectedSkill(skill);
                                this.skillList.setSelectedSkill(skill);

                                dragHandler.startPress(skill, mx, my, slot);
                                playUiSound(SoundEvents.UI_BUTTON_CLICK, 1.0f);
                            }
                        }
                    });
                    return true;
                }
            }
        }

        // 右键点击槽位：清除技能
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

    /**
     * 处理鼠标释放事件 (完成拖拽)
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 转发给详情页 (处理滚动条拖拽结束)
        if (this.infoWidget != null && this.infoWidget.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        if (skillList != null) {
            skillList.mouseReleased(mouseX, mouseY, button);
        }

        if (button == 0) {
            // 获取拖拽数据并结束拖拽状态
            AbstractSkill skill = dragHandler.getDraggingSkill();
            int sourceSlot = dragHandler.getSourceSlot();
            boolean wasDragging = dragHandler.stopDrag();

            if (wasDragging) {
                int mx = (int) mouseX;
                int my = (int) mouseY;
                if (slotGrid != null) {
                    int targetSlot = slotGrid.getSlotAt(mx, my);

                    // 放置到新槽位
                    if (targetSlot != -1 && skill != null) {
                        PacketHandler.CHANNEL.sendToServer(new C2SUpdateSkillBarPacket(targetSlot, skill.getId()));
                        playUiSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f);
                    }
                    // 拖出网格区域：如果是从槽位拖出来的，则视为移除
                    else if (sourceSlot != -1 && !slotGrid.isInGridArea(mx, my)) {
                        PacketHandler.CHANNEL.sendToServer(new C2SUpdateSkillBarPacket(sourceSlot, null));
                        playUiSound(SoundEvents.UI_BUTTON_CLICK, 0.9f);
                    }
                }
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 处理鼠标拖拽事件
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 转发给详情页 (滚动条拖拽)
        if (this.infoWidget != null && this.infoWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        // 转发给技能列表 (滚动条拖拽)
        if (skillList != null && skillList.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        // 更新全局拖拽处理器的状态
        dragHandler.onMouseDragged(mouseX, mouseY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 处理鼠标滚轮事件
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 转发给详情页
        if (this.infoWidget != null && this.infoWidget.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        // 转发给技能列表
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
