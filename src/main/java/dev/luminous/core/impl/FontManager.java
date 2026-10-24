/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.util.math.MatrixStack
 */
package dev.luminous.core.impl;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.gui.fonts.FontRenderer;
import dev.luminous.mod.modules.impl.client.Fonts;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Objects;
import org.joml.Matrix3x2fStack;

public class FontManager {
    public static FontRenderer ui;
    public static FontRenderer small;
    public static FontRenderer icon;

    public static synchronized void ensureInitialized() {
        if (ui != null && small != null && icon != null) {
            return;
        }
        if (Wrapper.mc == null || Wrapper.mc.getWindow() == null) {
            return;
        }
        init();
    }

    public static synchronized void init() {
        if (ui != null && small != null && icon != null) {
            return;
        }
        try {
            FontRenderer nextUi = FontManager.assets(8.0f, "default", 0);
            FontRenderer nextSmall = FontManager.assets(6.0f, "default", 0);
            FontRenderer nextIcon = FontManager.assetsWithoutOffset(8.0f, "icon", 0);
            ui = nextUi;
            small = nextSmall;
            icon = nextIcon;
        } catch (Exception e) {
            // Keep the GUI usable when bundled font resources are unavailable.
            System.err.println("[Alien] Custom fonts unavailable, using system fallback: " + e.getMessage());
            Font fallback = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
            Font smallFallback = fallback.deriveFont(6.0f);
            Font iconFallback = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
            ui = new FontRenderer(fallback, 8.0f);
            small = new FontRenderer(smallFallback, 6.0f);
            icon = new FontRenderer(iconFallback, 8.0f);
        }
    }

    public static FontRenderer assets(float size, String font, int style, String alternate) throws IOException, FontFormatException {
        ClassLoader classLoader = FontManager.class.getClassLoader();
        InputStream primary = classLoader.getResourceAsStream("assets/alienclient/font/" + font + ".ttf");
        InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/default.ttf");
        InputStream stream = primary != null ? primary : fallback;
        if (stream == null) {
            stream = FontManager.class.getResourceAsStream("/assets/minecraft/font/default.ttf");
        }
        return new FontRenderer(Font.createFont(0, Objects.requireNonNull(stream)).deriveFont(style, size), FontManager.getFont(alternate, style, (int)size), size){

            @Override
            public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
                super.drawString(stack, s, x + (float)Fonts.INSTANCE.translate.getValueInt(), y + (float)Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
            }
        };
    }

    public static FontRenderer assetsWithoutOffset(float size, String name, int style) throws IOException, FontFormatException {
        ClassLoader classLoader = FontManager.class.getClassLoader();
        InputStream primary = classLoader.getResourceAsStream("assets/alienclient/font/" + name + ".ttf");
        InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/default.ttf");
        InputStream stream = primary != null ? primary : fallback;
        if (stream == null) {
            stream = FontManager.class.getResourceAsStream("/assets/minecraft/font/default.ttf");
        }
        return new FontRenderer(Font.createFont(0, Objects.requireNonNull(stream)).deriveFont(style, size), size);
    }

    public static FontRenderer assets(float size, String name, int style) throws IOException, FontFormatException {
        ClassLoader classLoader = FontManager.class.getClassLoader();
        InputStream primary = classLoader.getResourceAsStream("assets/alienclient/font/" + name + ".ttf");
        InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/default.ttf");
        InputStream stream = primary != null ? primary : fallback;
        return new FontRenderer(Font.createFont(0, Objects.requireNonNull(stream)).deriveFont(style, size), size){

            @Override
            public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
                super.drawString(stack, s, x + (float)Fonts.INSTANCE.translate.getValueInt(), y + (float)Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
            }
        };
    }

    public static FontRenderer create(int size, String font, int style, String alternate) {
        return new FontRenderer(FontManager.getFont(font, style, size), FontManager.getFont(alternate, style, size), size){

            @Override
            public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
                super.drawString(stack, s, x + (float)Fonts.INSTANCE.translate.getValueInt(), y + (float)Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
            }
        };
    }

    public static FontRenderer create(int size, String font, int style) {
        return new FontRenderer(FontManager.getFont(font, style, size), size){

            @Override
            public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
                super.drawString(stack, s, x + (float)Fonts.INSTANCE.translate.getValueInt(), y + (float)Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
            }
        };
    }

    private static Font getFont(String font, int style, int size) {
        File fontDir = new File("C:\\Windows\\Fonts");
        try {
            for (File file : fontDir.listFiles()) {
                if (!file.getName().replace(".ttf", "").replace(".ttc", "").replace(".otf", "").equalsIgnoreCase(font)) continue;
                try {
                    return Font.createFont(0, file).deriveFont(style, size);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (File file : fontDir.listFiles()) {
                if (!file.getName().startsWith(font)) continue;
                try {
                    return Font.createFont(0, file).deriveFont(style, size);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return new Font(null, style, size);
    }
}

