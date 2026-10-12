package fi.rotclient;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class QolDashboardSearchPolicyTest {
    private static QolUtilityCatalog.SettingDef row(String id,String label,QolUtilityCatalog.SettingType type) {
        return new QolUtilityCatalog.SettingDef(id,label,"",type,List.of(),List.of());
    }
    @Test void searchKeepsOnlyRelevantSectionsWithTheirChildrenInOrder() {
        var rows = List.of(row("s1","Panel colors",QolUtilityCatalog.SettingType.SECTION),
                row("one","Background",QolUtilityCatalog.SettingType.COLOR),
                row("two","Outline",QolUtilityCatalog.SettingType.COLOR),
                row("s2","Timers",QolUtilityCatalog.SettingType.SECTION),
                row("three","Cooldown",QolUtilityCatalog.SettingType.NUMBER));
        assertEquals(List.of(rows.get(0),rows.get(1)),QolDrawerSearchPolicy.filter(rows,"panel background"));
        assertEquals(List.of(rows.get(3),rows.get(4)),QolDrawerSearchPolicy.filter(rows,"cooldown"));
        assertEquals(rows,QolDrawerSearchPolicy.filter(rows,"  ")); assertTrue(QolDrawerSearchPolicy.filter(rows,"missing").isEmpty());
    }
    @Test void aliasesAndOptionLabelsAreSearchableWithoutMutatingTheSource() {
        var row = new QolUtilityCatalog.SettingDef("qol.hud.map_mode","Mode","",QolUtilityCatalog.SettingType.ENUM,
                List.of("Explored","Reveal Hidden"),List.of("dungeon map"));
        var rows = List.of(row); assertEquals(rows,QolDrawerSearchPolicy.filter(rows,"DUNGEON hidden"));
        assertEquals(rows,QolDrawerSearchPolicy.filter(rows,"hud map")); assertEquals(2,row.enumOptions().size());
    }
    @Test void sectionAliasesAndNormalizedWordsFindChildrenWithoutChangingTheirIds() {
        var section = new QolUtilityCatalog.SettingDef("display-group", "Display", "", QolUtilityCatalog.SettingType.SECTION,
                List.of(), List.of("first-person view"));
        var setting = row("unchanged_action_id", "Reset offsets", QolUtilityCatalog.SettingType.ACTION);
        var rows = List.of(section, setting);
        assertEquals(rows, QolDrawerSearchPolicy.filter(rows, "ＦＩＲＳＴ person reset"));
        assertEquals(rows, QolDrawerSearchPolicy.filter(rows, "display-group offsets"));
        assertEquals("unchanged_action_id", QolDrawerSearchPolicy.filter(rows, "reset").get(1).id());
    }
    @Test void longGroupedListsKeepLastSettingsReachableBySearch() {
        var rows = new ArrayList<QolUtilityCatalog.SettingDef>();
        for (int group = 0; group < 8; group++) {
            rows.add(row("group_" + group, "Group " + group, QolUtilityCatalog.SettingType.SECTION));
            for (int index = 0; index < 10; index++) {
                rows.add(row("setting_" + group + "_" + index, "Offset " + index, QolUtilityCatalog.SettingType.NUMBER));
            }
        }
        var hits = QolDrawerSearchPolicy.filter(rows, "group 7 offset 9");
        assertEquals(2, hits.size());
        assertEquals("group_7", hits.get(0).id());
        assertEquals("setting_7_9", hits.get(1).id());
        assertEquals(88, rows.size());
    }
    @Test void oldDisplayGroupLinksResolveToOnePageWithoutChangingCatalogIds() {
        for (var group : List.of(QolUtilityCatalog.Group.GUI,QolUtilityCatalog.Group.RENDER,
                QolUtilityCatalog.Group.INTERFACE,QolUtilityCatalog.Group.HUD_DISPLAY))
            assertEquals(QolUtilityCatalog.Group.GUI,QolDashboardNavigationPolicy.page(group));
        assertEquals(QolUtilityCatalog.Group.MINING,QolDashboardNavigationPolicy.page(QolUtilityCatalog.Group.MINING));
        var modules = QolDashboardNavigationPolicy.modules(QolUtilityCatalog.Group.GUI);
        assertEquals(modules.size(),modules.stream().map(QolUtilityCatalog.ModuleDef::id).distinct().count());
        for (var m : modules) assertEquals(m,QolUtilityCatalog.findById(m.id()));
        assertEquals(QolDashboardNavigationPolicy.pages().size(),QolDashboardNavigationPolicy.pages().stream().distinct().count());
    }
}
