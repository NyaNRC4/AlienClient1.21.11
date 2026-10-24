package dev.luminous.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.ImmediateDrawUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.BindButton;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
    private final ArrayList<Component> components = new ArrayList();
    private static float renderingAlpha = 1.0F;

    public ClickGuiScreen() {
        super((Text) Text.literal((String) "Alien"));
        this.setInstance();
        this.load();
    }

    public static ClickGuiScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClickGuiScreen();
        }
        return INSTANCE;
    }

    private void setInstance() {
        INSTANCE = this;
    }

    private void load() {
        int x = -84;
        for (final Module.Category category : Module.Category.values()) {
            this.components.add(new Component(category.toString(), category, x += 94, 4, true) {
                @Override
                public void setupItems() {
                    for (Module module : Alien.MODULE.getModules()) {
                        if (!module.getCategory().equals((Object) category)) continue;
                        this.addButton(new ModuleButton(module));
                    }
                }
            });
        }
        this.components.forEach(components -> components.getItems().sort(Comparator.comparing(Mod::getName)));
    }
    public static int applyAlpha(int color) {
        if (renderingAlpha >= 0.999F) {
            return color;
        }
        int alpha = Math.round((float) (color >>> 24 & 0xFF) * renderingAlpha);
        return color & 0x00FFFFFF | Math.max(0, Math.min(255, alpha)) << 24;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        flushDeferredGui();
        float alpha = (float) ClickGui.getInstance().alphaValue;
        Item.context = context;
        renderingAlpha = alpha;
        ImmediateDrawUtil.setColorModulatorAlpha(alpha);
        try {
            if (alpha > 0.01F) {
                this.components.forEach(components -> components.drawScreen(context, mouseX, mouseY, delta));
            }
        } finally {
            ImmediateDrawUtil.resetColorModulator();
            renderingAlpha = 1.0F;
        }
    }

    //别管他,写accessWidener里了可以过编译=)
    private static void flushDeferredGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.gameRenderer.guiRenderer.render(client.gameRenderer.fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
    }

    private int toContentX(double mouseX, float scale) {
        return Math.round((float) mouseX / scale);
    }

    private int toContentY(double mouseY, float scale, float slideY) {
        return Math.round(((float) mouseY - slideY) / scale);
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        float alpha = (float) ClickGui.getInstance().alphaValue;
        float scale = 0.92f + 0.08f * alpha;
        float slideY = (1.0f - alpha) * 20.0f;
        int x = this.toContentX(click.x(), scale);
        int y = this.toContentY(click.y(), scale, slideY);
        this.components.forEach(components -> components.mouseClicked(x, y, click.button()));
        return super.mouseClicked(click, doubled);
    }

    public boolean mouseReleased(Click click) {
        this.components.forEach(components -> components.mouseReleased((int) click.x(), (int) click.y(), click.button()));
        return super.mouseReleased(click);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (InputUtil.isKeyPressed(Wrapper.mc.getWindow(), 340)) {
            if (verticalAmount < 0.0) {
                this.components.forEach(component -> component.setX(component.getX() - 15));
            } else if (verticalAmount > 0.0) {
                this.components.forEach(component -> component.setX(component.getX() + 15));
            }
        } else if (verticalAmount < 0.0) {
            this.components.forEach(component -> component.setY(component.getY() - 15));
        } else if (verticalAmount > 0.0) {
            this.components.forEach(component -> component.setY(component.getY() + 15));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !BindButton.ANY_LISTENING;
    }

    public boolean keyPressed(KeyInput input) {
        boolean bindListening = BindButton.ANY_LISTENING;
        if (input.isEscape() && bindListening) {
            int key = input.key();
            this.components.forEach(component -> component.onKeyPressed(key));
            return true;
        }

        this.components.forEach(component -> component.onKeyPressed(input.key()));
        return super.keyPressed(input);
    }

    public boolean charTyped(CharInput input) {
        this.components.forEach(component -> component.onKeyTyped((char) input.codepoint(), input.modifiers()));
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public final ArrayList<Component> getComponents() {
        return this.components;
    }

    public int getTextOffset() {
        return -ClickGui.getInstance().textOffset.getValueInt() - 6;
    }
}
