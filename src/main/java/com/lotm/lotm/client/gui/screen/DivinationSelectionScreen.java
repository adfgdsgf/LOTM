package com.lotm.lotm.client.gui.screen;

import com.lotm.lotm.client.gui.divination.DivinationCategoryRegistry;
import com.lotm.lotm.client.gui.divination.IDivinationCategory;
import com.lotm.lotm.client.gui.widget.LotMScrollbar;
import com.lotm.lotm.client.util.LotMClientColors;
import com.lotm.lotm.client.util.LotMFuzzySearchHelper;
import com.lotm.lotm.common.network.PacketHandler;
import com.lotm.lotm.common.network.packet.c2s.C2SRequestDivinationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 占卜目标选择界面 (分层级版 - 优化布局)
 * <p>
 * 结构：
 * 1. 一级菜单：分类选择 (Category Selection) - 标题与内容分离
 * 2. 二级菜单：具体条目选择 (Item Selection)
 */
public class DivinationSelectionScreen extends Screen {

    private enum ScreenState {
        CATEGORY_SELECT, // 一级菜单：选大类
        ITEM_SELECT      // 二级菜单：选具体目标
    }

    private final InteractionHand hand;
    private ScreenState state = ScreenState.CATEGORY_SELECT;

    // 数据源
    private final List<IDivinationCategory> allCategories;
    private IDivinationCategory currentCategory = null;

    // 二级菜单组件
    private EditBox searchBox;
    private LotMScrollbar scrollbar;
    private List<IDivinationCategory.TargetEntry> filteredList = new ArrayList<>();
    private IDivinationCategory.TargetEntry selectedEntry = null;

    // 布局常量
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 230;

    // 一级菜单布局
    private static final int TITLE_AREA_HEIGHT = 40; // 标题区域高度
    private static final int CONTENT_AREA_Y_OFFSET = 50; // 内容区域起始 Y 偏移 (相对于 GUI 顶部)
    private static final int CAT_BTN_WIDTH = 180; // 按钮宽度 (单列布局更清晰，或者双列)
    private static final int CAT_BTN_HEIGHT = 20;
    private static final int CAT_SPACING = 8;

    // 二级菜单布局
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_PADDING = 2;
    private static final int ITEM_COLS = 9;
    private static final int PADDING = 10;
    private static final int TOP_BAR_HEIGHT = 40;
    private static final int BOTTOM_BAR_HEIGHT = 30;

    public DivinationSelectionScreen(InteractionHand hand) {
        super(Component.translatable("gui.lotmmod.divination.title"));
        this.hand = hand;
        this.allCategories = new ArrayList<>(DivinationCategoryRegistry.getAllCategories());
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // 清除旧组件

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;

        if (state == ScreenState.CATEGORY_SELECT) {
            initCategoryScreen(centerX, startY);
        } else {
            initItemScreen(startX, startY);
        }
    }

    // ==================== 一级菜单初始化 ====================

    private void initCategoryScreen(int centerX, int guiStartY) {
        if (allCategories.isEmpty()) return;

        // 计算内容区域的起始 Y
        int contentStartY = guiStartY + CONTENT_AREA_Y_OFFSET;

        // 这里采用单列布局，居中显示，看起来更像是一个菜单列表
        for (int i = 0; i < allCategories.size(); i++) {
            IDivinationCategory cat = allCategories.get(i);

            int x = centerX - CAT_BTN_WIDTH / 2;
            int y = contentStartY + i * (CAT_BTN_HEIGHT + CAT_SPACING);

            this.addRenderableWidget(Button.builder(cat.getDisplayName(), b -> {
                this.currentCategory = cat;
                this.state = ScreenState.ITEM_SELECT;
                this.selectedEntry = null;
                this.init();
            }).bounds(x, y, CAT_BTN_WIDTH, CAT_BTN_HEIGHT).build());
        }
    }

    // ==================== 二级菜单初始化 ====================

