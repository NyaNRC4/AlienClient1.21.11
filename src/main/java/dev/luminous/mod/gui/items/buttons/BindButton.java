package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindButton extends Button {
    public static boolean ANY_LISTENING = false;
    public static BindButton ACTIVE_BIND = null;

    private final BindSetting setting;
    public boolean isListening;

    public BindButton(BindSetting setting) {
        super(setting.getName());
        this.setting = setting;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        Color color = ClickGui.getInstance().color.getValue();
        Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float) this.width + 7.0f, this.y + (float) this.height - 0.5f, this.getState() ? (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor) : (!this.isHovering(mouseX, mouseY) ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB() : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()));
        if (this.isListening) {
            drawString("Key ...", this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), enableTextColor);
        } else {
            String str = this.setting.getKeyString();
            if (!this.isListening && this.isHovering(mouseX, mouseY) && InputUtil.isKeyPressed(mc.getWindow(), 340) && this.setting.getName().equals("Key")) {
                if (setting.isHoldEnable()) {
                    drawString("§7Toggle/§fHold", this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), enableTextColor);
                } else {
                    drawString("§fToggle§7/Hold", this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), enableTextColor);
                }
            } else {
                drawString(this.setting.getName() + " " + Formatting.GRAY + str, this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor);
            }
        }
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            if (InputUtil.isKeyPressed(mc.getWindow(), 340) && this.setting.getName().equals("Key")) {
                setting.setHoldEnable(!setting.isHoldEnable());
                sound();
            } else {
                onMouseClick();
            }
        } else {
            if (this.isListening) {
                this.setting.setValue(-mouseButton - 2);
                this.onMouseClick();
            }
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (this.isListening) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                this.setting.setValue(-1);
                toggle();
                return;
            }
            this.setting.setValue(key);
            if (setting.getKeyString().equalsIgnoreCase("DELETE")) {
                setting.setValue(-1);
            }
            this.onMouseClick();
        }
    }

    @Override
    public void toggle() {
        if (ACTIVE_BIND != null && ACTIVE_BIND != this) {
            ACTIVE_BIND.isListening = false;
        }

        this.isListening = !this.isListening;

        if (this.isListening) {
            ACTIVE_BIND = this;
            ANY_LISTENING = true;
        } else {
            ACTIVE_BIND = null;
            ANY_LISTENING = false;
        }
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }
}
