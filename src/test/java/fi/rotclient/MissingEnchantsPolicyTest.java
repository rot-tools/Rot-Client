package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissingEnchantsPolicyTest {
    @Test
    void tooltipPlanSummarizesMissingAndUpgradableCounts() {
        var plan = new MissingEnchantsPolicy.TooltipPlan(
                List.of("Sharpness", "Critical"), List.of("Looting IV → V"));

        assertEquals("2 missing · 1 upgradable", plan.summary());
    }
}
