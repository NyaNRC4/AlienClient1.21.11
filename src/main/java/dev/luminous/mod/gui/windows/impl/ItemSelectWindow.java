package dev.luminous.mod.gui.windows.impl;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.Manager;
import dev.luminous.core.impl.*;
import dev.luminous.mod.gui.items.buttons.StringButton;
import dev.luminous.mod.gui.windows.WindowBase;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.StringHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

import static dev.luminous.api.utils.Wrapper.mc;

public class ItemSelectWindow extends WindowBase {
    private final Manager manager;
    private final ArrayList<ItemPlate> itemPlates = new ArrayList<>();
    private final ArrayList<ItemPlate> allItems = new ArrayList<>();

    private boolean allTab = true, listening = false;
    private String search = "Search";

    public ItemSelectWindow(Manager manager) {
        this(mc.getWindow().getScaledWidth() / 2f - 100, mc.getWindow().getScaledHeight() / 2f - 150, 200, 300, manager);
    }

    public ItemSelectWindow(float x, float y, float width, float height, Manager manager) {
        super(x, y, width, height, "Items", null);
        this.manager = manager;
        refreshItemPlates();

        int id1 = 0;
        for (Block block : Registries.BLOCK) {
            allItems.add(new ItemPlate(id1, id1 * 20, block.asItem(), block.getTranslationKey()));
            id1++;
        }

        for (Item item : Registries.ITEM) {
            allItems.add(new ItemPlate(id1, id1 * 20, item, item.getTranslationKey()));
            id1++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        super.render(context, mouseX, mouseY);
        boolean hover1 = Render2DUtil.isHovered(mouseX, mouseY, getX() + getWidth() - 90, getY() + 3, 70, 10);

        Render2DUtil.drawRect(context.getMatrices(), getX() + getWidth() - 90, getY() + 3, 70, 10, hover1 ? new Color(0xC5838383, true) : new Color(0xC5575757, true));
        FontManager.small.drawString(context.getMatrices(), search, getX() + getWidth() - 86, getY() + 7, new Color(0xD5D5D5).getRGB());
        int tabColor1 = allTab ? new Color(0xD5D5D5).getRGB() : Color.GRAY.getRGB();
        int tabColor2 = allTab ? Color.GRAY.getRGB() : new Color(0xBDBDBD).getRGB();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(getX() + 1.5f, getY() + 29, 0f).color(Color.DARK_GRAY.getRGB());
        bufferBuilder.vertex(getX() + 8, getY() + 29, 0f).color(tabColor1);
        bufferBuilder.vertex(getX() + 8, getY() + 19, 0f).color(tabColor1);
        bufferBuilder.vertex(getX() + 48, getY() + 19, 0f).color(tabColor1);
        bufferBuilder.vertex(getX() + 54, getY() + 29, 0f).color(tabColor1);
        bufferBuilder.vertex(getX() + 52, getY() + 25, 0f).color(tabColor2);
        bufferBuilder.vertex(getX() + 52, getY() + 19, 0f).color(tabColor2);
        bufferBuilder.vertex(getX() + 92, getY() + 19, 0f).color(tabColor2);
        bufferBuilder.vertex(getX() + 100, getY() + 29, 0f).color(Color.GRAY.getRGB());
        bufferBuilder.vertex(getX() + getWidth() - 1, getY() + 29, 0f).color(Color.DARK_GRAY.getRGB());
        dev.luminous.api.utils.render.ImmediateDrawUtil.drawGui(bufferBuilder);

        FontManager.small.drawString(context.getMatrices(), "All", getX() + 25, getY() + 25, tabColor1);
        FontManager.small.drawString(context.getMatrices(), "Selected", getX() + 60, getY() + 25, tabColor2);

        if (!allTab && itemPlates.isEmpty()) {
            FontManager.ui.drawCenteredString(context.getMatrices(), "It's empty here yet",
                    getX() + getWidth() / 2f, getY() + getHeight() / 2f, new Color(0xBDBDBD).getRGB());
        }
        context.enableScissor((int) getX(), (int) (getY() + 30), (int) (getX() + getWidth()), (int) (getY() + getHeight() - 1));

        for (ItemPlate itemPlate : (allTab ? allItems : itemPlates)) {
            if (itemPlate.offset + getY() + 25 + getScrollOffset() > getY() + getHeight() || itemPlate.offset + getScrollOffset() + getY() + 10 < getY())
                continue;

            context.drawItem(itemPlate.item().getDefaultStack(), (int)this.getX() + 6, (int)(itemPlate.offset + this.getY() + 32.0f + this.getScrollOffset()));

            FontManager.ui.drawString(context.getMatrices(), I18n.translate(itemPlate.key()), getX() + 26, itemPlate.offset + getY() + 38 + getScrollOffset(), new Color(0xBDBDBD).getRGB());

            boolean hover2 = Render2DUtil.isHovered(mouseX, mouseY, getX() + getWidth() - 20, itemPlate.offset + getY() + 35 + getScrollOffset(), 11, 11);

            Render2DUtil.drawRect(context.getMatrices(), getX() + getWidth() - 20, itemPlate.offset + getY() + 35 + getScrollOffset(), 11, 11,
                    hover2 ? new Color(0xC57A7A7A, true) : new Color(0xC5575757, true));

            boolean selected = itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key, itemPlate.key));

            if (allTab && !selected) {
                FontManager.ui.drawString(context.getMatrices(), "+", getX() + getWidth() - 17, itemPlate.offset + getY() + 37 + getScrollOffset(), -1);
            } else {
                FontManager.ui.drawString(context.getMatrices(), "-", getX() + getWidth() - 16.5, itemPlate.offset + getY() + 37.5 + getScrollOffset(), -1);
            }
        }
        setMaxElementsHeight((allTab ? allItems : itemPlates).size() * 20);
        context.disableScissor();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (Render2DUtil.isHovered(mouseX, mouseY, getX() + getWidth() - 90, getY() + 3, 70, 10)) {
            listening = true;
            search = "";
            refreshAllItems();
            return;
        }

