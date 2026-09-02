package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Appends Info / Price tooltip lines to vanilla item tooltips.
 */
public final class SkyBlockTooltipRuntime {
    private SkyBlockTooltipRuntime() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty() || lines == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        List<String> lore = InventoryChromeRuntime.loreLines(stack);
        if (extras.infoTooltipsEnabled) {
            if (extras.infoRevertMasterStars && !lines.isEmpty()) {
                SkyblockFlavorPolicy.revertMasterStars(
                                true,
                                lines.get(0).getString(),
                                SkyBlockItemData.dungeonStars(stack))
                        .ifPresent(text -> lines.set(0, Component.literal(text)));
            }
            for (String line : InfoTooltipsPolicy.lines(
                    true,
                    extras.infoDungeonQuality,
                    extras.infoCreatedDate,
                    extras.infoHexColor,
                    extras.infoMuseum,
                    extras.infoItemId,
                    SkyBlockItemData.infoSnapshot(stack))) {
                lines.add(Component.literal(line));
            }
            String stars = SkyblockFlavorPolicy.starTooltip(
                    extras.infoStarCount, SkyBlockItemData.dungeonStars(stack));
            if (!stars.isBlank()) {
                lines.add(Component.literal(stars));
            }
            String candy = SkyblockFlavorPolicy.petCandyTooltip(
                    extras.infoPetCandy, SkyBlockItemData.petCandyUsed(stack));
            if (!candy.isBlank()) {
                lines.add(Component.literal(candy));
            }
        }
        if (extras.priceTooltipsEnabled) {
            String itemId = SkyBlockItemData.marketId(stack);
            PriceTooltipsPolicy.Quote quote = SkyBlockMarketQuoteService.current().quote(itemId);
            double npc = quote.npcCoins() > 0.0D ? quote.npcCoins() : PriceTooltipsPolicy.npcFromLore(lore);
            double motes = quote.motes() > 0.0D ? quote.motes() : PriceTooltipsPolicy.motesFromLore(lore);
            quote = new PriceTooltipsPolicy.Quote(
                    quote.lowestBin(),
                    quote.bazaarBuy(),
                    quote.bazaarSell(),
                    npc,
                    motes);
            int quantity = PriceTooltipsPolicy.stackQuantity(lore, stack.getCount());
            long paid = extras.pricePaidFor(SkyBlockItemData.uuid(stack));
            for (String line : PriceTooltipsPolicy.lines(
                    true,
                    extras.priceLowestBin,
                    extras.priceBazaar,
                    extras.priceNpc,
                    extras.priceMotes,
                    extras.pricePaid,
                    extras.priceBurgerCount,
                    inRift(),
                    quantity,
                    quote,
                    paid)) {
                lines.add(Component.literal(line));
            }
        }
        ItemToolsRuntime.appendMuseumTooltip(stack, lines);
        ItemToolsRuntime.appendCraftTooltip(stack, lines);
        appendCalendarDates(extras, stack, lore, lines);
    }

    private static void appendCalendarDates(
            QolSkyblockExtras extras,
            ItemStack stack,
            List<String> lore,
            List<Component> lines) {
        List<CalendarDatePolicy.Entry> entries = CalendarDatePolicy.entries(
                extras.calendarDateEnabled,
                screenTitle(),
                stack.getHoverName().getString(),
                lore,
                System.currentTimeMillis());
        String minister = SkyblockFlavorPolicy.ministerTooltip(
                extras.calendarDateEnabled && extras.calendarMinister,
                SkyblockFlavorPolicy.parseMinister(lore).orElse(SkyblockFlavorRuntime.ministerName()));
        if (entries.isEmpty() && minister.isBlank()) {
            return;
        }
        lines.add(Component.empty());
        for (CalendarDatePolicy.Entry entry : entries) {
            lines.add(Component.literal(entry.label() + ": §b" + formatDate(entry.epochMs())));
        }
        if (!minister.isBlank()) {
            lines.add(Component.literal(minister));
        }
    }

    private static String formatDate(long epochMs) {
        ZonedDateTime time = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault());
        String weekday = time.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
        String stamp = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .format(time);
        return weekday + " " + stamp;
    }

    private static String screenTitle() {
        Minecraft client = Minecraft.getInstance();
        if (client == null
                || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> container)) {
            return "";
        }
        return container.getTitle() == null ? "" : container.getTitle().getString();
    }

    public static void rememberPurchase(String title, ItemStack clicked, List<ItemStack> containerStacks) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        if (!extras.priceTooltipsEnabled || !extras.pricePaid) {
            return;
        }
        if (title == null || !title.equals("Confirm Purchase") || clicked == null || clicked.isEmpty()) {
            return;
        }
        String path = itemPath(clicked);
        if (!path.contains("green_terracotta") && !path.contains("lime_terracotta")) {
            return;
        }
        long cost = PriceTooltipsPolicy.purchaseCostFromLore(
                InventoryChromeRuntime.loreLines(clicked)).orElse(0L);
        String uuid = "";
        if (containerStacks != null) {
            for (ItemStack stack : containerStacks) {
                String found = SkyBlockItemData.uuid(stack);
                if (!found.isBlank()) {
                    uuid = found;
                    break;
                }
            }
        }
        if (cost > 0L && !uuid.isBlank()) {
            extras.rememberPricePaid(uuid, cost);
            TrackerStore.save(RotClientClient.trackerConfig());
        }
    }

    private static boolean inRift() {
        return sidebarText().toLowerCase(Locale.ROOT).contains("the rift");
    }

    private static String sidebarText() {
        return SkyBlockSidebar.text();
    }

    private static String itemPath(ItemStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }
}