    private void initItemScreen(int startX, int startY) {
        // 1. 搜索栏
        this.searchBox = new EditBox(this.font, startX + PADDING, startY + 15, GUI_WIDTH - PADDING * 2, 16, Component.literal("Search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.addRenderableWidget(this.searchBox);

        // 2. 滚动条
        int scrollBarX = startX + GUI_WIDTH - 6;
        int scrollBarY = startY + TOP_BAR_HEIGHT;
        int scrollBarH = GUI_HEIGHT - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 5;

        if (this.scrollbar == null) {
            this.scrollbar = new LotMScrollbar(scrollBarX, scrollBarY, 4, scrollBarH);
        } else {
            this.scrollbar.setBounds(scrollBarX, scrollBarY, 4, scrollBarH);
            this.scrollbar.setScrollOffset(0);
        }

        // 3. 底部按钮
        int btnWidth = (GUI_WIDTH - PADDING * 3) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            this.state = ScreenState.CATEGORY_SELECT;
            this.currentCategory = null;
            this.searchBox = null;
            this.init();
        }).bounds(startX + PADDING, startY + GUI_HEIGHT - 25, btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.lotmmod.divination.start"), b -> confirmSelection())
                .bounds(startX + PADDING * 2 + btnWidth, startY + GUI_HEIGHT - 25, btnWidth, 20).build());

        updateFilteredList();
    }

    private void updateFilteredList() {
        if (currentCategory == null) return;

        String query = (searchBox != null) ? searchBox.getValue() : "";
        this.filteredList = currentCategory.getEntries().stream()
                .filter(e -> LotMFuzzySearchHelper.matches(e.name().getString(), query))
                .collect(Collectors.toList());

        int rows = (int) Math.ceil((double) filteredList.size() / ITEM_COLS);
        int totalHeight = rows * (SLOT_SIZE + SLOT_PADDING);

        if (this.scrollbar != null) {
            this.scrollbar.setContentHeight(totalHeight);
        }
    }

