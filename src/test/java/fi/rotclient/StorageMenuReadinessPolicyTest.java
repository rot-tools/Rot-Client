package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageMenuReadinessPolicyTest {
    @Test
    void fullSnapshotConfirmsEvenAnEmptyPageWithoutExaminingItemIdentity() {
        var state = new StorageMenuReadinessPolicy();
        state.beginVisit(4, 90);
        assertFalse(state.ready());
        assertTrue(state.acceptContents(4, 90));
        assertTrue(state.ready());
    }

    @Test
    void rejectsForeignPartialAndPlayerInventorySnapshots() {
        var state = new StorageMenuReadinessPolicy();
        assertFalse(state.acceptContents(-1, 0));
        state.beginVisit(4, 90);
        assertFalse(state.acceptContents(3, 90));
        assertFalse(state.acceptContents(4, 89));
        assertFalse(state.acceptContents(4, 91));
        assertFalse(state.ready());
        state.beginVisit(0, 36);
        assertFalse(state.acceptContents(0, 36));
    }

    @Test
    void reopeningSameIdAndDisconnectBothRequireNewSnapshot() {
        var state = new StorageMenuReadinessPolicy();
        state.beginVisit(4, 90);
        assertTrue(state.acceptContents(4, 90));
        state.beginVisit(4, 90);
        assertFalse(state.ready());
        assertTrue(state.acceptContents(4, 90));
        state.reset();
        assertFalse(state.ready());
        assertFalse(state.acceptContents(4, 90));
    }
}
