package fi.rotclient.rebuild.illegal;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;

final class AutoHealingPolicy {
    static final long HEALTH_SAMPLE_MAX_AGE_NANOS = 2_000_000_000L;
    private static final String NUMBER = "([0-9][0-9,]*(?:\\.[0-9]+)?)";
    private static final String RATIO = NUMBER + "\\s*/\\s*" + NUMBER;
    private static final String HEART = "[❤♥\uE010]\uFE0F?";
    private static final Pattern HEALTH = Pattern.compile(
            "(?<![\\p{L}\\d.,+/])" + RATIO + "\\s*" + HEART
                    + "|(?:" + HEART + "|\\b(?:Health|HP)\\s*:)\\s*" + RATIO, Pattern.CASE_INSENSITIVE);
    private static final Pattern COOLDOWN = Pattern.compile(
            "^Cooldown:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:s|seconds?)\\s*$", Pattern.CASE_INSENSITIVE);

    private AutoHealingPolicy() {}

    static Optional<Health> readHealth(String text) {
        var match = HEALTH.matcher(StringUtil.stripColor(text).replace('\u00a0', ' ').replace('\u202f', ' '));
        if (!match.find()) return Optional.empty();
        int group = match.group(1) == null ? 3 : 1;
        double current = number(match.group(group));
        double maximum = number(match.group(group + 1));
        if (!Double.isFinite(current) || !Double.isFinite(maximum) || maximum <= 0) return Optional.empty();
        return Optional.of(new Health(current, maximum));
    }

    static boolean shouldHeal(Health health, int threshold, long ageNanos) {
        return ageNanos >= 0 && ageNanos <= HEALTH_SAMPLE_MAX_AGE_NANOS
                && health.current() > 0 && health.current() < health.maximum()
                && health.current() / health.maximum() <= threshold / 100.0;
    }

    static long cooldownNanos(int minimumSeconds, List<String> lore) {
        long minimum = Math.clamp(minimumSeconds, 1, 20) * 1_000_000_000L;
        for (String line : lore) {
            var cooldown = COOLDOWN.matcher(StringUtil.stripColor(line).strip());
            if (cooldown.matches()) {
                double seconds = number(cooldown.group(1));
                if (Double.isFinite(seconds) && seconds >= 0 && seconds <= 86_400) {
                    minimum = Math.max(minimum, (long) Math.ceil(seconds * 1_000_000_000L));
                }
            }
        }
        return minimum;
    }

    static boolean matchesHealingItem(ItemStack stack, AutoHealingSettings.Item item) {
        if (stack.isEmpty()) return false;
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return false;
        var tag = custom.copyTag();
        String id = tag.getString("id").filter(value -> !value.isBlank())
                .or(() -> tag.getCompound("ExtraAttributes").flatMap(extra -> extra.getString("id"))).orElse("");
        return item.name().equals(id);
    }

    static int findReadySlot(int selected, int hotbarSize, IntPredicate ready) {
        if (ready.test(selected)) return selected;
        for (int slot = 0; slot < hotbarSize; slot++) {
            if (slot != selected && ready.test(slot)) return slot;
        }
        return -1;
    }

    static void useFromSlot(int healingSlot, IntSupplier currentSlot, IntConsumer selectSlot, Runnable use) {
        int original = currentSlot.getAsInt();
        if (healingSlot != original) selectSlot.accept(healingSlot);
        try {
            use.run();
        } finally {
            if (healingSlot != original && currentSlot.getAsInt() == healingSlot) selectSlot.accept(original);
        }
    }

    private static double number(String text) {
        try {
            return Double.parseDouble(text.replace(",", ""));
        } catch (NumberFormatException invalid) {
            return Double.NaN;
        }
    }

    record Health(double current, double maximum) {}
}
