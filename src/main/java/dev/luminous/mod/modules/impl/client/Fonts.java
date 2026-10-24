package dev.luminous.mod.modules.impl.client;

import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class Fonts extends Module {
    public static Fonts INSTANCE;

    public final StringSetting font = add(new StringSetting("Font", "default"));
    public final StringSetting alternate = add(new StringSetting("Alternate", "msyh"));
    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.PLAIN));
    public final SliderSetting size = add(new SliderSetting("Size", 8, 1, 15, 1));
    public final SliderSetting shift = add(new SliderSetting("Shift", 0, -10, 10, 1));
    public final SliderSetting translate = add(new SliderSetting("Translate", 0, -10, 10, 1));

    public Fonts() {
        super("Fonts", Category.Client);
        setChinese("字体");
        INSTANCE = this;
    }

    @Override
    public void enable() {
        refresh();
    }

    public void refresh() {
        try {
            if (font.getValue().equals("default")) {
                if (alternate.getValue().equals("null")) {
                    FontManager.ui = FontManager.assets(size.getValueInt(), font.getValue(), style.getValue().get());
                    FontManager.small = FontManager.assets(6, font.getValue(), style.getValue().get());
                } else {
                    FontManager.ui = FontManager.assets(size.getValueInt(), font.getValue(), style.getValue().get(), alternate.getValue());
                    FontManager.small = FontManager.assets(6, font.getValue(), style.getValue().get(), alternate.getValue());
                }
            } else {
                if (alternate.getValue().equals("null")) {
                    FontManager.ui = FontManager.create(size.getValueInt(), font.getValue(), style.getValue().get());
                    FontManager.small = FontManager.create(6, font.getValue(), style.getValue().get());
                } else {
                    FontManager.ui = FontManager.create(size.getValueInt(), font.getValue(), style.getValue().get(), alternate.getValue());
                    FontManager.small = FontManager.create(6, font.getValue(), style.getValue().get(), alternate.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum Style {
        PLAIN {
            @Override
            public int get() {
                return 0;
            }
        },
        BOLD {
            @Override
            public int get() {
                return 1;
            }
        },
        ITALIC {
            @Override
            public int get() {
                return 2;
            }
        };

        public abstract int get();
    }
}
