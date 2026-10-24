package dev.luminous.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static dev.luminous.api.utils.Wrapper.mc;

public class PeekScreen extends ShulkerBoxScreen {
    private final Identifier TEXTURE = Identifier.of("textures/gui/container/shulker_box.png");
    private final ItemStack[] contents;
    private final ItemStack storageBlock;

    public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
        super(new ShulkerBoxScreenHandler(0, mc.player.getInventory(), new SimpleInventory(contents)), mc.player.getInventory(), storageBlock.getName());
        this.contents = contents;
        this.storageBlock = storageBlock;
    }

    public static Color getShulkerColor(ItemStack shulkerItem) {
        if (shulkerItem.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ShulkerBoxBlock shulkerBlock) {
                DyeColor dye = shulkerBlock.getColor();
                if (dye == null) return Color.WHITE;
                final int color = dye.getEntityColor();
                return new Color(color);
            }
        }
        return Color.WHITE;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2 && this.focusedSlot != null && !this.focusedSlot.getStack().isEmpty() && Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            ItemStack itemStack = this.focusedSlot.getStack();
            if (ShulkerViewer.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
                return ShulkerViewer.openContainer(this.focusedSlot.getStack(), this.contents, false);
            }
            if (itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null || itemStack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null) {
                this.close();
                Wrapper.mc.setScreen((Screen) new BookScreen(BookScreen.Contents.create(itemStack)));
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || Wrapper.mc.options.inventoryKey.matchesKey(new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers))) {
            this.close();
            return true;
        }
        return false;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return false;
    }

    //Fuck you scale problem
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        if (ClientSetting.INSTANCE.darkening.getValue()) {
            context.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, 0x80101010);
        }
        Color color = PeekScreen.getShulkerColor(this.storageBlock);
        int tint = ColorHelper.getArgb(255, color.getRed(), color.getGreen(), color.getBlue());

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                i,
                j,
                0.0f,
                0.0f,
                backgroundWidth,
                backgroundHeight,
                backgroundWidth,
                backgroundHeight,
                256,
                256,
                tint
        );
    }
}
