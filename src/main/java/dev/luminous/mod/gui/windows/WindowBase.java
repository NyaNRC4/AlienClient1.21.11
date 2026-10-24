package dev.luminous.mod.gui.windows;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.math.AnimateUtil;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ColorsModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import java.awt.*;

public class WindowBase {

    private final String name;
    private final Identifier icon;
    private float x;
    private float y;
    private float width;
    private float height;
    private float dragX;
    private float dragY;
    private float scrollOffset;
    private float prevScrollOffset;
    private float maxElementsHeight;
    private boolean dragging, hoveringWindow, scaling, scrolling;
    private boolean visible = true;

    protected WindowBase(float x, float y, float width, float height, String name, Identifier icon) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        this.name = name;
        this.icon = icon;
    }

    protected void render(DrawContext context, int mouseX, int mouseY) {
        this.prevScrollOffset = AnimateUtil.fast(this.prevScrollOffset, this.scrollOffset, 12.0f);
        Color color2 = new Color(-983868581, true);
        GlStateManager._enableBlend();
        Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, this.width + 10.0f, this.height, -1072689136);
        Render2DUtil.drawRect(context.getMatrices(), this.x + 0.5f, this.y, this.width + 9.0f, 16.0f, new Color(0x5F000000, true));
        Render2DUtil.horizontalGradient(context.getMatrices(), this.x + 2.0f, this.y + 16.0f, this.x + 2.0f + this.width / 2.0f - 2.0f, this.y + 16.5f, ColorUtil.injectAlpha(ColorsModule.INSTANCE.clientColor.getValue(), 0), ColorsModule.INSTANCE.clientColor.getValue());
        Render2DUtil.horizontalGradient(context.getMatrices(), this.x + 2.0f + this.width / 2.0f - 2.0f, this.y + 16.0f, this.x + 2.0f + this.width - 4.0f, this.y + 16.5f, ColorsModule.INSTANCE.clientColor.getValue(), ColorUtil.injectAlpha(ColorsModule.INSTANCE.clientColor.getValue(), 0));
        FontManager.ui.drawString(context.getMatrices(), this.name, (double)(this.x + 4.0f), (double)(this.y + 5.5f), -1);
        boolean hover1 = Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width - 4.0f, this.y + 3.0f, 10.0, 10.0);
        Render2DUtil.drawRectWithOutline(context.getMatrices(), this.x + this.width - 4.0f, this.y + 3.0f, 10.0f, 10.0f, hover1 ? new Color(-982026377, true) : new Color(-984131753, true), color2);
        float ratio = this.maxElementsHeight <= 0.0f ? 1.0f : (this.getHeight() - 35.0f) / this.maxElementsHeight;
        boolean hover2 = Render2DUtil.isHovered(mouseX, mouseY, this.x + this.width, this.y + 19.0f, 6.0, this.getHeight() - 34.0f);
        Render2DUtil.drawRectWithOutline(context.getMatrices(), this.x + this.width, this.y + 19.0f, 6.0f, this.getHeight() - 34.0f, hover2 ? new Color(1595085587, true) : new Color(0x5F000000, true), color2);
        Render2DUtil.drawRect(context.getMatrices(), this.x + this.width, Math.max(this.y + 19.0f - this.scrollOffset * ratio, this.y + 19.0f), 6.0f, Math.min((this.getHeight() - 34.0f) * ratio, this.getHeight() - 34.0f), new Color(-1590611663, true));
        Render2DUtil.drawLine(context.getMatrices(), this.x + this.width - 2.0f, this.y + 5.0f, this.x + this.width + 4.0f, this.y + 11.0f, -1);
        Render2DUtil.drawLine(context.getMatrices(), this.x + this.width - 2.0f, this.y + 11.0f, this.x + this.width + 4.0f, this.y + 5.0f, -1);
        GlStateManager._disableBlend();
        if (this.scrolling) {
            float diff = ((float)mouseY - this.y - 19.0f) / (this.getHeight() - 34.0f);
            this.scrollOffset = -(diff * this.maxElementsHeight);
            float maxScroll = Math.max(0.0f, this.maxElementsHeight - (this.getHeight() - 40.0f));
            this.scrollOffset = MathUtil.clamp(this.scrollOffset, -maxScroll, 0.0f);
        }
        this.hoveringWindow = Render2DUtil.isHovered(mouseX, mouseY, this.getX(), this.getY(), this.getWidth() + 10.0f, this.getHeight());
        Render2DUtil.drawLine(context.getMatrices(), this.getX() + this.getWidth(), this.getY() + this.getHeight() - 3.0f, this.getX() + this.getWidth() + 7.0f, this.getY() + this.getHeight() - 10.0f, color2.getRGB());
        Render2DUtil.drawLine(context.getMatrices(), this.getX() + this.getWidth() + 5.0f, this.getY() + this.getHeight() - 3.0f, this.getX() + this.getWidth() + 7.0f, this.getY() + this.getHeight() - 5.0f, color2.getRGB());
    }

    protected void mouseClicked(double mouseX, double mouseY, int button) {

        if (Render2DUtil.isHovered(mouseX, mouseY, x + width - 4, y + 3, 10, 10)) {
            setVisible(false);
            return;
        }

        if (Render2DUtil.isHovered(mouseX, mouseY, x, y, width, 16)) {
            if (WindowsScreen.draggingWindow == null)
                dragging = true;

            if (WindowsScreen.draggingWindow == null)
                WindowsScreen.draggingWindow = this;

            WindowsScreen.lastClickedWindow = this;
            dragX = (int) (mouseX - getX());
            dragY = (int) (mouseY - getY());
            return;
        }

        if (Render2DUtil.isHovered(mouseX, mouseY, x + width, y + height - 10, 10, 10)) {
            WindowsScreen.lastClickedWindow = this;
            dragX = (int) (mouseX - getWidth());
            dragY = (int) (mouseY - getHeight());
            scaling = true;
            return;
        }

        if (Render2DUtil.isHovered(mouseX, mouseY, x + width, y + 19, 6, getHeight() - 34)) {
            WindowsScreen.lastClickedWindow = this;
            dragX = (int) (mouseX - getWidth());
            dragY = (int) (mouseY - getHeight());
            scrolling = true;
        }
    }

    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    protected boolean charTyped(char key, int keyCode) {
        return false;
    }

    protected void mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        if (dragging) {
            setX((float) mouseX - dragX);
            setY((float) mouseY - dragY);
        } else if (scaling) {
            setWidth(Math.max(60.0f, (float) mouseX - dragX));
            setHeight(Math.max(40.0f, (float) mouseY - dragY));
        }
    }

    protected void mouseScrolled(int i) {
        if (hoveringWindow) {
            scrollOffset += i * 2;
            float maxScroll = Math.max(0.0f, maxElementsHeight - (getHeight() - 40.0f));
            scrollOffset = MathUtil.clamp(scrollOffset, -maxScroll, 0.0f);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        scaling = false;
        scrolling = false;
        WindowsScreen.draggingWindow = null;
    }

    protected float getX() {
        return x;
    }

    protected void setX(float x) {
        this.x = x;
    }

    protected float getY() {
        return y;
    }

    protected void setY(float y) {
        this.y = y;
    }

    protected float getWidth() {
        return width;
    }

    protected void setWidth(float width) {
        this.width = width;
    }

    protected float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    protected float getScrollOffset() {
        return prevScrollOffset;
    }

    protected void resetScroll() {
        prevScrollOffset = 0;
        scrollOffset = 0;
    }

    protected void setMaxElementsHeight(float maxElementsHeight) {
        this.maxElementsHeight = maxElementsHeight;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Identifier getIcon() {
        return icon;
    }
}
