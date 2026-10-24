package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;

public class EnumButton
        extends Button {
    public final EnumSetting<?> setting;

    private boolean open;

    public EnumButton(EnumSetting<?> setting) {
        super(setting.getName());
        this.setting = setting;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        Color color = ClickGui.getInstance().color.getValue();
        Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float) this.width + 7.0f, this.y + (float) this.height - 0.5f, this.getState() ? (!this.isHovering(mouseX, mouseY) ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB() : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()) : (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor));
        drawString(this.setting.getName() + " " + Formatting.GRAY + (this.setting.getValue().name().equalsIgnoreCase("ABC") ? "ABC" : this.setting.getValue().name()), this.x + 2.3f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor);

        if (open) {
            int y = (int) this.y;
            for (Enum<?> e : setting.getValue().getDeclaringClass().getEnumConstants()) {

                String s = e.name();

                drawString(s, width / 2.0f - getWidth(s) / 2.0f + 2.0f + x, y + (float) this.height - 3 - (float) ClickGuiScreen.getInstance().getTextOffset(), (setting.getValue().name().equals(s) ? enableTextColor : defaultTextColor));
                y += 11;
            }
        }
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 1 && isHovering(mouseX, mouseY)) {
            open = !open;
            sound();
        } else if (mouseButton == 0 && open) {
            int y = (int) this.y;

            for (Object o : setting.getValue().getDeclaringClass().getEnumConstants()) {
                if (mouseX > x && mouseX < x + width && mouseY >= y + height + 1 && mouseY < y + height + 11 + 1) {
                    setting.setEnumValue(String.valueOf(o));
                    sound();
                }
                y += 11;
            }
        }
    }

    @Override
    public int getHeight() {
        return super.getHeight() + (open ? 11 * this.setting.getValue().getDeclaringClass().getEnumConstants().length : 0);
    }

    @Override
    public void toggle() {
        this.setting.increaseEnum();
    }

    @Override
    public boolean getState() {
        return true;
    }
}