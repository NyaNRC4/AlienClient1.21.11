package dev.luminous.mod.gui.items;

import dev.luminous.Alien;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.buttons.Button;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Component
        extends Mod {
    private final List<ModuleButton> items = new ArrayList<>();
    private final Module.Category category;
    public boolean drag;
    protected DrawContext context;
    private int x;
    private int y;
    private int x2;
    private int y2;
    private int width;
    private int height;
    private boolean open;
    private boolean hidden = false;

    public Component(String name, Module.Category category, int x, int y, boolean open) {
        super(name);
        this.category = category;
        setX(x);
        setY(y);
        setWidth(93);
        setHeight(18);
        this.open = open;
        this.setupItems();
    }

    public void setupItems() {
    }

    private void drag(int mouseX, int mouseY) {
        if (!this.drag) {
            return;
        }
        this.x = this.x2 + mouseX;
        this.y = this.y2 + mouseY;
    }

    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        FontManager.ensureInitialized();
        this.context = context;
        this.drag(mouseX, mouseY);

        float totalItemHeight = this.open ? this.getTotalItemHeight() - 2.0f : 0.0f;
        int color = ColorUtil.injectAlpha(ClickGui.getInstance().topColor.getValue().getRGB(), ClickGui.getInstance().topAlpha.getValueInt());
        int lineColor = ColorUtil.injectAlpha(ClickGui.getInstance().color.getValue().getRGB(), ClickGui.getInstance().topAlpha.getValueInt());

        Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + this.width, this.y + this.height - 5f, color);

        if (ClickGui.getInstance().line.getValue()) {
            //top
            Render2DUtil.drawLine(context.getMatrices(), x, y, x + width, y, lineColor);
            //left
            Render2DUtil.drawLine(context.getMatrices(), x + 0.2f, y, x, y + height - 5f, lineColor);
            //right
            Render2DUtil.drawLine(context.getMatrices(), x + width, y, x + width, y + height - 5f, lineColor);
        }

        if (this.open) {
            if (ClickGui.getInstance().blur.getValue()) {
                Alien.BLUR.applyBlur(1 + (ClickGui.getInstance().radius.getValueFloat() - 1) * (float) ClickGui.getInstance().alphaValue, this.x, (float) this.y + height - 5f, this.width, totalItemHeight + 5f);
            }
            if (ClickGui.getInstance().backGround.booleanValue) {
                Render2DUtil.rect(context.getMatrices(), this.x, (float) this.y + height - 5f, this.x + this.width, (float) (this.y + this.height) + totalItemHeight, ClickGui.getInstance().backGround.getValue().getRGB());
            }
            if (ClickGui.getInstance().line.getValue()) {
                //origin line
                Render2DUtil.drawLine(context.getMatrices(), x + 0.2f, (this.y + this.height) + totalItemHeight, this.x + 0.2f, (float) this.y + height - 5f, lineColor);
                Render2DUtil.drawLine(context.getMatrices(), x + this.width, (this.y + this.height) + totalItemHeight, this.x + this.width, (float) this.y + height - 5f, lineColor);
                Render2DUtil.drawLine(context.getMatrices(), x, (this.y + this.height) + totalItemHeight, this.x + this.width, (this.y + this.height) + totalItemHeight, lineColor);
            }
        }

        if (FontManager.icon != null) {
            FontManager.icon.drawString(context.getMatrices(), category.getIcon(), (float) this.x + 3.0f, (float) this.y + 4, Button.enableTextColor);
        }

        drawString(this.getName(), (float) this.x + 13.0f, (float) this.y - 1.0f - ( -ClickGui.getInstance().titleOffset.getValueInt() - 6), Button.enableTextColor);
        if (this.open) {
            float y = (float) (this.getY() + this.getHeight()) - 3.0f;
            for (ModuleButton item : this.getItems()) {
                if (item.isHidden()) continue;
                item.setLocation((float) this.x + 2.0f, y);
                item.setWidth(this.getWidth() - 4);
                if (item.itemHeight > 0 || item.subOpen) {
                    context.enableScissor((int) item.x, (int) item.y, mc.getWindow().getScaledWidth(), (int) (y + item.getButtonHeight() + 1.5f + item.itemHeight));
                    item.drawScreen(context, mouseX, mouseY, partialTicks);
                    context.disableScissor();
                } else {
                    item.drawScreen(context, mouseX, mouseY, partialTicks);
                }
                y += (float) item.getButtonHeight() + 1.5f + (float) item.itemHeight;
            }
        }
    }


    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.x2 = this.x - mouseX;
            this.y2 = this.y - mouseY;
            ClickGuiScreen.getInstance().getComponents().forEach(component -> {
                if (component.drag) {
                    component.drag = false;
                }
            });
            this.drag = true;
            return;
        }
        if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            Item.sound();
            return;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            this.drag = false;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseReleased(mouseX, mouseY, releaseButton));
    }

    public void onKeyTyped(char typedChar, int keyCode) {
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.onKeyTyped(typedChar, keyCode));
    }

    public void onKeyPressed(int key) {
        if (!open) return;
        this.getItems().forEach(item -> item.onKeyPressed(key));
    }

    public void addButton(ModuleButton button) {
        this.items.add(button);
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isOpen() {
        return this.open;
    }

    public final List<ModuleButton> getItems() {
        return this.items;
    }

    private boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight() - 5;
    }

    private float getTotalItemHeight() {
        float height = 0.0f;
        for (ModuleButton item : this.getItems()) {
            item.update();
            item.itemHeight = item.animation.get(item.subOpen ? item.getItemHeight() : 0, 200, Easing.CubicInOut);
            height += (float) item.getButtonHeight() + 1.5f + (float) item.itemHeight;
        }
        return height;
    }

    protected void drawString(String text, double x, double y, Color color) {
        drawString(text, x, y, color.hashCode());
    }

    protected void drawString(String text, double x, double y, int color) {
        if (ClickGui.getInstance().font.getValue()) {
            if (FontManager.ui != null) {
                FontManager.ui.drawString(context.getMatrices(), text, (int) x, (int) y, color, ClickGui.getInstance().shadow.getValue());
            }
        } else {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, ClickGuiScreen.applyAlpha(color), ClickGui.getInstance().shadow.getValue());
        }
    }
}
