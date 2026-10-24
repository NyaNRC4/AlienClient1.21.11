package dev.luminous.api.utils.render;

import dev.luminous.api.interfaces.IChatHudLineHook;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.OrderedText;
import org.joml.Matrix3x2f;

/**
 * 我滑不动了，交给ai吧
 */
public final class ChatHudUtil implements ChatHud.Backend {
    private final ChatHud.Backend delegate;
    private final Function<OrderedText, IChatHudLineHook> finder;

    public ChatHudUtil(ChatHud.Backend delegate, Function<OrderedText, IChatHudLineHook> finder) {
        this.delegate = delegate;
        this.finder = finder;
    }

    @Override
    public void updatePose(Consumer<Matrix3x2f> transformer) {
        this.delegate.updatePose(transformer);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.delegate.fill(x1, y1, x2, y2, color);
    }

    @Override
    public boolean text(int y, float opacity, OrderedText text) {
        IChatHudLineHook line = this.finder.apply(text);
        if (line == null) {
            return this.delegate.text(y, opacity, text);
        }

        FadeUtils fadeUtils = line.alienClient$getFade();
        double ease = fadeUtils == null ? 1.0 : fadeUtils.ease(ClientSetting.INSTANCE.ease.getValue());
        double fade = 1.0 - ease;

        if (line.alienClient$getSync() && ClientSetting.INSTANCE.isOn()) {
            text = recolor(text, ClientSetting.INSTANCE.color.getValue().getRGB());
        }

        int offset = (int) (fade * ClientSetting.INSTANCE.animateOffset.getValue());
        if (offset != 0) {
            this.delegate.updatePose(m -> m.translate(offset, 0.0f));
        }

        float newOpacity = opacity;
        if (ClientSetting.INSTANCE.fade.getValue()) {
            newOpacity = (float) (opacity * Math.max(0.04, ease));
        }

        boolean result = this.delegate.text(y, newOpacity, text);
        if (offset != 0) {
            this.delegate.updatePose(m -> m.translate(-offset, 0.0f));
        }
        return result;
    }

    /**
     * 重包 OrderedText，把“没有显式颜色”的字形设为主题色（作为兜底色）。
     *
     * <p>与原 1.21.1 中 {@code drawTextWithShadow(..., color)} 的语义一致：
     * 传入的 color 只是兜底色，字形 Style 里已有的颜色（来自 {@code §} 颜色字符）
     * 仍优先，因此文本内的原版颜色代码可以继续修改后续文本。
     */
    private static OrderedText recolor(OrderedText text, int color) {
        return visitor -> text.accept((index, style, codePoint) -> {
            if (style.getColor() != null) {
                return visitor.accept(index, style, codePoint);
            }
            return visitor.accept(index, style.withColor(color), codePoint);
        });
    }

    @Override
    public void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator) {
        if (ClientSetting.INSTANCE.hideIndicator.getValue()) {
            return;
        }
        this.delegate.indicator(x1, y1, x2, y2, opacity, indicator);
    }

    @Override
    public void indicatorIcon(int left, int bottom, boolean forceDraw, MessageIndicator indicator, MessageIndicator.Icon icon) {
        if (ClientSetting.INSTANCE.hideIndicator.getValue()) {
            return;
        }
        this.delegate.indicatorIcon(left, bottom, forceDraw, indicator, icon);
    }
}
