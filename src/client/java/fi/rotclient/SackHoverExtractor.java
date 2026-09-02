package fi.rotclient;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-owned extraction of Sack change hover texts from a system-chat
 * {@link Component} tree. Kept separate from {@link RotClientClient} so unit
 * tests can exercise the real ingress seam without Fabric mixins.
 */
final class SackHoverExtractor {
    private SackHoverExtractor() {
    }

    static List<String> changeHoverTexts(Component component) {
        List<String> texts = new ArrayList<>();
        collect(component, texts);
        return List.copyOf(texts);
    }

    private static void collect(Component component, List<String> texts) {
        if (component == null) {
            return;
        }
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            String text = showText.value().getString();
            if (text != null) {
                String trimmed = text.trim();
                boolean sackChange = trimmed.regionMatches(true, 0, "Added", 0, 5)
                        || trimmed.regionMatches(true, 0, "Removed", 0, 7);
                if (sackChange && !texts.contains(trimmed)) {
                    texts.add(trimmed);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collect(sibling, texts);
        }
    }
}
