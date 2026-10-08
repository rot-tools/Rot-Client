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
