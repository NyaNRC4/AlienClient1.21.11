package dev.luminous.mod.gui.items.buttons;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton
        extends Button {
    private final Module module;
    private List<Item> items = new ArrayList<>();
    public boolean subOpen;
    public double itemHeight;
    public final Animation animation = new Animation();

    public ModuleButton(Module module) {
        super(module.getName());
        this.module = module;
        this.initSettings();
    }

    public void initSettings() {
        ArrayList<Item> newItems = new ArrayList<>();
        for (Setting setting : this.module.getSettings()) {
            if (setting instanceof BooleanSetting s) {
                newItems.add(new BooleanButton(s));
            }
            if (setting instanceof BindSetting s) {
                newItems.add(new BindButton(s));
            }
            if (setting instanceof StringSetting s) {
                newItems.add(new StringButton(s));
            }
            if (setting instanceof SliderSetting s) {
                newItems.add(new SliderButton(s));
            }
            if (setting instanceof EnumSetting<?> s) {
                newItems.add(new EnumButton(s));
            }
            if (setting instanceof ColorSetting s) {
                newItems.add(new PickerButton(s));
            }
        }
        //newItems.add(new BindButton(this.module.getBind()));
        this.items = newItems;
    }

    @Override
    public void update() {
        for (Item item : this.items) {
            item.update();
        }
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        Color color = ClickGui.getInstance().color.getValue();
        int rgba = this.getState() ? (!this.isHovering(mouseX, mouseY) ? ColorUtil.injectAlpha(color, ClickGui.getInstance().alpha.getValueInt()).getRGB() : ColorUtil.injectAlpha(color, ClickGui.getInstance().hoverAlpha.getValueInt()).getRGB()) : (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor);
        Render2DUtil.rect(context.getMatrices(), this.x, this.y, this.x + (float) this.width, this.y + (float) this.height - 0.5f, rgba);
        drawString(module.getDisplayName(), this.x + 2.3f, this.y - 2.0f - (float) ClickGuiScreen.getInstance().getTextOffset(), this.getState() ? enableTextColor : defaultTextColor);
        if (ClickGui.getInstance().gear.booleanValue) {
            drawString(subOpen ? "-" : "+", this.x + width - 8f, this.y - 1.7f - (float) ClickGuiScreen.getInstance().getTextOffset(), ClickGui.getInstance().gear.getValue().getRGB());
        }

        double animatedHeight = this.itemHeight;
        if (animatedHeight > 0) {
            float startY = this.y + this.height;
            context.enableScissor(
                    (int) this.x,
                    (int) startY,
                    (int) (this.x + this.width),
                    (int) (startY + animatedHeight)
            );

            if (ClickGui.getInstance().line.getValue()) {
                Render2DUtil.drawLine(context.getMatrices(), x + 0.2f, (float) (startY + animatedHeight - .5f), this.x + 0.2f, this.y + height - .5f, rgba);
                Render2DUtil.drawLine(context.getMatrices(), x + this.width, (float) (startY + animatedHeight - .5f), this.x + this.width, this.y + height - .5f, rgba);
                Render2DUtil.drawLine(context.getMatrices(), x + 0.3f, (float) (startY + animatedHeight - .5f), this.x + this.width - 0.2f, (float) (startY + animatedHeight - .7f), rgba);
            }

            float currentY = startY + 1;
            for (Item item : this.items) {
                if (!item.isHidden()) {
                    if (currentY > startY + animatedHeight) break;

                    item.setLocation(this.x + 1.0f, currentY);
                    item.setWidth(this.width - 9);
                    item.drawScreen(context, mouseX, mouseY, partialTicks);
                    currentY += item.getHeight() + 1;
                }
            }
            context.disableScissor();
        }
    }


    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!this.items.isEmpty()) {
            if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
                this.subOpen = !this.subOpen;
                sound();
            }
            if (this.subOpen) {
                for (Item item : this.items) {
                    if (item.isHidden()) continue;
                    item.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void onKeyTyped(char typedChar, int keyCode) {
        super.onKeyTyped(typedChar, keyCode);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.onKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onKeyPressed(int key) {
        super.onKeyPressed(key);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.onKeyPressed(key);
            }
        }
    }

    public int getButtonHeight() {
        return super.getHeight();
    }

    public int getItemHeight() {
        int height = 2;
        for (Item item : this.items) {
            if (item.isHidden()) continue;
            height += item.getHeight() + 1;
        }
        return height;
    }

    @Override
    public int getHeight() {
        if (this.subOpen) {
            int height = super.getHeight();
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                height += item.getHeight() + 1;
            }
            return height + 2;
        }
        return super.getHeight();
    }

    public Module getModule() {
        return this.module;
    }

    @Override
    public void toggle() {
        this.module.toggle();
    }

    @Override
    public boolean getState() {
        return this.module.isOn();
    }
}