        super.mouseClicked(mouseX, mouseY, button);

        if (!isVisible()) {
            return;
        }

        if (listening) {
            listening = false;
            if (search.isEmpty()) {
                search = "Search";
            }
            refreshAllItems();
        }

        if (Render2DUtil.isHovered(mouseX, mouseY, getX() + 8, getY() + 19, 44, 19)) {
            allTab = true;
            resetScroll();
        }

        if (Render2DUtil.isHovered(mouseX, mouseY, getX() + 54, getY() + 19, 46, 19)) {
            allTab = false;
            resetScroll();
        }

        ArrayList<ItemPlate> copy = Lists.newArrayList(allTab ? allItems : itemPlates);
        for (ItemPlate itemPlate : copy) {
            float rowY = itemPlate.offset + getY() + 35 + getScrollOffset();
            if (rowY < getY() + 30 || rowY + 10 > getY() + getHeight())
                continue;

            String name = itemPlate.key().replace("item.minecraft.", "").replace("block.minecraft.", "");

            if (Render2DUtil.isHovered(mouseX, mouseY, getX() + getWidth() - 20, rowY, 11, 11)) {
                boolean selected = itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key(), itemPlate.key));

                if (allTab && !selected) {
                    if (manager instanceof TradeManager m) {
                        if (!m.inWhitelist(name)) {
                            m.add(name);
                            refreshItemPlates();
                        }
                    } else if (manager instanceof CleanerManager m) {
                        if (!m.inList(name)) {
                            m.add(name);
                            refreshItemPlates();
                        }
                    } else if (manager instanceof XrayManager m) {
                        if (!m.inWhitelist(name)) {
                            m.add(name);
                            refreshItemPlates();
                        }
                    }
                } else {
                    if (manager instanceof TradeManager m) {
                        m.remove(name);
                    } else if (manager instanceof CleanerManager m) {
                        m.remove(name);
                    } else if (manager instanceof XrayManager m) {
                        m.remove(name);
                    }
                    refreshItemPlates();
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean altDown = (modifiers & (GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL)) != 0
                || InputUtil.isKeyPressed(Wrapper.mc.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(Wrapper.mc.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT)
                || InputUtil.isKeyPressed(Wrapper.mc.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(Wrapper.mc.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);

        if (keyCode == GLFW.GLFW_KEY_F && altDown) {
            listening = !listening;
            if (listening) {
                search = "";
                refreshAllItems();
            }
            return true;
        }

        if (!listening) {
            return false;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                listening = false;
                if (search.isEmpty()) {
                    search = "Search";
                }
                refreshAllItems();
            }

            case GLFW.GLFW_KEY_BACKSPACE -> {
                search = StringButton.removeLastChar(search);
                refreshAllItems();

                if (search.isEmpty()) {
                    listening = false;
                    search = "Search";
                }
            }

            case GLFW.GLFW_KEY_SPACE -> {
                if (search.equals("Search")) {
                    search = "";
                }
                search = search + " ";
                refreshAllItems();
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char key, int keyCode) {
        if (listening && StringHelper.isValidChar(key)) {
            if (search.equals("Search")) {
                search = "";
            }
            search = search + key;
            refreshAllItems();
            return true;
        }
        return false;
    }

    private void refreshItemPlates() {
        itemPlates.clear();

        int id = 0;

        for (Item item : Registries.ITEM)
            if (manager instanceof TradeManager m) {
                if (m.inWhitelist(item.getTranslationKey())) {
                    itemPlates.add(new ItemPlate(id, id * 20, item.asItem(), item.getTranslationKey()));
                    id++;
                }
            } else if (manager instanceof CleanerManager m) {
                if (m.inList(item.getTranslationKey())) {
                    itemPlates.add(new ItemPlate(id, id * 20, item.asItem(), item.getTranslationKey()));
                    id++;
                }
            } else if (manager instanceof XrayManager m) {
                if (m.inWhitelist(item.getTranslationKey())) {
                    itemPlates.add(new ItemPlate(id, id * 20, item.asItem(), item.getTranslationKey()));
                    id++;
                }
            }
    }

    private void refreshAllItems() {
        allItems.clear();
        resetScroll();
        int id1 = 0;

        for (Item item : Registries.ITEM) {
            if (search.equals("Search") || search.isEmpty() || item.getTranslationKey().contains(search) || item.getName().getString().toLowerCase().contains(search.toLowerCase())) {
                allItems.add(new ItemPlate(id1, id1 * 20, item, item.getTranslationKey()));
                id1++;
            }
        }
    }

    private record ItemPlate(float id, float offset, Item item, String key) {
    }
}