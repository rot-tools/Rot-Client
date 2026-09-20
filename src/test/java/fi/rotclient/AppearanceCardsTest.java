package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class AppearanceCardsTest {
    private static AppearanceCards.Layout layout(Map<String, Double> open) {
        return layout(open, List.of(), Set.of());
    }

    private static AppearanceCards.Layout layout(
            Map<String, Double> open, List<String> files, Set<String> confirming) {
        return AppearanceCards.layout(new AppearanceCards.Input(
                id -> open.getOrDefault(id, 0.0D), files, confirming));
    }

    private static Map<String, Double> everythingOpen() {
        java.util.HashMap<String, Double> open = new java.util.HashMap<>();
        for (String id : AppearanceCards.allExpandIds()) {
            open.put(id, 1.0D);
        }
        return open;
    }

    private static long count(AppearanceCards.Layout layout, AppearanceCards.Kind kind) {
        return layout.items().stream().filter(item -> item.kind() == kind).count();
    }

    @Test
    void collapsedPageIsJustTheFiveCardHeaders() {
        AppearanceCards.Layout layout = layout(Map.of());
        assertEquals(5, layout.items().size());
        assertEquals(5, count(layout, AppearanceCards.Kind.CARD));
        assertEquals(
                List.of("dashboard", "colors", "background", "charts", "reset"),
                layout.items().stream().map(AppearanceCards.Item::id).toList());
        assertEquals(5 * (AppearanceCards.CARD_HEIGHT + AppearanceCards.CARD_GAP), layout.height());
    }

    @Test
    void openingACardPutsItsRowsBelowItsHeaderAndPushesTheRestDown() {
        AppearanceCards.Card dashboard = AppearanceCards.card("dashboard");
        AppearanceCards.Layout collapsed = layout(Map.of());
        AppearanceCards.Layout open = layout(Map.of(dashboard.expandId(), 1.0D));

        assertEquals(7, count(open, AppearanceCards.Kind.COLOR));
        int headerBottom = open.items().get(0).y() + AppearanceCards.CARD_HEIGHT;
        assertTrue(open.items().get(1).y() >= headerBottom, "rows start under the header");
        assertTrue(open.height() > collapsed.height());

        AppearanceCards.Item colorsHeaderBefore = collapsed.items().stream()
                .filter(item -> item.id().equals("colors")).findFirst().orElseThrow();
        AppearanceCards.Item colorsHeaderAfter = open.items().stream()
                .filter(item -> item.kind() == AppearanceCards.Kind.CARD && item.id().equals("colors"))
                .findFirst().orElseThrow();
        assertTrue(colorsHeaderAfter.y() > colorsHeaderBefore.y());
    }

    @Test
    void rowsNeverOverlapWhenEverythingIsOpen() {
        AppearanceCards.Layout layout = layout(everythingOpen(), List.of("a.png", "b.png"), Set.of());
        List<AppearanceCards.Item> items = layout.items();
        for (int i = 1; i < items.size(); i++) {
            AppearanceCards.Item previous = items.get(i - 1);
            AppearanceCards.Item next = items.get(i);
            assertTrue(next.y() >= previous.y() + previous.height(),
                    previous.id() + " overlaps " + next.id());
        }
        AppearanceCards.Item last = items.get(items.size() - 1);
        assertTrue(layout.height() >= last.y() + last.height());
        for (AppearanceCards.Item item : items) {
            assertFalse(item.clipped(), item.id() + " should be fully visible once open");
        }
    }

    @Test
    void aClosingCardClipsItsBodyInsteadOfSnappingShut() {
        AppearanceCards.Card colors = AppearanceCards.card("colors");
        AppearanceCards.Layout full = layout(everythingOpen());
        java.util.HashMap<String, Double> half = new java.util.HashMap<>(everythingOpen());
        half.put(colors.expandId(), 0.5D);
        AppearanceCards.Layout mid = layout(half);
        AppearanceCards.Layout shut = layout(Map.of());

        assertTrue(mid.height() < full.height());
        assertTrue(mid.height() > shut.height());

        AppearanceCards.Item header = mid.items().stream()
                .filter(item -> item.kind() == AppearanceCards.Kind.CARD && item.id().equals("colors"))
                .findFirst().orElseThrow();
        int bodyTop = header.y() + AppearanceCards.CARD_HEIGHT;
        AppearanceCards.Item next = mid.items().stream()
                .filter(item -> item.kind() == AppearanceCards.Kind.CARD && item.id().equals("background"))
                .findFirst().orElseThrow();
        // Every row of the half-open body stays above the next card, and rows that would poke past
        // the visible part say so through their clip range.
        for (AppearanceCards.Item item : mid.items()) {
            if (item.y() > header.y() && item.y() < next.y()) {
                assertTrue(item.clipBottom() <= next.y(), item.id() + " leaks into the next card");
                assertTrue(item.clipTop() >= bodyTop, item.id());
            }
        }
        assertTrue(mid.items().stream().anyMatch(AppearanceCards.Item::clipped));
    }

    @Test
    void groupsOpenAndCloseInsideTheColorsCard() {
        AppearanceCards.Card colors = AppearanceCards.card("colors");
        java.util.HashMap<String, Double> open = new java.util.HashMap<>();
        open.put(colors.expandId(), 1.0D);
        // Only the card is open; its four titled groups are closed.
        AppearanceCards.Layout closedGroups = layout(open);
        assertEquals(4, count(closedGroups, AppearanceCards.Kind.GROUP));
        assertEquals(0, count(closedGroups, AppearanceCards.Kind.COLOR));

        for (AppearanceCards.Group group : colors.groups()) {
            open.put(group.id(), 1.0D);
        }
        AppearanceCards.Layout openGroups = layout(open);
        assertEquals(24, count(openGroups, AppearanceCards.Kind.COLOR));
        assertTrue(openGroups.height() > closedGroups.height());
    }

    @Test
    void clicksResolveToTheRowThatWasDrawn() {
        AppearanceCards.Card dashboard = AppearanceCards.card("dashboard");
        AppearanceCards.Layout layout = layout(Map.of(dashboard.expandId(), 1.0D));

        AppearanceCards.Item firstColor = layout.items().stream()
                .filter(item -> item.kind() == AppearanceCards.Kind.COLOR).findFirst().orElseThrow();
        AppearanceCards.Item hit = layout.itemAt(firstColor.y() + 3);
        assertNotNull(hit);
        assertEquals(firstColor.id(), hit.id());
        assertEquals("dashboardBackdrop", hit.id());

        // The gap between two rows belongs to nothing.
        assertNull(layout.itemAt(firstColor.y() + AppearanceCards.COLOR_HEIGHT + 1));
        assertNull(layout.itemAt(-5));
    }

    @Test
    void aRowThatIsClippedAwayIsNotClickable() {
        AppearanceCards.Card dashboard = AppearanceCards.card("dashboard");
        AppearanceCards.Layout layout = layout(Map.of(dashboard.expandId(), 0.3D));
        for (AppearanceCards.Item item : layout.items()) {
            if (item.kind() == AppearanceCards.Kind.COLOR) {
                assertTrue(item.visible());
                int belowClip = item.clipBottom();
                AppearanceCards.Item at = layout.itemAt(belowClip);
                assertTrue(at == null || !at.id().equals(item.id()));
            }
        }
    }

    @Test
    void buttonsAreFoundByHorizontalPosition() {
        AppearanceCards.Card background = AppearanceCards.card("background");
        AppearanceCards.Layout layout = layout(
                Map.of(background.expandId(), 1.0D), List.of("wall.png"), Set.of());
        AppearanceCards.Item row = layout.items().stream()
                .filter(item -> item.id().equals("background.amounts")).findFirst().orElseThrow();
        int bodyLeft = 100;
        assertEquals(AppearanceCards.BG_OPACITY_DOWN, row.buttonAt(bodyLeft + 5, bodyLeft).id());
        assertEquals(AppearanceCards.BG_OPACITY_UP, row.buttonAt(bodyLeft + 125, bodyLeft).id());
        assertEquals(AppearanceCards.BG_DIM_UP, row.buttonAt(bodyLeft + 340, bodyLeft).id());
        assertNull(row.buttonAt(bodyLeft - 1, bodyLeft));
        assertNull(row.buttonAt(bodyLeft + 500, bodyLeft));
        assertEquals(1, count(layout, AppearanceCards.Kind.FILE));
    }

    @Test
    void backgroundCardShowsAnEmptyNoticeWhenThereAreNoImages() {
        AppearanceCards.Card background = AppearanceCards.card("background");
        AppearanceCards.Layout none = layout(Map.of(background.expandId(), 1.0D));
        assertTrue(none.items().stream().anyMatch(item -> item.id().equals(AppearanceCards.BG_EMPTY)));
        assertEquals(0, count(none, AppearanceCards.Kind.FILE));
    }

    @Test
    void everyEditableCardCanUndoAndReset() {
        AppearanceCards.Layout layout = layout(everythingOpen(), List.of("a.png"), Set.of());
        for (AppearanceCards.Card card : AppearanceCards.cards()) {
            AppearanceCards.Item row = layout.items().stream()
                    .filter(item -> item.id().equals("card." + card.id())).findFirst().orElse(null);
            if (card.isReset()) {
                assertNull(row, "the Reset card has its own buttons");
                continue;
            }
            assertNotNull(row, card.id());
            assertEquals(
                    List.of(AppearanceCards.CARD_UNDO, AppearanceCards.CARD_RESET),
                    row.buttons().stream().map(AppearanceCards.Button::id).toList());
        }
    }

    @Test
    void destructiveResetsNeedASecondClick() {
        AppearanceCards.Card reset = AppearanceCards.card("reset");
        Map<String, Double> open = Map.of(reset.expandId(), 1.0D);

        AppearanceCards.Layout idle = layout(open);
        assertTrue(labels(idle).contains("Reset all appearance"));
        assertFalse(labels(idle).contains("Confirm reset all appearance"));

        AppearanceCards.Layout armed = layout(open, List.of(), Set.of(AppearanceCards.RESET_ALL));
        assertTrue(labels(armed).contains("Confirm reset all appearance"));
        assertTrue(armed.items().stream().anyMatch(item -> item.id().equals("reset.all.warning")));
        // Only the armed button changes.
        assertTrue(labels(armed).contains("Reset UI Positions"));
        assertTrue(labels(armed).contains("Reset HUD Visibility"));
    }

    private static List<String> labels(AppearanceCards.Layout layout) {
        return layout.items().stream()
                .flatMap(item -> item.buttons().stream())
                .map(AppearanceCards.Button::label)
                .toList();
    }

    @Test
    void everyEditedFieldExistsInTheConfigAndNoColorIsMissing() throws Exception {
        List<String> fields = AppearanceCards.allFields();
        assertEquals(fields.size(), new HashSet<>(fields).size(), "a field is listed twice");

        Set<String> configInts = new HashSet<>();
        for (Field field : RotClientAppearanceConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() == int.class
                    && !field.getName().equals("schemaVersion")
                    && !field.getName().startsWith("customBackground")) {
                configInts.add(field.getName());
            }
        }
        for (AppearanceCards.Card card : AppearanceCards.cards()) {
            for (AppearanceCards.Group group : card.groups()) {
                for (AppearanceCards.ColorField color : group.colors()) {
                    Field field = RotClientAppearanceConfig.class.getDeclaredField(color.field());
                    assertEquals(int.class, field.getType(), color.field());
                    assertFalse(color.label().isBlank());
                }
            }
        }
        for (String name : AppearanceCards.card("background").fields()) {
            assertNotNull(RotClientAppearanceConfig.class.getDeclaredField(name), name);
        }
        Set<String> listed = new HashSet<>(fields);
        for (String color : configInts) {
            assertTrue(listed.contains(color), color + " is a color the Appearance page cannot edit");
        }
    }

    @Test
    void geometryMatchesTheControlsThatDrawIt() {
        assertEquals(RotClientColorRowLayout.ROW_HEIGHT, AppearanceCards.COLOR_HEIGHT);
        assertEquals(RotClientColorRowLayout.ROW_STEP, AppearanceCards.COLOR_STEP);
        assertEquals(RotClientUiDraw.BUTTON_HEIGHT, AppearanceCards.BUTTON_HEIGHT);
    }

    @Test
    void cardsAreTheLandingPolicyCardsInTheSameOrder() {
        List<String> landing = AppearanceLandingPolicy.cards().stream()
                .map(card -> AppearanceLandingPolicy.sectionId(card.actionId())).toList();
        assertEquals(landing, AppearanceCards.cards().stream().map(AppearanceCards.Card::id).toList());
        for (AppearanceCards.Card card : AppearanceCards.cards()) {
            assertFalse(card.title().isBlank());
            assertFalse(card.subtitle().isBlank());
        }
    }

    @Test
    void cardOpenStateIsPersistedAndCollapsedByDefault() {
        for (AppearanceCards.Card card : AppearanceCards.cards()) {
            assertTrue(RotClientAppearanceNav.isCardId(card.expandId()));
            assertEquals(card.expandId(), RotClientAppearanceNav.normalizeSectionId(card.expandId()));
            assertFalse(RotClientAppearanceNav.defaultExpandedSections().contains(card.expandId()));
        }
        assertNull(RotClientAppearanceNav.normalizeSectionId(RotClientAppearanceNav.CARD_PREFIX + "nope"));
        List<String> toggled = RotClientAppearanceNav.toggleSection(
                RotClientAppearanceNav.defaultExpandedSections(),
                AppearanceCards.card("charts").expandId());
        assertTrue(RotClientAppearanceNav.isExpanded(toggled, AppearanceCards.card("charts").expandId()));
        // Opening a card leaves the color groups' own state alone.
        assertTrue(RotClientAppearanceNav.isExpanded(toggled, RotClientAppearanceNav.HUD_BASICS));
    }

    @Test
    void everyTitledGroupHasAnAnimatableExpandId() {
        List<String> ids = AppearanceCards.allExpandIds();
        for (AppearanceCards.Card card : AppearanceCards.cards()) {
            assertTrue(ids.contains(card.expandId()));
            for (AppearanceCards.Group group : card.groups()) {
                if (!group.title().isBlank()) {
                    assertTrue(ids.contains(group.id()), group.id());
                    assertNotNull(RotClientAppearanceNav.normalizeSectionId(group.id()), group.id());
                }
            }
        }
    }
}