    private void confirmSelection() {
        if (selectedEntry != null) {
            PacketHandler.CHANNEL.sendToServer(new C2SRequestDivinationPacket(
                    this.hand, selectedEntry.typeId(), selectedEntry.id()
            ));
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;

        // 绘制通用背景框
        graphics.fill(startX, startY, startX + GUI_WIDTH, startY + GUI_HEIGHT, LotMClientColors.CONTAINER_BG);
        graphics.renderOutline(startX, startY, GUI_WIDTH, GUI_HEIGHT, LotMClientColors.CONTAINER_BORDER);

        if (state == ScreenState.CATEGORY_SELECT) {
            // ★★★ 视觉分层渲染 ★★★

            // 1. 标题区域背景 (稍微亮一点的背景，区分头部)
            graphics.fill(startX + 1, startY + 1, startX + GUI_WIDTH - 1, startY + TITLE_AREA_HEIGHT, LotMClientColors.GROUP_HEADER_BG);
            // 标题区域底部分割线
            graphics.fill(startX + 1, startY + TITLE_AREA_HEIGHT, startX + GUI_WIDTH - 1, startY + TITLE_AREA_HEIGHT + 1, LotMClientColors.SEPARATOR_LINE);

            // 绘制标题文字 (居中)
            graphics.drawCenteredString(this.font, Component.translatable("gui.lotmmod.divination.select_category"), centerX, startY + 15, LotMClientColors.TEXT_TITLE);

            // 内容区域 (按钮区) 已经在 initCategoryScreen 中添加了 Widget，这里不需要额外绘制背景，保持通用的 CONTAINER_BG 即可

        } else {
            renderItemScreen(graphics, mouseX, mouseY, startX, startY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderItemScreen(GuiGraphics graphics, int mouseX, int mouseY, int startX, int startY) {
        int gridX = startX + PADDING;
        int gridY = startY + TOP_BAR_HEIGHT;
        int gridW = GUI_WIDTH - PADDING * 2;
        int gridH = GUI_HEIGHT - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 5;

        graphics.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);

        int currentY = (int) (gridY - scrollbar.getScrollOffset());

        for (int i = 0; i < filteredList.size(); i++) {
            IDivinationCategory.TargetEntry entry = filteredList.get(i);
            int col = i % ITEM_COLS;
            int row = i / ITEM_COLS;

            int x = gridX + col * (SLOT_SIZE + SLOT_PADDING);
            int y = currentY + row * (SLOT_SIZE + SLOT_PADDING);

            if (y + SLOT_SIZE > gridY && y < gridY + gridH) {
                boolean isHovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
                boolean isSelected = (entry == selectedEntry);

                if (isSelected) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LotMClientColors.LIST_ITEM_BG_SELECTED);
                    graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, LotMClientColors.LIST_ITEM_BORDER_SELECTED);
                } else if (isHovered) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LotMClientColors.LIST_ITEM_BG_HOVER);
                }

                graphics.renderItem(entry.icon(), x + 2, y + 2);
            }
        }
        graphics.disableScissor();

        this.scrollbar.render(graphics, mouseX, mouseY);

        if (mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH) {
            int relY = (int) (mouseY - gridY + scrollbar.getScrollOffset());
            int col = (mouseX - gridX) / (SLOT_SIZE + SLOT_PADDING);
            int row = relY / (SLOT_SIZE + SLOT_PADDING);
            int index = row * ITEM_COLS + col;

            if (index >= 0 && index < filteredList.size()) {
                graphics.renderTooltip(this.font, filteredList.get(index).name(), mouseX, mouseY);
            }
        }
    }

    // ==================== 交互事件处理 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (state == ScreenState.ITEM_SELECT && button == 0) {
            if (this.scrollbar.mouseClicked(mouseX, mouseY, button)) return true;

            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int startX = centerX - GUI_WIDTH / 2;
            int startY = centerY - GUI_HEIGHT / 2;
            int gridX = startX + PADDING;
            int gridY = startY + TOP_BAR_HEIGHT;
            int gridW = GUI_WIDTH - PADDING * 2;
            int gridH = GUI_HEIGHT - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 5;

            if (mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH) {
                int relY = (int) (mouseY - gridY + scrollbar.getScrollOffset());
                int col = (int) ((mouseX - gridX) / (SLOT_SIZE + SLOT_PADDING));
                int row = relY / (SLOT_SIZE + SLOT_PADDING);
                int index = row * ITEM_COLS + col;

                if (index >= 0 && index < filteredList.size()) {
                    this.selectedEntry = filteredList.get(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (state == ScreenState.ITEM_SELECT && this.scrollbar != null) {
            if (this.scrollbar.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (state == ScreenState.ITEM_SELECT && this.scrollbar != null) {
            if (this.scrollbar.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (state == ScreenState.ITEM_SELECT && this.scrollbar != null) {
            int startX = (this.width - GUI_WIDTH) / 2;
            int startY = (this.height - GUI_HEIGHT) / 2;
            int gridY = startY + TOP_BAR_HEIGHT;
            int gridH = GUI_HEIGHT - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 5;

            if (mouseX >= startX && mouseX <= startX + GUI_WIDTH && mouseY >= gridY && mouseY <= gridY + gridH) {
                return this.scrollbar.mouseScrolled(mouseX, mouseY, delta);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (state == ScreenState.ITEM_SELECT && this.searchBox != null && this.searchBox.isFocused()) {
            boolean res = this.searchBox.charTyped(codePoint, modifiers);
            if (res) updateFilteredList();
            return res;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state == ScreenState.ITEM_SELECT && this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            boolean res = this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            if (res) updateFilteredList();
            return res;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && state == ScreenState.ITEM_SELECT) {
            this.state = ScreenState.CATEGORY_SELECT;
            this.currentCategory = null;
            this.init();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
