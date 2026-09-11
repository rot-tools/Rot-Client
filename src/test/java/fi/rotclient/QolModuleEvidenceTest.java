package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class QolModuleEvidenceTest {
    @Test
    void everyParentHasOneHonestEvidenceState() {
        assertEquals(111, QolUtilityCatalog.modules().size());
        assertEquals(
                111,
                QolUtilityCatalog.modules().stream()
                        .map(QolUtilityCatalog.ModuleDef::evidenceStatus)
                        .filter(status -> status == QolModuleEvidence.Status.NEEDS_TESTING)
                        .count());
    }

    @Test
    void readyRequiresExplicitRuntimeEvidence() {
        assertFalse(QolUtilityCatalog.modules().stream()
                .anyMatch(module ->
                        module.evidenceStatus() == QolModuleEvidence.Status.READY
                                && !QolModuleEvidence.runtimeVerified(module.id())));
        assertEquals(
                QolModuleEvidence.runtimeVerifiedIds(),
                QolUtilityCatalog.modules().stream()
                        .filter(module -> module.evidenceStatus() == QolModuleEvidence.Status.READY)
                        .map(QolUtilityCatalog.ModuleDef::id)
                        .collect(java.util.stream.Collectors.toSet()));
    }
}
