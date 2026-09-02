package fi.rotclient;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.concurrent.ThreadLocalRandom;

/** Standard presentation for local Rot Client notifications. */
final class RotClientChat {
    private RotClientChat() {
    }

    static Component message(String action) {
        boolean addOneLiner = ThreadLocalRandom.current().nextBoolean();
        int index = ThreadLocalRandom.current().nextInt(RotClientChatCopy.oneLinerCount());
        return message(action, addOneLiner, index);
    }

    static Component message(String action, boolean addOneLiner, int oneLinerIndex) {
        MutableComponent result = Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal("Rot").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(RotClientChatCopy.normalizedAction(action))
                        .withStyle(ChatFormatting.WHITE));
        if (addOneLiner) {
            result = result.append(Component.literal("  · "
                            + RotClientChatCopy.oneLiner(oneLinerIndex))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
        return result;
    }
}
