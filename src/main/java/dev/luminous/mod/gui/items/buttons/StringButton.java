package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class StringButton
        extends Button {
    private static final Timer idleTimer = new Timer();
    private static boolean idle;
    private final StringSetting setting;
    public boolean isListening;
    private String currentString = "";

    public StringButton(StringSetting setting) {
        super(setting.getName());
        this.setting = setting;
    }

    public static String removeLastChar(String str) {
        String output = "";
        if (str != null && !str.isEmpty()) {
            output = str.substring(0, str.length() - 1);
        }
        return output;
    }

    public static String getIdleSign() {
        if (idleTimer.passed(500)) {
            idle = !idle;
            idleTimer.reset();
        }
        if (idle) return "_";
        return "";
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        Color color = ClickGui.getInstance().color.getValue();
        Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float) this.width + 7.0f, this.y + (float) this.height - 0.5f, this.getState() ? (!this.isHovering(mouseX, mouseY) ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB() : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()) : (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor));
        if (this.isListening) {
            drawString(this.currentString + getIdleSign(), this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor);
        } else {
            drawString(this.setting.getName() + ": " + Formatting.GRAY + this.setting.getValue(), this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onKeyTyped(char typedChar, int keyCode) {
        if (this.isListening) {
            //if (SharedConstants.isValidChar(typedChar)) {
            this.setString(this.currentString + typedChar);
            //}
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (this.isListening) {
            switch (key) {
                case 256: {
                    this.isListening = false;
                    break;
                }
                case 257:
                case 335: {
                    this.enterString();
                    break;
                }
                case 86: {
                    if (!InputUtil.isKeyPressed(mc.getWindow(), 341)) break;
                    this.setString(this.currentString + SelectionManager.getClipboard((MinecraftClient)mc));
                    break;
                }
                case 259: {
                    this.setString(StringButton.removeLastChar(this.currentString));
                }
            }
        }
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    private void enterString() {
        if (this.currentString.isEmpty()) {
            this.setting.setValue(this.setting.getDefaultValue());
        } else {
            this.setting.setValue(this.currentString);
        }
        this.onMouseClick();
    }

    @Override
    public void toggle() {
        this.setString(setting.getValue());
        this.isListening = !this.isListening;
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }

    public void setString(String newString) {
        this.currentString = newString;
    }
}